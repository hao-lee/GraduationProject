package ServerSide;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
	private int listeningPort = 10000;//默认监听的端口
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
			serverSocket = new ServerSocket(listeningPort);// start
			// 初始化线程池
			executorService = Executors.newCachedThreadPool();
			Config config = new Config();
			this.listeningPort = Integer.valueOf(
					config.readConfig("listeningPort"));//读取配置文件监听端口
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
				executorService.submit(new ServerCallable(socketToClient));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Exception in listenAccetp!");
			}

		} // while
	}// function listenAccept
}// class end
// 这个类用于连接客户端，回传数据
