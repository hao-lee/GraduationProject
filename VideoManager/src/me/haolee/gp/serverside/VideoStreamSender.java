package me.haolee.gp.serverside;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import me.haolee.gp.common.Config;
import me.haolee.gp.common.Command;

public class VideoStreamSender {
	/*
	 * 发送视频流
	 * */
	public void sendVideoStream(String fileRelativePath,
			BufferedReader readFromClient, PrintWriter printToClient) {
		InputStream inputFromShell = null;//读取shell
		Process pc = null;
		ProcessBuilder pb = null;
		try {
			
			int streamID = StreamID.getStreamID();
			if(streamID == -1)//返回-1说明所有可用的流标识都被占用
				return;//不用再往下执行了。客户端会收到一系列null自动退出的。
			
			//告诉客户端流名称，本次发送不需要心跳应答
			printToClient.println(streamID);
			
			ArrayList<String> command = new ArrayList<>();//命令数组
			command.add("ffmpeg");
			
			//文件的默认绝对路径前缀
			String pathPrefix = Config.getValue("pathPrefix", "/home/mirage/rtsp-relay/file/");
			//拼接绝对路径
			String fileAbsolutePath = pathPrefix + fileRelativePath;
			//如果是.live文件则读出里面的内容作为输入地址(网络地址)
			if(fileAbsolutePath.endsWith(".live")){
				fileAbsolutePath = new BufferedReader(new InputStreamReader(new FileInputStream(fileAbsolutePath))).readLine();
				command.add("-rtsp_transport");
				command.add("tcp");
			}else{//读取的本地文件
				command.add("-re");
			}
			
			command.add("-i");
			command.add(fileAbsolutePath);
			command.add("-c:v");
			command.add("libx264");
			command.add("-c:a");
			command.add("libfaac");
			command.add("-f");
			command.add("rtsp");
			command.add("rtsp://"+"127.0.0.1"+"/live/"+streamID);
			pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			pc = pb.start();
			inputFromShell = pc.getInputStream();
			BufferedReader readFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
			String tmp_in = null;
			// 如果接收方挂了，底层socket不会关闭，所以发送方不会出现异常。但是客户端立即重启的话就会
			// 得不到端口而报异常，除非设置端口重用选项。实际测试中并未出现问题，
			// 所以客户端暂时不用设置SO_REUSEADDR。
			while ((tmp_in = readFromShell.readLine()) != null) {
				System.out.println(tmp_in);
				if( ! tmp_in.toLowerCase().startsWith("frame="))//还没开始正式发送
					printToClient.println(Command.CTRL_WAIT);
				else{//开始发送视频了
					printToClient.println(Command.CTRL_OK);
					break;
				}
			}
			//System.out.println(":::"+tmp_in);
			/*
			 * 如果FFmpeg播放出错，则此时它已经死了，不需要交互了
			 * 如果没死就是一切正常，可以进入心跳包应答模式
			 * */
			do {
				//Thread.sleep(1000);
				//读取FFmpeg的输出防止缓冲区满了而阻塞，字符串太长，丢弃不用
				if((tmp_in = readFromShell.readLine()) != null)
					;
				else
					break;//FFmpeg死了，没必要再探测客户端了
				printToClient.println("Probe");//发送一个短字符串探测客户端是否死亡
				//客户端死了就没必要继续了
			} while ((readFromClient.readLine()) != null);
			pc.destroy();
			StreamID.releaseStreamID(streamID);//释放数据流名字
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
	}
}
