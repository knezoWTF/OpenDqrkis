package xyz.dqrkis.module;

import xyz.dqrkis.utils.EncryptedString;

public enum Category {
	COMBAT(EncryptedString.of("Combat")),
	MISC(EncryptedString.of("Misc")),
	RENDER(EncryptedString.of("Render")),
	CLIENT(EncryptedString.of("Client")),
	CART(EncryptedString.of("Cart PvP"));
	public final CharSequence name;

	Category(CharSequence name) {
		this.name = name;
	}
}
