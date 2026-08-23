package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class AutoWeb extends Module implements TickListener {
	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 20, 2, 1)
			.setDescription(EncryptedString.of("Ticks between web placements"));
	private final BooleanSetting onlyHoldingWebs = new BooleanSetting(EncryptedString.of("Only Holding Webs"), true)
			.setDescription(EncryptedString.of("Only places webs while holding cobwebs"));

	private int placeIn;

	public AutoWeb() {
		super(EncryptedString.of("Auto Web"),
				EncryptedString.of("Places webs at enemies feet automatically"),
				-1,
				Category.COMBAT);
		addSettings(delay, onlyHoldingWebs);
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
		if (mc.player == null || mc.world == null || mc.currentScreen != null)
			return;

		if (placeIn > 0) {
			placeIn--;
			return;
		}

		if (onlyHoldingWebs.getValue() && !mc.player.getMainHandStack().isOf(Items.COBWEB))
			return;

		BlockPos pos = mc.player.getBlockPos().down();
		if (!mc.world.getBlockState(pos).isAir())
			return;

		BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
		mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
		mc.player.swingHand(Hand.MAIN_HAND);
		placeIn = delay.getValueInt();
	}
}
