package me.haolee.gp.common;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
	public static Properties properties = null;
	
	/**
	 * 读取配置文件
	 * @param configFileName 配置文件名字
	 */
	public static void readConfigFile(String configFileName) {
		//读取配置文件
		FileInputStream fileInputStream = null;
		InputStream inputStream = null;
		try {
			fileInputStream = new FileInputStream(configFileName);
			inputStream = new BufferedInputStream(fileInputStream);
			properties = new Properties();
			properties.load(inputStream);
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
	}
	
	/**
	 * 
	 * @param key 键值
	 * @param defaultValue 如果查询不到，则要使用的默认value
	 * @return 返回对应的value
	 */
	public static String getValue(String key, String defaultValue) {
		return properties.getProperty(key);
	}
}
