package uk.co.harieo.FurBridge.ranks;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RankCache {

	private static Cache<UUID, PlayerRankInfo> cachedInfo = CacheBuilder.newBuilder().expireAfterWrite(10,
			TimeUnit.MINUTES).build();

	public static boolean isPresent(UUID uuid) {
		return cachedInfo.getIfPresent(uuid) != null;
	}

	public static PlayerRankInfo getIfPresent(UUID uuid) {
		return cachedInfo.getIfPresent(uuid);
	}

	static void cache(UUID uuid, PlayerRankInfo info) {
		if (!info.hasErrorOccurred()) {
			cachedInfo.put(uuid, info);
		}
	}

}
