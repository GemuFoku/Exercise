package session3.exercise;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnectionFactory {
	public static Connection getLocalDBConnection() throws SQLException{
	    String url = "jdbc:oracle:thin:@localhost:1521:xe";
	    String user = "LOCAL";
	    String passwd = "root";
		Connection connection = DriverManager.getConnection(url, user, passwd);
		
		return connection;
	}

}
