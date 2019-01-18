package uk.co.harieo.FurBridge.sql;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import uk.co.harieo.FurBridge.players.PlayerInfo;

public abstract class InfoCore implements DatabaseHandler {

	private PlayerInfo playerInfo;
	private boolean hasErrorOccurred;

	protected InfoCore() {
		try {
			hasErrorOccurred = !verifyTables().get(); // If success is false, hasErrorOccurred would be true
			if (hasErrorOccurred) {
				System.out.println("An error occurred verifying the rank tables");
			}
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
			hasErrorOccurred = true;
		}
	}

	/**
	 * Sets the {@link #playerInfo} field of this instance
	 *
	 * @param info to set the field to
	 */
	void setPlayerInfo(PlayerInfo info) {
		this.playerInfo = info;
		if (!playerInfo.wasSuccessfullyLoaded()) {
			hasErrorOccurred = true;
		}
	}

	/**
	 * @return the instance of {@link PlayerInfo} that this instance was loaded for
	 */
	public PlayerInfo getPlayerInfo() {
		return playerInfo;
	}

	/**
	 * @return whether an error occurred in loading at any stage
	 */
	public boolean hasErrorOccurred() {
		return hasErrorOccurred;
	}

	/**
	 * Sets whether an error occurred in loading
	 *
	 * @param hasErrorOccurred whether an error occurred
	 */
	protected void setHasErrorOccurred(boolean hasErrorOccurred) {
		this.hasErrorOccurred = hasErrorOccurred;
	}

	/**
	 * Final stage of loading which is handled by the individual system this class is extended over. It will most likely
	 * be for referencing tables specific to the system and other functions specific to the system.
	 */
	protected abstract void load();

	/**
	 * Retrieves an instantiated instance of a class, which extends this class, asynchronously, locally setting necessary
	 * fields with the provided {@link PlayerInfo} and calling {@link #load()} to load the specified info class
	 *
	 * Make sure to reference {@link #hasErrorOccurred()} before using custom functions for safety
	 *
	 * @param infoClass to retrieve an instance of, which conforms to T
	 * @param uuid of the player you are loading information for
	 * @param <T> extends {@link InfoCore}
	 * @return the instantiated instance of this class
	 */
	public static <T extends InfoCore> CompletableFuture<T> get(Class<T> infoClass, UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				T instance = infoClass.newInstance();
				PlayerInfo playerInfo = PlayerInfo.queryPlayerInfo(uuid).get();
				instance.setPlayerInfo(playerInfo);
				instance.load();
				return instance;
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				throw new RuntimeException("Error occurred instantiating an InfoCore");
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException("Error occurred retrieving PlayerInfo for an InfoCore");
			}
		});
	}

	/**
	 * Retrieves an instantiated instance of a class, which extends this class, asynchronously, locally setting necessary
	 * fields with the provided {@link PlayerInfo} and calling {@link #load()} to load the specified info class. This
	 * method is used only when {@link UUID} is not available and an alternative {@link PlayerInfo} can be provided.
	 *
	 * Make sure to reference {@link #hasErrorOccurred()} before using custom functions for safety.
	 *
	 * @param infoClass to retrieve an instance of, which conforms to T
	 * @param playerInfo of the player you are loading information for
	 * @param <T> extends {@link InfoCore}
	 * @return the instantiated instance of this class
	 */
	public static <T extends InfoCore> CompletableFuture<T> get(Class<T> infoClass, PlayerInfo playerInfo) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				T instance = infoClass.newInstance();
				instance.setPlayerInfo(playerInfo);
				instance.load();
				return instance;
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				throw new RuntimeException("Error occurred instantiating an InfoCore");
			}
		});
	}

}
