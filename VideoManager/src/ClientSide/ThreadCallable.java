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
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

//这是CLICallable的图形界面版，逻辑没有改变，只是加入了图形控件的输出
public class ThreadCallable implements Callable<Integer> {
	private String serverIP = null;
	private int serverPort = -1;
	private int requestCode = 0;
	private String videoName = null;// 复用为视频名或挂载点名

	private DefaultTableModel defaultTableModel = null;

	/*
	 * 不再使用造型转换，转而使用setter将数据注入
	 */
	public void setTableModel(DefaultTableModel dtm) {
		this.defaultTableModel = dtm;
	}
	/**
	 * 
	 * @param ip
	 *            服务器IP
	 * @param port
	 *            服务器端口
	 * @param reqCode
	 *            请求码
	 * @param videoName
	 *            参数复用：视频名或被终止的挂载点
	 */
	public ThreadCallable(String serverIP, int serverPort, int reqCode, String videoName) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.requestCode = reqCode;
		this.videoName = videoName;
	}

	@Override
	public Integer call() throws Exception {
		BufferedReader readFromServer = null;
		PrintWriter printToServer = null;
		Socket socketToServer = null;
		try {
			socketToServer = new Socket(serverIP, serverPort);// 客户端暂时不用设置SO_REUSEADDR
			// 打开输入输出流
			InputStream inputStream = socketToServer.getInputStream();
			OutputStream outputStream = socketToServer.getOutputStream();
			readFromServer = new BufferedReader(new InputStreamReader(inputStream));
			printToServer = new PrintWriter(outputStream, true);// auto flush
			// 向服务器发送请求
			printToServer.println(requestCode + "|" + videoName);
			// 读取服务器的状态回应,如果服务端线程死亡，socket中断，这里不会报异常，原因未知
			String response = null;
			
			if (requestCode == DefineConstant.PLAYVIDEO) {//播放视频
				String streamName = null;
				//读取服务器发来的视频流名字，本次接收不需要发送心跳应答
				streamName = readFromServer.readLine();
				//
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
				FFplayCallable ffplayCallable = new FFplayCallable("rtsp://"
									+serverIP+"/live/"+streamName);
				Future<Integer> ffplayFuture = Executors.newSingleThreadExecutor().submit(ffplayCallable);
				//播放器线程已经启动，现在继续收发消息，让服务器知道客户端还活着
				while ((response = readFromServer.readLine()) != null){
					printToServer.println("I am alive.");
					/*
					 * 检测ffplay进程所在的线程是否已经结束，如果是，那么本线程也就没有继续执行的必要了
					 * */
					if (requestCode == DefineConstant.PLAYVIDEO 
							&& ffplayFuture.isDone()) break;
				}
				/*
				 * 注意，上面的循环正常结束的原因就是服务器不再发送数据，可能是视频传输完毕，
				 * 也可能服务器因为未知原因异常终止，但是这种情况基本不会出现。
				 */
			}else if(requestCode == DefineConstant.GETVIDEOLIST){//获取列表
				/*
				 * 这种情况很简单，服务器和客户端不需要使用心跳包，服务器将数据发送完，
				 * 客户端再慢慢接收
				 * */
				while ((response = readFromServer.readLine()) != null) {
				/*
				 * 该将数据输出到table_videolist上了，不能直接去写控件，而是交给事件调度线程去做
				 * response不是final变量，不能在匿名内部类使用，所以传递一下
				 */
					final String finalString = response;
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							defaultTableModel.addRow(new String[] { finalString, "-", "-" });
						}// run
					});
				} // while
			}//else
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Server is stoped or IP:port is wrong");
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

class FFplayCallable implements Callable<Integer>{

	private String rtspURL = null;
	public FFplayCallable(String url) {
		this.rtspURL = url;
	}
	@Override
	public Integer call() throws Exception {
		//开启ffplay进程播放视频
		try {
			Process pc = null;
			ProcessBuilder pb = null;
			String[] cmd = new String[2];
			String os = System.getProperty("os.name");
			if(os.toLowerCase().startsWith("win"))
				{cmd[0] = "ffplay.exe";cmd[1]=rtspURL;}
			else{cmd[0] = "ffplay";cmd[1] = rtspURL;}
			
			pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			pc = pb.start();
			InputStream inputFFplayStatus = pc.getInputStream();
			BufferedReader readFFplayStatus = new BufferedReader(new InputStreamReader(inputFFplayStatus));
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append(cmd[0]+cmd[1]);
			try {
				String tmp_in = null;
				while ((tmp_in = readFFplayStatus.readLine()) != null) {
					System.out.println(tmp_in);
					stringBuffer.append(tmp_in+"\n");
				}
			} catch (Exception e) {e.printStackTrace();
			}finally {
				if (inputFFplayStatus != null)inputFFplayStatus.close();
				System.out.println("FFplay has finished");
			}
			//pc.waitFor();//如果关闭了ffplay，那么pc进程就死了，这个函数等待是没有意义的。
			pc.destroy();
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
}