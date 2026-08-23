package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class AntiWeb extends Module implements TickListener {
	private final BooleanSetting swing = new BooleanSetting(EncryptedString.of("Swing Hand"), true)
			.setDescription(EncryptedString.of("Swings the hand when breaking webs"));
	private final BooleanSetting breakAbove = new BooleanSetting(EncryptedString.of("Break Above Head"), true)
			.setDescription(EncryptedString.of("Also breaks the web above your head"));

	public AntiWeb() {
		super(EncryptedString.of("Anti Web"),
				EncryptedString.of("Breaks webs around you instantly"),
				-1,
				Category.COMBAT);
		addSettings(swing, breakAbove);
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
		if (mc.player == null || mc.world == null || mc.interactionManager == null)
			return;

		BlockPos feet = mc.player.getBlockPos();

		if (mc.world.getBlockState(feet).isOf(Blocks.COBWEB)) {
			mc.interactionManager.attackBlock(feet, Direction.UP);
			if (swing.getValue())
				mc.player.swingHand(Hand.MAIN_HAND);
		}

		if (breakAbove.getValue()) {
			BlockPos above = feet.up();
			if (mc.world.getBlockState(above).isOf(Blocks.COBWEB))
				mc.interactionManager.attackBlock(above, Direction.UP);
		}
	}
}
