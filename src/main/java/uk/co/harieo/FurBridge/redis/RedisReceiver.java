package uk.co.harieo.FurBridge.redis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import redis.clients.jedis.JedisPubSub;
import uk.co.harieo.FurBridge.redis.listeners.RedisListener;
import uk.co.harieo.FurBridge.redis.messages.RedisMessage;

public class RedisReceiver extends JedisPubSub {

	private static RedisReceiver instance = new RedisReceiver();

	private JsonParser parser = new JsonParser();
	private List<RedisListener> listeners = new ArrayList<>();

	@Override
	public void onMessage(String channel, String message) {
		if (channel.equals(RedisClient.CHANNEL)) {
			JsonObject json = parser.parse(message).getAsJsonObject();
			String messageType = json.get(RedisMessage.messageTypeKey).getAsString();

			for (RedisListener listener : listeners) { // Loop through registered listeners
				if (listener.listeningFor().contains(messageType)) { // If the listener is listening for this type
					int messageVersion = json.get(RedisMessage.versionKey).getAsInt();
					JsonObject messageBody = json.get(RedisMessage.messageBodyKey).getAsJsonObject();
					listener.onMessage(messageType, messageVersion, messageBody); // Call implementation
				}
			}
		}
	}

	/**
	 * Registers a listener of specified message types with custom implementations
	 *
	 * @param listener to be registered
	 */
	public static void registerListener(RedisListener listener) {
		instance.listeners.add(listener);
	}

	public static RedisReceiver getInstance() {
		return instance;
	}

}
