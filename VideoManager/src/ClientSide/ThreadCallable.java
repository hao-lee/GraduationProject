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

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

//这是CLICallable的图形界面版，逻辑没有改变，只是加入了图形控件的输出
public class ThreadCallable implements Callable<Integer> {
	private String serverIP = null;
	private int serverPort = -1;
	private int requestCode = 0;
	private String videoName = null;// 复用为视频名或挂载点名
	/*
	 * 图形控件defaulttablemodel用于向主界面输出视频列表 jtextarea用于向子窗口输出状态信息
	 * jFrame仅用于改变子窗口的标题栏信息
	 */
	private int mainORsub;// mainORsub = 0 表示向主窗口输出，==1是向子窗口输出
	private DefaultTableModel defaultTableModel = null;
	private JTextArea jTextArea = null;
	private JFrame jFrame = null;

	/*
	 * 不再使用造型转换，转而使用setter将数据注入
	 */
	public void setTableModel(DefaultTableModel dtm) {
		this.defaultTableModel = dtm;
		this.mainORsub = 0;
	}

	public void setJtaFrame(JTextArea jta, JFrame jFrame) {
		this.jTextArea = jta;
		this.jFrame = jFrame;
		this.mainORsub = 1;
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
			String streamName = null, subFrameTitle = null;
			//确定子窗口的标题栏内容字符串
			if(requestCode == DefineConstant.PLAYVIDEO){
				try {
					streamName = readFromServer.readLine();
					subFrameTitle = "Video Name:"+videoName+"Stream Name:"+streamName;
				} catch (IOException e) {e.printStackTrace();}}
			else if(requestCode == DefineConstant.STOPVTHREAD)
				subFrameTitle = "Stop Playing Video";
			else if(requestCode == DefineConstant.GETVIDEOSTATUS)
				subFrameTitle = "Get Playing Status";
			else if(requestCode == DefineConstant.GETVIDEOLIST)
				subFrameTitle = null;//对于获取列表功能，没有子窗口标题需要设置
			//设置子窗口标题栏
			final String finalTitle = subFrameTitle;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if(finalTitle != null)jFrame.setTitle(finalTitle);
				}
			});
			//如果此时是在请求播放视频，那么开启ffplay视频播放线程
			Future<Integer> ffplayFuture = null;
			if (requestCode == DefineConstant.PLAYVIDEO) {
				FFplayCallable ffplayCallable = new FFplayCallable("rtsp://"
									+serverIP+"/live/"+streamName);
				ffplayFuture = Executors.newSingleThreadExecutor().submit(ffplayCallable);
			}

			while ((response = readFromServer.readLine()) != null) {
				/*
				 * 如果这个线程的任务是播放视频，那么检测ffplay进程所在的线程是否已经结束，
				 * 如果是，那么本线程也就没有继续执行的必要了
				 */
				if (requestCode == DefineConstant.PLAYVIDEO 
						&& ffplayFuture.isDone()) break;
				/*
				 * System.out.println(response);
				 * 该将数据输出到table_videolist上了，不能直接去写控件，而是交给事件调度线程去做
				 * response不是final变量，不能在匿名内部类使用，所以传递一下
				 */
				final String finalString = response;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (mainORsub == 0) {
							defaultTableModel.addRow(new String[] { finalString, "-", "-" });
						} else if (mainORsub == 1) {// subFrame
							// 如果播放状态窗口subThreadFrame被关闭，那么在此EDT内继续向disComp写数据不会发生异常。
							jTextArea.append(finalString + "\n");
						}
					}// run
				});
			} // while
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Server is stoped or IP:port is wrong", "Alert",
					JOptionPane.ERROR_MESSAGE);
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
			String[] cmd = { "ffplay", rtspURL};
			pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			pc = pb.start();
			InputStream inputFFplayStatus = pc.getInputStream();
			BufferedReader readFFplayStatus = new BufferedReader(new InputStreamReader(inputFFplayStatus));
			try {
				String tmp_in = null;
				while ((tmp_in = readFFplayStatus.readLine()) != null) {
					System.out.println(tmp_in);
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