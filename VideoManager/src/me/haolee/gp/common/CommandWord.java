package me.haolee.gp.common;

public enum CommandWord {
	//请求命令字
	REQUEST_STREAMINGMEDIA,
	REQUEST_CATEGORYLIST,
	REQUEST_VIDEOLIST,
	
	//应答命令字
	RESPONSE_DATA,
	RESPONSE_IDLE,
	RESPONSE_CONTINUE,

	//连接控制字，表示数据已经发送完毕
	CTRL_END,
	CTRL_HARTBEAT,
	
	//播放模式
	MODE_LIVE,
	MODE_VOD
}
