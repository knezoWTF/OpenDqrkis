package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.module.setting.StringSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public final class AhSell extends Module implements TickListener {
	private static final int CONFIRM_SLOT = 15;
	private static final int GUI_TIMEOUT_TICKS = 10;
	private static final int CONFIRM_DELAY_TICKS = 2;

	private final StringSetting sellPrice = new StringSetting(EncryptedString.of("Sell Price"), EncryptedString.of("15000").toString());
	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay (ticks)"), 5, 100, 20, 1);

	private int delayTicks;
	private State state = State.IDLE;
	private int stateTicks;

	private enum State { IDLE, WAITING_FOR_GUI, CLICKING_CONFIRM }

	public AhSell() {
		super(EncryptedString.of("Ah Sell"),
				EncryptedString.of("Automatically sells items from your hotbar."),
				-1,
				Category.MISC);
		addSettings(sellPrice, delay);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		resetToIdle();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		resetToIdle();
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.interactionManager == null)
			return;

		if (delayTicks > 0) {
			delayTicks--;
			return;
		}

		switch (state) {
			case IDLE -> tryStartSale();
			case WAITING_FOR_GUI -> {
				stateTicks--;
				if (mc.currentScreen instanceof GenericContainerScreen) {
					state = State.CLICKING_CONFIRM;
					stateTicks = CONFIRM_DELAY_TICKS;
				} else if (stateTicks <= 0) {
					resetToIdle();
				}
			}
			case CLICKING_CONFIRM -> {
				stateTicks--;
				if (stateTicks <= 0) {
					if (mc.currentScreen instanceof GenericContainerScreen) {
						mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, CONFIRM_SLOT, 0, SlotActionType.PICKUP, mc.player);
						mc.player.closeHandledScreen();
					}
					resetToIdle();
				}
			}
		}
	}

	private void tryStartSale() {
		int slot = -1;
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (!stack.isEmpty()) {
				slot = i;
				break;
			}
		}

		if (slot == -1) {
			ChatUtils.info("Hotbar is empty. Disabling.");
			setEnabled(false);
			return;
		}

		try {
			Integer.parseInt(sellPrice.getValue());
		} catch (NumberFormatException e) {
			ChatUtils.error("Invalid sell price. Disabling.");
			setEnabled(false);
			return;
		}

		InventoryUtils_setSelectedSlot(slot);
		mc.player.networkHandler.sendChatCommand("ah sell " + sellPrice.getValue());
		state = State.WAITING_FOR_GUI;
		stateTicks = GUI_TIMEOUT_TICKS;
	}

	private void InventoryUtils_setSelectedSlot(int slot) {
		mc.player.getInventory().setSelectedSlot(slot);
		((xyz.dqrkis.mixin.ClientPlayerInteractionManagerAccessor) mc.interactionManager).syncSlot();
	}

	private void resetToIdle() {
		state = State.IDLE;
		delayTicks = delay.getValueInt();
		stateTicks = 0;
	}
}
