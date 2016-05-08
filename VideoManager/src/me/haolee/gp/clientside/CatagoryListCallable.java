package me.haolee.gp.clientside;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import me.haolee.gp.common.Convention;



public class CatagoryListCallable implements Callable<HashMap<String, String>> {
	
	/*
	 * 这些变量都要接收上级函数传值，而call方法本身无法接收参数
	 * ，所以在此定义成员变量，使用构造方法传值。
	 * */
	private String serverIP = null;
	private int serverPort = -1;
	private int mode = -1;
	private DefaultListModel<String> categoryListModel = null;
	
	/*获取分类用*/
	public CatagoryListCallable(String serverIP, int serverPort
			,int mode, DefaultListModel<String> categoryListModel) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.mode = mode;
		this.categoryListModel = categoryListModel;
	}

	@Override
	public HashMap<String, String> call() throws Exception {
		/*套接字和原生输入输出流*/
		Socket socketToServer = null;
		InputStream inputStream = null;
		OutputStream outputStream =null;
		/*包装后的输入输出流*/
		ObjectInputStream objectInputStream = null;
		BufferedReader readFromServer = null;
		PrintWriter printToServer = null;
		HashMap<String, String> categoryList = null;
		try {
			// 客户端暂时不用设置SO_REUSEADDR
			socketToServer = new Socket(serverIP, serverPort);
			// 打开输入输出流
			inputStream = socketToServer.getInputStream();
			outputStream = socketToServer.getOutputStream();
			// auto flush
			printToServer = new PrintWriter(outputStream, true);
			
			/*要发送的请求*/
			/* 请求格式：reqCode | mode */
			printToServer.println(Convention.ACTION_GETCATEGORY);
			printToServer.println(mode);
			
			/*打开反序列化输入流，
			这时服务端已经得到了categorySet并准备发给客户端*/
			objectInputStream = new ObjectInputStream(inputStream);
			categoryList = (HashMap<String, String>)
					objectInputStream.readObject();
			
			Set set = categoryList.entrySet();
			for(Iterator iter = set.iterator();iter.hasNext();){
				Map.Entry<String, String> categoryElement = 
						(Map.Entry<String, String>)iter.next();
				//不要把iter.next()直接放入事件调度线程，会出现异常的
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						categoryListModel.addElement(categoryElement.getKey());
					}
				});
			}

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "无法连接服务器", "错误", JOptionPane.ERROR_MESSAGE);
			System.out.println("无法连接服务器"+serverIP+serverPort);
		} finally {
			try {
				if(objectInputStream != null) objectInputStream.close();
				if (readFromServer != null)readFromServer.close();
				if (printToServer != null)printToServer.close();
				if (socketToServer != null)socketToServer.close();
				System.out.println("All Has been closed! in GUICallable finally block");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return categoryList;
	}// function call
}
