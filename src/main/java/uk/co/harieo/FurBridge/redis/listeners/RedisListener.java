package uk.co.harieo.FurBridge.redis.listeners;

import com.google.gson.JsonObject;
import java.util.Set;
import uk.co.harieo.FurBridge.redis.RedisReceiver;

public abstract class RedisListener {

	private final Set<String> listeningFor;

	/**
	 * A custom listener of specified message types, which will have {@link #onMessage(String, int, JsonObject)} called
	 * when a message of one of the specified types is received in {@link RedisReceiver}
	 *
	 * @param toListenFor types of messages to listen for
	 */
	public RedisListener(Set<String> toListenFor) {
		this.listeningFor = toListenFor;
		RedisReceiver.registerListener(this);
	}

	/**
	 * @return a set of message types to listen for
	 */
	public Set<String> listeningFor() {
		return listeningFor;
	}

	/**
	 * A method which will be called when the {@link RedisReceiver} receives a Redis message of one of the specified
	 * types provided in {@link #listeningFor()}
	 *
	 * @param messageType of the received message
	 * @param version of the received message
	 * @param message body of the received message in the original JSON format
	 */
	public abstract void onMessage(String messageType, int version, JsonObject message);

}
