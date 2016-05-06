package ClientSide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import CommonPackage.VideoInfo;

public class VideoListCallable implements Callable<Integer> {
	
	/*
	 * 这些变量都要接收上级函数传值，而call方法本身无法接收参数
	 * ，所以在此定义成员变量，使用构造方法传值。
	 * */
	private String serverIP = null;
	private int serverPort = -1;
	private int mode = -1;
	private String category = null;
	
	private int videoDisplayStart = 0;//起始行数从0计
	private int videoDisplayStep = 9;
	
	private JPanel mainPanel = null;
	
	/*刷新视频列表用*/
	public VideoListCallable(String serverIP, int serverPort,int mode
		, String category,int videoDisplayStart,int videoDisplayStep,JPanel mainPanel) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.mode = mode;
		this.category = category;
		this.videoDisplayStart = videoDisplayStart;
		this.videoDisplayStep = videoDisplayStep;
		this.mainPanel = mainPanel;
	}

	@Override
	public Integer call() throws Exception {
		/*套接字和原生输入输出流*/
		Socket socketToServer = null;
		InputStream inputStream = null;
		OutputStream outputStream =null;
		/*包装后的输入输出流*/
		ObjectInputStream objectInputStream = null;
		BufferedReader readFromServer = null;
		PrintWriter printToServer = null;
		
		VideoInfo videoInfo = null;//获取本显示块内的视频信息数据结构
		
		try {
			//点播不需要建立连接
			// 客户端暂时不用设置SO_REUSEADDR
			socketToServer = new Socket(serverIP, serverPort);
			// 打开输入输出流
			inputStream = socketToServer.getInputStream();
			outputStream = socketToServer.getOutputStream();
			// auto flush
			printToServer = new PrintWriter(outputStream, true);
			/*要发送的请求*/
			String request = null;
				
			if(videoDisplayStart == 0)//起点是0，禁止上翻
				Client.ProhibitPreviousPage();
			else Client.AllowPreviousPage();
			
			/*请求格式：req|mode|cate|start|step*/
			request = DefineConstant.ACTION_GETVIDEOLIST
					+"|"+mode+"|"+category+"|"
					+videoDisplayStart+"|"+videoDisplayStep;
			printToServer.println(request);//发送请求
			/*打开反序列化输入流*/
			objectInputStream = new ObjectInputStream(inputStream);
			/*读取对象个数,这里是用的Integer对象来传送，因为对象流和普通流不能混用
			所以我们就统一用对象流来输入*/
			int count = (Integer)objectInputStream.readObject();
			
			if(count < videoDisplayStep)//查询到末尾了，数据量不足一页，禁止下翻
				Client.ProhibitNextPage();
			else Client.AllowNextPage();
			
			/*count是循环接收对象的个数，不可直接用videoDisplayStep，
			因为实际查到的个数可能小于videoDisplayStep*/
			for(;count != 0;count --){
				videoInfo = (VideoInfo)objectInputStream.readObject();
				DisplayBlock displayBlock = new DisplayBlock(videoInfo);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						mainPanel.add(displayBlock);
						mainPanel.revalidate();
						//mainPanel.repaint();//添加组件不许要调用repaint
					}
				});
			}

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
