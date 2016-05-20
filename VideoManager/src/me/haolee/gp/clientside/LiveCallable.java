package me.haolee.gp.clientside;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.JOptionPane;
import me.haolee.gp.common.CommandWord;
import me.haolee.gp.common.Config;
import me.haolee.gp.common.Packet;
import me.haolee.gp.common.VideoInfo;

public class LiveCallable implements Callable<Integer> {
	
	/*
	 * 这些变量都要接收上级函数传值，而call方法本身无法接收参数
	 * ，所以在此定义成员变量，使用构造方法传值。
	 * */
	private String serverIP = null;
	private int serverPort = -1;
	/*被选择的块，由静态全局方法和变量得到*/
	private VideoPanel selectedVideoPanel = null;
	
	/*播放视频用，具体的视频信息可以通过取读SelectBlock全局类来得到*/
	public LiveCallable(VideoPanel selectedVideoPanel) {
		this.serverIP = Config.getValue("serverIP", "127.0.0.1");
		this.serverPort = Integer.valueOf(Config.getValue("serverPort", "10000"));
		this.selectedVideoPanel = selectedVideoPanel;
	}

	@Override
	public Integer call() throws Exception {
		/*套接字和原生输入输出流*/
		Socket socketToServer = null;
		InputStream inputStream = null;
		OutputStream outputStream =null;
		/*包装后的输入输出流*/
		ObjectOutputStream objectOutputStream = null;
		ObjectInputStream objectInputStream = null;
		
		VideoInfo videoInfo = null;//获取本显示块内的视频信息数据结构
		String fileRelativePath = null;//完成的相对路径（包括文件名）
		
		try {
			//点播不需要建立连接
			// 客户端暂时不用设置SO_REUSEADDR
			socketToServer = new Socket(serverIP, serverPort);
			// 打开输入输出流
			outputStream = socketToServer.getOutputStream();
			objectOutputStream = new ObjectOutputStream(outputStream);
			inputStream = socketToServer.getInputStream();
			objectInputStream = new ObjectInputStream(inputStream);
			
			/*因为视频列表刷新时，已经用mode和category进行了过滤，
			 * 所以看到的都是符合要求的，所以播放视频时只需要获取哪个显示块被选中了，
			 * 然后找到所对应视频的路径+文件名即可
			 * */
			/*只需告诉服务端请求码+(视频相对路径+视频文件名)*/
			videoInfo = selectedVideoPanel.getVideoInfo();//获取本显示块内的视频信息数据结构
			
			fileRelativePath = videoInfo.getFileRelativePath();//相对路径
			
			/*请求格式：req|fileRelativePath，
			 * 服务端会再加上前缀拼凑出文件绝对路径送给ffmpeg
			 * 客户端只需要知道流的名字即可*/
			Packet sendPacket = new Packet(CommandWord.REQUEST_STREAMINGMEDIA,fileRelativePath);
			objectOutputStream.writeObject(sendPacket);
			
			//服务端开启流化进程后会发送一个数据包告知，这里为了获取这个通知
			Packet recvPacket = null;
			recvPacket = (Packet)objectInputStream.readObject();
			/*
			 * 如果服务端的FFmpeg没有正常开启，可能遇到了错误
			 * 后面就不用费劲了，直接结束吧，省得浪费资源。
			 * */
			if(recvPacket.getCommandWord() == CommandWord.RESPONSE_ABORT){
				System.out.println("服务端可能播放失败");
				JOptionPane.showMessageDialog(null, "服务端视频播放失败", "错误", JOptionPane.ERROR_MESSAGE);
				return null;
			}
			//现在可以开启播放线程播放视频了
			String fileName = new File(fileRelativePath).getName();
			int dot = fileName.lastIndexOf(".");
			String fileID = fileName.substring(0, dot);
			//String rtspURL = "rtsp://"+serverIP+"/live/"+streamID;
			String rtspURL = "rtsp://"+serverIP+"/"+fileID+".sdp";
			Callable<Integer> callable = new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					FFplay ffplay = new FFplay(rtspURL);
					ffplay.play();
					return null;
				}
			};
			Future<Integer> ffplayFuture = Executors.newSingleThreadExecutor().
					submit(callable);
			/*
			 * 播放器线程已经启动，现在本线程进入心跳包应答模式
			 * */
			while(true){
				//接收服务端的包
				recvPacket = (Packet)objectInputStream.readObject();
				//如果服务端发来心跳包
				if(recvPacket.getCommandWord() == CommandWord.CTRL_HARTBEAT){
					//看看FFPLAY是否活着，如果活着就回复心跳包
					if(!ffplayFuture.isDone()){//ffplay活着
						sendPacket = new Packet(CommandWord.CTRL_HARTBEAT,null);
						objectOutputStream.writeObject(sendPacket);
					}else{//如果FFplay死了，就回复END包，同时自己页退出
						sendPacket = new Packet(CommandWord.CTRL_END,null);
						objectOutputStream.writeObject(sendPacket);
						break;//退出
					}
				}else{//收到CommandWord.CTRL_END，说明服务端已经结束
					break;//退出
				}
			}
			/*
			 * 注意，上面的循环正常结束的原因就是服务器不再发送数据，可能是视频传输完毕，
			 * 也可能服务器因为未知原因异常终止，但是这种情况基本不会出现。
			 */
			/*
			 * 如果视频放完了，上面的循环会跳出，但是ffplay不能关闭，要等用户自己关闭
			 * 此处就是等待用户关闭FFplay，然后播放线程FFplayCallable就会退出
			 * 那么ffplayFuture.get()就会结束等待并返回，那么本线程就可以退出了。
			 * 如果本线程不管这些等视频放完了就结束，那么上面的线程池也会销毁，估计播放线程也要挂
			 * */
			/*
			 * 线程call函数的返回值对我们没用，
			 * 这里只是为了等待播放线程FFplayCallable因用户关闭播放窗口而死亡
			 * */
			ffplayFuture.get();
			
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "无法连接服务器", "错误", JOptionPane.ERROR_MESSAGE);
			System.out.println("无法连接服务器"+serverIP+serverPort);
		} finally {
			try {
				if(objectInputStream!=null)objectInputStream.close();
				if(objectOutputStream!=null)objectOutputStream.close();
				if (socketToServer != null)socketToServer.close();
				System.out.println("All Has been closed! in GUICallable finally block");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}// function call
}
