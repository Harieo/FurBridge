package uk.co.harieo.FurBridge.players;

import java.util.UUID;
import java.util.regex.Pattern;

public class UniqueIdManipulation {

	private static final Pattern UUID_WITH_HYPHENS = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

	public static UUID uuidFromString(String id) {
		if (id != null) {
			try {
				return UUID.fromString(id);
			} catch (IllegalArgumentException ignored) {
				try {
					return UUID.fromString(UUID_WITH_HYPHENS.matcher(id).replaceAll("$1-$2-$3-$4-$5"));
				} catch (Exception ex) {
					return null;
				}
			}
		} else {
			return null;
		}
	}

	public static String uuidToString(UUID id) {
		return id.toString();
	}

	public static String uuidToShortString(UUID id) {
		return uuidToString(id).replace("-", "");
	}

}
