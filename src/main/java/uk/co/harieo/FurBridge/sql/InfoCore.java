package uk.co.harieo.FurBridge.sql;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import uk.co.harieo.FurBridge.players.PlayerInfo;

public abstract class InfoCore implements DatabaseHandler {

	private PlayerInfo playerInfo;
	private boolean hasErrorOccurred;

	protected InfoCore() {
		try {
			hasErrorOccurred = !verifyTables().get(); // If success is false, hasErrorOccurred would be true
			load();
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
			hasErrorOccurred = true;
		}
	}

	void setPlayerInfo(PlayerInfo info) {
		this.playerInfo = info;
		if (!playerInfo.wasSuccessfullyLoaded()) {
			hasErrorOccurred = true;
			throw new IllegalArgumentException("Passed a failed PlayerInfo load to an InfoCore");
		}
	}

	protected PlayerInfo getPlayerInfo() {
		return playerInfo;
	}

	public boolean hasErrorOccurred() {
		return hasErrorOccurred;
	}

	protected void setHasErrorOccurred(boolean hasErrorOccurred) {
		this.hasErrorOccurred = hasErrorOccurred;
	}

	protected abstract void load();

	public static <T extends InfoCore> CompletableFuture<T> get(Class<T> infoClass, PlayerInfo info) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				T instance = infoClass.newInstance();
				instance.setPlayerInfo(info);
				return instance;
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				throw new RuntimeException("Error occurred instantiating an InfoCore");
			}
		});
	}

}
