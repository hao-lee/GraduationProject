package me.haolee.gp.serverside;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

			StreamManager.generateStream(fileAbsolutePath);//启动流传输
			//上述方法返回后，要么复用流，要么创建了流。
			/*
			 * 如果此时流ID fileID又不存在了，说明FFmpeg播放失败
			 * */
			Packet sendPacket = null;
			if (StreamManager.exists(fileID)) {
				sendPacket = new Packet(CommandWord.RESPONSE_CONTINUE,null);
				objectOutputStream.writeObject(sendPacket);
			} else {//播放失败
				sendPacket = new Packet(CommandWord.RESPONSE_ABORT,null);
				objectOutputStream.writeObject(sendPacket);
				return;
			}
			
			/*
			 * 如果FFmpeg播放出错，则此时它已经死了，不需要交互了
			 * 如果没死就是一切正常，可以进入心跳包应答模式
			 * */
			while(true){
				Thread.sleep(2000);
				//只要fileID还在引用数组里就说明FFMPEG没死，就发送探测心跳包
				if(StreamManager.exists(fileID)){
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
			if(StreamManager.exists(fileID)){//说明FFmpeg还活着，循环跳出是因为客户端终止，所以释放流媒体ID
				StreamManager.releaseStream(fileID);
				System.out.println("Stream released");
			}
			else{ //是因为FFmpeg死了才跳出循环的，直接清掉流ID
				StreamManager.removeStream(fileID);
				System.out.println("Stream removed");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
