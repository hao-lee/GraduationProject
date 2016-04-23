package ClientSide;

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