package ClientSide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.JOptionPane;
import CommonPackage.VideoInfo;

public class LiveCallable implements Callable<Integer> {
	
	/*
	 * 这些变量都要接收上级函数传值，而call方法本身无法接收参数
	 * ，所以在此定义成员变量，使用构造方法传值。
	 * */
	private String serverIP = null;
	private int serverPort = -1;
	
	/*播放视频用，具体的视频信息可以通过取读SelectBlock全局类来得到*/
	public LiveCallable(String serverIP, int serverPort) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
	}

	@Override
	public Integer call() throws Exception {
		/*套接字和原生输入输出流*/
		Socket socketToServer = null;
		InputStream inputStream = null;
		OutputStream outputStream =null;
		/*包装后的输入输出流*/
		BufferedReader readFromServer = null;
		PrintWriter printToServer = null;
		/*线程对象*/
		FFplayCallable ffplayCallable = null;
		Future<Integer> ffplayFuture = null;
		
		VideoInfo videoInfo = null;//获取本显示块内的视频信息数据结构
		String fileID = null;//文件名
		String extension = null;//扩展名
		String location = null;//相对纯路径（不包括文件名）
		String fileRelativePath = null;//完成的相对路径（包括文件名）
		
		try {
			//点播不需要建立连接
			// 客户端暂时不用设置SO_REUSEADDR
			socketToServer = new Socket(serverIP, serverPort);
			// 打开输入输出流
			inputStream = socketToServer.getInputStream();
			outputStream = socketToServer.getOutputStream();
			// auto flush
			printToServer = new PrintWriter(outputStream, true);
			/*
			 * 根据请求的功能类型，发送的信息域数量也不同
			 * */
			/*接受到的服务端应答信息*/
			String response = null;
			/*要发送的请求*/
			String request = null;
			/*被选择的块，由静态全局方法和变量得到*/
			DisplayBlock selectedVideoBlock = null;
			/*因为视频列表刷新时，已经用mode和category进行了过滤，
			 * 所以看到的都是符合要求的，所以播放视频时只需要获取哪个显示块被选中了，
			 * 然后找到所对应视频的路径+文件名即可
			 * */
			/*只需告诉服务端请求码+(视频相对路径+视频文件名)*/
			selectedVideoBlock = SelectBlock.getSelectedBlock();//获得被选视频块
			
			videoInfo = selectedVideoBlock.getVideoInfo();//获取本显示块内的视频信息数据结构
			fileID = videoInfo.getFileID();//文件ID
			extension = videoInfo.getExtension();//扩展名
			location = videoInfo.getLocation();//文件位置
			fileRelativePath = location+fileID+"."+extension;//拼凑相对路径
			
			/*请求格式：req|fileRelativePath，
			 * 服务端会再加上前缀拼凑出文件绝对路径送给ffmpeg
			 * 客户端只需要知道流的名字即可*/
			request = DefineConstant.ACTION_PLAYLIVE+"|"+fileRelativePath;
			printToServer.println(request);//发送请求
			String streamName = null;
			//读取服务器发来的视频流名字，本次接收不需要发送心跳应答
			readFromServer = new BufferedReader(new InputStreamReader(inputStream));
			streamName = readFromServer.readLine();
			while ((response = readFromServer.readLine()) != null){
			//如果持续接收到WAIT信息，说明服务端ffmpeg还没还是发送数据帧
				if(Integer.valueOf(response) == DefineConstant.WAIT){
					printToServer.println("I am alive.");//该字符串任意
					continue;	
				}else{//收到OK消息，跳出循环
					printToServer.println("I am alive.");
					break;
				}
			}
			//根据服务端的指示，现在可以开启ffplay放视频了
			ffplayCallable = new FFplayCallable("rtsp://"
								+serverIP+"/live/"+streamName);
			ffplayFuture = Executors.newSingleThreadExecutor().submit(ffplayCallable);
			//播放器线程已经启动，现在继续收发消息，让服务器知道客户端还活着
			while ((response = readFromServer.readLine()) != null){
				//*检测ffplay进程所在的线程是否已经结束，如果是，那么本线程也就没有继续执行的必要了
				if (ffplayFuture.isDone()) break;
				printToServer.println("I am alive.");
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
			 * 这里只是为了等待播放线程FFplayCallable死亡
			 * */
			ffplayFuture.get();
			
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "无法连接服务器", "错误", JOptionPane.ERROR_MESSAGE);
			System.out.println("无法连接服务器"+serverIP+serverPort);
		} finally {
			try {
				if (readFromServer != null)readFromServer.close();
				if (printToServer != null)printToServer.close();
				if (socketToServer != null)socketToServer.close();
				System.out.println("All Has been closed! in GUICallable finally block");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}// function call
}
