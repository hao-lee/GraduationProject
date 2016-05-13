package me.haolee.gp.serverside;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;

import me.haolee.gp.common.Config;
import me.haolee.gp.common.VideoInfo;

public class VideoListSender {
	/*
	 * 发送视频列表
	 * */
	public void sendVideoList(int mode,String category,
			int videoDisplayStart,int videoDisplayStep,
			ObjectOutputStream objectOutputStream) {
		/* 这个功能：
		 * 使用DatebaseOperation类获取指定数量的视频
		 * */
		DatebaseQuery datebaseQuery = null;//数据库查询类
		/*数据库查询对象*/
		datebaseQuery = new DatebaseQuery();
		/*总条数*/
		int totalCount = datebaseQuery.getTotalCount(mode, category);

		/*
		 * 序列化对象不能用读取到null这样的方法来判断读取完毕，
		 * 所以先告诉客户端有几个对象。不同类型的流不可混用，在此全部用对象流*/
		//打开序列化输出流
		//告诉客户端，该分类下的视频总数
		try {
			objectOutputStream.writeObject(new Integer(totalCount));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		/*
		 * 小插曲：如果videoDisplayStart==-1说明要取最后一页，
		 * 根据总记录数和步长，重置此时的起点
		 * */
		if(videoDisplayStart == -1){
			//根据总记录数和步长，算出最后一页的起点
			videoDisplayStart = (totalCount/videoDisplayStep)*videoDisplayStart;
			//总记录数恰好为步长倍数（1倍、2倍等），最后一页没内容，自动前推一页
			if((totalCount/videoDisplayStep)>=1 
					&& (totalCount%videoDisplayStep == 0))
				videoDisplayStart -=videoDisplayStep;
		}
		System.out.println(videoDisplayStart);
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
				, "/home/mirage/rtsp-relay/file/");
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
				String fileID = videoInfo.getFileID();
				
				//如果是直播就用默认贴图
				if(videoInfo.getExtension().equals("live"))
					thumbnailPath = "live_defaultcover.png";
				else
					thumbnailPath = pathPrefix+thumbnailRelativePath+fileID+".jpg";
				/*读取缩略图*/
				BufferedImage bufferedImage = 
						ImageIO.read(new FileInputStream(thumbnailPath));
				/*缩略图设入videoInfo*/
				videoInfo.setBufferedImage(bufferedImage);//将图片对象设入videoInfo对象
				/*将填充好的videoInfo发给客户端*/
				objectOutputStream.writeObject(videoInfo);//序列化发给客户端
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
