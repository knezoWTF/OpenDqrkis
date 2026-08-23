package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.MathUtils;

public final class AutoJumpReset extends Module implements TickListener {
	private final NumberSetting chance = new NumberSetting(EncryptedString.of("Chance"), 0, 100, 100, 1);

	public AutoJumpReset() {
		super(EncryptedString.of("Auto Jump Reset"),
				EncryptedString.of("Automatically jumps for you when you get hit so you take less knockback (not good for crystal pvp)"),
				-1,
				Category.COMBAT);
		addSettings(chance);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if(MathUtils.randomInt(1, 100) <= chance.getValueInt()) {
			if (mc.currentScreen != null)
				return;

			if (mc.player.isUsingItem())
				return;

			if (mc.player.hurtTime == 0)
				return;

			if (mc.player.hurtTime == mc.player.maxHurtTime)
				return;

			if (!mc.player.isOnGround())
				return;

			if (mc.player.hurtTime == 9 && MathUtils.randomInt(1, 100) <= chance.getValueInt())
				mc.player.jump();
		}
	}
}
