package me.haolee.gp.clientside;

import java.util.concurrent.Callable;
import javax.swing.JOptionPane;

import me.haolee.gp.common.Config;
import me.haolee.gp.common.VideoInfo;

public class VodCallable implements Callable<Integer>{
	
	/*
	 * 这些变量都要接收上级函数传值，而call方法本身无法接收参数
	 * ，所以在此定义成员变量，使用构造方法传值。
	 * */
	private String serverIP = null;
	private int serverPort = -1;
	/*播放视频用，具体的视频信息可以通过取读SelectBlock全局类来得到*/

	public VodCallable() {
		this.serverIP = Config.getValue("serverIP", "127.0.0.1");
		this.serverPort = Integer.valueOf(Config.getValue("serverPort", "10000"));
	}


	@Override
	public Integer call() throws Exception {
		VideoInfo videoInfo = null;//获取本显示块内的视频信息数据结构
		String fileRelativePath = null;//相对路径（包括文件名）
		
		try {
			//点播不需要建立server连接
			/*被选择的块，由静态全局方法和变量得到*/
			DisplayBlock selectedVideoBlock = null;
			/*点播功能不需要再给服务端发消息了，直接干*/
			selectedVideoBlock = SelectedBlock.getSelectedBlock();//获得被选视频块
			
			
			videoInfo = selectedVideoBlock.getVideoInfo();//获取本显示块内的视频信息数据结构
			
			fileRelativePath = videoInfo.getFileRelativePath();//相对路径
//			String rtspURL = "rtsp://"
//					+serverIP+"/file/"+fileRelativePath;
			String rtspURL = "rtsp://"+serverIP+"/"+fileRelativePath;
			FFplay ffplay = new FFplay(rtspURL);
			ffplay.play();
			
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "无法连接服务器", "错误", JOptionPane.ERROR_MESSAGE);
			System.out.println("无法连接服务器"+serverIP+serverPort);
		} finally {
		}
		return null;
	}// function call
}
