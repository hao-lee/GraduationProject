package me.haolee.gp.common;

/**
 * 这是一个客户端和服务端的约定
 * 获取分类时的请求：reqCode + mode
 * 获取视频列表的请求：requestCode + mode + category + start + step
 * 直播请求：requestCode + fileRelativePath
 * @author mirage
 *
 */
public class Command {
	//用于发送控制指令
	//点播模式不与服务器交互，所以不需要请求码
	public static final int ACTION_PLAYLIVE = 1;
	public static final int ACTION_GETCATEGORY = 2;
	public static final int ACTION_GETVIDEOLIST = 3;
	//视频模式，直播和点播
	public static final int MODE_LIVE = 4;
	public static final int MODE_VOD = 5;
	//应答控制字
	public static final int CTRL_WAIT = 6;
	public static final int CTRL_OK = 7;
}