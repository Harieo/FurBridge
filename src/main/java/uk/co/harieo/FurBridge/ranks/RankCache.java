package uk.co.harieo.FurBridge.ranks;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import uk.co.harieo.FurBridge.sql.InfoCore;

public class RankCache {

	private static Cache<UUID, PlayerRankInfo> cachedInfo = CacheBuilder.newBuilder().expireAfterWrite(10,
			TimeUnit.MINUTES).build();

	public static boolean isPresent(UUID uuid) {
		return cachedInfo.getIfPresent(uuid) != null;
	}

	public static PlayerRankInfo getIfPresent(UUID uuid) {
		return cachedInfo.getIfPresent(uuid);
	}

	public static CompletableFuture<PlayerRankInfo> getOrCreate(UUID uuid) {
		if (isPresent(uuid)) {
			return CompletableFuture.completedFuture(getIfPresent(uuid));
		} else {
			return InfoCore.get(PlayerRankInfo.class, uuid);
		}
	}

	static void cache(UUID uuid, PlayerRankInfo info) {
		if (!info.hasErrorOccurred()) {
			cachedInfo.put(uuid, info);
		}
	}

	public static void clearCache() {
		cachedInfo.invalidateAll();
	}

	public static void removeFromCache(UUID uuid) {
		cachedInfo.invalidate(uuid);
	}

}
