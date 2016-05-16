package me.haolee.gp.serverside;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class CategoryListSender {
	/*
	 * 发送分类列表
	 * */
	
	public void sendCategoryList(int mode,
			ObjectOutputStream objectOutputStream) {
		//这个功能使用DatebaseOperation类
		DatebaseQuery datebaseQuery = null;//数据库查询类
		datebaseQuery = new DatebaseQuery();
		ArrayList<String> categoryList = datebaseQuery.getCategoryList(mode);
		///打开序列化输出流
		try {
			objectOutputStream.writeObject(categoryList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
