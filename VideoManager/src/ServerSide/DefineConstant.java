package ServerSide;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;

class DefineConstant {
	//用于发送控制指令
	public final static int ACTION_PLAYVOD = 1;
	public final static int ACTION_PLAYLIVE = 2;
	public final static int ACTION_GETCATEGORY = 5;
	public final static int ACTION_REFRESHVIDEOLIST = 6;
	public final static int ACTION_UNDEFINED= 3;
	//用于服务器告诉客户端什么时候可以启动播放线程了
	public final static int WAIT = 7;
	public final static int OK =8;
	//视频模式，直播和点播
	public final static int MODE_LIVE = 9;
	public final static int MODE_VOD = 10;
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

class Config{
	public String readConfig(String key) {
		//读取配置
		String value = null;
		FileInputStream fileInputStream = null;
		InputStream inputStream = null;
		try {
			fileInputStream = new FileInputStream("server.config");
			inputStream = new BufferedInputStream(fileInputStream);
			Properties properties = new Properties();
			properties.load(inputStream);
			value = properties.getProperty(key);
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if(fileInputStream!=null)fileInputStream.close();
				if(inputStream!=null)inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return value;
	}
}