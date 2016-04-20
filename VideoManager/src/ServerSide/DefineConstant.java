package ServerSide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class DefineConstant {
	public final static int PLAYVIDEO = 1;
	public final static int GETVIDEOLIST = 2;
	public final static int STOPVTHREAD = 3;
	public final static int GETVIDEOSTATUS = 4;
}

final class MountPoint{
	private static int MAX = 65535;
	private static int stream = 0;
	private static HashSet<Integer> streamNameSet = new HashSet<>();
	
	//
	public static int getStreamName() {
		while(true){
			stream = (stream+1)%MAX;
			if(streamNameSet.add(stream))
				return stream;
			if (streamNameSet.size() == MAX)
				return -1;//如果集合满了说明当前达到了视频流数量的极限，返回-1
		}
	}
}//class MountPoint