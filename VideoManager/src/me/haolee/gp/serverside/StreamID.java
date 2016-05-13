package me.haolee.gp.serverside;

import java.util.HashSet;

public class StreamID {
	private static final int MAX = 65535;
	private static int currentStreamID = 0;
	private static HashSet<Integer> streamIDSet = new HashSet<>();
	
	//获取可用的数据流名字
	public static int getStreamID() {
		while(true){
			currentStreamID = (currentStreamID+1)%MAX;
			if(streamIDSet.add(currentStreamID))
				return currentStreamID;
			if (streamIDSet.size() == MAX)
				return -1;//如果集合满了说明当前达到了视频流数量的极限，返回-1
		}
	}
	//释放用完的流媒体ID
	public static void releaseStreamID(int streamID) {
		streamIDSet.remove(streamID);
	}
}
