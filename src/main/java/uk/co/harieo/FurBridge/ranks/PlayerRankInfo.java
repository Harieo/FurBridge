package uk.co.harieo.FurBridge.ranks;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import uk.co.harieo.FurBridge.ranks.modules.RankModule;
import uk.co.harieo.FurBridge.sql.FurDB;
import uk.co.harieo.FurBridge.sql.InfoCore;
import uk.co.harieo.FurBridge.sql.InfoTable;

/**
 * This class is an asynchronous {@link InfoCore} which loads and handles all of a player's ranks/permissions with the
 * database. This core should only be loaded if there is a valid {@link RankModule} because most methods will not
 * function without one.
 *
 * Make sure to always use {@link #injectModule()} before calling other methods or you will receive an exception. On top
 * of this, make sure you are not injecting a failed module or else you will experience major issues.
 */
public class PlayerRankInfo extends InfoCore {

	private static RankModule rankModule;
	private static Cache<Integer, List<Integer>> cache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES).build();

	private List<Integer> rawRanks = new ArrayList<>(); // Rank ids that haven't been compared to a rank module
	private List<Rank> ranks = new ArrayList<>();  // Ranks that have been pulled from a rank module
	private List<Rank> confirmedParents = new ArrayList<>(); // Ranks that this player indirectly owns in the hierarchy

	@Override
	protected void load() {
		List<Integer> cachedRawRanks = cache.getIfPresent(getPlayerInfo().getPlayerId());
		if (cachedRawRanks != null) {
			rawRanks = cachedRawRanks;
			injectModule();
		} else {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement = connection
							.prepareStatement("SELECT rank_id FROM player_ranks WHERE player_id=?")) {
				statement.setInt(1, getPlayerInfo().getPlayerId());
				ResultSet result = statement.executeQuery();

				// Adds the raw rank ids in preparation for a rank module being provided
				while (result.next()) {
					rawRanks.add(result.getInt(1));
				}

				injectModule(); // Compare raw ranks to loaded ones
				cache.put(getPlayerInfo().getPlayerId(), rawRanks); // Cache this to spare resources
				RankCache.cache(getPlayerInfo().getUniqueId(), this);
			} catch (SQLException e) {
				e.printStackTrace();
				setHasErrorOccurred(true);
			}
		}
	}

	public static void setRankModule(RankModule module) {
		rankModule = module;
	}

	/**
	 * Injects a fully functioning {@link RankModule} which this class will use to convert its raw rank data into fully
	 * instantiated data, primarily {@link Rank} which contains proper permission data.
	 *
	 * Attempting to inject a module which was not successfully loaded will result in an exception. Make sure to utilise
	 * {@link RankModule#wasLoadedSuccessfully()} to ensure correct error handling.
	 *
	 * @return a list of all the ranks this player owns after injection is complete
	 */
	public List<Rank> injectModule() {
		if (!rankModule.wasLoadedSuccessfully()) {
			throw new IllegalArgumentException("Attempted to insert a malfunctioning rank module");
		}

		ranks.clear(); // Safety in-case of duplicate injection

		for (int rankId : rawRanks) {
			Rank rank = rankModule.getRank(rankId);
			if (rank != null) { // Possible if a rank is deleted without proper attention
				ranks.add(rank);
			}
		}

		for (Rank rank : rankModule.getLoadedRanks().values()) {
			if (rank.isDefault()) { // Adds default ranks as all players are entitled to them
				ranks.add(rank);
			}
		}

		return ranks;
	}

	/**
	 * @return whether a {@link RankModule} has been injected into this class
	 */
	public boolean isInjectedWithModule() {
		return rankModule != null;
	}

	/**
	 * Gives this player the specified rank and updates the database accordingly
	 *
	 * @param rank to add to this player
	 * @return whether the async update was successful
	 */
	public CompletableFuture<Boolean> addRank(Rank rank) {
		if (rank == null) {
			throw new NullPointerException("Attempted to add null rank to player");
		} else if (ranks.contains(rank)) {
			return CompletableFuture.completedFuture(false); // Returns a soft error as this is likely a user error
		}

		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement =
							connection.prepareStatement("INSERT INTO player_ranks (player_id,rank_id) VALUES (?,?)")) {
				statement.setInt(1, getPlayerInfo().getPlayerId());
				statement.setInt(2, rank.getId());
				statement.executeUpdate();

				rawRanks.add(rank.getId());
				ranks.add(rank);
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	/**
	 * Removes the specified rank from this player and updates the database accordingly
	 *
	 * @param rank to remove from this player
	 * @return whether the async update was successful
	 */
	public CompletableFuture<Boolean> removeRank(Rank rank) {
		if (rank == null) {
			throw new NullPointerException("Attempted to add null rank to player");
		} else if (!ranks.contains(rank)) {
			return CompletableFuture.completedFuture(false); // Returns a soft error as this is likely a user error
		}

		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement =
							connection.prepareStatement("DELETE FROM player_ranks WHERE player_id=? AND rank_id=?")) {
				statement.setInt(1, getPlayerInfo().getPlayerId());
				statement.setInt(2, rank.getId());
				statement.executeUpdate();

				rawRanks.remove(rank.getId());
				ranks.remove(rank);
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	/**
	 * @return a list of cached ranks, only available after injecting a proper rank module
	 */
	public List<Rank> getRanks() {
		return ranks;
	}

	/**
	 * Checks whether this player posses the given rank, or higher
	 *
	 * @param rank to check the possession of
	 * @return whether the player posses this rank or one if its children
	 */
	public boolean hasRank(Rank rank) {
		verifyInjection();
		if (ranks.contains(rank)) {
			return true;
		} else if (confirmedParents.contains(rank)) {
			return true;
		} else {
			// Scan all ranks this player has and see if that rank is a child of the parameter rank
			for (Rank possessedRank : ranks) {
				Rank parent = rankModule.getRank(possessedRank.getParentRankId());
				while (parent != null) {
					if (parent.getId() == rank.getId()) {
						confirmedParents.add(rank); // Caches to save doing this loop again
						return true;
					} else {
						parent = rankModule.getRank(parent.getParentRankId()); // Move on to next parent
					}
				}
			}
			return false;
		}
	}

	/**
	 * Retrieves all permissions that this player is entitled to, including that of all parent ranks accordingly
	 *
	 * @return a map of all string permission nodes and whether the permission is allowed (true) or denied (false)
	 */
	public Map<String, Boolean> getAllPermissions() {
		verifyInjection();
		Map<String, Boolean> permissions = new HashMap<>();
		for (Rank rank : ranks) {
			// Developer note: The RankModule adds the standard permission when it is loaded so don't do it here
			Map<String, Boolean> permissionsMap = rankModule.getAllPermissions(rank);
			for (String permission : permissionsMap.keySet()) {
				permissions.putIfAbsent(permission, permissionsMap.get(permission));
			}
		}
		return permissions;
	}

	/**
	 * Retrieves the rank with the highest weight from the list of owned ranks
	 *
	 * @return the heaviest rank by highest weight
	 */
	public Rank getHeaviestRank() {
		verifyInjection();
		List<Rank> ranks = new ArrayList<>(this.ranks);
		Collections.sort(ranks);
		if (!ranks.isEmpty()) {
			return ranks.get(ranks.size() - 1);
		} else {
			throw new IllegalStateException("Call for heaviest rank when there are no ranks");
		}
	}

	/**
	 * Clears all loaded data from this class. This should only be used if this class is being deleted or re-loaded.
	 */
	public void clear() {
		rawRanks.clear();
		ranks.clear();
		confirmedParents.clear();
	}

	/**
	 * Verifies whether a {@link RankModule} has been injected into this class or throws an exception otherwise
	 */
	private void verifyInjection() {
		if (rankModule == null) {
			throw new IllegalStateException("Failed to verify injection of valid rank module");
		}
	}

	@Override
	public List<InfoTable> getReferencedTables() {
		return Collections.singletonList(InfoTable
				.get("player_ranks", "player_id int, rank_id int, FOREIGN KEY (rank_id) REFERENCES ranks(id)"));
	}

}
