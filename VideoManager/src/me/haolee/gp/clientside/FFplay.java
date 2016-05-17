package me.haolee.gp.clientside;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.JOptionPane;

class FFplay{

	private String rtspURL = null;
	public FFplay(String rtspUrl) {
		this.rtspURL = rtspUrl;
	}
	public void play() {
		//开启ffplay进程播放视频
		try {
			Process pc = null;
			ProcessBuilder pb = null;
			ArrayList<String> command = new ArrayList<>();//命令数组
			String os = System.getProperty("os.name");
			if(os.toLowerCase().startsWith("win")){
				File file = new File("ffplay.exe");//启动程序前先检测存在性
				if(!file.exists()){
					JOptionPane.showMessageDialog(null, "ffplay.exe不存在"
							, "错误", JOptionPane.ERROR_MESSAGE);
					return;
				}
				command.add("ffplay.exe");
			}
			else{//Linux系统只要有ffplay命令即可，不必非要存在可执行文件
				command.add("ffplay");
			}
			//注意，每个被空格隔开的都算是参数，都要分别追加
			//rtspURL文件名的空格也会被java自动处理好（相当于命令行上用引号把这一串括起来）
			//，但是很遗憾，转发程序不支持文件名空格，所以点播不能播放文件名含有空格的文件
			//直播时播放的是流名字，而ffmpeg可以很好的处理文件名空格，所以直播可以播放文件名含有空格的文件
			command.add(rtspURL);
			command.add("-x");
			command.add("1066");//指定宽度
			command.add("-y");
			command.add("600");//指定高度

			pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			pc = pb.start();
			InputStream inputFFplayStatus = pc.getInputStream();
			BufferedReader readFFplayStatus = new BufferedReader(new InputStreamReader(inputFFplayStatus));
			
			try {
				while (readFFplayStatus.readLine() != null) {
					//System.out.println(tmp_in);//读取输入流防止因为缓冲区满了而阻塞
				}
			} catch (Exception e) {e.printStackTrace();
			}finally {
				if(readFFplayStatus != null)readFFplayStatus.close();
				if (inputFFplayStatus != null)inputFFplayStatus.close();
				System.out.println("FFplay has finished");
			}
			/*
			 * 直播时，若服务端视频放完了，则在此等待用户手动关闭FFplay
			 * 点播时，若视频读取完了，则在此等待用户手动关闭FFplay
			 * */
			pc.waitFor();
			pc.destroy();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}