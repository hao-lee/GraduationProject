package ClientSide;

import java.util.HashMap;
import java.util.concurrent.Future;

public class DefineConstant {
	public final static int PLAYVIDEO = 1;
	public final static int GETVIDEOLIST = 2;
	public final static int STOPVTHREAD = 3;
}

final class HASHMAP{
	//sdpFutureMap用于控制每个sdp只能有一个线程使用，其实可以用Set的，
	//因为映射对中的Future没什么用，只保存sdpName用来判断sdp是否被占用即可
	private static final HashMap<String, Future<Integer>> sdpFutureMap = new HashMap<>();
	//sdpPortMap用于映射sdp-port对，方便根据sdp查出port送给服务端,以终结这条socket传输线路，
	//不过目前改为用sdpName去直接终止ffmpeg线程了，这个HashMap暂时没用上
	private static final HashMap<String, Integer> sdpPortMap = new HashMap<>();
	public static boolean isInSdpFutureMap(String sdpName) {
		if(sdpFutureMap.containsKey(sdpName) == true)
			return true;
		else
			return false;
	}
	public static boolean isInSdpPortMap(String sdpName) {
		if(sdpPortMap.containsKey(sdpName) == true)
			return true;
		else
			return false;
	}
	//根据sdp查出port送给服务端终结这条socket传输线路
	public static int getPortBySdp(String sdpName) {
		return sdpPortMap.get(sdpName);
	}
	//add to sdpFutureMap
	synchronized public static void putToMap(String sdpName, Future<Integer> future) {
			sdpFutureMap.put(sdpName, future);
	}//function putToMap
	// add to sdpPortMap
	synchronized public static void putToMap(String sdpName, Integer port) {
			sdpPortMap.put(sdpName, port);
	}//function putToMap
	synchronized public static void removeFromSdpFutureMap(String sdpName){
			sdpFutureMap.remove(sdpName);
	}
	synchronized public static void removeFromSdpPortMap(String sdpName){
		sdpPortMap.remove(sdpName);
	}
}