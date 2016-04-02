package ServerSide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ServerMain {
	public static void main(String[] args) {
		Server server = new Server();
		server.startServer();
		server.listenAccept();
	}
}// class end

// a server
class Server {
	private int port = 10000;
	private ServerSocket serverSocket = null;
	private Socket socketToClient = null;
	private ExecutorService executorService = null;

	public void startServer() {
		try {
			serverSocket = new ServerSocket(port);// start
			// create a ThreadPool
			executorService = Executors.newCachedThreadPool();
			System.out.println("Server started...");
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
				Future<Integer> future = executorService.submit(new ServerCallable(socketToClient));
				// add to portFutureMap,
				// it will be used to kill a thread which has certain port.
				// two thread's port must be different
				HASHMAP.putToMap(socketToClient.getPort(), future);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Exception in listenAccetp!");
			}

		} // while
	}// function listenAccept
}// class end
//这个类用于连接客户端，回传数据
class ServerCallable implements Callable<Integer> {
	private Socket socketToClient = null;
	private BufferedReader readFromClient = null;
	private PrintWriter printToClient = null;

	public ServerCallable(Socket s) {
		this.socketToClient = s;
	}

	@Override
	public Integer call() throws Exception {
		try {
			InputStream inputStream = socketToClient.getInputStream();
			readFromClient = new BufferedReader(new InputStreamReader(inputStream));
			OutputStream outputStream = socketToClient.getOutputStream();
			printToClient = new PrintWriter(outputStream, true);// auto flush
			//分析客户端请求
			String msg = readFromClient.readLine();
			String[] msgField = msg.split("\\|");// reqCode|videoName|sdpName
			int requestCode = Integer.valueOf(msgField[0]);
			String videoName = "",sdpName = "";
			if (msgField.length > 1) {
				//如果是GETVIDEOLIST，那么后两个参数为空，split操作后msgField只有0号元素
				//此时要取出元素1和2会在此处异常，所以要检测一下，若参数存在则覆盖一下
				//若是STOPVTHREAD，因为第三个参数存在，所以第二个参数也会存在，只不过是空串
				videoName = msgField[1];
				sdpName = msgField[2];
			}
			//getInetAddress获得的IP形如/111.111.111.111，多了斜杠，去掉它
			String clientIP = socketToClient.getInetAddress().toString().substring(1);
			//传入printToClient给getVideoList和playVideo，最终printToClient依然由本函数关闭
			if (requestCode == DefineConstant.GETVIDEOLIST) {
				//查询视频列表，此处videoName和sdpName参数没用
				new ShellCmd("", "", clientIP, printToClient).getVideoList();
			} else if (requestCode == DefineConstant.PLAYVIDEO) {
				new ShellCmd(videoName,sdpName,clientIP,printToClient).playVideo();
			}else if(requestCode == DefineConstant.STOPVTHREAD){
				new ShellCmd("", sdpName, clientIP, printToClient).stopProcess();
				printToClient.println("stopProcess() has been executed!");
				//原计划用sdp查出客户端port，让服务器终止和这个port通信的线程，可惜这么做没成功，
				//cancel函数返回的是成功，但是线程并没有取消。现在服务器使用Linux命令直接干掉使用sdpName的进程
				//这样在服务端向sdpName发数据的线程就会因为内建shell进程的终止而从playVideo函数返回，
				//进而正常结束call函数，线程正常结束。
			}
		} catch (IOException e) {
			// e.printStackTrace();
			System.out.println("Client and its socket have exited!");
		} finally {
			try {
				// 线程结束时自己把port-future映射对从HASHMAP里面移除
				HASHMAP.removeFromMap(socketToClient.getPort());
				if (readFromClient != null)
					readFromClient.close();
				if (printToClient != null)
					printToClient.close();
				if (socketToClient != null)
					socketToClient.close();
				System.out.println("All Has been closed! in communicateWithClient() finally block");
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		} // catch-finally
		return null;
	}// function call
}// class end

// 这个类里面的函数用于执行Shell命令，并读取Shell输出通过printToClient发给客户端
class ShellCmd {
	private String videoName = null;
	private String sdpName = null;
	private String clientIP = null;
	private InputStream inputFromShell = null;
	private PrintWriter printToClient = null;
	String[] cmd1 = { "sh", "-c", "ping -c 20 111.1.1.1" };
	String[] cmd2 = { "sh", "-c", "ping -c 20 127.0.0.1" };

