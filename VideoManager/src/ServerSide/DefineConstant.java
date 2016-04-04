package ServerSide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefineConstant {
	public final static int PLAYVIDEO = 1;
	public final static int GETVIDEOLIST = 2;
	public final static int STOPVTHREAD = 3;
	public final static int GETVIDEOSTATUS = 4;
}

final class MountPoint{
	private static List<String> mountPointList = 
			Collections.synchronizedList(new ArrayList<>());
	private static List<String> usedPointList = 
			Collections.synchronizedList(new ArrayList<>());
	
	//初始化挂载点存储列表
	public static void initMountPointList() {
		mountPointList.add("1.sdp");
		mountPointList.add("2.sdp");
		mountPointList.add("3.sdp");
	}
	
	//一次只允许一个线程前来取得挂载点，这个函数称作临界区，是线程安全的
	synchronized public static String getMountPoint() {
		if(mountPointList.isEmpty())
			return "";
		else{
			String mp = mountPointList.remove(0);
			usedPointList.add(mp);
			return mp;
		}
	}//getMountPoint
	
	//释放挂载点，同理必须是线程安全的
	synchronized public static boolean releaseMountPoint(String mp){
		if(usedPointList.remove(mp)){
			mountPointList.add(mp);//将挂载点释放到资源库中
			return true;
		}else{//仅仅是为了以防万一
			System.out.println("\nFatal Error:Can't Release MountPoint!\n");
			return false;
		}
	}//releaseMountPoint
	
}//class MountPoint