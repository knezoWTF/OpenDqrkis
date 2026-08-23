package xyz.dqrkis.module.modules.render;

import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.utils.EncryptedString;

public final class HideScoreboard extends Module {
	public HideScoreboard() {
		super(EncryptedString.of("Hide Scoreboard"),
				EncryptedString.of("Hides the sidebar scoreboard"),
				-1,
				Category.RENDER);
	}
}
