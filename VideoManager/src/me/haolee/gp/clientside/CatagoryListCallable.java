package me.haolee.gp.clientside;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import me.haolee.gp.common.Command;



public class CatagoryListCallable implements Callable<Integer> {
	
	/*
	 * 这些变量都要接收上级函数传值，而call方法本身无法接收参数
	 * ，所以在此定义成员变量，使用构造方法传值。
	 * */
	private String serverIP = null;
	private int serverPort = -1;
	private int mode = -1;
	
	/*获取分类用*/
	public CatagoryListCallable(String serverIP, int serverPort,int mode) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.mode = mode;
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
		ArrayList<String> categoryList = null;
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
			printToServer.println(Command.ACTION_GETCATEGORY);
			printToServer.println(mode);
			
			/*打开反序列化输入流，
			这时服务端已经得到了categorySet并准备发给客户端*/
			objectInputStream = new ObjectInputStream(inputStream);
			categoryList = (ArrayList<String>)
					objectInputStream.readObject();
			
			for(int i = 0; i< categoryList.size();i++){
				String item = categoryList.get(i);
				Client.addToCategoryList(item);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JOptionPane.showMessageDialog(null, "无法连接服务器", "错误", JOptionPane.ERROR_MESSAGE);
				}
			});
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
		return null;
	}// function call
}
