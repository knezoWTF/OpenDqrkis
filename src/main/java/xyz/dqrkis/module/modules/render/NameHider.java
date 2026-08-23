package xyz.dqrkis.module.modules.render;

import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.StringSetting;
import xyz.dqrkis.utils.EncryptedString;

public final class NameHider extends Module {
	private final StringSetting fakeName = new StringSetting(EncryptedString.of("Fake Name"), EncryptedString.of("Player").toString());
	private final BooleanSetting hideInChat = new BooleanSetting(EncryptedString.of("Hide In Chat"), true);
	private final BooleanSetting hideInTab = new BooleanSetting(EncryptedString.of("Hide In Tab"), true);
	private final BooleanSetting hideNametag = new BooleanSetting(EncryptedString.of("Hide Nametag"), true);

	public NameHider() {
		super(EncryptedString.of("Name Hider"),
				EncryptedString.of("Hides your name everywhere"),
				-1,
				Category.RENDER);
		addSettings(fakeName, hideInChat, hideInTab, hideNametag);
	}

	public static NameHider get() {
		return Dqrkis.INSTANCE.getModuleManager().getModule(NameHider.class);
	}

	public String getFakeName() {
		return fakeName.getValue();
	}

	public boolean shouldHideInChat() {
		return isEnabled() && hideInChat.getValue();
	}

	public boolean shouldHideInTab() {
		return isEnabled() && hideInTab.getValue();
	}

	public boolean shouldHideNametag() {
		return isEnabled() && hideNametag.getValue();
	}

	public String filter(String text) {
		if (!isEnabled() || mc.player == null || text == null)
			return text;

		String ownName = mc.player.getName().getString();
		if (ownName == null || ownName.isEmpty())
			return text;

		return text.contains(ownName) ? text.replace(ownName, fakeName.getValue()) : text;
	}
}
