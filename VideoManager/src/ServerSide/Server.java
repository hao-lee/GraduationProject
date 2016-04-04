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

public class Server {
	private int port = 10000;
	private ServerSocket serverSocket = null;
	private Socket socketToClient = null;
	private ExecutorService executorService = null;

	public static void main(String[] args) {
		// 初始化挂载点集合
		MountPoint.initMountPointList();
		// 启动服务器监听
		Server server = new Server();
		server.startServer();
		server.listenAccept();
	}

	public void startServer() {
		try {
			serverSocket = new ServerSocket(port);// start
			// 初始化线程池
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
				// "Future<Integer> future = " is not needed
				executorService.submit(new ServerCallable(socketToClient));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Exception in listenAccetp!");
			}

		} // while
	}// function listenAccept
}// class end
	// 这个类用于连接客户端，回传数据

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
			// 分析客户端请求
			String msg = readFromClient.readLine();
			String[] msgField = msg.split("\\|");// reqCode|videoName or sdpName
			int requestCode = Integer.valueOf(msgField[0]);
			// vmName可能是视频名或sdp挂载点名，此为变量复用。
			// 如果reqCode是播放视频那么msgField[]不存在index=1的元素，这里检测下防止数组越界访问
			String vmName = (msgField.length == 2 ? msgField[1] : "");
			// getInetAddress获得的IP形如/111.111.111.111，多了斜杠，去掉它
			String clientIP = socketToClient.getInetAddress().toString().substring(1);
			// 传入printToClient给getVideoList和playVideo，最终printToClient依然由本函数关闭
			if (requestCode == DefineConstant.GETVIDEOLIST) {
				// 查询视频列表，此时不许要vmName参数，也不需要获取挂载点，也不需要传递客户端IP
				new ShellCmd("", "", "", printToClient).getVideoList();
			} else if (requestCode == DefineConstant.PLAYVIDEO) {
				// 获取可用目标挂载点
				String mountpoint = MountPoint.getMountPoint();
				// 先发送一行信息，客户端将之作为子窗口的标题
				printToClient.println("Video Name: " + vmName + " Mount Point: " + mountpoint);
				if (mountpoint == "") {
					// System.out.println("Can't get mountpoint. The mountpoint
					// is not enough.");
					printToClient.println("Video Playing Error!");// SubFrame's
																	// Title
					printToClient.println("Can't get mountpoint. The mountpoint is not enough.");
				} else {
					// 此时vmName为视频名
					new ShellCmd(vmName, mountpoint, clientIP, printToClient).playVideo();
					// 视频发送过程完毕，释放挂载点
					MountPoint.releaseMountPoint(mountpoint);
				}
			} else if (requestCode == DefineConstant.STOPVTHREAD) {
				printToClient.println("Stop Playing:");// 子窗口的标题
				// 此时vmName复用为“要被停止的”挂载点名称
				new ShellCmd("", vmName, "", printToClient).stopProcess();
				printToClient.println("stopProcess() has been executed!");
				// 现在服务器使用Linux命令直接干掉使用name这个挂载点的进程
				// 这样在服务端向该挂载点发数据的线程就会因为内建shell进程的终止而从playVideo函数返回，
				// 进而正常结束call函数，线程正常结束。
			} else if (requestCode == DefineConstant.GETVIDEOSTATUS) {
				printToClient.println("Video Status:");// 子窗口的标题
				printToClient.println("Videos that are playing:\n");
				new ShellCmd("", "", "", printToClient).getPlayingVideoList();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Client and its socket have exited!");
		} finally {
			try {
				if (readFromClient != null)readFromClient.close();
				if (printToClient != null)printToClient.close();
				if (socketToClient != null)socketToClient.close();
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
	private String mountPoint = null;
	private String clientIP = null;
	private InputStream inputFromShell = null;
	private PrintWriter printToClient = null;
	String[] cmd1 = { "sh", "-c", "ping -c 20 111.1.1.1" };
	String[] cmd2 = { "sh", "-c", "ping -c 20 127.0.0.1" };

	/**
	 * 
	 * @param videoname
	 *            视频名
	 * @param mountPoint
	 *            被停止的挂载点，仅用于停止视频
	 * @param clientIP
	 *            客户端IP，仅用于播放视频
	 * @param pwr
	 *            printToClient
	 */
	public ShellCmd(String videoname, String mountPoint, String clientIP, PrintWriter pwr) {
		this.videoName = videoname;
		this.mountPoint = mountPoint;
		this.clientIP = clientIP;
		this.printToClient = pwr;
	}

	/*
	 * 获取视频文件列表
	 */
	public void getVideoList() {
		try {
			Process pc = null;
			ProcessBuilder pb = null;
			String[] cmd = { "sh", "-c", "ls /usr/local/movies/" };
			pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			pc = pb.start();
			inputFromShell = pc.getInputStream();
			BufferedReader inFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
			String tmp_in = null;
			try {
				while ((tmp_in = inFromShell.readLine()) != null) {
					// System.out.println(tmp_in);
					printToClient.println(tmp_in);
				}
				// 捕获异常是为了当客户端断掉socket时，本服务端线程不会跳过pc.waitFor()
				// 从而可以继续视频传输，等待ffmpeg结束后本线程再结束
			} catch (Exception e) {
				e.printStackTrace();
			}
			pc.waitFor();// 若出现客户端断开socket的意外，此语句可以保证shell程序继续执行完毕
			pc.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {// 上级函数负责printToClient的关闭，本函数只负责关掉自己的命令行读入流即可
				if (inputFromShell != null)inputFromShell.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // finally
	}

	/*
	 * 播放某个视频
	 */
	public void playVideo() {
		try {
			Process pc = null;
			ProcessBuilder pb = null;
			String[] cmd = { "sh", "-c",
					"ffmpeg -re -i /usr/local/movies/" + videoName + " -f rtsp rtsp://" + clientIP + "/" + mountPoint };
			pb = new ProcessBuilder(cmd);
			System.out.println(cmd[2]);//
			pb.redirectErrorStream(true);
			pc = pb.start();
			inputFromShell = pc.getInputStream();
			BufferedReader inFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
			String tmp_in = null;
			// 如果接收方挂了，底层socket不会关闭，所以发送方不会出现异常。但是客户端立即重启的话就会
			// 得不到端口而报异常，除非设置端口重用选项。实际测试中并未出现问题，
			// 所以客户端暂时不用设置SO_REUSEADDR。
			try {
				while ((tmp_in = inFromShell.readLine()) != null) {
					System.out.println(tmp_in);
					printToClient.println(tmp_in);
				}
				// 捕获异常是为了当客户端断掉socket时，本服务端线程不会跳过pc.waitFor()
				// 从而可以继续视频传输，等待ffmpeg结束后本线程再结束
			} catch (Exception e) {
				e.printStackTrace();
			}
			pc.waitFor();// 若出现客户端断开socket的意外，此语句可以保证shell程序继续执行完毕
			pc.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {// 上级函数负责printToClient的关闭，本函数只负责关掉自己的命令行读入流即可
				if (inputFromShell != null)
					inputFromShell.close();
				System.out.println("Shell ffmpeg has stopped");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // finally
	}// function play video

	/*
	 * 获取当前正在播放的视频列表
	 */
	public void getPlayingVideoList() {
		try {
			Process pc = null;
			ProcessBuilder pb = null;
			String[] cmd = { "sh", "-c", "ps aux | grep ffmpeg | grep -v grep | awk '{print $14,$17}'" };
			pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			pc = pb.start();
			inputFromShell = pc.getInputStream();
			BufferedReader inFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
			String tmp_in = null;
			try {
				while ((tmp_in = inFromShell.readLine()) != null) {
					// System.out.println(tmp_in);
					printToClient.println(tmp_in);
				}
				// 捕获异常是为了当客户端断掉socket时，本服务端线程不会跳过pc.waitFor()
				// 从而可以继续视频传输，等待ffmpeg结束后本线程再结束
			} catch (Exception e) {
				e.printStackTrace();
			}
			pc.waitFor();// 若出现客户端断开socket的意外，此语句可以保证shell程序继续执行完毕
			pc.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {// 上级函数负责printToClient的关闭，本函数只负责关掉自己的命令行读入流即可
				if (inputFromShell != null)
					inputFromShell.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // finally
	}// getPlayingVideoList

	/*
	 * 停止播放某个视频
	 */
	public void stopProcess() {
		try {
			Process pc = null;
			ProcessBuilder pb = null;
			String[] cmd = { "sh", "-c",
					"ps aux | grep ffmpeg |grep " + mountPoint + " | grep -v grep | awk '{print $2}' | xargs kill -9" };
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
			} catch (Exception e) {
				e.printStackTrace();
			}
			pc.waitFor();
			pc.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {// 上级函数负责printToClient的关闭，本函数只负责关掉自己的命令行读入流即可
				if (inputFromShell != null)inputFromShell.close();
				System.out.println("Shell ffmpeg has been stopped");
				printToClient.println("Shell ffmpeg has been stopped");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // finally
	}
}