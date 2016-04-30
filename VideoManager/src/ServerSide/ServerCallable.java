package ServerSide;

import CommonPackage.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;

class ServerCallable implements Callable<Integer> {
	private Socket socketToClient = null;
	private BufferedReader readFromClient = null;
	private PrintWriter printToClient = null;
	private ObjectOutputStream objectOutputStream = null;

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
			String[] msgField = msg.split("\\|");//切分请求
			int requestCode = Integer.valueOf(msgField[0]);
			
			/*根据请求的不同，行为不同*/
			if(requestCode == DefineConstant.ACTION_GETCATEGORY){
				//这个功能使用DatebaseOperation类
				int mode = Integer.valueOf(msgField[1]);
				DatebaseOperation db = new DatebaseOperation();
				ArrayList<String> categoryList = db.getCategory(mode);
				///打开序列化输出流
				objectOutputStream = new ObjectOutputStream(outputStream);
				objectOutputStream.writeObject(categoryList);
			}else if (requestCode == DefineConstant.ACTION_REFRESHVIDEOLIST) {
				/* 这个功能：
				 * 使用DatebaseOperation类获取指定数量的视频
				 * 使用ShellCmd类获得每个视频对应的缩略图
				 * */
				int mode = Integer.valueOf(msgField[1]);
				String category = msgField[2];
				int videoDisplayStart = Integer.valueOf(msgField[3]);
				int videoDisplayStep = Integer.valueOf(msgField[4]);
				/*查询数据库*/
				DatebaseOperation db = new DatebaseOperation();
				ArrayList<VideoInfo> videoInfoList=db.getVideoSet(mode, 
						category, videoDisplayStart, videoDisplayStep);
				/*
				 * 序列化对象不能用读取到null这样的方法来判断读取完毕，
				 * 所以先告诉客户端有几个对象。不同类型的流不可混用，在此全部用对象流*/
				//打开序列化输出流
				objectOutputStream = new ObjectOutputStream(outputStream);
				objectOutputStream.writeObject(new Integer(videoInfoList.size()));
				/*
				 * 上面读取到的videoInfo集合，每个对象的bufferedImage字段没有被填充*
				 * 现在开始填充，填充完一个就发给客户端一个
				 */
				Iterator<VideoInfo> iterator = videoInfoList.iterator();
				//对每个视频分别取截图并设置到视频对象里，然后写回客户端
				while(iterator.hasNext()){
					VideoInfo videoInfo = iterator.next();
					/*拼接文件完整的相对路径，以便读取缩略图*/
					String relativePurePath = videoInfo.getRelativePath();//视频相对路径
					String fileName = videoInfo.getVideoName();//取视频文件名
					String fileRelativePath = relativePurePath+fileName;//拼接相对路径
					ShellCmd shellCmd = new ShellCmd(readFromClient,printToClient);
					/*读取缩略图*/
					BufferedImage bufferedImage = 
							shellCmd.generateThumbnail(fileRelativePath);
					/*缩略图设入videoInfo*/
					videoInfo.setBufferedImage(bufferedImage);//将图片对象设入videoInfo对象
					/*将填充好的videoInfo发给客户端*/
					objectOutputStream.writeObject(videoInfo);//序列化发给客户端
				}
			}else if (requestCode == DefineConstant.ACTION_PLAYLIVE) {
				String fileRelativePath = msgField[1];
				ShellCmd shellCmd = new ShellCmd(readFromClient,printToClient);
				shellCmd.streamVideo(fileRelativePath);
			}else {
				System.out.println("Undefined Command: ");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Client and its socket have exited!");
		} finally {
			try {
				if (objectOutputStream!=null)objectOutputStream.close();
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