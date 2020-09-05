package uk.co.harieo.FurBridge.ranks.redis;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import java.util.Map;
import uk.co.harieo.FurBridge.ranks.Rank;
import uk.co.harieo.FurBridge.ranks.modules.PermissionNode;
import uk.co.harieo.FurBridge.ranks.modules.RankModule;
import uk.co.harieo.FurBridge.redis.listeners.RedisListener;

public abstract class PermissionListener extends RedisListener {

	private final RankModule rankModule;

	/**
	 * A listener which listens for any {@link PermissionMessage} from Redis
	 *
	 * @param rankModule which is currently active and up to date
	 */
	public PermissionListener(RankModule rankModule) {
		super(Sets.newHashSet(PermissionMessage.TYPE));
		this.rankModule = rankModule;
	}

	@Override
	public void onMessage(String type, int version, JsonObject message) {
		String messageType = message.get("message-type").getAsString();
		String permission = message.get("permission").getAsString();

		boolean isAllowed = false;
		if (message.has("is-allowed")) {
			isAllowed = message.get("is-allowed").getAsBoolean();
		}

		boolean forced = false;
		if (message.has("forced")) {
			forced = message.get("forced").getAsBoolean();
		}

		int rankId = message.get("rank-id").getAsInt();
		Rank rank = rankModule.getRank(rankId);
		if (rank == null) {
			for (Rank excludedRank : rankModule.getExcludedRanks()) {
				if (excludedRank.getId() == rankId) {
					PermissionNode node = excludedRank.getPermissions().get(permission);

					boolean wasForced = node != null && node.isForced(); // Node isn't forced if it doesn't exist
					boolean isRemoving = messageType.equals(PermissionRemoveMessage.MESSAGE_TYPE);
					if (forced || wasForced || isRemoving) { // If it is forced or the forced status changed to false
						rank = excludedRank;
						if (wasForced && !forced) { // If it was forced and is not forced now
							messageType = PermissionRemoveMessage.MESSAGE_TYPE; // Remove the permission
						}
					}
				}
			}

			if (rank == null) {
				System.out.println("Failed to handle permissions update for unrecognised rank id: " + rankId);
				return; // We can't handle this without knowing the rank it's for
			}
		}

		updateCache(rank, messageType, permission, isAllowed, forced);
		onMessage(rank, messageType, permission, isAllowed);
	}

	/**
	 * Updates the cached permissions in the provided {@link RankModule} to reflect this permissions change
	 *
	 * @param rank to be updated
	 * @param messageType the type of message which was sent
	 * @param permission which has been updated
	 * @param isAllowed whether the permission is true or false
	 */
	private void updateCache(Rank rank, String messageType, String permission, boolean isAllowed, boolean forced) {
		Map<String, PermissionNode> permissions = rank.getPermissions();
		if (messageType.equals(PermissionSetMessage.MESSAGE_TYPE)) {
			PermissionNode node;
			if (permissions.containsKey(permission)) {
				node = permissions.get(permission);
				node.setAllowed(isAllowed);
				node.setForce(forced);
			} else {
				node = new PermissionNode(permission, isAllowed, forced);
			}

			permissions.put(permission, node);
			System.out.println(
					"Updated the permission " + permission + " to " + isAllowed + " for " + rank.getRankName());
		} else if (messageType.equals(PermissionRemoveMessage.MESSAGE_TYPE)) {
			permissions.remove(permission);
			System.out.println("Removed the permission " + permission + " from " + rank.getRankName());
		} else {
			System.out.println("Received unhandled permission message of type: " + messageType);
		}
	}

	/**
	 * An abstract method to allow custom implementations of this listener
	 *
	 * @param rank to be updated
	 * @param messageType the type of message which was sent
	 * @param permission which has been updated
	 * @param isAllowed whether the permission is true or false
	 */
	public abstract void onMessage(Rank rank, String messageType, String permission, boolean isAllowed);

}
