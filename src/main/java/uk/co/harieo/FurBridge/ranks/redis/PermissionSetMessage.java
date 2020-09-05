package uk.co.harieo.FurBridge.ranks.redis;

import uk.co.harieo.FurBridge.ranks.Rank;
import uk.co.harieo.FurBridge.ranks.modules.PermissionNode;

public class PermissionSetMessage extends PermissionMessage {

	public static final String MESSAGE_TYPE = "set-permission";

	/**
	 * Represents a permission being added or its boolean being set
	 *
	 * @param rank which is being updated
	 * @param permission which is being set
	 */
	public PermissionSetMessage(Rank rank, PermissionNode permission) {
		super(rank, MESSAGE_TYPE, permission);
	}

}
