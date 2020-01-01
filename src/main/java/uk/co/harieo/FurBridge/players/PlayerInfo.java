package uk.co.harieo.FurBridge.players;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import uk.co.harieo.FurBridge.sql.FurDB;
import uk.co.harieo.FurBridge.sql.InfoTable;

public class PlayerInfo {

	// If for some reason a player's data changes, it'll be updated in 10 minutes
	// Only successfully loaded instances should meet the cache, unsuccessful ones should be reattempted when needed
	private static Cache<UUID, PlayerInfo> CACHE = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES)
			.build();
	public static final InfoTable TABLE = InfoTable.get("users",
			"id int primary key auto_increment, uuid varchar(128) unique key not null, name varchar(64) not null");

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

	/**
	 * @return the unique number given to this player upon registering them
	 */
	public int getPlayerId() {
		return playerId;
	}

	/**
	 * @return this player's UUID
	 */
	public UUID getUniqueId() {
		return uuid;
	}

	/**
	 * @return the last known name of this player
	 */
	public String getName() {
		return name;
	}

	/**
	 * This should always be checked before using any instance of this class. This will return false if an error
	 * occurred or if the player could not be found. If this does return false, one or more of the other methods will
	 * return null and should never be used.
	 *
	 * @return whether this information was loaded without errors
	 */
	public boolean wasSuccessfullyLoaded() {
		return successfulLoad;
	}

	/**
	 * Retrieves a player's information from the database by their {@link UUID} but will NOT add them to the database if
	 * the information can't be found. Instead, it will return {@link #wasSuccessfullyLoaded()} as false to indicate
	 * that the information couldn't be found.
	 *
	 * It is recommended to always try to use {@link #loadPlayerInfo(String, UUID)} as that will add to the database but
	 * this method can still be used when needed. However, if you use this method then referencing {@link
	 * #wasSuccessfullyLoaded()} is a necessity.
	 *
	 * @param uuid of the player you are searching for
	 * @return the retrieved instance of {@link PlayerInfo}
	 */
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

	/**
	 * Retrieves a player's information from the database by their player name but will NOT add them to the database if
	 * the information can't be found. Instead, it will return {@link #wasSuccessfullyLoaded()} as false to indicate
	 * that the information couldn't be found.
	 *
	 * It is recommended to always try to use {@link #loadPlayerInfo(String, UUID)} as that will add to the database but
	 * this method can still be used when needed. However, if you use this method then referencing {@link
	 * #wasSuccessfullyLoaded()} is a necessity.
	 *
	 * @param playerName of the player you are searching for
	 * @return the retrieved instance of {@link PlayerInfo}
	 */
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

	/**
	 * Loads a player's information from the database or creates new information if none already exists. This should be
	 * used when possible as it registers new players.
	 *
	 * This method is most likely to be able to retrieve a full instance of {@link PlayerInfo} but you should still
	 * reference {@link #wasSuccessfullyLoaded()} as it will show if any errors occurred in loading
	 *
	 * @param playerName of the player you're loading information for
	 * @param uuid of the player you're loading information for
	 * @return the retrieved instance of {@link PlayerInfo}
	 */
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
								System.out.println(
										"An error occurred updating a user's name, system will compensate but this is a severe issue");
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
							PlayerInfo info = new PlayerInfo(insertResult.getInt(1), uuid, playerName, true);
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

	/**
	 * Updates a player's last known name in the database
	 *
	 * @param playerId of the player to update the name of
	 * @param newName to update the record to
	 * @return whether the update was successful
	 */
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
