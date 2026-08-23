package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.utils.EncryptedString;

public final class NoBreakDelay extends Module {
	public NoBreakDelay() {
		super(EncryptedString.of("No Break Delay"),
				EncryptedString.of("Removes the break delay from mining blocks"),
				-1,
				Category.MISC);
	}
}
