package uk.co.harieo.FurBridge.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class InfoTable {

	private static Map<String, InfoTable> cache = new HashMap<>();

	private String tableName;
	private String tableParameters;

	private InfoTable(String tableName, String tableParameters) {
		this.tableName = tableName;
		this.tableParameters = tableParameters;
		cache.put(tableName, this);
	}

	public CompletableFuture<Boolean> createTable() {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement =
							connection.prepareStatement(
									"CREATE TABLE IF NOT EXISTS " + tableName + "(" + tableParameters + ")")) {
				statement.executeUpdate();
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	public CompletableFuture<Boolean> forceCreateTable() {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement =
							connection.prepareStatement("CREATE TABLE " + tableName + "(" + tableParameters + ")")) {
				statement.executeUpdate();
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	public static InfoTable get(String tableName, String tableParameters) {
		if (cache.containsKey(tableName)) {
			return cache.get(tableName);
		} else {
			return new InfoTable(tableName, tableParameters);
		}
	}

	public static InfoTable getFromCache(String tableName) {
		return cache.get(tableName);
	}

}
