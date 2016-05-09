package me.haolee.gp.serverside;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.haolee.gp.common.Config;

public class Server {
	private int listeningPort = -1;//监听的端口
	private ServerSocket serverSocket = null;
	private Socket socketToClient = null;
	private ExecutorService executorService = null;

	public static void main(String[] args) {
		// 启动服务器监听
		Server server = new Server();
		server.startServer();
		server.listenAccept();
	}

	public void startServer() {
		try {
			// 初始化线程池
			executorService = Executors.newCachedThreadPool();
			/*
			 * 将配置文件一次性读取进来
			 * */
			Config.readConfigFile("server.config");
			
			/*按需取key对应的value*/
			this.listeningPort = Integer.valueOf(
					Config.getValue("listeningPort","10000"));//读取配置文件监听端口
			
			serverSocket = new ServerSocket(listeningPort);// start
			
			System.out.println("Server started... Listening port "+listeningPort);
		} catch (IOException e) {
			e.printStackTrace();
			try {
				if (serverSocket != null)
					serverSocket.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}// function startServer

	public void listenAccept() {
		while (true) {
			try {
				socketToClient = serverSocket.accept();
				executorService.submit(new AnalyseRequest(socketToClient));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Exception in listenAccetp!");
			}

		} // while
	}// function listenAccept
}// class end
// 这个类用于连接客户端，回传数据
