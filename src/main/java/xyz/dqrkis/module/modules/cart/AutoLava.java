package xyz.dqrkis.module.modules.cart;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.KeybindSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.InventoryUtils;
import xyz.dqrkis.utils.KeyUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class AutoLava extends Module implements TickListener {
	private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), GLFW.GLFW_KEY_R, false);
	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 10, 0, 1);

	private BlockPos targetPos;
	private int state;
	private int delayTicks;
	private boolean keyWasPressed;

	public AutoLava() {
		super(EncryptedString.of("Auto Lava"),
				EncryptedString.of("Places lava at player feet"),
				-1,
				Category.CART);
		addSettings(activateKey, delay);
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
		resetState();
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.currentScreen != null)
			return;

		boolean pressed = activateKey.getKey() != -1 && KeyUtils.isKeyPressed(activateKey.getKey());

		if (pressed && !keyWasPressed && state == 0) {
			if (!InventoryUtils.hasItemInHotbar(item -> item == Items.LAVA_BUCKET)) {
				keyWasPressed = pressed;
				return;
			}

			if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
				Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
				if (entity instanceof PlayerEntity player) {
					targetPos = predictLandingPos(player);
					if (targetPos != null && isValidPlacement(targetPos))
						state = 1;
					else
						targetPos = null;
				}
			}
		}

		keyWasPressed = pressed;

		if (state != 0) {
			if (delayTicks > 0) {
				delayTicks--;
			} else {
				mc.execute(this::step);
			}
		}
	}

	private void step() {
		if (mc.player == null || mc.world == null)
			return;

		switch (state) {
			case 1 -> {
				if (InventoryUtils.selectItemFromHotbar(Items.LAVA_BUCKET)) {
					state = 2;
					delayTicks = 1;
				} else {
					resetState();
				}
			}
			case 2 -> {
				if (placeLava()) {
					state = 3;
				} else {
					resetState();
				}
			}
			case 3 -> {
				pickLavaBackUp();
				resetState();
			}
		}
	}

	private BlockPos predictLandingPos(PlayerEntity player) {
		Vec3d pos = player.getEntityPos();
		Vec3d velocity = player.getVelocity();

		if (player.isOnGround() && velocity.horizontalLength() < 0.1)
			return new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));

		double x = pos.x, y = pos.y, z = pos.z;
		double vx = velocity.x, vy = velocity.y, vz = velocity.z;

		for (int i = 0; i < 20; i++) {
			vy = (vy - 0.08D) * 0.98D;
			vx *= 0.91D;
			vz *= 0.91D;
			x += vx;
			y += vy;
			z += vz;

			BlockPos below = new BlockPos((int) Math.floor(x), (int) Math.floor(y) - 1, (int) Math.floor(z));
			if (!mc.world.getBlockState(below).isAir())
				return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
		}

		return new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
	}

	private boolean isValidPlacement(BlockPos pos) {
		BlockPos below = pos.down();
		if (!mc.world.getBlockState(below).isSolidBlock(mc.world, below))
			return false;
		if (!mc.world.getBlockState(pos).isReplaceable() && !mc.world.getBlockState(pos).isAir())
			return false;
		return mc.player.getEntityPos().squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 25.0D;
	}

	private boolean placeLava() {
		if (targetPos == null || mc.player == null || mc.interactionManager == null)
			return false;

		if (!mc.player.getMainHandStack().isOf(Items.LAVA_BUCKET))
			return false;

		lookAt(targetPos.down().toCenterPos().add(0.0D, 0.5D, 0.0D));
		ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
		if (result.isAccepted()) {
			mc.player.swingHand(Hand.MAIN_HAND);
			return true;
		}
		return false;
	}

	private void pickLavaBackUp() {
		if (targetPos == null || mc.player == null || mc.interactionManager == null)
			return;

		if (!InventoryUtils.hasItemInHotbar(item -> item == Items.BUCKET))
			return;

		lookAt(targetPos.toCenterPos());
		ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
		if (result.isAccepted())
			mc.player.swingHand(Hand.MAIN_HAND);
	}

	private void lookAt(Vec3d target) {
		Vec3d delta = target.subtract(mc.player.getEyePos());
		double horizontal = delta.horizontalLength();
		mc.player.setYaw((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
		mc.player.setPitch((float) (-Math.toDegrees(Math.atan2(delta.y, horizontal))));
	}

	private void resetState() {
		targetPos = null;
		state = 0;
		delayTicks = 0;
		keyWasPressed = false;
	}
}
