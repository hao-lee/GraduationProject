package me.haolee.gp.clientside;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.swing.JLabel;

import me.haolee.gp.common.CommandWord;
import me.haolee.gp.common.Config;
import me.haolee.gp.common.Packet;

public class TotalNumberCallable implements Callable<Integer>{

	private String serverIP = null;
	private int serverPort = -1;
	private CommandWord mode = null;
	private String category = null;
	
	public TotalNumberCallable(CommandWord mode, String category) {
		this.serverIP = Config.getValue("serverIP", "127.0.0.1");
		this.serverPort = Integer.valueOf(Config.getValue("serverPort", "10000"));
		this.mode = mode;
		this.category = category;
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
		
		int totalNumber = 0;
		try {
			socketToServer = new Socket(serverIP, serverPort);
			// 打开输入输出流
			outputStream = socketToServer.getOutputStream();
			objectOutputStream = new ObjectOutputStream(outputStream);
			inputStream = socketToServer.getInputStream();
			objectInputStream = new ObjectInputStream(inputStream);
			
			ArrayList<String> fields = new ArrayList<>();
			fields.add(String.valueOf(mode));
			fields.add(category);
			Packet sendPacket = new Packet(CommandWord.REQUEST_TOTALNUMBER,fields);
			objectOutputStream.writeObject(sendPacket);
			
			Packet recvPacket = (Packet)objectInputStream.readObject();
			totalNumber = (Integer)recvPacket.getFields();
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if(objectInputStream!=null)objectInputStream.close();
				if(objectOutputStream!=null)objectOutputStream.close();
				if (socketToServer != null)socketToServer.close();
				System.out.println("视频总数量获取完成");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return totalNumber;
	}
	
}
