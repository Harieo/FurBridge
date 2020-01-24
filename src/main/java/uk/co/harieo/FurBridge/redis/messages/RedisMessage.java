package uk.co.harieo.FurBridge.redis.messages;

import com.google.gson.JsonObject;
import redis.clients.jedis.Jedis;
import uk.co.harieo.FurBridge.redis.RedisClient;
import uk.co.harieo.FurBridge.redis.listeners.RedisListener;

public abstract class RedisMessage {

	// These are constant to ensure integrity when receiving as well as sending messages
	public static final String messageTypeKey = "type";
	public static final String versionKey = "version";
	public static final String messageBodyKey = "message";

	private String messageType;
	private int messageVersion;

	private JsonObject messageJson; // The frame JSON with the main information
	private JsonObject messageBody; // An encapsulated message to prevent overlapping primary data

	/**
	 * A message in JSON format to be sent over Redis for the purposes of being received and handled by a {@link RedisListener}
	 *
	 * @param messageType to identify the message when received
	 * @param version of the message, to identify the implementation version of the message
	 */
	public RedisMessage(String messageType, int version) {
		this.messageType = messageType;
		this.messageVersion = version;

		messageJson = new JsonObject();
		messageJson.addProperty(messageTypeKey, messageType);
		messageJson.addProperty(versionKey, version);

		messageBody = new JsonObject();
	}

	/**
	 * This should be edited to include the exclusive message information when implemented
	 *
	 * @return the editable {@link JsonObject} for the message content
	 */
	public JsonObject body() {
		return messageBody;
	}

	/**
	 * @return the message type
	 */
	public String getMessageType() {
		return messageType;
	}

	/**
	 * @return the message version
	 */
	public int getMessageVersion() {
		return messageVersion;
	}

	/**
	 * Publishes this message to Redis by attaching the message body and publishing via {@link Jedis}
	 */
	public void publish() {
		try (Jedis jedis = RedisClient.getPublishResource()) {
			messageJson.add(messageBodyKey,
					messageBody); // Add before publish to make sure it has had time to edit the body
			jedis.publish(RedisClient.CHANNEL, messageJson.toString());
		}
	}

}
