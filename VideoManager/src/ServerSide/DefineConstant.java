package ServerSide;

import java.util.HashSet;

public class DefineConstant {
	//用于发送控制指令
	public final static int PLAYVIDEO = 1;
	public final static int GETVIDEOLIST = 2;
	public final static int STOPVTHREAD = 3;
	public final static int GETVIDEOSTATUS = 4;
	//用于服务器告诉客户端什么时候可以启动播放线程了
	public final static int WAIT = 5;
	public final static int OK =6;
}

final class MountPoint{
	private static int MAX = 65535;
	private static int stream = 0;
	private static HashSet<Integer> streamNameSet = new HashSet<>();
	
	//获取可用的数据流名字
	public static int getStreamName() {
		while(true){
			stream = (stream+1)%MAX;
			if(streamNameSet.add(stream))
				return stream;
			if (streamNameSet.size() == MAX)
				return -1;//如果集合满了说明当前达到了视频流数量的极限，返回-1
		}
	}
	//释放用完的数据流名字
	public static void releaseStreamName(int sname) {
		streamNameSet.remove(sname);
	}
}//class MountPoint