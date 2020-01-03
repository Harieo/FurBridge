package uk.co.harieo.FurBridge.redis.listeners;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import java.util.UUID;
import uk.co.harieo.FurBridge.redis.messages.ReloadPlayerInfoMessage;

public abstract class ReloadPlayerInfoListener extends RedisListener {

	private static final ImmutableList<String> listeningFor = ImmutableList.of(ReloadPlayerInfoMessage.MESSAGE_TYPE);

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

	public abstract void onReloadMessageReceived(UUID uuidToReload);

}
