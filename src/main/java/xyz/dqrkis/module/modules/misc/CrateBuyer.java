package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.ModeSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class CrateBuyer extends Module implements TickListener {
	public enum CrateAction {
		All, Helmet, Chestplate, Leggings, Boots, Sword, Pickaxe, Shovel
	}

	private static final int CONFIRM_SLOT = 15;
	private static final int CLICKS_PER_ACTION = 4;

	private final ModeSetting<CrateAction> action = new ModeSetting<>(EncryptedString.of("Action"), CrateAction.All, CrateAction.class);

	private int tickCounter;
	private int messageCooldown;
	private int clickPhase;
	private int allModeIndex;
	private boolean validScreenSeen;

	public CrateBuyer() {
		super(EncryptedString.of("Crate Buyer"),
				EncryptedString.of("Automatically buys items from the common crate"),
				-1,
				Category.MISC);
		addSettings(action);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		tickCounter = 0;
		messageCooldown = 0;
		clickPhase = 0;
		allModeIndex = 0;
		validScreenSeen = false;
		ChatUtils.info("Activated. Mode: " + action.getMode());
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		clickPhase = 0;
		allModeIndex = 0;
		validScreenSeen = false;
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.interactionManager == null)
			return;

		if (messageCooldown > 0)
			messageCooldown--;

		if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
			if (validScreenSeen && messageCooldown == 0) {
				ChatUtils.error("You need to be on the crate screen to use this module.");
				messageCooldown = 20;
			}
			return;
		}

		if (!isValidCrateScreen(screen)) {
			if (!validScreenSeen && messageCooldown == 0) {
				ChatUtils.error("This doesn't appear to be a valid crate screen. Closing screen.");
				messageCooldown = 20;
				mc.setScreen(null);
			}
			return;
		}

		validScreenSeen = true;
		tickCounter++;
		if (tickCounter < CLICKS_PER_ACTION)
			return;

		tickCounter = 0;
		if (action.getMode() == CrateAction.All) {
			runTwoPhaseClick(categorySlot(CrateAction.values()[1 + allModeIndex]));
			allModeIndex = (allModeIndex + 1) % 7;
		} else {
			runTwoPhaseClick(categorySlot(action.getMode()));
		}
	}

	private void runTwoPhaseClick(int categorySlot) {
		if (clickPhase == 0) {
			mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, categorySlot, 0, SlotActionType.PICKUP, mc.player);
			clickPhase = 1;
		} else {
			mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, CONFIRM_SLOT, 0, SlotActionType.PICKUP, mc.player);
			clickPhase = 0;
		}
	}

	private boolean isValidCrateScreen(HandledScreen<?> screen) {
		for (int i = 0; i <= 9; i++) {
			if (!screen.getScreenHandler().getSlot(i).getStack().isOf(Items.GRAY_STAINED_GLASS_PANE))
				return false;
		}
		for (int i = 17; i <= 26; i++) {
			if (!screen.getScreenHandler().getSlot(i).getStack().isOf(Items.GRAY_STAINED_GLASS_PANE))
				return false;
		}
		return true;
	}

	private static int categorySlot(CrateAction crateAction) {
		return switch (crateAction) {
			case Helmet -> 10;
			case Chestplate -> 11;
			case Leggings -> 12;
			case Boots -> 13;
			case Sword -> 14;
			case Pickaxe -> 15;
			case Shovel -> 16;
			default -> 10;
		};
	}
}
