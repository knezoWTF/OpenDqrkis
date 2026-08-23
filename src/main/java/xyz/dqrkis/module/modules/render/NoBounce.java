package xyz.dqrkis.module.modules.render;

import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.utils.EncryptedString;

public final class NoBounce extends Module {
	public NoBounce() {
		super(EncryptedString.of("No Bounce"),
				EncryptedString.of("Removes the crystal bounce"),
				-1,
				Category.RENDER);
	}
}
