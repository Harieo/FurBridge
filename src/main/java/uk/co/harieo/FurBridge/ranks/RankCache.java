package uk.co.harieo.FurBridge.ranks;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import uk.co.harieo.FurBridge.sql.InfoCore;

public class RankCache {

	private static final Cache<UUID, PlayerRankInfo> cachedInfo =
			CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();

	public static boolean isPresent(UUID uuid) {
		return cachedInfo.getIfPresent(uuid) != null;
	}

	/**
	 * Gets a player's rank information from the cache if it is present. Note: This is not thread safe and {@link
	 * #getOrCreate(UUID)} should be used instead where possible.
	 *
	 * @param uuid of the player
	 * @return the rank info or null if not present
	 */
	public static PlayerRankInfo getIfPresent(UUID uuid) {
		return cachedInfo.getIfPresent(uuid);
	}

	/**
	 * Gets rank information from the cache or loads it into the cache if not present
	 *
	 * @param uuid of the player
	 * @return a non-null instance of {@link PlayerRankInfo} for the player
	 */
	public static CompletableFuture<PlayerRankInfo> getOrCreate(UUID uuid) {
		if (isPresent(uuid)) {
			return CompletableFuture.completedFuture(getIfPresent(uuid));
		} else {
			return InfoCore.get(PlayerRankInfo.class, uuid);
		}
	}

	/**
	 * Adds information to the cache if an error has not occurred
	 *
	 * @param uuid of the player the information belongs to
	 * @param info the loaded information
	 */
	static void cache(UUID uuid, PlayerRankInfo info) {
		if (!info.hasErrorOccurred()) {
			cachedInfo.put(uuid, info);
		}
	}

	/**
	 * Clears the cache of all loaded player information
	 */
	public static void clearCache() {
		cachedInfo.invalidateAll();
	}

	/**
	 * Removes a specific player's information from the cache if it exists
	 *
	 * @param uuid to remove information for
	 */
	public static void removeFromCache(UUID uuid) {
		cachedInfo.invalidate(uuid);
	}

}
