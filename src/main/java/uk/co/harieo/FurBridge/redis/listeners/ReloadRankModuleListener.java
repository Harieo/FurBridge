package uk.co.harieo.FurBridge.redis.listeners;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import uk.co.harieo.FurBridge.redis.messages.ReloadRankModuleMessage;

public abstract class ReloadRankModuleListener extends RedisListener {

	public ReloadRankModuleListener() {
		super(Sets.newHashSet(ReloadRankModuleMessage.MESSAGE_TYPE));
	}

	@Override
	public void onMessage(String messageType, int version, JsonObject message) {
		if (version == ReloadRankModuleMessage.MESSAGE_VERSION) {
			onRequest();
		}
	}

	public abstract void onRequest();

}
