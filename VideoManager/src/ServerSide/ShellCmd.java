package ServerSide;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

//这个类里面的函数用于执行Shell命令，并读取Shell输出通过printToClient发给客户端
class ShellCmd {
	private InputStream inputFromShell = null;
	private BufferedReader readFromClient = null;
	private PrintWriter printToClient = null;
	Process pc = null;
	ProcessBuilder pb = null;
	String pathPrefix = "/home/mirage/rtsp-relay/file/";
	
	public ShellCmd(BufferedReader readFromClient,PrintWriter printToClient) {
		this.readFromClient = readFromClient;
		this.printToClient = printToClient;
	}
	
	/*截图视频缩略图*/
	public BufferedImage generateThumbnail(String fileRelativePath) {
			BufferedImage bufferedImage =null;
			Process pc = null;
			ProcessBuilder pb = null;
		try {
			//filePath是相对路径+文件名，还需要拼接前缀组成绝对路径，为了安全还要加引号
			String fileAbsolutePath  = "\""+pathPrefix + fileRelativePath+"\"";
			String[] cmd = { "sh", "-c", "ffmpeg -y -i "+fileAbsolutePath+" -f mjpeg -t 0.001 -s 320x240 tmp.jpg" };
			pb = new ProcessBuilder(cmd);
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
	public void playVideo(String fileRelativePath) {
		try {
			Process pc = null;
			ProcessBuilder pb = null;
			int streamName = MountPoint.getStreamName();
			//告诉客户端流名称，本次发送不需要心跳应答
			printToClient.println(streamName);
			//filePath是相对路径+文件名，还需要拼接前缀组成绝对路径，为了安全还要加引号
			String fileAbsolutePath  = "\""+pathPrefix + fileRelativePath+"\"";
			String[] cmd = { "sh", "-c",
					"ffmpeg -re -i " + fileAbsolutePath + " -c copy -f rtsp rtsp://" + "127.0.0.1" + "/live/" + streamName };
			pb = new ProcessBuilder(cmd);
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
