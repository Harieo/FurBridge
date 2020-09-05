package uk.co.harieo.FurBridge.ranks.redis;

import uk.co.harieo.FurBridge.ranks.PlayerRankInfo;
import uk.co.harieo.FurBridge.redis.messages.RedisMessage;

public class RankUpdateMessage extends RedisMessage {

	public static final String TYPE = "rank-update";
	public static final int VERSION = 1;

	public RankUpdateMessage(PlayerRankInfo playerRankInfo) {
		super(TYPE, VERSION);
		body().addProperty("system", "rank");
		body().addProperty("player-id", playerRankInfo.getPlayerInfo().getPlayerId());
	}

}
