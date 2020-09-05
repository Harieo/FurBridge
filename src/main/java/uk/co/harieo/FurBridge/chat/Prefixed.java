package uk.co.harieo.FurBridge.chat;

import net.md_5.bungee.api.ChatColor;

public interface Prefixed {

	String getPrefix();

	default String formatMessage(String message) {
		return ChatColor.translateAlternateColorCodes('&', getPrefix() + message);
	}

}
