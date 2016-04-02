package ServerSide;

import java.util.HashMap;
import java.util.concurrent.Future;

public class DefineConstant {
	public final static int PLAYVIDEO = 1;
	public final static int GETVIDEOLIST = 2;
	public final static int STOPVTHREAD = 3;
}

final class HASHMAP {
	//原计划用端口来识别一个发送线程并将之终止，但是没成功
	//目前改为用sdpName去直接终止ffmpeg线程，这个HashMap暂时没用上
	private static final HashMap<Integer, Future<Integer>> portFutureMap = new HashMap<>();

	public static boolean isInPortFutureMap(Integer port) {
		if (portFutureMap.containsKey(port) == true)
			return true;
		else
			return false;
	}
	//根据port返回正在使用此端口的线程Future对象
	public static Future<Integer> getFutureByPort(int port) {
		return portFutureMap.get(port);
	}
	// add to portFutureMap
	synchronized public static void putToMap(Integer port, Future<Integer> future) {
		portFutureMap.put(port, future);
	}// function putToMap
		// remove from portFutureMap

	synchronized public static void removeFromMap(Integer port) {
		if(portFutureMap.containsKey(port))portFutureMap.remove(port);
	}
}