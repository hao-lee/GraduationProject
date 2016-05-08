package me.haolee.gp.clientside;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.JOptionPane;

import me.haolee.gp.common.VideoInfo;

public class VodCallable implements Callable<Integer>{
	
	/*
	 * 这些变量都要接收上级函数传值，而call方法本身无法接收参数
	 * ，所以在此定义成员变量，使用构造方法传值。
	 * */
	private String serverIP = null;
	private int serverPort = -1;
	private String relativePath = null;
	/*播放视频用，具体的视频信息可以通过取读SelectBlock全局类来得到*/
	/**
	 * 
	 * @param serverIP
	 * @param serverPort
	 * @param relativePath 视频所在分类的路径
	 */
	public VodCallable(String serverIP, int serverPort, String relativePath) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.relativePath = relativePath;
	}


	@Override
	public Integer call() throws Exception {
		/*线程对象*/
		FFplayCallable ffplayCallable = null;
		Future<Integer> ffplayFuture = null;
		
		VideoInfo videoInfo = null;//获取本显示块内的视频信息数据结构
		String fileID = null;//文件名
		String extension = null;//扩展名
		String fileRelativePath = null;//相对路径（包括文件名）
		
		try {
			//点播不需要建立server连接
			/*被选择的块，由静态全局方法和变量得到*/
			DisplayBlock selectedVideoBlock = null;
			/*点播功能不需要再给服务端发消息了，直接干*/
			selectedVideoBlock = SelectBlock.getSelectedBlock();//获得被选视频块
			
			
			videoInfo = selectedVideoBlock.getVideoInfo();//获取本显示块内的视频信息数据结构
			fileID = videoInfo.getFileID();//文件ID
			extension = videoInfo.getExtension();//扩展名
			
			fileRelativePath = relativePath+fileID+"."+extension;//拼凑相对路径
			ffplayCallable = new FFplayCallable("rtsp://"
					+serverIP+"/file/"+fileRelativePath);
			ffplayFuture = Executors.newSingleThreadExecutor().submit(ffplayCallable);
			/*
			 * 线程call函数的返回值对我们没用，
			 * 这里只是为了等待播放线程FFplayCallable死亡
			 * */
			ffplayFuture.get();
			
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "无法连接服务器", "错误", JOptionPane.ERROR_MESSAGE);
			System.out.println("无法连接服务器"+serverIP+serverPort);
		} finally {
		}
		return null;
	}// function call
}
