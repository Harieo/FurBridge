package uk.co.harieo.FurBridge.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class InfoTable {

	private static Map<String, InfoTable> cache = new HashMap<>(); // Stores instances of this class based on the table name

	private String tableName;
	private String tableParameters;

	private InfoTable(String tableName, String tableParameters) {
		this.tableName = tableName;
		this.tableParameters = tableParameters;
		cache.put(tableName, this);
	}

	/**
	 * Creates a table if the table does not already exist based on the specified name and parameters
	 *
	 * @return whether the table was verified as created
	 */
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

	/**
	 * Creates a table but does not check if it already exists, should only be used if there is no doubt the table
	 * doesn't exist already or else an {@link SQLException} will be thrown
	 *
	 * @return whether the table was created successfully
	 */
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

	/**
	 * @return the assigned name of this table
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * Gets an instance of {@link InfoTable} from the cache or creates on if it is not cached
	 *
	 * @param tableName of the table
	 * @param tableParameters of the table
	 * @return the instance of {@link InfoTable}
	 */
	public static InfoTable get(String tableName, String tableParameters) {
		if (cache.containsKey(tableName)) {
			return cache.get(tableName);
		} else {
			return new InfoTable(tableName, tableParameters);
		}
	}

	/**
	 * Gets an instance of {@link InfoTable} from the cache but does not create it if it is not cached
	 *
	 * @param tableName of the table
	 * @return the cached {@link InfoTable} instance or null if no instance was found
	 */
	public static InfoTable getFromCache(String tableName) {
		return cache.get(tableName);
	}

}
