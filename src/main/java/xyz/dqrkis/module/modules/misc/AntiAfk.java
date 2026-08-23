package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.util.math.MathHelper;

public final class AntiAfk extends Module implements TickListener {
	private final NumberSetting interval = new NumberSetting(EncryptedString.of("Interval"), 20, 600, 100, 10)
			.setDescription(EncryptedString.of("Ticks between view rotations"));

	private int ticks;

	public AntiAfk() {
		super(EncryptedString.of("Anti AFK"),
				EncryptedString.of("Prevents you from being kicked for being AFK by rotating your view"),
				-1,
				Category.MISC);
		addSettings(interval);
	}

	@Override
	public void onEnable() {
		ticks = 0;
		eventManager.add(TickListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		ticks = 0;
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null)
			return;

		if (++ticks < interval.getValueInt())
			return;

		ticks = 0;
		float yaw = mc.player.getYaw() + (float) (Math.random() * 10.0 - 5.0);
		float pitch = MathHelper.clamp(mc.player.getPitch() + (float) (Math.random() * 10.0 - 5.0), -90.0F, 90.0F);
		mc.player.setYaw(yaw);
		mc.player.setPitch(pitch);
	}
}
