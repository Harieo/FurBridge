package uk.co.harieo.FurBridge.ranks;

public enum StaticRanks {

	OWNER("ranks.owner"),
	ADMIN("ranks.admin"),
	MODERATOR("ranks.moderator"),
	VIP("ranks.vip"),
	DEFAULT("ranks.default");

	private String permission;

	StaticRanks(String permission) {
		this.permission = permission;
	}

	@Override
	public String toString() {
		return permission;
	}
}
