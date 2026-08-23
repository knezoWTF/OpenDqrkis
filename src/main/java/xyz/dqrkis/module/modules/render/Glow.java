package xyz.dqrkis.module.modules.render;

import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.utils.EncryptedString;

public final class Glow extends Module {
	public Glow() {
		super(EncryptedString.of("Glow"),
				EncryptedString.of("Makes entities glow so you can see them through walls"),
				-1,
				Category.RENDER);
	}
}
