package me.haolee.gp.serverside;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;

import me.haolee.gp.common.Command;

class RequestAnalyser implements Callable<Integer> {
	private Socket socketToClient = null;
	
	public RequestAnalyser(Socket s) {
		this.socketToClient = s;
	}

	@Override
	public Integer call() throws Exception {
		InputStream inputStream = null;
		OutputStream outputStream = null;
		PrintWriter printToClient = null;
		BufferedReader readFromClient = null;
		ObjectOutputStream objectOutputStream = null;//序列化输出流，需要时再打开
		
		try {
			inputStream = socketToClient.getInputStream();
			outputStream = socketToClient.getOutputStream();
			
			// 分析客户端请求	
			readFromClient = new BufferedReader(new InputStreamReader(inputStream));
			int requestCode = Integer.valueOf(readFromClient.readLine());
			
			/*根据请求的不同，行为不同*/
			int mode = -1;
			
			switch (requestCode) {
			case Command.ACTION_GETCATEGORY:

				mode = Integer.valueOf(readFromClient.readLine());
				objectOutputStream = new ObjectOutputStream(outputStream);
				new CategoryListSender().sendCategoryList(mode,objectOutputStream);
				break;
				
			case Command.ACTION_GETVIDEOLIST:
				mode = Integer.valueOf(readFromClient.readLine());
				String category = readFromClient.readLine();
				int videoDisplayStart = Integer.valueOf(readFromClient.readLine());
				int videoDisplayStep = Integer.valueOf(readFromClient.readLine());
				objectOutputStream = new ObjectOutputStream(outputStream);
				new VideoListSender().sendVideoList(mode, category, 
						videoDisplayStart, videoDisplayStep,
						objectOutputStream);
				break;
			case Command.ACTION_PLAYLIVE:
				//filePath是相对路径+文件名，还需要拼接前缀组成绝对路径，
				//不需要加双引号，对于文件名的空格，java会自动处理
				String fileRelativePath = readFromClient.readLine();
				printToClient = new PrintWriter(outputStream, true);// auto flush
				new VideoStreamSender().sendVideoStream(fileRelativePath, 
						readFromClient, printToClient);
				break;
			default:
				System.out.println("Undefined Command: "+requestCode);
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Client and its socket have exited!");
		} finally {
			try {
				if (objectOutputStream!=null)objectOutputStream.close();
				if (readFromClient != null)readFromClient.close();
				if (printToClient != null)printToClient.close();
				if (inputStream != null)inputStream.close();
				if (outputStream != null)outputStream.close();
				if (socketToClient != null)socketToClient.close();
				System.out.println("All Has been closed! in communicateWithClient() finally block");
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		} // catch-finally
		return null;
		
	}// function call
	

	/*
	 * 获取缩略图
	 * */
//	private BufferedImage generateThumbnail(String fileRelativePath) {
//		InputStream inputFromShell = null;//读取shell
//		BufferedImage bufferedImage =null;
//		Process pc = null;
//		ProcessBuilder pb = null;
//		try {
//			//filePath是相对路径+文件名，还需要拼接前缀组成绝对路径
//			//不需要再用双引号把路径包起来，即使文件名有空格，java也会自己处理好的
//			String fileAbsolutePath  = pathPrefix + fileRelativePath;
//			ArrayList<String> command = new ArrayList<>();//命令数组
//			command.add("ffmpeg");
//			command.add("-y");
//			command.add("-i");
//			command.add(fileAbsolutePath);
//			command.add("-f");
//			command.add("mjpeg");
//			command.add("-t");
//			command.add("0.001");
//			command.add("-s");
//			command.add("320x240");
//			command.add("tmp.jpg");
//			//String[] cmd = { "sh", "-c", "ffmpeg -y -i "+ "\"" +fileAbsolutePath+"\""+" -f mjpeg -t 0.001 -s 320x240 tmp.jpg" };
//			pb = new ProcessBuilder(command);
//			pb.redirectErrorStream(true);
//			pc = pb.start();
//			inputFromShell = pc.getInputStream();
//			BufferedReader readFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
//			String tmp_in = null;
//			try {
//				while ((tmp_in = readFromShell.readLine()) != null) {
//					System.out.println(tmp_in);
//				}
//			} catch (Exception e) {e.printStackTrace();}
//			pc.destroy();
//			bufferedImage = ImageIO.read(new FileInputStream("tmp.jpg"));
//			File file = new File("tmp.jpg");
//			if(file.exists())file.delete();
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				if (inputFromShell != null)inputFromShell.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		} // finally
//		return bufferedImage;
//	}
	
}// class end