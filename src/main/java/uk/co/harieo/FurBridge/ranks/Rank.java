package uk.co.harieo.FurBridge.ranks;

import java.util.HashMap;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;
import uk.co.harieo.FurBridge.ranks.modules.PermissionNode;

public class Rank implements Comparable<Rank> {

	private int id; // Auto Increment id assigned by the database on creation
	private final String rankName; // Unique user-friendly id for front end handling
	private String longPrefix; // Longer prefix to be used with a player
	private String shortPrefix; // Optional shortened prefix to be used with a player
	private int weight = 0; // The amount of weight this rank holds (more weight means more important)
	private int parentRank = -1; // The id of this rank's parent, if it has any
	private boolean isDefault = false; // Whether this rank is inherited by default

	private final Map<String, PermissionNode> permissions = new HashMap<>();

	/**
	 * Creates a rank with assigned id and name to be used to handle user permissions
	 *
	 * @param id of the rank from the database
	 * @param name of the rank
	 */
	public Rank(int id, String name) {
		this.id = id;
		this.rankName = name;
	}

	/**
	 * This constructor should be used only as a template to create a rank as it will assign a null id, which will cause
	 * various errors in practise.
	 *
	 * @param name of the rank
	 */
	public Rank(String name) {
		this(-1, name);
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public String getRankName() {
		return rankName;
	}

	public void setLongPrefix(String longPrefix) {
		this.longPrefix = longPrefix;
	}

	public String getLongPrefix() {
		return ChatColor.translateAlternateColorCodes('&', longPrefix);
	}

	public void setShortPrefix(String shortPrefix) {
		this.shortPrefix = shortPrefix;
	}

	public String getShortPrefix() {
		if (shortPrefix == null) {
			return getLongPrefix(); // Long prefix can't be null and is assumed as fallback
		} else {
			return ChatColor.translateAlternateColorCodes('&', shortPrefix);
		}
	}

	public boolean hasShortPrefix() {
		return shortPrefix != null;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public int getWeight() {
		return weight;
	}

	public void setParentRank(int parentRank) {
		this.parentRank = parentRank;
	}

	public int getParentRankId() {
		return parentRank;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean aDefault) {
		isDefault = aDefault;
	}

	public Map<String, PermissionNode> getPermissions() {
		return permissions;
	}

	@Override
	public int compareTo(Rank o) {
		return Integer.compare(getWeight(), o.getWeight());
	}

}
