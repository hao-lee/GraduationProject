package me.haolee.gp.serverside;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import me.haolee.gp.common.CommandWord;
import me.haolee.gp.common.Config;
import me.haolee.gp.common.Packet;

public class VideoStreamSender {
	/*
	 * 发送视频流
	 * */
	public void sendVideoStream(String fileRelativePath,
			ObjectInputStream objectInputStream
			, ObjectOutputStream objectOutputStream) {
		InputStream inputFromShell = null;//读取shell
		Process pc = null;
		ProcessBuilder pb = null;
		try {
			
			//文件的默认绝对路径前缀
			String pathPrefix = Config.getValue("pathPrefix", "/home/mirage/rtsp-relay/file/");
			//拼接绝对路径
			String fileAbsolutePath = pathPrefix + fileRelativePath;
			
			//新建文件对象，借用它的getName方法获得文件全名
			String fileName = new File(fileAbsolutePath).getName();
			int dot = fileName.lastIndexOf(".");
			//主文件名（不含扩展名）
			String fileID = fileName.substring(0, dot);//0~dot-1
			//扩展名
			String fileExtension = fileName.substring(dot+1);
			//获取流媒体ID
			String streamID = StreamIDManager.getStreamID(fileID);
			
			//告诉客户端流名称，本次发送不需要心跳应答
			Packet sendPacket = new Packet(CommandWord.RESPONSE_DATA,streamID);
			objectOutputStream.writeObject(sendPacket);
			System.out.println(streamID);
			
			ArrayList<String> command = new ArrayList<>();//命令数组
			command.add("ffmpeg");
			
			//如果扩展名是live，则读出里面的内容作为输入地址(网络地址)
			if(fileExtension.equals("live")){
				InputStream fileInputStream = new FileInputStream(fileAbsolutePath);
				String cameraURL = new BufferedReader(new InputStreamReader(fileInputStream)).readLine();
				fileInputStream.close();
				command.add("-i");
				command.add(cameraURL);
			}else{//读取的本地文件
				command.add("-re");
				command.add("-i");
				command.add(fileAbsolutePath);
			}
			
			command.add("-c");
			command.add("copy");
			//转码占用CPU过高，直接原样拷贝
//			command.add("-c:v");
//			command.add("libx264");
//			command.add("-c:a");
//			command.add("libfaac");
			command.add("-f");
			command.add("rtsp");
			//command.add("rtsp://"+"127.0.0.1"+"/live/"+streamID);
			command.add("rtsp://"+"127.0.0.1"+"/"+streamID+".sdp");
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
				
				if( ! tmp_in.toLowerCase().startsWith("frame=")){//还没开始正式发送
					sendPacket = new Packet(CommandWord.RESPONSE_IDLE,null);
					objectOutputStream.writeObject(sendPacket);
				}
				else{//开始发送视频了
					sendPacket = new Packet(CommandWord.RESPONSE_CONTINUE,null);
					objectOutputStream.writeObject(sendPacket);
					break;
				}
			}
			/*
			 * 如果FFmpeg播放出错，则此时它已经死了，不需要交互了
			 * 如果没死就是一切正常，可以进入心跳包应答模式
			 * */
			while(true){
				//如果FFMPEG没死，就发送探测心跳包
				if((tmp_in = readFromShell.readLine()) != null){
					System.out.println(tmp_in);
					//send
					sendPacket = new Packet(CommandWord.CTRL_HARTBEAT,null);
					objectOutputStream.writeObject(sendPacket);
				}else{//FFMPEG死了，发送END包告诉客户端自己要死了，然后直接退出
					sendPacket = new Packet(CommandWord.CTRL_END,null);
					objectOutputStream.writeObject(sendPacket);
					break;//退出
				}
				/*
				 * 如果上一步发送了心跳包，则本次接收应答看看客户端给出了什么回复
				 * 如果客户端也是心跳包，则继续下一次循环
				 * 否则，说明客户端死了，直接退出。
				 * */
				Packet recvPacket = (Packet)objectInputStream.readObject();
				if(recvPacket.getCommandWord() == CommandWord.CTRL_HARTBEAT)
					;
				else//收到CommandWord.CTRL_END，说明客户端已经结束 
					break;//退出
			}


			pc.destroy();
			StreamIDManager.releaseStreamID(fileID);//释放流媒体ID
			
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
