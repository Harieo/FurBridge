package uk.co.harieo.FurBridge.ranks.modules;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import uk.co.harieo.FurBridge.ranks.Rank;
import uk.co.harieo.FurBridge.sql.FurDB;

public class RankDatabaseHandler {

	private RankModule module;

	RankDatabaseHandler(RankModule module) {
		this.module = module;
	}

	/**
	 * Create a new rank group in the database based on a {@link Rank} template
	 *
	 * @param toBeCreated containing all the values to be added
	 * @return whether the update was successful
	 */
	public CompletableFuture<Boolean> createRank(Rank toBeCreated) {
		if (!module.wasLoadedSuccessfully()) {
			throw new IllegalStateException("Attempted to handle a malfunctioning RankModule");
		} else if (toBeCreated == null) {
			throw new NullPointerException("Can't create a null rank"); // Likely a logic error
		} else if (module.getRank(toBeCreated.getRankName()) != null) {
			return CompletableFuture.completedFuture(false); // Likely user error
		}

		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement =
							connection.prepareStatement(
									"INSERT INTO ranks (rank_name, long_prefix, short_prefix, weight, parent_rank, is_default) VALUES (?,?,?,?,?,?)",
									Statement.RETURN_GENERATED_KEYS)) {
				statement.setString(1, toBeCreated.getRankName());

				// Short prefixes can be null if the rank will only be using the long prefix
				String longPrefix = toBeCreated.getLongPrefix();
				String shortPrefix = toBeCreated.getShortPrefix();

				if (longPrefix.length() > 32 || (shortPrefix != null && shortPrefix.length() > 32)) {
					return false; // This would violate max char constraints
				}

				statement.setString(2, longPrefix);
				if (shortPrefix == null) {
					statement.setNull(3, Types.VARCHAR);
				} else {
					statement.setString(3, toBeCreated.getShortPrefix());
				}

				statement.setInt(4, toBeCreated.getWeight());
				statement.setInt(5, toBeCreated.getParentRankId());
				statement.setBoolean(6, toBeCreated.isDefault());
				statement.executeUpdate();

				ResultSet generatedKeys = statement.getGeneratedKeys();
				int key;
				if (generatedKeys.next()) {
					key = generatedKeys.getInt(1);
				} else {
					return false; // If the entry didn't get a key, we can't fulfil the request as ranks require keys
				}

				toBeCreated.setId(key); // Sets the database generated id for use in code
				module.addRank(toBeCreated.getRankName(), toBeCreated);
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	/**
	 * Removes a rank from the database and the cache, along with any of its foreign keys and dependencies. This is a
	 * dangerous procedure as it will affect the permissions of any player which owns it.
	 *
	 * @param id of the rank to be deleted
	 * @return whether the update was successful
	 */
	public CompletableFuture<Boolean> deleteRank(int id) {
		if (!module.wasLoadedSuccessfully()) {
			throw new IllegalStateException("Attempted to handle a malfunctioning RankModule");
		} else if (module.getRank(id) == null) {
			return CompletableFuture.completedFuture(false); // Likely user error
		} else if (id < 0) {
			return CompletableFuture.completedFuture(false); // Likelihood of both logic and user error, assume user
		}

		return CompletableFuture.supplyAsync(() -> {
			// Ranks are foreign keys for any rank-handling database and must be handled first before primarily deletion
			try (Connection connection = FurDB.getConnection();
					PreparedStatement playerRanksStatement =
							connection.prepareStatement("DELETE FROM player_ranks WHERE rank_id=?")) {
				playerRanksStatement.setInt(1, id);
				playerRanksStatement.executeUpdate();

				try (PreparedStatement permissionNodesStatement =
						connection.prepareStatement("DELETE FROM permission_nodes WHERE rank_id=?")) {
					permissionNodesStatement.setInt(1, id);
					permissionNodesStatement.executeUpdate();

					try (PreparedStatement ranksStatement =
							connection.prepareStatement("DELETE FROM ranks WHERE id=?")) {
						ranksStatement.setInt(1, id);
						ranksStatement.executeUpdate();

						module.deleteRank(id);
						return true;
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	/**
	 * Sets a permission node by either adding it to the database or editing the 'allowed' value accordingly.
	 *
	 * @param toBeEdited rank to be edited
	 * @param permission to add or change the value of
	 * @param isAllowed whether the permission is allowed or denied
	 * @return whether the update was successful
	 */
	public CompletableFuture<Boolean> setPermissionNode(Rank toBeEdited, String permission, boolean isAllowed) {
		if (!module.wasLoadedSuccessfully()) {
			throw new IllegalStateException("Attempted to handle a malfunctioning RankModule");
		} else if (toBeEdited == null) {
			return CompletableFuture.completedFuture(false); // Likely user error
		}

		permission = permission.toLowerCase(); // For safety to prevent logic error
		String finalPermission = permission;

		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection()) {
				Map<String, Boolean> permissions = toBeEdited.getPermissions();
				String statementString;
				Map<Integer, Object> parameters = new HashMap<>();

				if (permissions.containsKey(finalPermission)) {
					if (permissions.get(finalPermission) == isAllowed) {
						return false; // Everything is already as it should be, why is this call being made?
					} else {
						statementString = "UPDATE " + RankModule.PERMISSIONS_TABLE.getTableName()
								+ " SET allowed=? WHERE rank_id=? AND permission=?";
						parameters.put(1, isAllowed);
						parameters.put(2, toBeEdited.getId());
						parameters.put(3, finalPermission);
					}
				} else {
					statementString = "INSERT INTO " + RankModule.PERMISSIONS_TABLE.getTableName()
							+ " (rank_id,permission,allowed) VALUES (?,?,?)";
					parameters.put(1, toBeEdited.getId());
					parameters.put(2, finalPermission);
					parameters.put(3, isAllowed);
				}

				try (PreparedStatement statement = connection.prepareStatement(statementString)) {
					// Put all the values where they are expected
					for (int parameterOrdinal : parameters.keySet()) {
						Object rawValue = parameters.get(parameterOrdinal);
						if (rawValue instanceof String) {
							statement.setString(parameterOrdinal, (String) rawValue);
						} else if (rawValue instanceof Integer) {
							statement.setInt(parameterOrdinal, (int) rawValue);
						} else if (rawValue instanceof Boolean) {
							statement.setBoolean(parameterOrdinal, (boolean) rawValue);
						} else {
							throw new IllegalStateException("A value of unknown local type was passed");
						}
					}

					// Make sure the cached value is absolutely correct
					permissions.remove(finalPermission);
					permissions.put(finalPermission, isAllowed);

					statement.executeUpdate();
					return true;
				}
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	/**
	 * Removes all record of a permission node from the database, effectively revoking it under normal circumstances.
	 * This is useful if you set an exclusion that you no longer which to have in place.
	 *
	 * @param toBeEdited the rank which owns the permission node
	 * @param permission node to remove
	 * @return whether the update was successful
	 */
	public CompletableFuture<Boolean> removePermissionNode(Rank toBeEdited, String permission) {
		if (!module.wasLoadedSuccessfully()) {
			throw new IllegalStateException("Attempted to handle a malfunctioning RankModule");
		} else if (toBeEdited == null) {
			return CompletableFuture.completedFuture(false); // Likely user error
		}

		permission = permission.toLowerCase(); // For safety to prevent logic error
		String finalPermission = permission;

		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement = connection.prepareStatement(
							"DELETE FROM " + RankModule.PERMISSIONS_TABLE.getTableName()
									+ " WHERE rank_id=? AND permission=?")) {
				statement.setInt(1, toBeEdited.getId());
				statement.setString(2, finalPermission);
				toBeEdited.getPermissions().remove(finalPermission); // Update cache
				statement.executeUpdate();
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	/**
	 * Edits the record for a rank where the database value is a generic integer, such as weight or parent rank id.
	 *
	 * @param toBeEdited which rank should be edited in the database
	 * @param valueToEdit which value are we editing
	 * @param value to set to
	 * @return whether the update was successful
	 */
	public CompletableFuture<Boolean> setIntegerValue(Rank toBeEdited, GenericIntegerValue valueToEdit, int value) {
		if (!module.wasLoadedSuccessfully()) {
			throw new IllegalStateException("Attempted to handle a malfunctioning RankModule");
		} else if (toBeEdited == null) {
			return CompletableFuture.completedFuture(false); // Likely user error
		}

		String valueName;
		Consumer<Integer> cacheUpdate; // To be performed after database update (for thread safety)

		switch (valueToEdit) {
			case PARENT:
				valueName = "parent_rank";
				cacheUpdate = toBeEdited::setParentRank;
				break;
			case WEIGHT:
				valueName = "weight";
				cacheUpdate = toBeEdited::setWeight;
				break;
			default:
				throw new IllegalStateException(
						"Attempted to set an integer value to rank that hasn't been coded properly!");
		}

		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement =
							connection.prepareStatement("UPDATE " + RankModule.RANKS_TABLE.getTableName()
									+ " SET " + valueName + "=? WHERE id=?")) {
				if (value < 0) { // This should never actually be true for a real value
					statement.setNull(1, Types.INTEGER);
				} else {
					statement.setInt(1, value);
				}
				statement.setInt(2, toBeEdited.getId());
				statement.executeUpdate();
				cacheUpdate.accept(value); // Update the cache generically
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	/**
	 * Sets whether the specified rank is a default rank
	 *
	 * @param toBeEdited rank to change the default of
	 * @param isDefault to set the value to
	 * @return whether the update was successful
	 */
	public CompletableFuture<Boolean> setDefault(Rank toBeEdited, boolean isDefault) {
		if (!module.wasLoadedSuccessfully()) {
			throw new IllegalStateException("Attempted to handle a malfunctioning RankModule");
		} else if (toBeEdited == null) {
			return CompletableFuture.completedFuture(false); // Likely user error
		} else if (toBeEdited.isDefault() == isDefault) {
			return CompletableFuture.completedFuture(true); // Already completed
		}

		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement = connection.prepareStatement(
							"UPDATE " + RankModule.RANKS_TABLE.getTableName() + " SET is_default=? WHERE id=?")) {
				statement.setBoolean(1, isDefault);
				statement.setInt(2, toBeEdited.getId());
				statement.executeUpdate();
				toBeEdited.setDefault(isDefault);
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	/**
	 * Edits the long prefix of a rank
	 *
	 * @param toBeEdited rank to set the prefix for
	 * @param newPrefix to set the prefix to
	 * @return whether the update was successful
	 */
	public CompletableFuture<Boolean> setLongPrefix(Rank toBeEdited, String newPrefix) {
		return setPrefix(toBeEdited, newPrefix, true);
	}

	/**
	 * Edits the long short of a rank
	 *
	 * @param toBeEdited rank to set the prefix for
	 * @param newPrefix to set the prefix to
	 * @return whether the update was successful
	 */
	public CompletableFuture<Boolean> setShortPrefix(Rank toBeEdited, String newPrefix) {
		return setPrefix(toBeEdited, newPrefix, false);
	}

	/**
	 * Sets the either short or long prefix of a rank in the database.
	 *
	 * @param toBeEdited rank to edit the prefix of
	 * @param newPrefix to set the prefix to
	 * @param isLongPrefix whether we're editing the long prefix (false means short prefix)
	 * @return whether the update was successful
	 */
	private CompletableFuture<Boolean> setPrefix(Rank toBeEdited, String newPrefix, boolean isLongPrefix) {
		if (!module.wasLoadedSuccessfully()) {
			throw new IllegalStateException("Attempted to handle a malfunctioning RankModule");
		} else if (toBeEdited == null) {
			return CompletableFuture.completedFuture(false); // Likely user error
		} else if (newPrefix.length() > 32) { // Also vicarious test of whether newPrefix is null
			return CompletableFuture.completedFuture(false);
		}

		return CompletableFuture.supplyAsync(() -> {
			String parameterName = isLongPrefix ? "long_prefix" : "short_prefix";
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement = connection.prepareStatement(
							"UPDATE " + RankModule.RANKS_TABLE.getTableName() + " SET " + parameterName + "=? WHERE id=?")) {
				statement.setString(1, newPrefix);
				statement.setInt(2, toBeEdited.getId());
				statement.executeUpdate();
				if (isLongPrefix) {
					toBeEdited.setLongPrefix(newPrefix);
				} else {
					toBeEdited.setShortPrefix(newPrefix);
				}
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	/**
	 * Used with {@link #setIntegerValue(Rank, GenericIntegerValue, int)} to identify which value is actually being
	 * edited in the database.
	 */
	public enum GenericIntegerValue {
		WEIGHT, PARENT
	}

}
