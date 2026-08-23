package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.mixin.MinecraftClientAccessor;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.item.BlockItem;

public final class FastPlace extends Module implements TickListener {
	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 4, 0, 1)
			.setDescription(EncryptedString.of("Ticks between placements"));

	private int placeIn;

	public FastPlace() {
		super(EncryptedString.of("Fast Place"),
				EncryptedString.of("Removes the delay when holding right click with blocks"),
				-1,
				Category.MISC);
		addSettings(delay);
	}

	@Override
	public void onEnable() {
		placeIn = 0;
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
		if (mc.player == null || mc.interactionManager == null)
			return;

		if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem))
			return;

		if (!mc.options.useKey.isPressed())
			return;

		if (placeIn > 0) {
			placeIn--;
			return;
		}

		((MinecraftClientAccessor) mc).setItemUseCooldown(0);
		placeIn = delay.getValueInt();
	}
}
