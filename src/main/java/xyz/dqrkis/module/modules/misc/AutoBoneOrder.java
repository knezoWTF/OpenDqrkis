package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.ItemSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.module.setting.StringSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class AutoBoneOrder extends Module implements TickListener {
    private final StringSetting orderName = new StringSetting(EncryptedString.of("Order Name"), "bones");
    private final ItemSetting orderItem = new ItemSetting(EncryptedString.of("Order Item"), Items.BONE);
    private final NumberSetting clickDelay = new NumberSetting(EncryptedString.of("Click Delay (ticks)"), 1, 10, 2, 1);
    private final NumberSetting guiTimeout = new NumberSetting(EncryptedString.of("GUI Timeout (ticks)"), 20, 200, 60, 5);

    private enum State { IDLE, OPEN_ORDERS, WAIT_ORDERS_GUI, CLICK_SLOT_51, WAIT_SECOND_GUI, CLICK_TARGET_ITEM, WAIT_THIRD_GUI, CLICK_CHEST_SLOT, WAIT_ITEMS_GUI, COLLECT_ITEMS }
    private State state = State.IDLE;
    private int ticks;
    private long stateEnteredAt;

    public AutoBoneOrder() {
        super(EncryptedString.of("Auto Bone Order"), EncryptedString.of("Automates ordering bones"), -1, Category.MISC);
        addSettings(orderName, orderItem, clickDelay, guiTimeout);
    }

    @Override public void onEnable() { eventManager.add(TickListener.class, this); state = State.IDLE; ticks = 0; super.onEnable(); }
    @Override public void onDisable() { eventManager.remove(TickListener.class, this); super.onDisable(); }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        switch (state) {
            case IDLE -> {
                mc.player.networkHandler.sendChatCommand("order");
                state = State.WAIT_ORDERS_GUI; stateEnteredAt = System.currentTimeMillis(); ticks = clickDelay.getValueInt();
            }
            case WAIT_ORDERS_GUI -> {
                if (ticks-- > 0) return;
                if (mc.currentScreen instanceof GenericContainerScreen) { state = State.CLICK_SLOT_51; ticks = clickDelay.getValueInt(); }
                else if (System.currentTimeMillis() - stateEnteredAt > guiTimeout.getValueInt() * 50L) { ChatUtils.error("Order GUI timeout"); setEnabled(false); }
            }
            case CLICK_SLOT_51 -> {
                if (ticks-- > 0) return;
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    var handler = screen.getScreenHandler();
                    if (handler.slots.size() > 51) mc.interactionManager.clickSlot(handler.syncId, 51, 0, SlotActionType.PICKUP, mc.player);
                    state = State.WAIT_SECOND_GUI; stateEnteredAt = System.currentTimeMillis(); ticks = clickDelay.getValueInt();
                }
            }
            case WAIT_SECOND_GUI -> {
                if (ticks-- > 0) return;
                if (mc.currentScreen instanceof GenericContainerScreen) { state = State.CLICK_TARGET_ITEM; ticks = clickDelay.getValueInt(); }
            }
            case CLICK_TARGET_ITEM -> {
                if (ticks-- > 0) return;
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    var handler = screen.getScreenHandler();
                    // Find slot containing the desired item (bone)
                    for (int i = 0; i < Math.min(handler.slots.size(), 54); i++) {
                        if (handler.getSlot(i).getStack().isOf(orderItem.getItem())) {
                            mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                            break;
                        }
                    }
                    state = State.WAIT_THIRD_GUI; stateEnteredAt = System.currentTimeMillis(); ticks = clickDelay.getValueInt();
                }
            }
            case WAIT_THIRD_GUI -> {
                if (ticks-- > 0) return;
                if (mc.currentScreen instanceof GenericContainerScreen) { state = State.CLICK_CHEST_SLOT; ticks = clickDelay.getValueInt(); }
            }
            case CLICK_CHEST_SLOT -> {
                if (ticks-- > 0) return;
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    // Click chest slots 11-16 etc. (original checks 11-16,20-24)
                    int[] chestSlots = {11,12,13,14,15,16,20,21,22,23,24};
                    for (int s : chestSlots) if (screen.getScreenHandler().slots.size() > s && screen.getScreenHandler().getSlot(s).getStack().isOf(Items.CHEST)) { mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, s, 0, SlotActionType.PICKUP, mc.player); break; }
                    state = State.WAIT_ITEMS_GUI; stateEnteredAt = System.currentTimeMillis(); ticks = clickDelay.getValueInt();
                }
            }
            case WAIT_ITEMS_GUI -> {
                if (ticks-- > 0) return;
                if (mc.currentScreen instanceof GenericContainerScreen) { state = State.COLLECT_ITEMS; ticks = clickDelay.getValueInt(); }
            }
            case COLLECT_ITEMS -> {
                if (ticks-- > 0) return;
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    boolean collected = false;
                    for (int i = 0; i < Math.min(screen.getScreenHandler().slots.size(), 54); i++) {
                        if (screen.getScreenHandler().getSlot(i).getStack().isOf(orderItem.getItem())) {
                            mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, i, 0, SlotActionType.PICKUP, mc.player);
                            collected = true; break;
                        }
                    }
                    mc.player.closeHandledScreen();
                    ChatUtils.info(collected ? "Collected " + orderItem.getItem().toString() : "No items found to collect");
                    setEnabled(false);
                }
            }
        }
    }
}
