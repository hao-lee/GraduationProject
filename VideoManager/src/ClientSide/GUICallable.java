package ClientSide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

//这是CLICallable的图形界面版，逻辑没有改变，只是加入了图形控件的输出
public class GUICallable implements Callable<Integer>{
	String serverIP = null;
	int serverPort = -1;
	String vmName = null;//复用为视频名或挂载点名
	int reqCode = 0;
	//图形控件,可能是defaulttablemodel也可能是jtextarea。
	//前者是显示视频列表，后者显示视频播放状态。因为不确定是什么，所以使用造型转换。
	Object disComp = null;
	public GUICallable(String ip,int port,int reqCode,
			String vmName, Object disComp) {
		this.serverIP = ip;
		this.serverPort = port;
		this.vmName = vmName;
		this.reqCode = reqCode;
		this.disComp = disComp;//图形控件
	}
	@Override
	public Integer call() throws Exception {
		BufferedReader readFromServer = null;
		PrintWriter printToServer = null;
		Socket socketToServer = null;
		try {
			socketToServer = new Socket(serverIP,serverPort);//客户端暂时不用设置SO_REUSEADDR
			//打开输入输出流
			InputStream inputStream = socketToServer.getInputStream();
			OutputStream outputStream = socketToServer.getOutputStream();
			readFromServer = new BufferedReader(new InputStreamReader(inputStream));
			printToServer = new PrintWriter(outputStream,true);//auto flush
			//向服务器发送请求
			printToServer.println(reqCode+"|"+vmName);
			//读取服务器的状态回应,如果服务端线程死亡，socket中断，这里不会报异常，原因未知
			String response = null;
			while((response = readFromServer.readLine()) != null){
				//System.out.println(response);
				//该将数据输出到table_videolist上了，不能直接去写控件，而是交给事件调度线程去做
				//response不是final变量，不能在匿名内部类使用，所以传递一下
				final String response_tmp = response;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if(disComp instanceof DefaultTableModel){
							DefaultTableModel defaultTableModel = (DefaultTableModel)disComp;
							defaultTableModel.addRow(new String[]{response_tmp,"-","-"});
						}else if (disComp instanceof JTextArea) {
							JTextArea jTextArea = (JTextArea)disComp;
							jTextArea.append(response_tmp+"\n");
						}//if
					}//run
				});
			}//while
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Server is stoped or IP:port is wrong",
					"Alert", JOptionPane.ERROR_MESSAGE); 
			System.out.println("Server is stoped or IP:port is wrong");
		} finally {
			try {
				if(readFromServer != null)readFromServer.close();
				if(printToServer != null)printToServer.close();
				if(socketToServer != null)socketToServer.close();
				System.out.println("All Has been closed! in GUICallable finally block");
			} catch (IOException e) {e.printStackTrace();}
		}
		return null;
	}//function call
}
