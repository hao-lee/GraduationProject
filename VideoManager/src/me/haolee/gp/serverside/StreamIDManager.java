package me.haolee.gp.serverside;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class StreamIDManager {
	private static HashMap<String, StreamIDInfo> fileID_streamID_MAP = new HashMap<>();
	//获取流媒体ID
	public static String getStreamID(String fileID) {
		//如果fileID已经存在，说明该直播正在进行，直接取出ID即可
		if(fileID_streamID_MAP.containsKey(fileID)){
			StreamIDInfo streamIDInfo = fileID_streamID_MAP.get(fileID);
			streamIDInfo.incRefCount();//引用+1
			fileID_streamID_MAP.put(fileID, streamIDInfo);//更新键值对
			return streamIDInfo.getStreamID();
		}else{
			//fileID所代表的直播不存在，生成一个新的流ID与之绑定
			String streamID = UUID.randomUUID().toString();
			
			StreamIDInfo streamIDInfo = new StreamIDInfo(streamID);
			
			streamIDInfo.incRefCount();//引用+1
			fileID_streamID_MAP.put(fileID, streamIDInfo);//添加键值对
			return streamID;
		}
	}
	//释放流媒体ID
	public static void releaseStreamID(String fileID) {
		StreamIDInfo streamIDInfo = fileID_streamID_MAP.get(fileID);
		streamIDInfo.decRefCount();
		if(streamIDInfo.getRefCount() == 0)//引用为0，可以删除了
			fileID_streamID_MAP.remove(fileID);
		else//否则更新一下里面的值
			fileID_streamID_MAP.put(fileID, streamIDInfo);
	}
	
}

//流媒体ID以及该ID的引用数目，引用为0则释放
class StreamIDInfo{
	private String streamID = null;
	private int refCount = 0;
	public StreamIDInfo(String id) {
		streamID = id;
	}
	public String getStreamID() {
		return streamID;
	}
	public int getRefCount() {
		return refCount;
	}
	public void incRefCount() {
		refCount ++;
	}
	public void decRefCount() {
		refCount --;
	}
}
