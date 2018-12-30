package uk.co.harieo.FurBridge.rank;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import uk.co.harieo.FurBridge.sql.FurDB;
import uk.co.harieo.FurBridge.sql.InfoCore;
import uk.co.harieo.FurBridge.sql.InfoTable;

public class RankInfo extends InfoCore {

	private List<Rank> ranks = new ArrayList<>();

	@Override
	public List<InfoTable> getReferencedTables() {
		return Collections.singletonList(InfoTable.get("ranks", "id int primary key, ranks varchar(128) not null"));
	}

	@Override
	protected void load() {
		int playerId = getPlayerInfo().getPlayerId();
		try (Connection connection = FurDB.getConnection();
				PreparedStatement statement =
						connection.prepareStatement("SELECT ranks FROM ranks WHERE id=?")) {
			statement.setInt(1, playerId);
			ResultSet result = statement.executeQuery();

			if (result.next()) {
				ranks.addAll(Rank.deserialise(result.getString(1)));
			} else {
				try (PreparedStatement insertStatement = connection
						.prepareStatement("INSERT INTO ranks VALUES (?,?)")) {
					insertStatement.setInt(1, playerId);
					insertStatement.setString(2, Rank.GUEST.getId());
					insertStatement.executeUpdate();
					ranks.add(Rank.GUEST); // This rank is default and simply prevents having to use null or empty lists
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();

		}
	}

	public CompletableFuture<Boolean> addRank(Rank rankToAdd) {
		if (ranks.contains(rankToAdd)) { // If an SQL update is not necessary, don't waste connections
			System.out.println("Attempt to add rank to " + getPlayerInfo().getName() + " that they already had");
			return CompletableFuture.completedFuture(false);
		}

		ranks.add(rankToAdd); // Adds rank to their cached rank list
		return updateRanks(); // Uploads the updated cache list to the database
	}

	public CompletableFuture<Boolean> removeRank(Rank rankToRemove) {
		if (!ranks.contains(rankToRemove)) { // If an SQL update is not necessary, don't waste connections
			System.out.println("Attempt to remove rank from " + getPlayerInfo().getName() + " that they didn't have");
			return CompletableFuture.completedFuture(false);
		}

		ranks.remove(rankToRemove); // Remove rank from their cached rank list
		return updateRanks(); // Uploads the updated cache list to the database
	}

	private CompletableFuture<Boolean> updateRanks() {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = FurDB.getConnection();
					PreparedStatement statement =
							connection.prepareStatement("UPDATE ranks SET ranks=? WHERE id=?")) {
				statement.setString(1, Rank.serialise(ranks));
				statement.setInt(2, getPlayerInfo().getPlayerId());
				statement.executeUpdate();
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		});
	}

	public List<Rank> getRanks() {
		return ranks;
	}

	public boolean hasPermission(Rank rank) {
		if (ranks.contains(rank)) {
			return true;
		}

		for (Rank each : ranks) {
			if (each.isChild(rank)) {
				return true;
			}
		}

		return false;
	}

	public Rank getPrimaryRank() {
		return ranks.stream().max(Comparator.comparingInt(Enum::ordinal)).orElse(Rank.GUEST);
	}

	public static RankInfo getUnloadedInfo() {
		RankInfo info = new RankInfo();
		info.ranks.add(Rank.GUEST); // Guest is equivalent to no rank and used to prevent system disruption
		return info;
	}

}
