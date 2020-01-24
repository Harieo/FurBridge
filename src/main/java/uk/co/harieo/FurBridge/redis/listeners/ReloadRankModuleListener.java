package uk.co.harieo.FurBridge.redis.listeners;

import com.google.gson.JsonObject;
import java.util.Collections;
import uk.co.harieo.FurBridge.redis.messages.ReloadRankModuleMessage;

public abstract class ReloadRankModuleListener extends RedisListener {

	public ReloadRankModuleListener() {
		super(Collections.singletonList(ReloadRankModuleMessage.MESSAGE_TYPE));
	}

	@Override
	public void onMessage(String messageType, int version, JsonObject message) {
		if (version == ReloadRankModuleMessage.MESSAGE_VERSION) {
			onRequest();
		}
	}

	public abstract void onRequest();

}
