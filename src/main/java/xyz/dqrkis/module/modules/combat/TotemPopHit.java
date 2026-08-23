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
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class TotemPopHit extends Module implements TickListener, ShieldDisabledListener {
	private final BooleanSetting swapToSword = new BooleanSetting(EncryptedString.of("Swap To Sword"), true)
			.setDescription(EncryptedString.of("Swaps to a sword when the shield gets disabled"));
	private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true)
			.setDescription(EncryptedString.of("Switches back to your previous slot"));
	private final NumberSetting switchBackDelay = new NumberSetting(EncryptedString.of("Switch Back Delay"), 0, 5, 1, 1);

	private boolean swapped;
	private int previousSlot = -1;
	private int switchClock;

	public TotemPopHit() {
		super(EncryptedString.of("Totem Pop Hit"),
				EncryptedString.of("Attacks instantly when enemy pops a totem"),
				-1,
				Category.COMBAT);
		addSettings(swapToSword, switchBack, switchBackDelay);
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
		if (swapped && previousSlot != -1 && mc.player != null)
			InventoryUtils.setInvSlot(previousSlot);
		resetState();
		super.onDisable();
	}

	@Override
	public void onShieldDisabled() {
		if (mc.player == null || swapped)
			return;

		if (!(mc.crosshairTarget instanceof EntityHitResult hit) || hit.getEntity() == null)
			return;

		if (isGoodWeapon(mc.player.getMainHandStack().getItem()))
			return;

		if (!swapToSword.getValue())
			return;

		int slot = findSwordSlot();
		if (slot == -1)
			return;

		if (switchBack.getValue() && previousSlot == -1)
			previousSlot = mc.player.getInventory().getSelectedSlot();

		InventoryUtils.setInvSlot(slot);

		if (switchBack.getValue()) {
			swapped = true;
			switchClock = 0;
		}
	}

	@Override
	public void onTick() {
		if (mc.player == null)
			return;

		if (!swapped || previousSlot == -1)
			return;

		if (switchClock < switchBackDelay.getValueInt()) {
			switchClock++;
			return;
		}

		InventoryUtils.setInvSlot(previousSlot);
		resetState();
	}

	private boolean isGoodWeapon(net.minecraft.item.Item item) {
		return item instanceof TridentItem || item instanceof MaceItem
				|| new ItemStack(item).isIn(ItemTags.SWORDS) || new ItemStack(item).isIn(ItemTags.AXES);
	}

	private int findSwordSlot() {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (stack.isIn(ItemTags.SWORDS))
				return i;
		}
		return -1;
	}

	private void resetState() {
		swapped = false;
		previousSlot = -1;
		switchClock = 0;
	}
}
