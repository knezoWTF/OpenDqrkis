package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.ModeSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class ShopBuyer extends Module implements TickListener {
	public enum ShopItem {
		Obsidian(9, Items.OBSIDIAN),
		EndCrystal(10, Items.END_CRYSTAL),
		RespawnAnchor(11, Items.RESPAWN_ANCHOR),
		Glowstone(12, Items.GLOWSTONE),
		Totem(13, Items.TOTEM_OF_UNDYING),
		EnderPearl(14, Items.ENDER_PEARL),
		GoldenApple(15, Items.GOLDEN_APPLE),
		XpBottle(16, Items.EXPERIENCE_BOTTLE),
		TippedArrow(17, Items.TIPPED_ARROW);

		public final int slot;
		public final net.minecraft.item.Item item;

		ShopItem(int slot, net.minecraft.item.Item item) {
			this.slot = slot;
			this.item = item;
		}
	}

	private final ModeSetting<ShopItem> item = new ModeSetting<>(EncryptedString.of("Item"), ShopItem.Obsidian, ShopItem.class);
	private final BooleanSetting autoDrop = new BooleanSetting(EncryptedString.of("Auto Drop"), true)
			.setDescription(EncryptedString.of("Drops the bought items on the ground"));

	private int delayTicks;
	private boolean inCategoryPage;
	private boolean inBuyPage;

	public ShopBuyer() {
		super(EncryptedString.of("Shop Buyer"),
				EncryptedString.of("Automatically buys selected items from PVP shop category"),
				-1,
				Category.MISC);
		addSettings(item, autoDrop);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		delayTicks = 0;
		resetPageFlags();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		delayTicks = 0;
		resetPageFlags();
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null)
			return;

		if (delayTicks > 0) {
			delayTicks--;
			return;
		}

		ScreenHandler handler = mc.player.currentScreenHandler;
		if (!(handler instanceof GenericContainerScreenHandler container)) {
			mc.player.networkHandler.sendChatCommand("shop");
			delayTicks = 1;
			resetPageFlags();
			return;
		}

		int rows = container.getRows();
		updatePageFlags(handler);

		if (rows == 3) {
			if (isBuyConfirmPage(handler)) {
				collectPurchase(handler);
				return;
			}

			if (isMainCategoryPage(handler)) {
				buySelected(handler);
				return;
			}

			if (isRootPage(handler)) {
				openPvpCategory(handler);
				return;
			}
		}

		resetPageFlags();
	}

	private void updatePageFlags(ScreenHandler handler) {
		if (isBuyConfirmPage(handler)) {
			inBuyPage = true;
		} else if (isMainCategoryPage(handler)) {
			inCategoryPage = true;
			inBuyPage = false;
		} else if (isRootPage(handler)) {
			inCategoryPage = false;
			inBuyPage = false;
		}
	}

	private boolean isRootPage(ScreenHandler handler) {
		return handler.getSlot(13).getStack().isOf(Items.TOTEM_OF_UNDYING) && !isBuyConfirmPage(handler);
	}

	private boolean isMainCategoryPage(ScreenHandler handler) {
		return handler.getSlot(9).getStack().isOf(Items.OBSIDIAN)
				|| handler.getSlot(10).getStack().isOf(Items.END_CRYSTAL)
				|| handler.getSlot(11).getStack().isOf(Items.RESPAWN_ANCHOR)
				|| handler.getSlot(12).getStack().isOf(Items.GLOWSTONE);
	}

	private boolean isBuyConfirmPage(ScreenHandler handler) {
		for (int i = 0; i < handler.slots.size(); i++) {
			if (handler.getSlot(i).getStack().isOf(Items.LIME_STAINED_GLASS_PANE))
				return true;
		}
		return false;
	}

	private void openPvpCategory(ScreenHandler handler) {
		mc.interactionManager.clickSlot(handler.syncId, 13, 0, SlotActionType.PICKUP, mc.player);
		delayTicks = 1;
		inCategoryPage = true;
	}

	private void buySelected(ScreenHandler handler) {
		ShopItem shopItem = item.getMode();
		int slot = shopItem.slot;

		if (slot != -1 && handler.getSlot(slot).getStack().isOf(shopItem.item))
			clickAndAwait(handler, slot);
	}

	private void clickAndAwait(ScreenHandler handler, int slot) {
		mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
		delayTicks = 1;
		inBuyPage = true;
	}

	private void collectPurchase(ScreenHandler handler) {
		for (int i = 0; i < handler.slots.size(); i++) {
			if (handler.getSlot(i).getStack().isOf(Items.LIME_STAINED_GLASS_PANE) && handler.getSlot(i).getStack().getCount() == 64) {
				mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
				delayTicks = 1;
				return;
			}
		}

		for (int i = 0; i < handler.slots.size(); i++) {
			if (handler.getSlot(i).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
				mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
				delayTicks = 1;

				if (autoDrop.getValue())
					mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
							PlayerActionC2SPacket.Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, Direction.DOWN));

				resetPageFlags();
				return;
			}
		}
	}

	private void resetPageFlags() {
		inCategoryPage = false;
		inBuyPage = false;
	}
}