	public ShellCmd(String vname, String sname, String cip, PrintWriter pr) {
		this.videoName = vname;
		this.sdpName = sname;
		this.clientIP = cip;
		this.printToClient = pr;
	}

	// get list
	public void getVideoList() {
		try {
			Process pc = null;
			ProcessBuilder pb = null;
			String[] cmd = { "sh", "-c","ls /usr/local/movies/"};
			pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			pc = pb.start();
			inputFromShell = pc.getInputStream();
			BufferedReader inFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
			String tmp_in = null;
			try{
				while ((tmp_in = inFromShell.readLine()) != null) {
					//System.out.println(tmp_in);
					printToClient.println(tmp_in);
				}
				//捕获异常是为了当客户端断掉socket时，本服务端线程不会跳过pc.waitFor()
				//从而可以继续视频传输，等待ffmpeg结束后本线程再结束
			} catch (Exception e) {e.printStackTrace();}
			pc.waitFor();//若出现客户端断开socket的意外，此语句可以保证shell程序继续执行完毕
			pc.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {//上级函数负责printToClient的关闭，本函数只负责关掉自己的命令行读入流即可
				if (inputFromShell != null)
					inputFromShell.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // finally
	}

	// play video
	public void playVideo() {
		try {
			Process pc = null;
			ProcessBuilder pb = null;
			String[] cmd = { "sh", "-c", "ffmpeg -re -i /usr/local/movies/" + videoName + " -f rtsp rtsp://" + clientIP + "/" + sdpName };
			pb = new ProcessBuilder(cmd);
			System.out.println(cmd[2]);//
			pb.redirectErrorStream(true);
			pc = pb.start();
			inputFromShell = pc.getInputStream();
			BufferedReader inFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
			String tmp_in = null;
			//如果接收方挂了，底层socket不会关闭，所以发送方不会出现异常。但是客户端立即重启的话就会
			//得不到端口而报异常，除非设置端口重用选项。实际测试中并未出现问题，
			//所以客户端暂时不用设置SO_REUSEADDR。
			try {
				while ((tmp_in = inFromShell.readLine()) != null) {
					System.out.println(tmp_in);
					printToClient.println(tmp_in);
				}
				//捕获异常是为了当客户端断掉socket时，本服务端线程不会跳过pc.waitFor()
				//从而可以继续视频传输，等待ffmpeg结束后本线程再结束
			} catch (Exception e) {e.printStackTrace();}
			pc.waitFor();//若出现客户端断开socket的意外，此语句可以保证shell程序继续执行完毕
			pc.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {//上级函数负责printToClient的关闭，本函数只负责关掉自己的命令行读入流即可
				if (inputFromShell != null)inputFromShell.close();
				System.out.println("Shell ffmpeg has stopped");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // finally
	}// function play video
	
	//stop a process
	public void stopProcess() {
		try {
			Process pc = null;
			ProcessBuilder pb = null;
			String[] cmd = { "sh", "-c", "ps aux | grep ffmpeg |grep " + sdpName
					+ " | grep -v grep | awk '{print $2}' | xargs kill -9"};
			pb = new ProcessBuilder(cmd);
			System.out.println(cmd[2]);//
			printToClient.print(cmd[2]);//
			pb.redirectErrorStream(true);
			pc = pb.start();
			inputFromShell = pc.getInputStream();
			BufferedReader inFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
			String tmp_in = null;
			try {
				while ((tmp_in = inFromShell.readLine()) != null) {
					System.out.println(tmp_in);
					printToClient.println(tmp_in);
				}
			} catch (Exception e) {e.printStackTrace();}
			pc.waitFor();
			pc.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {//上级函数负责printToClient的关闭，本函数只负责关掉自己的命令行读入流即可
				if (inputFromShell != null)inputFromShell.close();
				System.out.println("Shell ffmpeg has stopped");
				printToClient.println("Shell ffmpeg has stopped");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // finally
	}
}