package me.haolee.gp.serverside;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;

import me.haolee.gp.common.CommandWord;
import me.haolee.gp.common.Config;
import me.haolee.gp.common.Packet;
import me.haolee.gp.common.VideoInfo;

public class VideoListSender {
	/*
	 * 发送视频列表
	 * */
	public void sendVideoList(CommandWord mode,String category,
			int videoDisplayStart,int videoDisplayStep,
			ObjectOutputStream objectOutputStream) {
		/* 这个功能：
		 * 使用DatebaseOperation类获取指定数量的视频
		 * */
		DatebaseQuery datebaseQuery = null;//数据库查询类
		/*数据库查询对象*/
		datebaseQuery = new DatebaseQuery();
		
		/*
		 * 查询指定范围的视频
		 * */
		ArrayList<VideoInfo> videoInfoList=datebaseQuery.getVideoSet(mode, 
				category, videoDisplayStart, videoDisplayStep);
		/*
		 * 从数据库读取到的videoInfo集合，每个对象的bufferedImage字段没有被填充*
		 * 现在开始填充，填充完一个就发给客户端一个
		 */
		//默认绝对路径前缀
		String pathPrefix = Config.getValue("pathPrefix"
				, "/home/mirage/EasyDarwin/Movies/");
		/*获得缩略图路径，以便读取缩略图*/
		String thumbnailRelativePath = Config.getValue(
				"thumbnailRelativePath","thumbnail/");//缩略图路径
		Iterator<VideoInfo> iterator = videoInfoList.iterator();
		//对每个视频分别取截图并设置到视频对象里，然后写回客户端
		try{
			//缩略图绝对路径
			String thumbnailPath = null;
			while(iterator.hasNext()){
				VideoInfo videoInfo = iterator.next();
				
				//新建文件对象，借用它的getName方法获得文件全名
				String fileName = new File(videoInfo.getFileRelativePath()).getName();
				int dot = fileName.lastIndexOf(".");
				//主文件名（不含扩展名）
				String fileID = fileName.substring(0, dot);//0~dot-1
				//扩展名
				String fileExtension = fileName.substring(dot+1);
				
				//如果是直播就用默认贴图
				if(fileExtension.equals("live"))
					thumbnailPath = "live_defaultcover.png";
				else
					thumbnailPath = pathPrefix+thumbnailRelativePath+fileID+".jpg";
				/*读取缩略图*/
				BufferedImage bufferedImage = 
						ImageIO.read(new FileInputStream(thumbnailPath));
				/*缩略图设入videoInfo*/
				videoInfo.setBufferedImage(bufferedImage);//将图片对象设入videoInfo对象
				/*将填充好的videoInfo发给客户端*/
				Packet sendPacket = new Packet(CommandWord.RESPONSE_DATA,videoInfo);
				objectOutputStream.writeObject(sendPacket);//序列化发给客户端
			}
			objectOutputStream.writeObject(new Packet(CommandWord.CTRL_END, null));
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
