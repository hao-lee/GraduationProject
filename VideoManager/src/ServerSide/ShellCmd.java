package ServerSide;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;

//这个类里面的函数用于执行ffmpeg命令
class ShellCmd {
	Process pc = null;
	ProcessBuilder pb = null;
	/*
	 * 仅在刷新列表获取缩略图以及视频直播时使用绝对路径前缀，
	 * 拼凑出的绝对路径是送给ffmpeg用的，
	 * 在点播时不需要和服务器交互，ffplay直接用文件相对于file目录的路径拼凑URL，
	 * 所以点播目录绝对不能随便移动。
	 * 但是直播中ffplay实际使用的是流名字，所以服务端直播视频的存放路径可以更改，
	 * 为了统一，直播文件的路径也尽量不要更改。
	 * */
	String pathPrefix = "/home/mirage/rtsp-relay/file/";//文件的默认绝对路径前缀
	
	public ShellCmd() {
		Config config = new Config();
		pathPrefix = config.readConfig("pathPrefix");
	}
	
	/*截图视频缩略图*/
	public BufferedImage generateThumbnail(String fileRelativePath) {
		InputStream inputFromShell = null;//读取shell
		BufferedImage bufferedImage =null;
		Process pc = null;
		ProcessBuilder pb = null;
		try {
			//filePath是相对路径+文件名，还需要拼接前缀组成绝对路径
			//不需要再用双引号把路径包起来，即使文件名有空格，java也会自己处理好的
			String fileAbsolutePath  = pathPrefix + fileRelativePath;
			ArrayList<String> command = new ArrayList<>();//命令数组
			command.add("ffmpeg");
			command.add("-y");
			command.add("-i");
			command.add(fileAbsolutePath);
			command.add("-f");
			command.add("mjpeg");
			command.add("-t");
			command.add("0.001");
			command.add("-s");
			command.add("320x240");
			command.add("tmp.jpg");
			//String[] cmd = { "sh", "-c", "ffmpeg -y -i "+ "\"" +fileAbsolutePath+"\""+" -f mjpeg -t 0.001 -s 320x240 tmp.jpg" };
			pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			pc = pb.start();
			inputFromShell = pc.getInputStream();
			BufferedReader readFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
			String tmp_in = null;
			try {
				while ((tmp_in = readFromShell.readLine()) != null) {
					System.out.println(tmp_in);
				}
			} catch (Exception e) {e.printStackTrace();}
			pc.destroy();
			bufferedImage = ImageIO.read(new FileInputStream("tmp.jpg"));
			File file = new File("tmp.jpg");
			if(file.exists())file.delete();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (inputFromShell != null)inputFromShell.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} // finally
		return bufferedImage;
	}
	
	/*
	 * 播放某个视频
	 */
	public void streamVideo(String fileRelativePath,
			BufferedReader readFromClient,PrintWriter printToClient) {
		InputStream inputFromShell = null;//读取shell
		Process pc = null;
		ProcessBuilder pb = null;
		try {
			
			int streamName = MountPoint.getStreamName();
			//告诉客户端流名称，本次发送不需要心跳应答
			printToClient.println(streamName);
			//filePath是相对路径+文件名，还需要拼接前缀组成绝对路径，
			//不需要加双引号，对于文件名的空格，java会自动处理
			String fileAbsolutePath  = pathPrefix + fileRelativePath;
			ArrayList<String> command = new ArrayList<>();//命令数组
			command.add("ffmpeg");
			command.add("-re");
			command.add("-i");
			command.add(fileAbsolutePath);
			command.add("-c");
			command.add("copy");
			command.add("-f");
			command.add("rtsp");
			command.add("rtsp://"+"127.0.0.1"+"/live/"+streamName);
			pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			pc = pb.start();
			inputFromShell = pc.getInputStream();
			BufferedReader readFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
			String tmp_in = null;
			// 如果接收方挂了，底层socket不会关闭，所以发送方不会出现异常。但是客户端立即重启的话就会
			// 得不到端口而报异常，除非设置端口重用选项。实际测试中并未出现问题，
			// 所以客户端暂时不用设置SO_REUSEADDR。
			try {
				while ((tmp_in = readFromShell.readLine()) != null) {
					System.out.println(tmp_in);
					if( ! tmp_in.toLowerCase().startsWith("frame="))//还没开始正式发送
						printToClient.println(DefineConstant.WAIT);
					else//开始发送视频了
						printToClient.println(DefineConstant.OK);
					//从客户端读取生存响应心跳包
					if(readFromClient.readLine() == null)//如果客户端关闭了，本次响应结束
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			pc.destroy();
			MountPoint.releaseStreamName(streamName);//释放数据流名字
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
}
