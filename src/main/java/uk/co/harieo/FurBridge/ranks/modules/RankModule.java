package uk.co.harieo.FurBridge.ranks.modules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import uk.co.harieo.FurBridge.ranks.Rank;
import uk.co.harieo.FurBridge.sql.FurDB;
import uk.co.harieo.FurBridge.sql.InfoTable;

/**
 * This class retrieves all dynamics ranks from the database and all their associated information, such as prefixes and
 * weight. Once loaded, this class will be required by other rank handling classes as a basis for all information on
 * what ranks are available and the information attached to them.
 */
public class RankModule {

	public static final InfoTable RANKS_TABLE = InfoTable.get("ranks", "id int primary key auto_increment, "
			+ "rank_name varchar(64) unique key, long_prefix varchar(32) not null, short_prefix varchar(32), weight int not null, "
			+ "parent_rank int, is_default tinyint(1) not null DEFAULT 0");
	public static final InfoTable PERMISSIONS_TABLE = InfoTable
			.get("permission_nodes",
					"rank_id int, permission varchar(128), allowed tinyint(1), FOREIGN KEY (rank_id) REFERENCES ranks(id)");

	private Map<String, Rank> loadedRanks = new HashMap<>();
	private boolean wasLoadedSuccessfully = true;
	private RankDatabaseHandler databaseHandler;

	private RankModule() {
		databaseHandler = new RankDatabaseHandler(this);
	}

	/**
	 * Adds a rank to the cache but not to the database
	 *
	 * @param rankName of the rank
	 * @param rank to be added
	 */
	void addRank(String rankName, Rank rank) {
		loadedRanks.putIfAbsent(rankName, rank);
	}

	/**
	 * Deletes a rank from the cache but not from the database
	 *
	 * @param rankId of the rank to be deleted
	 */
	void deleteRank(int rankId) {
		String key = null;
		for (Rank rank : loadedRanks.values()) {
			if (rank.getId() == rankId) {
				key = rank.getRankName();
				break; // Cannot be a duplicate key, no point continuing
			}
		}

		if (key != null) {
			loadedRanks.remove(key);
		}
	}

	/**
	 * @return an instance of {@link RankDatabaseHandler} which is handling this module
	 */
	public RankDatabaseHandler getDatabaseHandler() {
		return databaseHandler;
	}

	/**
	 * Gets a rank by its numerical id
	 *
	 * @param rankId of the rank to find
	 * @return the rank or null of none are found
	 */
	public Rank getRank(int rankId) {
		for (Rank rank : loadedRanks.values()) {
			if (rank.getId() == rankId) {
				return rank;
			}
		}

		return null;
	}

	/**
	 * Gets a rank by its rank name
	 *
	 * @param rankName of the rank to find
	 * @return the rank or null of none are found
	 */
	public Rank getRank(String rankName) {
		return loadedRanks.get(rankName);
	}

	/**
	 * @return all ranks which this module has retrieved
	 */
	public Map<String, Rank> getLoadedRanks() {
		return loadedRanks;
	}

	/**
	 * @return whether the loading of this module was successful
	 */
	public boolean wasLoadedSuccessfully() {
		return wasLoadedSuccessfully;
	}

	/**
	 * Returns all permission this rank owns, including those of its parents
	 *
	 * @param rank to retrieve permissions for
	 * @return a list of all applicable permissions
	 */
	public Map<String, Boolean> getAllPermissions(Rank rank) {
		if (rank.getParentRankId() < 0) {
			return rank.getPermissions();
		} else {
			Map<String, Boolean> permissions = new HashMap<>(rank.getPermissions()); // Adds the base rank's permissions

			Rank parent = getRank(rank.getParentRankId());
			while (parent != null && parent.getParentRankId() >= 0) { // While the next parent exists
				Map<String, Boolean> parentPermissions = parent.getPermissions();
				for (String permission : parentPermissions.keySet()) {
					permissions.putIfAbsent(permission,
							parentPermissions.get(permission)); // Add permission from current parent to list
				}
				parent = getRank(parent.getParentRankId()); // Move to the next rank
			}

			return permissions;
		}
	}

	/**
	 * @return a blank module which reports an error on instantiation
	 */
	private static RankModule createErroneousModule() {
		RankModule module = new RankModule();
		module.wasLoadedSuccessfully = false;
		return module;
	}

	/**
	 * Loads all ranks from the database, including metadata and the permissions they own
	 *
	 * @return the module containing all ranks in existence
	 */
	public static CompletableFuture<RankModule> loadModule() {
		return CompletableFuture.supplyAsync(() -> {
			// Make sure both the required tables exist in the context, with blocking to prevent thread issues
			try {
				if (!RANKS_TABLE.createTable().get() || !PERMISSIONS_TABLE.createTable().get()) {
					throw new RuntimeException("Couldn't verify the required tables for the rank module");
				}
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				return createErroneousModule();
			}

			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement = connection
							.prepareStatement("SELECT * FROM " + RANKS_TABLE.getTableName())) {
				ResultSet result = statement.executeQuery();
				RankModule module = new RankModule();

				while (result.next()) {
					Rank rank = new Rank(result.getInt(1), result.getString(2));

					// Set all rank data
					rank.setLongPrefix(result.getString(3));

					String shortPrefix = result.getString(4);
					if (shortPrefix != null) {
						rank.setShortPrefix(shortPrefix);
					}

					int weight = result.getInt(5);
					if (weight > -1) {
						rank.setWeight(weight);
					}

					int parentId = result.getInt(6);
					if (parentId >= 0) {
						rank.setParentRank(parentId);
					}

					// Add ranks to the list
					module.loadedRanks.putIfAbsent(rank.getRankName(), rank);
					// Adds the rank name as a permission so that it can be simply referenced via Spigot/BungeeCord
					rank.getPermissions().put("ranks." + rank.getRankName(), true);

					// Load all the permission nodes that this rank owns
					try (PreparedStatement permissionStatement = connection.prepareStatement(
							"SELECT permission,allowed FROM " + PERMISSIONS_TABLE.getTableName() + " WHERE rank_id=?")) {
						permissionStatement.setInt(1, rank.getId());
						ResultSet permissionResult = permissionStatement.executeQuery();

						while (permissionResult.next()) {
							rank.getPermissions()
									.putIfAbsent(permissionResult.getString(1), permissionResult.getBoolean(2));
						}
					}

					rank.setDefault(result.getBoolean(7));
				}

				return module;
			} catch (SQLException e) {
				e.printStackTrace();
				return createErroneousModule();
			}
		});
	}

}
