package DatabaseSync;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utils {

}


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