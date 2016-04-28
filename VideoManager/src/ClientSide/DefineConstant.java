package ClientSide;

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