package uk.co.harieo.FurBridge.ranks.redis;

import uk.co.harieo.FurBridge.ranks.Rank;
import uk.co.harieo.FurBridge.ranks.modules.PermissionNode;
import uk.co.harieo.FurBridge.redis.messages.RedisMessage;

public class PermissionMessage extends RedisMessage {

	public static final String TYPE = "permissions-update";
	public static final int VERSION = 1;

	/**
	 * A generic message which represents a permission being updated
	 *
	 * @param rank which is being updated
	 * @param messageType to show what type of update is happening
	 * @param permission which is being updated
	 */
	public PermissionMessage(Rank rank, String messageType, PermissionNode permission) {
		super(TYPE, VERSION);
		body().addProperty("rank-id", rank.getId());
		body().addProperty("message-type", messageType);
		body().addProperty("permission", permission.getPermission());

		body().addProperty("is-allowed", permission.isAllowed());
		body().addProperty("forced", permission.isForced());
	}

}
