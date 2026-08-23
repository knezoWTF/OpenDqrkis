package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public final class AutoSell extends Module implements TickListener {
	private enum State { IDLE, OPENING_GUI, DEPOSITING, CLOSING }

	private final NumberSetting clickDelay = new NumberSetting(EncryptedString.of("Click Delay (ticks)"), 1, 10, 2, 1);

	private State state = State.IDLE;
	private int searchStartSlot;
	private int timeoutTicks;
	private int delayTicks;

	public AutoSell() {
		super(EncryptedString.of("Auto Sell"),
				EncryptedString.of("Automatically sells all items in your inventory."),
				-1,
				Category.MISC);
		addSettings(clickDelay);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);

		if (mc.player != null && mc.world != null) {
			state = State.OPENING_GUI;
			mc.player.networkHandler.sendChatCommand("sell");
			timeoutTicks = 40;
		} else {
			disable();
		}
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
		if (mc.player == null || mc.interactionManager == null) {
			mc.execute(this::disable);
			return;
		}

		if (delayTicks > 0) {
			delayTicks--;
			return;
		}

		switch (state) {
			case OPENING_GUI -> {
				if (mc.currentScreen instanceof GenericContainerScreen) {
					state = State.DEPOSITING;
					searchStartSlot = mc.player.playerScreenHandler.slots.size() - 36;
				} else if (--timeoutTicks <= 0) {
					ChatUtils.error("Timed out waiting for sell GUI.");
					mc.execute(this::disable);
				}
			}
			case DEPOSITING -> {
				if (!(mc.currentScreen instanceof GenericContainerScreen)) {
					ChatUtils.error("Sell GUI closed unexpectedly.");
					mc.execute(this::disable);
					return;
				}

				int slot = -1;
				for (int i = searchStartSlot; i < mc.player.playerScreenHandler.slots.size(); i++) {
					if (mc.player.playerScreenHandler.getSlot(i).inventory == mc.player.getInventory()) {
						ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
						if (!stack.isEmpty()) {
							slot = i;
							break;
						}
					}
				}

				if (slot != -1) {
					final int syncId = mc.player.playerScreenHandler.syncId;
					final int targetSlot = slot;
					mc.execute(() -> mc.interactionManager.clickSlot(syncId, targetSlot, 0, SlotActionType.QUICK_MOVE, mc.player));
					searchStartSlot = slot + 1;
					delayTicks = clickDelay.getValueInt();
				} else {
					state = State.CLOSING;
				}
			}
			case CLOSING -> {
				mc.execute(() -> {
					if (mc.currentScreen != null && mc.player != null)
						mc.player.closeHandledScreen();
					ChatUtils.info("Finished selling items.");
					disable();
				});
				state = State.IDLE;
			}
			case IDLE -> {}
		}
	}

	private void disable() {
		setEnabled(false);
	}

	private void resetState() {
		state = State.IDLE;
		searchStartSlot = 0;
		timeoutTicks = 0;
		delayTicks = 0;
	}
}
