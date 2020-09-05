package uk.co.harieo.FurBridge.players;

import java.util.UUID;
import org.shanerx.mojang.Mojang;

public class MojangLookup {

	private static final MojangLookup instance = new MojangLookup();

	private final Mojang api;

	private MojangLookup() {
		api = new Mojang().connect();
	}

	public Mojang getApi() {
		return api;
	}

	public UUID lookupUniqueId(String playerName) {
		try {
			return UniqueIdManipulation.uuidFromString(getApi().getUUIDOfUsername(playerName));
		} catch (Exception ignored) {
			return null; // API is likely down
		}
	}

	public static MojangLookup getInstance() {
		return instance;
	}

}
