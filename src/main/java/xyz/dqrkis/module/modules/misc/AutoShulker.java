package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.ModeSetting;
import xyz.dqrkis.module.setting.StringSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public final class AutoShulker extends Module implements TickListener {
    public enum ItemMode { SHULKERS, SHELLS }
    public enum Action { BUY_AND_SELL, BUY_ONLY, SELL_ONLY, ORDER_ONLY }

    private final ModeSetting<ItemMode> itemMode = new ModeSetting<>(EncryptedString.of("Item Mode"), ItemMode.SHULKERS, ItemMode.class);
    private final ModeSetting<Action> action = new ModeSetting<>(EncryptedString.of("Action"), Action.BUY_AND_SELL, Action.class);
    private final StringSetting minPrice = new StringSetting(EncryptedString.of("Min Price"), "850");
    private final StringSetting targetPlayer = new StringSetting(EncryptedString.of("Target Player"), "");
    private final BooleanSetting autoDrop = new BooleanSetting(EncryptedString.of("Auto Drop"), false);

    private int ticks;
    private State state = State.IDLE;

    private enum State { IDLE, OPEN_SHOP, SHOP_BUY, SHOP_CONFIRM, OPEN_ORDERS, ORDERS_SELECT, CYCLE_PAUSE }

    public AutoShulker() {
        super(EncryptedString.of("Auto Shulker"), EncryptedString.of("Automatically buys/sells shulkers and shulker shells with player targeting"), -1, Category.MISC);
        addSettings(itemMode, action, minPrice, targetPlayer, autoDrop);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        ticks = 0;
        state = State.IDLE;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (ticks > 0) { ticks--; return; }

        ScreenHandler handler = mc.player.currentScreenHandler;
        if (!(handler instanceof GenericContainerScreenHandler container)) {
            if (state == State.IDLE) {
                if (action.getMode() == Action.SELL_ONLY || action.getMode() == Action.BUY_AND_SELL) {
                    // Alternate between shop and orders; simplified: open shop
                    mc.player.networkHandler.sendChatCommand("shop");
                    state = State.OPEN_SHOP;
                    ticks = 20;
                }
            }
            return;
        }

        int rows = container.getRows();
        switch (state) {
            case OPEN_SHOP -> {
                // Shop GUI: look for shulker related slots (simplified placeholder)
                // In original: clicks shop category then item slot then confirm
                // Simplified: try to click any shulker/lime pane that matches price filter
                boolean bought = tryBuyFromShop(container);
                if (bought) { state = State.SHOP_CONFIRM; ticks = 10; }
                else { mc.player.closeHandledScreen(); state = State.CYCLE_PAUSE; ticks = 40; }
            }
            case SHOP_CONFIRM -> {
                // Confirm purchase (slot 15/confirm lime pane)
                if (container.getSlot(15).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                    mc.interactionManager.clickSlot(container.syncId, 15, 0, SlotActionType.PICKUP, mc.player);
                }
                mc.player.closeHandledScreen();
                state = State.CYCLE_PAUSE;
                ticks = 20;
            }
            case CYCLE_PAUSE -> {
                state = State.IDLE;
                ticks = 60;
            }
            default -> { state = State.IDLE; ticks = 10; }
        }
    }

    private boolean tryBuyFromShop(ScreenHandler handler) {
        for (int i = 0; i < handler.slots.size(); i++) {
            var stack = handler.getSlot(i).getStack();
            boolean isTarget = (itemMode.getMode() == ItemMode.SHULKERS && stack.isOf(Items.SHULKER_BOX))
                    || (itemMode.getMode() == ItemMode.SHELLS && stack.isOf(Items.SHULKER_SHELL));
            if (isTarget) {
                // Price filter via lore would go here; simplified to always attempt
                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                return true;
            }
        }
        return false;
    }
}