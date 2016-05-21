package me.haolee.gp.clientside;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import me.haolee.gp.common.CommandWord;
import me.haolee.gp.common.Config;
import me.haolee.gp.common.Packet;
import me.haolee.gp.common.VideoInfo;

public class VideoListCallable implements Callable<Integer> {
	
	/*
	 * 这些变量都要接收上级函数传值，而call方法本身无法接收参数
	 * ，所以在此定义成员变量，使用构造方法传值。
	 * */
	private String serverIP = null;
	private int serverPort = -1;
	private CommandWord mode = null;
	private String category = null;
	int videoListStart = 0;
	int videoListStep = 9;
	private JPanel mainPanel = null;//子线程要用，静态方便
	
	/*刷新视频列表用*/
	public VideoListCallable(CommandWord mode, String category
			,int videoListStart,int videoListStep,JPanel mainPanel) {
		this.serverIP = Config.getValue("serverIP", "127.0.0.1");
		this.serverPort = Integer.valueOf(Config.getValue("serverPort", "10000"));
		this.mode = mode;
		this.category = category;
		this.videoListStart = videoListStart;
		this.videoListStep = videoListStep;
		this.mainPanel = mainPanel;
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
		
		try {
			//点播不需要建立连接
			// 客户端暂时不用设置SO_REUSEADDR
			socketToServer = new Socket(serverIP, serverPort);
			// 打开输入输出流
			outputStream = socketToServer.getOutputStream();
			objectOutputStream = new ObjectOutputStream(outputStream);
			inputStream = socketToServer.getInputStream();
			objectInputStream = new ObjectInputStream(inputStream);
			
			/*请求格式：req|mode|cate|start|step*/
			ArrayList<String> fields = new ArrayList<>();
			fields.add(String.valueOf(mode));
			fields.add(category);
			fields.add(String.valueOf(videoListStart));
			fields.add(String.valueOf(videoListStep));
			Packet sendPacket = new Packet(CommandWord.REQUEST_VIDEOLIST,fields);
			objectOutputStream.writeObject(sendPacket);

			
			/*不可直接用videoDisplayStep，
			因为实际查到的个数可能小于videoDisplayStep*/
			Packet recvPacket = null;
			while((recvPacket = (Packet)objectInputStream.readObject())
					.getCommandWord()!=CommandWord.CTRL_END){
				VideoInfo videoInfo = (VideoInfo)recvPacket.getFields();
				VideoPanel videoPanel = new VideoPanel(videoInfo);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						mainPanel.add(videoPanel);
						mainPanel.revalidate();
						//mainPanel.repaint();//添加组件不需要调用repaint
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
				if(objectOutputStream!=null)objectOutputStream.close();
				if (socketToServer != null)socketToServer.close();
				System.out.println("All Has been closed! in GUICallable finally block");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}// function call
}
