package ClientSide;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CLIClient {
	String serverIP = "127.0.0.1";
	int serverPort = 10000;
	private ExecutorService executorService = null;

	public static void main(String[] args) {
		CLIClient consoleClient = new CLIClient();
		consoleClient.getVideoList();
		//consoleClient.playVideo("~/transformers2.mp4", "test.sdp");
		consoleClient.clearThreadPool();
	}

	public CLIClient() {
		// create a ThreadPool
		executorService = Executors.newCachedThreadPool();
	}

	public void clearThreadPool() {
		executorService.shutdown();
		try {
			executorService.awaitTermination(100, TimeUnit.MINUTES);//最长等待100分钟
		} catch (InterruptedException e) {e.printStackTrace();}
	}

	public void getVideoList() {
		//在界面中会用进度条保证用户不能多次点击get list。所以此处不需要检测线程是否能够开启
		Callable<Integer> callable = new CLICallable(
				serverIP, serverPort, DefineConstant.GETVIDEOLIST,"", "");
		executorService.submit(callable);//不需要收集返回值
	}

	public void playVideo(String videoName, String sdpName) {
		if (HASHMAP.isInSdpFutureMap(sdpName) == true)
			System.out.println("This sdp has been occupied!"
					+ " Nothing to do.");//
		else {
				Callable<Integer> callable = new CLICallable(
						serverIP,serverPort, DefineConstant.PLAYVIDEO, 
						videoName, sdpName);
				Future<Integer> future = executorService.submit(callable);
				HASHMAP.putToMap(sdpName, future);
		}
	}
}// class end
