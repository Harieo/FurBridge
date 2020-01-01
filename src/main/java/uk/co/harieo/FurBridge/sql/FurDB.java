package uk.co.harieo.FurBridge.sql;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class FurDB {

	private static final String path = "/home/container/deployment-v2/db.json";
	private static Properties properties;
	private static String database = "minecraft";

	/**
	 * @return an open connection to the MySQL database
	 * @throws SQLException if an error occurs in the connection
	 */
	public static Connection getConnection() throws SQLException {
		verifyIntegrity();
		return DriverManager
				.getConnection("jdbc:mysql://" + properties.getProperty("address") + "/" + database + "?useSSL=false",
						properties);
	}

	/**
	 * Attempts to read and load information from the configuration file which contains all necessary connection
	 * information for the MySQL database
	 *
	 * @throws RuntimeException if the configuration file isn't valid
	 */
	private static void verifyIntegrity() throws RuntimeException {
		if (properties == null) {
			File file = new File(path);
			String error;
			if (file.exists()) {
				try (FileReader reader = new FileReader(file)) {
					JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();
					if (!jsonObject.has("username") || !jsonObject.has("password") || !jsonObject.has("address")) {
						error = "Database configuration is improperly formatted!";
					} else {
						String username = jsonObject.get("username").getAsString();
						String password = jsonObject.get("password").getAsString();
						String address = jsonObject.get("address").getAsString();

						properties = new Properties();
						properties.put("user", username);
						properties.put("password", password);
						properties.put("address", address);

						if (jsonObject.has("database")) {
							database = jsonObject.get("database").getAsString();
						}

						return;
					}
				} catch (IOException e) {
					throw new RuntimeException("Failed to retrieve Redis properties", e);
				}
			} else {
				error = "Database configuration file does not exist at " + path;
			}

			throw new RuntimeException(error);
		}
	}

}
