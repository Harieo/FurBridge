package uk.co.harieo.FurBridge.rank;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;

public enum Rank {

	GUEST("DEF", null, null, ChatColor.WHITE),
	PATRON("PAT", GUEST, ChatColor.GOLD + ChatColor.BOLD.toString() + "SUB", ChatColor.GOLD),
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

	public String getId() {
		return id;
	}

	public Rank getParent() {
		return parent;
	}

	public String getPrefix() {
		return prefix + " ";
	}

	public boolean hasPrefix() {
		return prefix != null;
	}

	public ChatColor getColor() {
		return color;
	}

	public boolean isChild(Rank compare) {
		return getParent() != null && (getParent() == compare || getParent().isChild(compare));
	}

	@Override
	public String toString() {
		return getId();
	}

	public static Rank getRankById(String value) {
		for (Rank rank : values()) {
			if (rank.getId().equalsIgnoreCase(value.toLowerCase())) {
				return rank;
			}
		}

		return null;
	}

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
