package uk.co.harieo.FurBridge.ranks.modules;

public class PermissionNode {

	private final String node;
	private boolean allow;
	private boolean force;

	/**
	 * Represents a permission node with the standard permissible and boolean used in both Spigot and BungeeCord, as well
	 * as a force value to bypass rank exclusions.
	 *
	 * @param permission the node
	 * @param allow whether the permission is allowed
	 * @param force whether the permission should ignore exclusions
	 */
	public PermissionNode(String permission, boolean allow, boolean force) {
		this.node = permission;
		this.allow = allow;
		this.force = force;
	}

	/**
	 * @return the permission node
	 */
	public String getPermission() {
		return node;
	}

	/**
	 * @return whether the permission is allowed
	 */
	public boolean isAllowed() {
		return allow;
	}

	/**
	 * Sets the value
	 *
	 * @param allow sets {@link #isAllowed()}
	 */
	public void setAllowed(boolean allow) {
		this.allow = allow;
	}

	/**
	 * @return whether this permission bypasses exclusions
	 */
	public boolean isForced() {
		return force;
	}

	/**
	 * Sets the value
	 *
	 * @param force sets {@link #isForced()}
	 */
	public void setForce(boolean force) {
		this.force = force;
	}

}
