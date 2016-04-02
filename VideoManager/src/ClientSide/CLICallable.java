package ClientSide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;
//注意，这个类只能用于命令行版客户端，它的修改版叫做GUICallable，用于图形客户端
//两者的区别就是后者的输出是在文本框。程序逻辑无修改。
//这是客户端的线程类，用于自己开启socket连接服务器
class CLICallable implements Callable<Integer>{
	String serverIP = null;
	int serverPort = -1;
	String videoName = null;
	String sdpName = null;
	int reqCode = 0;
	public CLICallable(String ip,int port,int reqCode,
			String videoName, String sdpName) {
		this.serverIP = ip;
		this.serverPort = port;
		this.videoName = videoName;
		this.sdpName = sdpName;
		this.reqCode = reqCode;
	}
	@Override
	public Integer call() throws Exception {
		BufferedReader readFromServer = null;
		PrintWriter printToServer = null;
		Socket socketToServer = null;
		try {
			socketToServer = new Socket(serverIP,serverPort);//open socket
			//加入sdp-port映射对，用于查找某个sdp对应的客户端port
			HASHMAP.putToMap(sdpName, socketToServer.getLocalPort());
			//打开输入输出流
			InputStream inputStream = socketToServer.getInputStream();
			OutputStream outputStream = socketToServer.getOutputStream();
			readFromServer = new BufferedReader(new InputStreamReader(inputStream));
			printToServer = new PrintWriter(outputStream,true);//auto flush
			//向服务器发送请求
			printToServer.println(reqCode+"|"+videoName+"|"+sdpName);
			//读取服务器的状态回应
			String response = null;
			while((response = readFromServer.readLine()) != null){
				System.out.println(response);
			}
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("ClientWorkThread: Exception in call");
		} finally {
			try {
				//线程结束前，将本线程对应的future从HASHMAP里移除
				HASHMAP.removeFromSdpFutureMap(sdpName);
				HASHMAP.removeFromSdpPortMap(sdpName);
				if(readFromServer != null)readFromServer.close();
				if(printToServer != null)printToServer.close();
				if(socketToServer != null)socketToServer.close();
				System.out.println("All Has been closed! in call() finally block");
			} catch (IOException e) {e.printStackTrace();}
		}
		return null;
	}//function call
}