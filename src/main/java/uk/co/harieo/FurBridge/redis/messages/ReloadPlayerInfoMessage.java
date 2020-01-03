package uk.co.harieo.FurBridge.redis.messages;

import java.util.UUID;

public class ReloadPlayerInfoMessage extends RedisMessage {

	public static final String MESSAGE_TYPE = "reload-player-info";
	public static final int MESSAGE_VERSION = 1;
	public static final String UUID_KEY = "uuid";

	/**
	 * A message which requests that a player's information be reloaded in all systems
	 *
	 * @param uuid of the player
	 */
	public ReloadPlayerInfoMessage(UUID uuid) {
		super(MESSAGE_TYPE, MESSAGE_VERSION);
		body().addProperty(UUID_KEY, uuid.toString());
	}

}
