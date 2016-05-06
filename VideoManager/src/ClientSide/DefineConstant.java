package ClientSide;

class DefineConstant {
	//用于发送控制指令
	//点播模式不与服务器交互，所以不需要请求码
	public static final int ACTION_PLAYLIVE = 2;
	public static final int ACTION_GETCATEGORY = 5;
	public static final int ACTION_GETVIDEOLIST = 6;
	//用于服务器告诉客户端什么时候可以启动播放线程了
	public static final int WAIT = 7;
	public static final int OK =8;
	//视频模式，直播和点播
	public static final int MODE_LIVE = 9;
	public static final int MODE_VOD = 10;
}