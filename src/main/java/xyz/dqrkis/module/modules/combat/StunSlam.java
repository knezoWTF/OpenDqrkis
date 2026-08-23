package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.ModeSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.InventoryUtils;
import xyz.dqrkis.utils.ItemUtils;
import xyz.dqrkis.utils.WorldUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public final class StunSlam extends Module implements TickListener {
	private enum WeaponMode { Mace, Density, Breach }

	private final NumberSetting minFallDistance = new NumberSetting(EncryptedString.of("Min Fall Distance"), 0, 10, 1.5, 0.1)
			.setDescription(EncryptedString.of("Only strikes after falling this far"));
	private final ModeSetting<WeaponMode> weapon = new ModeSetting<>(EncryptedString.of("Weapon"), WeaponMode.Mace, WeaponMode.class)
			.setDescription(EncryptedString.of("Which mace variant to swap to"));
	private final NumberSetting cooldown = new NumberSetting(EncryptedString.of("Cooldown"), 0, 20, 0, 1)
			.setDescription(EncryptedString.of("Ticks to wait between combos"));

	private enum State { IDLE, AXE_SWAPPED, MACE_SWAPPED }

	private int cooldownTicks;
	private State state = State.IDLE;
	private PlayerEntity target;

	public StunSlam() {
		super(EncryptedString.of("Stun Slam"),
				EncryptedString.of("Stuns your enemy with an axe then slams with a mace"),
				-1,
				Category.COMBAT);
		addSettings(minFallDistance, weapon, cooldown);
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
		state = State.IDLE;
		target = null;
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.interactionManager == null)
			return;

		if (state == State.AXE_SWAPPED && target != null) {
			attack(target);

			int maceSlot = findMaceSlot();
			if (maceSlot != -1) {
				InventoryUtils.setInvSlot(maceSlot);
				state = State.MACE_SWAPPED;
			} else {
				startCooldown();
			}
		} else if (state == State.MACE_SWAPPED && target != null) {
			attack(target);
			startCooldown();
		} else if (cooldownTicks > 0) {
			cooldownTicks--;
		} else {
			PlayerEntity player = WorldUtils.findNearestPlayer(mc.player, 4.5F, true, false);
			if (player == null)
				return;

			if (mc.player.fallDistance < minFallDistance.getValueFloat())
				return;

			if (player.isBlocking()) {
				int axeSlot = findAxeSlot();
				if (axeSlot != -1) {
					InventoryUtils.setInvSlot(axeSlot);
					state = State.AXE_SWAPPED;
					target = player;
					return;
				}
			}

			int maceSlot = findMaceSlot();
			if (maceSlot != -1) {
				InventoryUtils.setInvSlot(maceSlot);
				state = State.MACE_SWAPPED;
				target = player;
			}
		}
	}

	private void attack(PlayerEntity player) {
		if (!player.isAlive() || mc.player.squaredDistanceTo(player) >= 20.25D)
			return;

		mc.interactionManager.attackEntity(mc.player, player);
		mc.player.swingHand(Hand.MAIN_HAND);
	}

	private void startCooldown() {
		state = State.IDLE;
		target = null;
		cooldownTicks = cooldown.getValueInt();
	}

	private void resetState() {
		cooldownTicks = 0;
		state = State.IDLE;
		target = null;
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
		WeaponMode mode = weapon.getMode();

		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (!stack.isOf(Items.MACE))
				continue;

			if (mode == WeaponMode.Density && !ItemUtils.hasEnchant(stack, "density"))
				continue;
			if (mode == WeaponMode.Breach && !ItemUtils.hasEnchant(stack, "breach"))
				continue;

			return i;
		}

		return mode == WeaponMode.Mace ? -1 : findAnyMaceSlot();
	}

	private int findAnyMaceSlot() {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (ItemUtils.isMace(stack))
				return i;
		}
		return -1;
	}
}
