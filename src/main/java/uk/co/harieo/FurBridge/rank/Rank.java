package uk.co.harieo.FurBridge.rank;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;

public enum Rank {

	GUEST("DEF", null, null, ChatColor.WHITE),
	PATRON("PAT", GUEST, ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "PATRON", ChatColor.LIGHT_PURPLE),
	MODERATOR("MOD", PATRON, ChatColor.DARK_GREEN + ChatColor.BOLD.toString() + "MOD", ChatColor.DARK_GREEN),
	ADMINISTRATOR("ADM", MODERATOR, ChatColor.RED + ChatColor.BOLD.toString() + "ADMIN", ChatColor.RED);

	private String id;
	private Rank parent;
	private String prefix;
	private ChatColor color;

	Rank (String id, Rank parent, String prefix, ChatColor rankColor) {
		this.id = id;
		this.parent = parent;
		this.prefix = prefix;
		this.color = rankColor;
	}

	/**
	 * @return the unique identifier of this rank, used in the database
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the parent rank
	 */
	public Rank getParent() {
		return parent;
	}

	/**
	 * @return the prefix of this rank with an appended space to separate it from a player's name
	 */
	public String getPrefix() {
		return prefix + " ";
	}

	/**
	 * @return whether this rank has a prefix
	 */
	public boolean hasPrefix() {
		return prefix != null;
	}

	/**
	 * @return the colour of this rank
	 */
	public ChatColor getColour() {
		return color;
	}

	/**
	 * Checks whether this rank is a child of the specified rank
	 *
	 * @param compare to check
	 * @return whether this rank is a child of the specified rank
	 */
	public boolean isChild(Rank compare) {
		return getParent() != null && (getParent() == compare || getParent().isChild(compare));
	}

	@Override
	public String toString() {
		return getId();
	}

	/**
	 * Retrieves the Rank with {@link #getId()} matching the specified string, ignoring case
	 *
	 * @param value to compare with all Rank enums
	 * @return the matching Rank or null if none match
	 */
	public static Rank getRankById(String value) {
		for (Rank rank : values()) {
			if (rank.getId().equalsIgnoreCase(value.toLowerCase())) {
				return rank;
			}
		}

		return null;
	}

	/**
	 * Serialises a list of ranks into a String based on their {@link #getId()} value
	 *
	 * @param ranks to be serialised into a single String
	 * @return the serialised String
	 */
	public static String serialise(List<Rank> ranks) {
		if (!ranks.isEmpty()) {
			StringBuilder builder = new StringBuilder(128); // 128 chars is the SQL table capacity
			builder.append(ranks.get(0)); // 0 will exist as the list is not empty
			for (int i = 1; i < ranks.size(); i++) {
				builder.append(":");
				builder.append(ranks.get(i).getId());
			}
			return builder.toString();
		} else {
			return "";
		}
	}

	/**
	 * Deserialises a serialised list of Ranks
	 *
	 * @param serialisedString containing a list of ranks
	 * @return the list of ranks from the serialised string
	 */
	public static List<Rank> deserialise(String serialisedString) {
		List<Rank> list = new ArrayList<>();

		String[] rawRanks = serialisedString.split(":");
		for (String rawRank : rawRanks) {
			Rank rank = getRankById(rawRank);
			if (rank == null) {
				throw new NullPointerException("A rank was deserialised but not recognised");
			}
			list.add(rank);
		}

		return list;
	}

}
