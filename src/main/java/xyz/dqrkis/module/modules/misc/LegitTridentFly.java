package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.mixin.ClientPlayerInteractionManagerAccessor;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class LegitTridentFly extends Module implements TickListener {
	private boolean pressingUse;
	private boolean waitingForRelease;
	private int releaseDelay;
	private int savedSlot = -1;
	private long lastNanoTime;

	public LegitTridentFly() {
		super(EncryptedString.of("Legit Trident Fly"),
				EncryptedString.of("Undetectable Trident Fly. Requires Rain, RipTide"),
				-1,
				Category.MISC);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		pressingUse = false;
		waitingForRelease = false;
		releaseDelay = -1;
		savedSlot = -1;

		if (mc.player != null && !findRiptideTrident()) {
			ChatUtils.error("No riptide trident found in hotbar!");
			setEnabled(false);
			return;
		}

		lastNanoTime = System.nanoTime();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);

		if (mc.options.useKey.isPressed())
			mc.options.useKey.setPressed(false);

		if (savedSlot != -1 && mc.player != null) {
			mc.player.getInventory().setSelectedSlot(savedSlot);
			((ClientPlayerInteractionManagerAccessor) mc.interactionManager).syncSlot();
			savedSlot = -1;
		}
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null)
			return;

		long now = System.nanoTime();
		float delta = (float) (now - lastNanoTime) / 1.0E9F;
		lastNanoTime = now;

		if (mc.world.getRainGradient(delta) == 0.0F) {
			ChatUtils.error("It needs to be raining to use this!");
			setEnabled(false);
			return;
		}

		ItemStack mainHand = mc.player.getMainHandStack();
		if (!hasRiptide(mainHand)) {
			if (!findRiptideTrident()) {
				ChatUtils.error("No riptide trident found in hotbar!");
				setEnabled(false);
				return;
			}
			mainHand = mc.player.getMainHandStack();
		}

		if (mainHand.isDamageable()) {
			int maxDamage = mainHand.getMaxDamage();
			int remaining = maxDamage - mainHand.getDamage();
			double durabilityPercent = (double) remaining / maxDamage * 100.0D;
			if (durabilityPercent <= 20.0D) {
				ChatUtils.error("Trident durability too low!");
				setEnabled(false);
				return;
			}
		}

		if (waitingForRelease) {
			releaseDelay++;
			if (releaseDelay >= 1) {
				waitingForRelease = false;
				pressingUse = true;
				mc.options.useKey.setPressed(true);
			}
		} else if (pressingUse) {
			int useTicksTotal = mainHand.getMaxUseTime(mc.player);
			int useTicksLeft = mc.player.getItemUseTimeLeft();
			int elapsed = useTicksTotal - useTicksLeft;

			if (mc.player.isUsingItem() && elapsed >= 10) {
				mc.options.useKey.setPressed(false);
				pressingUse = false;
				waitingForRelease = true;
				releaseDelay = 0;
			} else if (!mc.player.isUsingItem()) {
				mc.options.useKey.setPressed(true);
			}
		} else {
			pressingUse = true;
			mc.options.useKey.setPressed(true);
		}
	}

	private boolean findRiptideTrident() {
		if (mc.player == null)
			return false;

		if (savedSlot == -1)
			savedSlot = mc.player.getInventory().getSelectedSlot();

		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (stack.isOf(Items.TRIDENT) && hasRiptide(stack)) {
				mc.player.getInventory().setSelectedSlot(i);
				((ClientPlayerInteractionManagerAccessor) mc.interactionManager).syncSlot();
				return true;
			}
		}
		return false;
	}

	private boolean hasRiptide(ItemStack stack) {
		return xyz.dqrkis.utils.ItemUtils.hasEnchant(stack, "riptide");
	}
}
