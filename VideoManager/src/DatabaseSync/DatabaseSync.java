package DatabaseSync;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import CommonPackage.VideoInfo;

public class DatabaseSync {

	
}

class DatabaseAlter{
	private String dbName = "VideoInfo";
	private String dbUsername = "root";
	private String dbPassword = "MyNewPass4!";
	private Connection connection = null;
	private String sql = null;
	private Statement stmt = null;
	private ResultSet resultSet = null;
	private String url = "jdbc:mysql://localhost:3306/";
	private String vodCategoryTable = "vodcategory";
	private String liveCategoryTable = "livecategory";
	private String vodTable = "vod";
	private String liveTable = "live";
	
	/**
	 * 构造函数
	 */
	public DatabaseAlter() {
		Config config = new Config();
		this.dbName = config.readConfig("dbName");
		this.dbUsername = config.readConfig("dbUsername");
		this.dbPassword = config.readConfig("dbPassword");
		this.url = url
				+this.dbName
				+"?user="+this.dbUsername
				+"&password="+this.dbPassword
				+"&useSSL=false";
		this.vodCategoryTable = config.readConfig("vodCategoryTable");
		this.liveCategoryTable = config.readConfig("liveCategoryTable");
		this.vodTable = config.readConfig("vodTable");
		this.liveTable = config.readConfig("liveTable");
	}
	
	/**
	 * 数据库插入,借用一下VideoInfo类，只不过里面的imageByteArray用不着
	 */
	public void insertRecords(VideoInfo videoInfo) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("成功加载MySQL驱动程序");
			connection = DriverManager.getConnection(url);
			stmt = connection.createStatement();
			sql = "";
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if(resultSet!=null)resultSet.close();
				if(stmt!=null)stmt.close();
				if(connection!=null)connection.close();
			} catch (SQLException e) {e.printStackTrace();}
		}//finally
	}
}