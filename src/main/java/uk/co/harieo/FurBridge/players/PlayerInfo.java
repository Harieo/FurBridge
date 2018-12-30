package uk.co.harieo.FurBridge.players;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import uk.co.harieo.FurBridge.sql.FurDB;

public class PlayerInfo {

	// If for some reason a player's data changes, it'll be updated in 10 minutes
	// Only successfully loaded instances should meet the cache, unsuccessful ones should be reattempted when needed
	private static Cache<UUID, PlayerInfo> CACHE = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES)
			.build();

	private int playerId;
	private UUID uuid;
	private String name;
	private boolean successfulLoad;

	private PlayerInfo(int playerId, UUID uuid, String playerName, boolean successfulLoad) {
		this.playerId = playerId;
		this.uuid = uuid;
		this.name = playerName;
		this.successfulLoad = successfulLoad;
	}

	public int getPlayerId() {
		return playerId;
	}

	public UUID getUniqueId() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public boolean wasSuccessfullyLoaded() {
		return successfulLoad;
	}

	public static CompletableFuture<PlayerInfo> queryPlayerInfo(UUID uuid) {
		if (CACHE.getIfPresent(uuid) != null) {
			return CompletableFuture.completedFuture(CACHE.getIfPresent(uuid));
		}

		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement =
							connection.prepareStatement("SELECT id,name FROM users WHERE uuid=?")) {
				statement.setString(1, uuid.toString());
				ResultSet result = statement.executeQuery();

				if (result.next()) {
					PlayerInfo info = new PlayerInfo(result.getInt(1), uuid,
							result.getString(2), true);
					CACHE.put(uuid, info);
					return info;
				} else {
					return new PlayerInfo(0, uuid, null, false);
				}
			} catch (SQLException e) {
				e.printStackTrace();
				return new PlayerInfo(0, uuid, null, false);
			}
		});
	}

	public static CompletableFuture<PlayerInfo> queryPlayerInfo(String playerName) {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement =
							connection.prepareStatement("SELECT id,uuid FROM users WHERE name=?")) {
				statement.setString(1, playerName);
				ResultSet result = statement.executeQuery();

				if (result.next()) {
					return new PlayerInfo(result.getInt(1), UUID.fromString(result.getString(2)),
							playerName, true);
				} else {
					return new PlayerInfo(0, null, playerName, false);
				}
			} catch (SQLException e) {
				e.printStackTrace();
				return new PlayerInfo(0, null, playerName, false);
			}
		});
	}

	public static CompletableFuture<PlayerInfo> loadPlayerInfo(String playerName, UUID uuid) {
		if (CACHE.getIfPresent(uuid) != null) {
			return CompletableFuture.completedFuture(CACHE.getIfPresent(uuid));
		}

		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement =
							connection.prepareStatement("SELECT id,name FROM users WHERE uuid=?")) {
				statement.setString(1, uuid.toString());
				ResultSet result = statement.executeQuery();

				if (result.next()) {
					if (!result.getString(2).equals(playerName)) {
						// Done async as this is not worth blocking the thread for
						updateName(result.getInt(1), playerName).whenComplete((success, error) -> {
							if (error != null) {
								error.printStackTrace();
							}

							if (error != null || !success) {
								System.out.println("An error occurred updating a user's name, system will compensate but this is a severe issue");
							}
						});
					}

					PlayerInfo info = new PlayerInfo(result.getInt(1), uuid,
							playerName, true);
					CACHE.put(uuid, info);
					return info;
				} else {
					try (PreparedStatement insertStatement = connection
							.prepareStatement("INSERT INTO users (uuid,name) VALUES (?,?)",
									Statement.RETURN_GENERATED_KEYS)) {
						insertStatement.setString(1, uuid.toString());
						insertStatement.setString(2, playerName);
						insertStatement.executeUpdate();

						ResultSet insertResult = insertStatement.getGeneratedKeys();
						if (insertResult.next()) {
							PlayerInfo info = new PlayerInfo(result.getInt(1), uuid, playerName, true);
							CACHE.put(uuid, info);
							return info;
						} else {
							return new PlayerInfo(0, uuid, playerName, false);
						}
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
				return new PlayerInfo(0, uuid, playerName, false);
			}
		});
	}

	private static CompletableFuture<Boolean> updateName(int playerId, String newName) {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement =
							connection.prepareStatement("UPDATE users SET name=? WHERE id=?")) {
				statement.setString(1, newName);
				statement.setInt(2, playerId);
				statement.executeUpdate();
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

}
