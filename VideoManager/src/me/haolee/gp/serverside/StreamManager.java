package me.haolee.gp.serverside;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StreamManager {
	private static HashMap<String, Integer> streamRefCounter = new HashMap<>();
	private static ExecutorService executorService = Executors.newCachedThreadPool();
	
	public synchronized static void sendStream(String fileAbsolutePath) {
		
		String fileName = new File(fileAbsolutePath).getName();
		int dot = fileName.lastIndexOf(".");
		String fileID = fileName.substring(0, dot);
		
		if(exists(fileID)){//fileID的流已经存在，借用一下，引用计数加1
			int refCounter = streamRefCounter.get(fileID);
			refCounter++;
			streamRefCounter.put(fileID, refCounter);
			
		}else{//不存在则创建流
			FFmpegCallable ffmpegCallable = new FFmpegCallable(fileAbsolutePath);
			executorService.submit(ffmpegCallable);
			streamRefCounter.put(fileID, 1);
		}
		
	}
	public static boolean exists(String fileID) {
		if(streamRefCounter.containsKey(fileID))
			return true;
		else 
			return false;
	}
	//用于ffmpeg结束前自己将fileID移除
	public synchronized static void removeStream(String fileID) {
		streamRefCounter.remove(fileID);
	}
	//用于当客户端中断时，给流ID的引用计数减1，减到0就说明没有人在观看了，
	//，于是干掉ffmpeg进程并删除对应的streamRefCounter元素
	public synchronized static void releaseStream(String fileID) {
		int refCounter = streamRefCounter.get(fileID);
		refCounter --;
		if(refCounter != 0)//还有客户端在使用fileID流
			streamRefCounter.put(fileID, refCounter);
		else{//没人用了，终止进程，删除元素
			InputStream inputFromShell = null;
			try {
			    Process pc = null;
			    ProcessBuilder pb = null;
			    String[] cmd = { "sh", "-c", "ps aux | grep ffmpeg |grep " + fileID + " | grep -v grep | awk '{print $2}' | xargs kill -9"};
			    pb = new ProcessBuilder(cmd);
			    pb.redirectErrorStream(true);
			    pc = pb.start();
			    inputFromShell = pc.getInputStream();
			    BufferedReader inFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
			    String tmp_in = null;
			    while ((tmp_in = inFromShell.readLine()) != null)
			        System.out.println(tmp_in);
			    pc.waitFor();
			    pc.destroy();
			} catch (Exception e) {
			    e.printStackTrace();
			}
			finally {
			    try {
			        if (inputFromShell != null)inputFromShell.close();
			        System.out.println("Shell ffmpeg has been killed");
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
			}
			streamRefCounter.remove(fileID);//删除元素
		}
	}
}

class FFmpegCallable implements Callable<Integer>{
	String fileAbsolutePath = null;
	public FFmpegCallable(String fileAbsolutePath) {
		this.fileAbsolutePath = fileAbsolutePath;
	}

	@Override
	public Integer call() throws Exception {
		
		InputStream inputFromShell = null;//读取shell
		Process pc = null;
		ProcessBuilder pb = null;
		
		String fileName = new File(fileAbsolutePath).getName();
		int dot = fileName.lastIndexOf(".");
		String fileID = fileName.substring(0, dot);
		String fileExtension = fileName.substring(dot+1);
		
		ArrayList<String> command = new ArrayList<>();//命令数组
		command.add("ffmpeg");
		
		//如果扩展名是live，则读出里面的内容作为输入地址(网络地址)
		if(fileExtension.equals("live")){
			InputStream fileInputStream = new FileInputStream(fileAbsolutePath);
			String cameraURL = new BufferedReader(new InputStreamReader(fileInputStream)).readLine();
			fileInputStream.close();
			command.add("-i");
			command.add(cameraURL);
		}else{//读取的本地文件
			command.add("-re");
			command.add("-i");
			command.add(fileAbsolutePath);
		}
		
		command.add("-c");
		command.add("copy");
		command.add("-f");
		command.add("rtsp");
		command.add("rtsp://"+"127.0.0.1"+"/"+fileID+".sdp");
		pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);
		pc = pb.start();
		inputFromShell = pc.getInputStream();
		BufferedReader readFromShell = new BufferedReader(new InputStreamReader(inputFromShell));
		String string = null;
		while((string = readFromShell.readLine()) != null){
			//System.out.println(string);
		}
		pc.destroy();
		System.out.println("FFmpeg stoped");
		return null;
	}
}