package uk.co.harieo.FurBridge.redis.messages;

public class ReloadRankModuleMessage extends RedisMessage {

	public static final String MESSAGE_TYPE = "reload-rank-module";
	public static final int MESSAGE_VERSION = 1;

	public ReloadRankModuleMessage() {
		super(MESSAGE_TYPE, MESSAGE_VERSION);
	}

}
