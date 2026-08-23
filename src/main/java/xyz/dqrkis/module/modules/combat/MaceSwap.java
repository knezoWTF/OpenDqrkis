package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.ShieldDisabledListener;
import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.InventoryUtils;
import xyz.dqrkis.utils.ItemUtils;
import xyz.dqrkis.utils.WorldUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class MaceSwap extends Module implements TickListener, ShieldDisabledListener {
	private final BooleanSetting density = new BooleanSetting(EncryptedString.of("Density"), true)
			.setDescription(EncryptedString.of("Prefers maces enchanted with Density"));
	private final BooleanSetting breach = new BooleanSetting(EncryptedString.of("Breach"), true)
			.setDescription(EncryptedString.of("Prefers maces enchanted with Breach"));
	private final BooleanSetting swords = new BooleanSetting(EncryptedString.of("Swords"), true)
			.setDescription(EncryptedString.of("Triggers when holding a sword"));
	private final BooleanSetting axes = new BooleanSetting(EncryptedString.of("Axes"), false)
			.setDescription(EncryptedString.of("Triggers when holding an axe"));
	private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true)
			.setDescription(EncryptedString.of("Switches back to your previous slot"));
	private final NumberSetting switchBackDelay = new NumberSetting(EncryptedString.of("Switch Back Delay"), 0, 20, 0, 1);

	private boolean swapped;
	private int previousSlot = -1;
	private int switchClock;

	public MaceSwap() {
		super(EncryptedString.of("Mace Swap"),
				EncryptedString.of("Swaps to a mace after disabling an opponents shield"),
				-1,
				Category.COMBAT);
		addSettings(density, breach, swords, axes, switchBack, switchBackDelay);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(ShieldDisabledListener.class, this);
		resetState();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(ShieldDisabledListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.currentScreen != null || mc.player == null)
			return;

		if (!swapped)
			return;

		if (switchBack.getValue()) {
			if (switchClock < switchBackDelay.getValueInt()) {
				switchClock++;
				return;
			}
			if (previousSlot != -1)
				InventoryUtils.setInvSlot(previousSlot);
		}

		resetState();
	}

	@Override
	public void onShieldDisabled() {
		if (mc.player == null)
			return;

		if (!weaponMatches(mc.player.getMainHandStack()))
			return;

		if (previousSlot == -1)
			previousSlot = mc.player.getInventory().getSelectedSlot();

		if (density.getValue() && breach.getValue()) {
			swapToAnyMace();
		} else if (!density.getValue() && !breach.getValue()) {
			swapToAnyMace();
		} else {
			if (density.getValue())
				swapToEnchantedMace("density");

			if (breach.getValue())
				swapToEnchantedMace("breach");
		}

		swapped = true;
	}

	private boolean weaponMatches(ItemStack stack) {
		if (swords.getValue() && axes.getValue())
			return WorldUtils.isSword(stack) || WorldUtils.isAxe(stack);
		return (!swords.getValue() || WorldUtils.isSword(stack)) && (!axes.getValue() || WorldUtils.isAxe(stack));
	}

	private void swapToAnyMace() {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (ItemUtils.isMace(stack)) {
				InventoryUtils.setInvSlot(i);
				return;
			}
		}
	}

	private void swapToEnchantedMace(String enchant) {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (stack.isOf(Items.MACE) && ItemUtils.hasEnchant(stack, enchant)) {
				InventoryUtils.setInvSlot(i);
				return;
			}
		}
	}

	private void resetState() {
		previousSlot = -1;
		switchClock = 0;
		swapped = false;
	}
}
