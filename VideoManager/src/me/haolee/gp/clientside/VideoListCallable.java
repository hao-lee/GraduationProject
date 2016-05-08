package me.haolee.gp.clientside;

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

import me.haolee.gp.common.Convention;
import me.haolee.gp.common.VideoInfo;

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
		, String category,JPanel mainPanel) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.mode = mode;
		this.category = category;
		this.videoDisplayStart = Client.getVideoDisplayStart();
		this.videoDisplayStep = Client.getVideoDisplayStep();
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
			
			/*请求格式：req|mode|cate|start|step*/
			printToServer.println(Convention.ACTION_GETVIDEOLIST);
			printToServer.println(mode);
			printToServer.println(category);
			printToServer.println(videoDisplayStart);
			printToServer.println(videoDisplayStep);

			/*打开反序列化输入流*/
			objectInputStream = new ObjectInputStream(inputStream);
			/*
			 * 读取该模式该分类下的视频总数,
			 * 这里是用的Integer对象来传送，因为对象流和普通流不能混用
			所以我们就统一用对象流来输入
			*/
			int totalCount = (Integer)objectInputStream.readObject();
			int totalLastIndex = totalCount -1;
			
			/*
			 * 小插曲：如果videoDisplayStart==-1说明要取最后一页，
			 * 根据总记录数和步长，重置此时的起点
			 * */
			if(videoDisplayStart == -1){
				//根据总记录数和步长，算出最后一页的起点
				videoDisplayStart = (totalCount/videoDisplayStep)*videoDisplayStart;
				//总记录数恰好为步长倍数（1倍、2倍等），最后一页没内容，自动前推一页
				if((totalCount/videoDisplayStep)>=1 
						&& (totalCount%videoDisplayStep == 0))
					videoDisplayStart -=videoDisplayStep;
			}
			/*
			 * 直到此时起点才完全消除不缺定因素并确定下来
			 * */
			if(videoDisplayStart == 0)//起点是0，禁止上翻
				Client.ProhibitPreviousPage();
			else Client.AllowPreviousPage();
			
			/*
			 * count是本次实际循环接收对象的个数
			 * 如果本次起点距总记录结尾的条数（也就是剩余记录数） 大于 步长，则按步长来算，此时允许下翻
			 * 否则，说明剩下的记录不够了，按照剩余记录数来算，同时禁止下翻
			 * */
			int remain = totalLastIndex-videoDisplayStart+1;
			int count;
			if( remain <= videoDisplayStep){
				//库存不够或刚够，都不允许再翻页
				count = totalLastIndex-videoDisplayStart+1;
				Client.ProhibitNextPage();//本次起点距末尾不够数（或恰好够数），不能再下翻了
				//我们用-1标识请求最后一页的列表，所以这里还要重置起点值为正数起点
				//注意此时的videoDisplayStart早已经被重置为正数。
				Client.setVideoDisplayStart(videoDisplayStart);
			}
			else{
				//库存充足
				count = videoDisplayStep;
				Client.AllowNextPage();//本次起点距所有记录的末尾还很远，允许下翻
			}
			/*不可直接用videoDisplayStep，
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
				if(objectInputStream!=null)objectInputStream.close();
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
