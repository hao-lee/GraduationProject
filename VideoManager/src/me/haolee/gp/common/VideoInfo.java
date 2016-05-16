package me.haolee.gp.common;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import javax.imageio.ImageIO;

public class VideoInfo implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String videoName = null;
	private String duration = null;
	private String resolution = null;
	private String fileRelativePath = null;
	//private BufferedImage bufferedImage = null;
	private byte[] imageByteArray= null;//类的使用者对这种内部转换不可知
	//前几个属性从数据库获取，最后的bufferedImage会在ServerCallable调用ffmpeg获取
	public VideoInfo(String videoName,String duration,String resolution
			, String relativePath) {
		this.videoName = videoName;
		this.duration = duration;
		this.resolution = resolution;
		this.fileRelativePath = relativePath;
	}
	/*getter*/
	public String getVideoName() {
		return videoName;
	}
	public String getDuration(){
		return duration;
	}
	public String getResolution(){
		return resolution;
	}
	public String getFileRelativePath() {
		return fileRelativePath;
	}
	//缩略图
	public BufferedImage getBufferedImage() {
		ByteArrayInputStream bytein = new ByteArrayInputStream(this.imageByteArray);
		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(bytein);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bufferedImage;
	}
	/*setter*/
	public void setBufferedImage(BufferedImage bufferedImage) {
		//this.bufferedImage = bufferedImage;
		 ByteArrayOutputStream byteout = new ByteArrayOutputStream();  
         try {
			ImageIO.write(bufferedImage, "jpg", byteout);
			byteout.flush();
	        this.imageByteArray  = byteout.toByteArray();  
		} catch (IOException e) {
			e.printStackTrace();
		}  
	}
}
