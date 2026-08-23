package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.HudListener;
import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.InventoryUtils;
import xyz.dqrkis.utils.MouseSimulation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class AutoMace extends Module implements TickListener, HudListener {
	private final NumberSetting targetRange = new NumberSetting(EncryptedString.of("Target Range"), 1, 6, 4.5, 0.1);
	private final NumberSetting minFallDistance = new NumberSetting(EncryptedString.of("Min Fall Distance"), 1, 10, 3, 0.1);
	private final NumberSetting minFallVelocity = new NumberSetting(EncryptedString.of("Min Fall Velocity"), 0.1, 2, 0.3, 0.1);
	private final NumberSetting attackCooldownTicks = new NumberSetting(EncryptedString.of("Attack Cooldown"), 0, 10, 1, 1);
	private final BooleanSetting elytraOnly = new BooleanSetting(EncryptedString.of("Elytra Only"), true)
			.setDescription(EncryptedString.of("Only works while gliding with an elytra"));
	private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true);
	private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), true);
	private final BooleanSetting seeOnly = new BooleanSetting(EncryptedString.of("See Only"), true)
			.setDescription(EncryptedString.of("Only targets players you can see"));
	private final BooleanSetting checkFallVelocity = new BooleanSetting(EncryptedString.of("Check Fall Velocity"), true);
	private final BooleanSetting waitForCrit = new BooleanSetting(EncryptedString.of("Wait For Crit"), true);
	private final BooleanSetting breakShieldsWithAxe = new BooleanSetting(EncryptedString.of("Break Shields With Axe"), false);
	private final BooleanSetting debugOverlay = new BooleanSetting(EncryptedString.of("Debug Overlay"), true);

	private int previousSlot = -1;
	private int attackCooldown;
	private int switchBackTimer;
	private boolean hitLanded;
	private PlayerEntity lastTarget;
	private final List<String> debugMessages = new ArrayList<>();
	private long lastDebugTime;
	private boolean wasHighEnough;

	public AutoMace() {
		super(EncryptedString.of("Auto Mace"),
				EncryptedString.of("Automatically attacks with mace on fall"),
				-1,
				Category.COMBAT);
		addSettings(targetRange, minFallDistance, minFallVelocity, attackCooldownTicks, elytraOnly,
				switchBack, clickSimulation, seeOnly, checkFallVelocity, waitForCrit, breakShieldsWithAxe, debugOverlay);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(HudListener.class, this);
		resetState();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(HudListener.class, this);
		switchBackToSavedSlot();
		resetState();
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.interactionManager == null)
			return;

		tickSwitchBack();

		if (attackCooldown > 0)
			attackCooldown--;

		updateDebugFallTracking();

		if (!InventoryUtils.hasItemInHotbar(item -> item == Items.MACE)) {
			if (previousSlot != -1 && switchBack.getValue())
				switchBackToSavedSlot();
			return;
		}

		PlayerEntity target = findTarget();
		if (target == null) {
			if (previousSlot != -1 && switchBack.getValue())
				switchBackToSavedSlot();
			debug("Waiting for player...");
			return;
		}

		lastTarget = target;
		double distance = mc.player.distanceTo(target);
		debug("Found player: " + target.getName().getString());

		if (distance > targetRange.getValueFloat()) {
			debug("Approaching target... (" + String.format("%.1fm", distance) + ")");
			return;
		}

		if (breakShieldsWithAxe.getValue() && holdingAxe() && isBlockingWithShield(target)) {
			shieldBreakCombo(target);
		} else {
			normalAttack(target);
		}
	}

	private void normalAttack(PlayerEntity target) {
		if (attackCooldown > 0)
			return;

		if (waitForCrit.getValue() && !isCritOpportunity()) {
			debug("Waiting for crit opportunity...");
			return;
		}

		if (!mc.player.getMainHandStack().isOf(Items.MACE) && !swapToMace())
			return;

		debug("Macing " + target.getName().getString() + "!");
		attack(target);
		attackCooldown = Math.max(attackCooldownTicks.getValueInt(), 10);
		hitLanded = true;

		if (switchBack.getValue())
			scheduleSwitchBack();
	}

	private void shieldBreakCombo(PlayerEntity target) {
		if (attackCooldown > 0)
			return;

		if (!findAndSwapToAxe()) {
			debug("No axe found - using mace only");
			normalAttack(target);
			return;
		}

		debug("Breaking shield with axe...");
		attack(target);

		if (!swapToMace()) {
			debug("Failed to switch to mace after axe!");
			return;
		}

		debug("Macing target for high damage!");
		attack(target);
		hitLanded = true;
		attackCooldown = Math.max(attackCooldownTicks.getValueInt(), 20);

		if (switchBack.getValue())
			scheduleSwitchBack();
	}

	private void attack(PlayerEntity target) {
		if (clickSimulation.getValue())
			MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT, 100);

		mc.interactionManager.attackEntity(mc.player, target);
		mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
	}

	private boolean isReadyToFall() {
		if (mc.player == null)
			return false;

		if (elytraOnly.getValue() && !mc.player.isGliding())
			return false;

		if (checkFallVelocity.getValue() && Math.abs(mc.player.getVelocity().y) < minFallVelocity.getValueFloat())
			return false;

		return !(getPotentialFallHeight() < minFallDistance.getValueFloat());
	}

	private double getPotentialFallHeight() {
		double y = mc.player.getY();
		for (int i = 1; i <= 256; i++) {
			int checkY = (int) (y - i);
			if (checkY < mc.world.getBottomY())
				break;

			if (!mc.world.getBlockState(mc.player.getBlockPos().withY(checkY)).isAir())
				return i;
		}
		return y - mc.world.getBottomY();
	}

	private PlayerEntity findTarget() {
		PlayerEntity best = null;
		double bestDistance = targetRange.getValueFloat();

		for (PlayerEntity player : mc.world.getPlayers()) {
			if (!isValidTarget(player))
				continue;

			double distance = mc.player.distanceTo(player);
			if (distance > bestDistance)
				continue;

			if (seeOnly.getValue() && !mc.player.canSee(player))
				continue;

			bestDistance = distance;
			best = player;
		}
		return best;
	}

	private boolean isValidTarget(PlayerEntity player) {
		if (player == null || player == mc.player)
			return false;

		if (player.isDead() || player.isRemoved())
			return false;

		return !player.isCreative() && !player.isSpectator();
	}

	private boolean isCritOpportunity() {
		boolean falling = !mc.player.isOnGround() && mc.player.getVelocity().y < 0.0D;
		boolean inFluidOrVehicle = mc.player.isTouchingWater() || mc.player.isInLava()
				|| mc.player.isClimbing() || mc.player.hasVehicle();

		return falling && !inFluidOrVehicle;
	}

	private boolean holdingAxe() {
		return InventoryUtils.hasItemInHotbar(item -> item.toString().contains("axe"));
	}

	private boolean isBlockingWithShield(PlayerEntity player) {
		boolean holdingShieldWhileUsing = player.isUsingItem()
				&& (player.getMainHandStack().getItem().toString().contains("shield")
				|| player.getOffHandStack().getItem().toString().contains("shield"));
		return holdingShieldWhileUsing || player.isBlocking();
	}

	private boolean swapToMace() {
		saveCurrentSlotIfNeeded();

		for (int i = 0; i < 9; i++) {
			if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
				InventoryUtils.setInvSlot(i);
				return true;
			}
		}
		return false;
	}

	private boolean findAndSwapToAxe() {
		saveCurrentSlotIfNeeded();

		for (int i = 0; i < 9; i++) {
			String itemName = mc.player.getInventory().getStack(i).getItem().toString();
			if (itemName.contains("axe")) {
				InventoryUtils.setInvSlot(i);
				return true;
			}
		}
		return false;
	}

	private void saveCurrentSlotIfNeeded() {
		if (previousSlot == -1)
			previousSlot = mc.player.getInventory().getSelectedSlot();
	}

	private void scheduleSwitchBack() {
		switchBackTimer = 4;
	}

	private void tickSwitchBack() {
		if (switchBackTimer > 0 && --switchBackTimer == 0)
			switchBackToSavedSlot();
	}

	private void switchBackToSavedSlot() {
		if (previousSlot != -1 && previousSlot != mc.player.getInventory().getSelectedSlot()) {
			InventoryUtils.setInvSlot(previousSlot);
			previousSlot = -1;
		}
	}

	private void updateDebugFallTracking() {
		if (!debugOverlay.getValue())
			return;

		double height = getPotentialFallHeight();
		boolean highEnough = height >= 5.0D;

		if (highEnough) {
			if (!wasHighEnough) {
				debugMessages.clear();
				debug("Mace mode activated - searching for targets...");
			}
		} else if (wasHighEnough) {
			switchBackTimer = Math.max(switchBackTimer, 60);
		}

		wasHighEnough = highEnough;
	}

	private void debug(String message) {
		if (!debugOverlay.getValue())
			return;

		long now = System.currentTimeMillis();
		if (now - lastDebugTime <= 100L)
			return;

		if (!debugMessages.contains(message)) {
			debugMessages.add(message);
			if (debugMessages.size() > 5)
				debugMessages.removeFirst();
		}

		lastDebugTime = now;
	}

	@Override
	public void onRenderHud(HudEvent event) {
		if (!debugOverlay.getValue() || debugMessages.isEmpty())
			return;

		DrawContext context = event.context;

		int textWidth = 0;
		for (String message : debugMessages)
			textWidth = Math.max(textWidth, mc.textRenderer.getWidth(message));

		int backgroundHeight = debugMessages.size() * 12 + 8;
		context.fill(6, 6, 10 + textWidth + 8, 10 + backgroundHeight, Integer.MIN_VALUE);
		context.fill(6, 6, textWidth + 12, backgroundHeight + 4, 0xFF00FF00);

		for (int i = 0; i < debugMessages.size(); i++)
			context.drawText(mc.textRenderer, debugMessages.get(i), 10, 10 + i * 12, 0xFFFFFF, true);
	}

	private void resetState() {
		previousSlot = -1;
		attackCooldown = 0;
		switchBackTimer = 0;
		hitLanded = false;
		lastTarget = null;
		debugMessages.clear();
		wasHighEnough = false;
	}
}
