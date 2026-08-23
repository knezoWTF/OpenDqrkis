package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.InventoryUtils;
import xyz.dqrkis.utils.ItemUtils;
import xyz.dqrkis.utils.WorldUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public final class QuickStrike extends Module implements TickListener {
	private final NumberSetting minFallDistance = new NumberSetting(EncryptedString.of("Min Fall Distance"), 1, 10, 3, 0.5)
			.setDescription(EncryptedString.of("Only strikes after falling this far"));
	private final NumberSetting attackDelay = new NumberSetting(EncryptedString.of("Attack Delay"), 0, 500, 100, 10)
			.setDescription(EncryptedString.of("Milliseconds between attacks"));
	private final NumberSetting densityFallDistance = new NumberSetting(EncryptedString.of("Density Fall Distance"), 1, 20, 7, 0.5)
			.setDescription(EncryptedString.of("Falls beyond this use a Density mace instead of Breach"));
	private final BooleanSetting targetPlayers = new BooleanSetting(EncryptedString.of("Target Players"), true);
	private final BooleanSetting targetMobs = new BooleanSetting(EncryptedString.of("Target Mobs"), false)
			.setDescription(EncryptedString.of("Also strikes mobs (ignores passive and tamed ones)"));
	private final BooleanSetting shieldSwap = new BooleanSetting(EncryptedString.of("Shield Swap"), false)
			.setDescription(EncryptedString.of("Swaps to an axe first against blocking players"));
	private final BooleanSetting autoSwap = new BooleanSetting(EncryptedString.of("Auto Swap"), true)
			.setDescription(EncryptedString.of("Swaps to a mace automatically"));

	private long lastAttackMs;
	private int previousSlot = -1;
	private double apexY = -1.0D;
	private boolean shieldHandled;
	private int stage;
	private boolean pendingMaceAttack;
	private Entity maceTarget;
	private boolean pendingAxeAttack;
	private Entity axeTarget;
	private boolean airborne;

	public QuickStrike() {
		super(EncryptedString.of("Quick Strike"),
				EncryptedString.of("Automatically attacks while falling with mace."),
				-1,
				Category.COMBAT);
		addSettings(minFallDistance, attackDelay, densityFallDistance, targetPlayers, targetMobs, shieldSwap, autoSwap);
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
		if (previousSlot != -1 && mc.player != null)
			InventoryUtils.setInvSlot(previousSlot);
		resetState();
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.interactionManager == null)
			return;

		if (pendingAxeAttack && axeTarget != null) {
			attack(axeTarget);
			pendingAxeAttack = false;
			swapToMace();
			shieldHandled = true;
			stage = 0;
			pendingMaceAttack = true;
		} else if (pendingMaceAttack && maceTarget != null) {
			long now = System.currentTimeMillis();
			if (holdingMace() && now - lastAttackMs >= attackDelayMs()) {
				attack(maceTarget);
				lastAttackMs = now;
			}
			pendingMaceAttack = false;
			maceTarget = null;
		} else {
			updateFallState();
			engageWhileFalling();
		}
	}

	private long attackDelayMs() {
		return attackDelay.getValueLong();
	}

	private void updateFallState() {
		boolean onGround = mc.player.isOnGround();
		boolean fallingFast = mc.player.getVelocity().y < -0.1;
		double y = mc.player.getY();

		if (onGround) {
			if (airborne)
				resetAirState();

			if (previousSlot != -1) {
				InventoryUtils.setInvSlot(previousSlot);
				previousSlot = -1;
			}
		} else {
			if (!airborne) {
				airborne = true;
				apexY = y;
				shieldHandled = false;
				stage = 0;
			} else if (fallingFast && apexY != -1.0D && y > apexY) {
				apexY = y;
			}
		}
	}

	private void engageWhileFalling() {
		if (!airborne || mc.player.getVelocity().y >= -0.1)
			return;

		double fallDistance = mc.player.fallDistance;
		if (fallDistance < minFallDistance.getValueFloat())
			return;

		Entity target = findNearestTarget();
		if (target == null)
			target = mc.targetedEntity;

		if (!isValidTarget(target))
			return;

		if (shieldSwap.getValue())
			tryShieldSwap(target, fallDistance);

		if (!shieldSwap.getValue() || shieldHandled || stage == 0)
			engage(target, fallDistance);
	}

	private void tryShieldSwap(Entity target, double fallDistance) {
		boolean blocking = target instanceof PlayerEntity player && player.isHolding(Items.SHIELD) && player.isBlocking();
		if (!(blocking && fallDistance > minFallDistance.getValueFloat() && !shieldHandled && stage == 0))
			return;

		if (previousSlot == -1)
			previousSlot = mc.player.getInventory().getSelectedSlot();

		int axeSlot = findAxeSlot();
		if (axeSlot != -1) {
			InventoryUtils.setInvSlot(axeSlot);
			pendingAxeAttack = true;
			axeTarget = target;
			maceTarget = target;
			stage = 1;
		}
	}

	private void engage(Entity target, double fallDistance) {
		if (!holdingMace()) {
			if (previousSlot == -1)
				previousSlot = mc.player.getInventory().getSelectedSlot();

			if (autoSwap.getValue())
				selectMaceForFall(fallDistance);
			else
				swapToMace();
		} else if (autoSwap.getValue()) {
			selectMaceForFall(fallDistance);
		}

		if (holdingMace()) {
			pendingMaceAttack = true;
			maceTarget = target;
		}
	}

	private void selectMaceForFall(double fallDistance) {
		int slot = fallDistance >= densityFallDistance.getValueFloat()
				? findEnchantedMace("density")
				: findEnchantedMace("breach");

		if (slot == -1)
			slot = findMaceSlot();

		if (slot != -1)
			InventoryUtils.setInvSlot(slot);
	}

	private void swapToMace() {
		int slot = findMaceSlot();
		if (slot != -1)
			InventoryUtils.setInvSlot(slot);
	}

	private void attack(Entity target) {
		if (!(target instanceof LivingEntity living))
			return;

		if (!living.isAlive() || mc.player.squaredDistanceTo(target) >= 20.25D)
			return;

		mc.interactionManager.attackEntity(mc.player, target);
		mc.player.swingHand(Hand.MAIN_HAND);
	}

	private boolean holdingMace() {
		return mc.player.getMainHandStack().isOf(Items.MACE);
	}

	private boolean isValidTarget(Entity entity) {
		if (entity == null || entity == mc.player || !(entity instanceof LivingEntity living))
			return false;

		if (!living.isAlive() || living.isDead())
			return false;

		if (entity instanceof PlayerEntity)
			return targetPlayers.getValue();

		if (!targetMobs.getValue())
			return false;

		return !(entity instanceof PassiveEntity) && !(entity instanceof Tameable);
	}

	private Entity findNearestTarget() {
		Entity best = null;
		double bestDistance = 16.0D;

		for (Entity entity : mc.world.getEntities()) {
			if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive())
				continue;

			double distance = mc.player.squaredDistanceTo(entity);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = entity;
			}
		}

		return best;
	}

	private int findAxeSlot() {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (WorldUtils.isAxe(stack))
				return i;
		}
		return -1;
	}

	private int findMaceSlot() {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (stack.isOf(Items.MACE))
				return i;
		}
		return -1;
	}

	private int findEnchantedMace(String enchant) {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (stack.isOf(Items.MACE) && ItemUtils.hasEnchant(stack, enchant))
				return i;
		}
		return -1;
	}

	private void resetAirState() {
		airborne = false;
		apexY = -1.0D;
		shieldHandled = false;
		stage = 0;
	}

	private void resetState() {
		previousSlot = -1;
		lastAttackMs = 0L;
		pendingMaceAttack = false;
		maceTarget = null;
		pendingAxeAttack = false;
		axeTarget = null;
		resetAirState();
	}
}
