package me.haolee.gp.clientside;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import me.haolee.gp.common.CommandWord;
import me.haolee.gp.common.Config;
import me.haolee.gp.common.Packet;



public class CatagoryListCallable implements Callable<Integer> {
	
	/*
	 * 这些变量都要接收上级函数传值，而call方法本身无法接收参数
	 * ，所以在此定义成员变量，使用构造方法传值。
	 * */
	private String serverIP = null;
	private int serverPort = -1;
	private CommandWord mode = null;
	private DefaultListModel<String> categoryListModel = null;
	/*获取分类用*/
	public CatagoryListCallable(CommandWord mode
			,DefaultListModel<String> categoryListModel) {
		this.serverIP = Config.getValue("serverIP", "127.0.0.1");
		this.serverPort = Integer.valueOf(Config.getValue("serverPort", "10000"));
		this.mode = mode;
		this.categoryListModel = categoryListModel;
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
		ArrayList<String> categoryList = null;
		try {
			// 客户端暂时不用设置SO_REUSEADDR
			socketToServer = new Socket(serverIP, serverPort);
			// 打开输入输出流
			outputStream = socketToServer.getOutputStream();
			objectOutputStream = new ObjectOutputStream(outputStream);
			inputStream = socketToServer.getInputStream();
			objectInputStream = new ObjectInputStream(inputStream);
			
			/*要发送的请求*/
			/* 请求格式：reqCode | mode */
			Packet sendPacket = new Packet(CommandWord.REQUEST_CATEGORYLIST,mode);
			objectOutputStream.writeObject(sendPacket);
			
			/*这时服务端已经得到了categoryList并发给客户端*/
			Packet recvPacket = (Packet)objectInputStream.readObject();
			//CommandWord commandWord = recvPacket.getCommandWord();
			categoryList = (ArrayList<String>)recvPacket.getFields();
			
			for(int i = 0; i< categoryList.size();i++){
				String item = categoryList.get(i);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						categoryListModel.addElement(item);
					}});
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
				if(objectOutputStream !=null)objectOutputStream.close();
				if(objectInputStream != null) objectInputStream.close();
				if (socketToServer != null)socketToServer.close();
				System.out.println("All Has been closed! in GUICallable finally block");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}// function call
}
