package ClientSide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

//这是CLICallable的图形界面版，逻辑没有改变，只是加入了图形控件的输出
public class ThreadCallable implements Callable<Integer> {
	private String serverIP = null;
	private int serverPort = -1;
	private int reqCode = 0;
	private String vmName = null;// 复用为视频名或挂载点名
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
	 * @param vmName
	 *            参数复用：视频名或被终止的挂载点
	 */
	public ThreadCallable(String serverIP, int serverPort, int reqCode, String vmName) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.reqCode = reqCode;
		this.vmName = vmName;
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
			printToServer.println(reqCode + "|" + vmName);
			// 读取服务器的状态回应,如果服务端线程死亡，socket中断，这里不会报异常，原因未知
			String response = null;
			boolean setTitleOnce = false;// 使子窗口标题设置语句只执行一次
			while ((response = readFromServer.readLine()) != null) {
				/*
				 * System.out.println(response);
				 * 该将数据输出到table_videolist上了，不能直接去写控件，而是交给事件调度线程去做
				 * response不是final变量，不能在匿名内部类使用，所以传递一下 setTitleOnce同理
				 */
				final String response_tmp = response;
				final boolean setTitleOnce_tmp = setTitleOnce;
				setTitleOnce = true;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (mainORsub == 0) {
							defaultTableModel.addRow(new String[] { response_tmp, "-", "-" });
						} else if (mainORsub == 1) {// subFrame
							// 如果播放状态窗口subThreadFrame被关闭，那么在此EDT内继续向disComp写数据不会发生异常。
							if (!setTitleOnce_tmp)jFrame.setTitle(response_tmp);
							else jTextArea.append(response_tmp + "\n");
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
