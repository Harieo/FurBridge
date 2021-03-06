package uk.co.harieo.FurBridge.redis.listeners;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import java.util.UUID;
import uk.co.harieo.FurBridge.redis.messages.ReloadPlayerInfoMessage;

public abstract class ReloadPlayerInfoListener extends RedisListener {

	private static final ImmutableSet<String> listeningFor = ImmutableSet.of(ReloadPlayerInfoMessage.MESSAGE_TYPE);

	/**
	 * An instance of {@link RedisListener} to listen for {@link ReloadPlayerInfoMessage}
	 */
	public ReloadPlayerInfoListener() {
		super(listeningFor);
	}

	@Override
	public void onMessage(String messageType, int version, JsonObject message) {
		if (version == ReloadPlayerInfoMessage.MESSAGE_VERSION) { // Make sure the version matches the current system
			try {
				UUID uuid = UUID.fromString(message.get(ReloadPlayerInfoMessage.UUID_KEY).getAsString());
				onReloadMessageReceived(uuid);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("Malformed reload player Redis message", e);
			}
		}
	}

	/**
	 * The method that is called when a message of the correct type is received
	 *
	 * @param uuidToReload received in the message
	 */
	public abstract void onReloadMessageReceived(UUID uuidToReload);

}
