package uk.co.harieo.FurBridge.ranks.redis;

import uk.co.harieo.FurBridge.ranks.Rank;
import uk.co.harieo.FurBridge.ranks.modules.PermissionNode;

public class PermissionRemoveMessage extends PermissionMessage {

	public static final String MESSAGE_TYPE = "remove-permission";

	/**
	 * Represents a permission being unset/removed from a rank
	 *
	 * @param rank which the permission is being removed from
	 * @param permission that is being removed
	 */
	public PermissionRemoveMessage(Rank rank, PermissionNode permission) {
		super(rank, MESSAGE_TYPE, permission);
	}

}
