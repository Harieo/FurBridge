package uk.co.harieo.FurBridge.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class FurDB {

	private static final String databaseName = System.getProperty("DatabaseName", "minecraft");
	private static final String username = System.getProperty("DatabaseUser", "FurCore");
	private static final String password = System.getProperty("DatabasePassword");

	public static Connection getConnection() throws SQLException {
		Properties properties = new Properties();
		if (password == null) {
			throw new IllegalArgumentException("Password is null");
		}
		properties.put("user", username);
		properties.put("password", password);

		return DriverManager.getConnection("jdbc:mysql://localhost:3306/" + databaseName, properties);
	}

}
