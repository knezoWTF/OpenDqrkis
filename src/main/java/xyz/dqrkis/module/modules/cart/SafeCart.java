package xyz.dqrkis.module.modules.cart;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.KeybindSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.InventoryUtils;
import xyz.dqrkis.utils.KeyUtils;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class SafeCart extends Module implements TickListener {
	private static final int STATE_PLACE_RAIL = 0;
	private static final int STATE_RAIL_DELAY = 1;
	private static final int STATE_PLACE_CART = 2;
	private static final int STATE_CART_DELAY = 3;
	private static final int STATE_PLACE_LOG = 4;
	private static final int STATE_LOG_DELAY = 5;
	private static final int STATE_START_CHARGE = 6;
	private static final int STATE_CHARGING = 7;

	private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), 1, false)
			.setDescription(EncryptedString.of("Hold this button to run the macro"));
	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 10, 0, 1);
	private final NumberSetting bowCharge = new NumberSetting(EncryptedString.of("Bow Charge"), 3, 20, 8, 1);

	private BlockPos railPos;
	private BlockPos logPos;
	private int state = STATE_PLACE_RAIL;
	private int delayTicks;
	private int chargeTicks;

	public SafeCart() {
		super(EncryptedString.of("Safe Cart"),
				EncryptedString.of("Places rail, minecart, and oak log"),
				-1,
				Category.CART);
		addSettings(activateKey, delay, bowCharge);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		resetState();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		if (mc.options != null) {
			mc.options.useKey.setPressed(false);
			if (mc.player != null && mc.interactionManager != null)
				mc.interactionManager.stopUsingItem(mc.player);
		}
		resetState();
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.currentScreen != null)
			return;

		boolean pressed = activateKey.getKey() != -1 && KeyUtils.isKeyPressed(activateKey.getKey());

		if (!pressed && state < STATE_START_CHARGE) {
			resetState();
			return;
		}

		if (delayTicks > 0) {
			delayTicks--;
			return;
		}

		mc.execute(this::step);
	}

	private void step() {
		if (mc.player == null || mc.world == null)
			return;

		int delayValue = delay.getValueInt();

		switch (state) {
			case STATE_PLACE_RAIL -> {
				if (tryPlaceRail()) {
					if (delayValue > 0) {
						state = STATE_RAIL_DELAY;
						delayTicks = delayValue;
					} else {
						state = STATE_PLACE_CART;
					}
				}
			}
			case STATE_RAIL_DELAY -> state = STATE_PLACE_CART;
			case STATE_PLACE_CART -> {
				if (tryPlaceMinecart()) {
					if (delayValue > 0) {
						state = STATE_CART_DELAY;
						delayTicks = delayValue;
					} else {
						state = STATE_PLACE_LOG;
					}
				} else {
					resetState();
				}
			}
			case STATE_CART_DELAY -> state = STATE_PLACE_LOG;
			case STATE_PLACE_LOG -> {
				if (tryPlaceLog()) {
					if (delayValue > 0) {
						state = STATE_LOG_DELAY;
						delayTicks = delayValue;
					} else {
						state = STATE_START_CHARGE;
					}
				} else {
					resetState();
				}
			}
			case STATE_LOG_DELAY -> state = STATE_START_CHARGE;
			case STATE_START_CHARGE -> {
				if (selectBow()) {
					state = STATE_CHARGING;
					chargeTicks = 0;
					pressUse();
					aimAtCart();
				} else {
					resetState();
				}
			}
			case STATE_CHARGING -> {
				aimAtCart();
				chargeTicks++;
				if (chargeTicks >= bowCharge.getValueInt()) {
					releaseUse();
					resetState();
				}
			}
		}
	}

	private boolean tryPlaceRail() {
		if (!(mc.crosshairTarget instanceof BlockHitResult blockHit) || mc.crosshairTarget.getType() != HitResult.Type.BLOCK)
			return false;

		BlockPos hitPos = blockHit.getBlockPos();
		BlockPos placePos = hitPos.offset(blockHit.getSide());

		if (logPos != null && placePos.equals(logPos))
			return false;

		if (mc.world.getBlockState(hitPos).isOf(Blocks.OAK_LOG) || mc.world.getBlockState(placePos).isOf(Blocks.OAK_LOG))
			return false;

		if (isRail(placePos)) {
			railPos = placePos;
			return true;
		}

		if (!mc.world.getBlockState(placePos).isReplaceable())
			return false;

		if (!InventoryUtils.hasItemInHotbar(item ->
				item == Items.RAIL || item == Items.POWERED_RAIL
						|| item == Items.DETECTOR_RAIL || item == Items.ACTIVATOR_RAIL))
			return false;

		ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
		if (result.isAccepted()) {
			mc.player.swingHand(Hand.MAIN_HAND);
			railPos = placePos;
			return true;
		}
		return false;
	}

	private boolean tryPlaceMinecart() {
		if (railPos == null || !isRail(railPos))
			return false;

		if (!InventoryUtils.selectItemFromHotbar(Items.TNT_MINECART))
			return false;

		BlockHitResult hit = new BlockHitResult(railPos.toCenterPos(), Direction.UP, railPos, false);
		ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
		if (result.isAccepted()) {
			mc.player.swingHand(Hand.MAIN_HAND);
			return true;
		}
		return false;
	}

	private boolean tryPlaceLog() {
		if (railPos == null || mc.player == null)
			return false;

		Vec3d playerPos = mc.player.getEntityPos();
		Vec3d center = railPos.toCenterPos();
		Vec3d away = center.subtract(playerPos).normalize();
		double distance = playerPos.distanceTo(center);
		BlockPos logTarget = new BlockPos(
				(int) Math.floor(playerPos.x + away.x * Math.min(distance * 0.6D, 3.0D)),
				railPos.getY(),
				(int) Math.floor(playerPos.z + away.z * Math.min(distance * 0.6D, 3.0D)));

		if (logTarget.equals(railPos))
			logTarget = railPos.add(-(int) Math.round(away.x), 0, -(int) Math.round(away.z));

		BlockPos belowLog = logTarget.down();
		boolean foundSupport = mc.world.getBlockState(belowLog).isSolidBlock(mc.world, belowLog);

		if (!foundSupport) {
			for (Direction direction : Direction.Type.HORIZONTAL) {
				BlockPos candidate = railPos.offset(direction);
				BlockPos candidateBelow = candidate.down();
				if (mc.world.getBlockState(candidateBelow).isSolidBlock(mc.world, candidateBelow)
						&& (mc.world.getBlockState(candidate).isAir() || mc.world.getBlockState(candidate).isReplaceable())) {
					logTarget = candidate;
					belowLog = candidateBelow;
					foundSupport = true;
					break;
				}
			}

			if (!foundSupport)
				return false;
		}

		if (!mc.world.getBlockState(logTarget).isAir() && !mc.world.getBlockState(logTarget).isReplaceable())
			return false;

		if (!InventoryUtils.selectItemFromHotbar(Items.OAK_LOG))
			return false;

		BlockHitResult hit = new BlockHitResult(belowLog.toCenterPos().add(0.0D, 0.5D, 0.0D), Direction.UP, belowLog, false);
		ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
		if (result.isAccepted()) {
			mc.player.swingHand(Hand.MAIN_HAND);
			logPos = logTarget;
			return true;
		}
		return false;
	}

	private boolean selectBow() {
		for (int i = 0; i < 9; i++) {
			if (mc.player.getInventory().getStack(i).getItem() instanceof BowItem) {
				InventoryUtils.setInvSlot(i);
				return true;
			}
		}
		return false;
	}

	private void aimAtCart() {
		if (railPos == null || mc.player == null)
			return;

		Vec3d target = railPos.toCenterPos().add(0.0D, 1.1D, 0.0D);
		Vec3d delta = target.subtract(mc.player.getEyePos());
		double horizontal = delta.horizontalLength();
		mc.player.setYaw((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
		mc.player.setPitch((float) (-Math.toDegrees(Math.atan2(delta.y, horizontal))));
	}

	private void pressUse() {
		if (mc.player != null)
			mc.options.useKey.setPressed(true);
	}

	private void releaseUse() {
		if (mc.player != null) {
			mc.options.useKey.setPressed(false);
			if (mc.interactionManager != null)
				mc.interactionManager.stopUsingItem(mc.player);
		}
	}

	private boolean isRail(BlockPos pos) {
		var state = mc.world.getBlockState(pos);
		return state.isOf(Blocks.RAIL) || state.isOf(Blocks.POWERED_RAIL)
				|| state.isOf(Blocks.DETECTOR_RAIL) || state.isOf(Blocks.ACTIVATOR_RAIL);
	}

	private void resetState() {
		state = STATE_PLACE_RAIL;
		delayTicks = 0;
		railPos = null;
		logPos = null;
		chargeTicks = 0;
	}
}
