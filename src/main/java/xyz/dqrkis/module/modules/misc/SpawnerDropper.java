package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class SpawnerDropper extends Module implements TickListener {
    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay (ticks)"), 1, 40, 10, 1);
    private final BooleanSetting boneOnly = new BooleanSetting(EncryptedString.of("Bone Only"), false);

    private enum State { IDLE, FINDING_SPAWNER, OPENING_SPAWNER, CLICKING_DROP_ALL, CLICKING_NEXT_PAGE }
    private State state = State.IDLE;
    private BlockPos spawnerPos;
    private int ticks;

    public SpawnerDropper() {
        super(EncryptedString.of("Spawner Dropper"), EncryptedString.of("Automates dropping items from a spawner."), -1, Category.MISC);
        addSettings(delay, boneOnly);
    }

    @Override public void onEnable() { eventManager.add(TickListener.class, this); state = State.FINDING_SPAWNER; spawnerPos = null; ticks = 0; super.onEnable(); }
    @Override public void onDisable() { eventManager.remove(TickListener.class, this); super.onDisable(); }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (ticks > 0) { ticks--; return; }
        switch (state) {
            case FINDING_SPAWNER -> {
                spawnerPos = findNearestSpawner();
                if (spawnerPos == null) { ChatUtils.error("No spawner found nearby"); setEnabled(false); return; }
                state = State.OPENING_SPAWNER; ticks = delay.getValueInt();
            }
            case OPENING_SPAWNER -> {
                if (spawnerPos == null) { state = State.FINDING_SPAWNER; return; }
                lookAt(spawnerPos);
                BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(spawnerPos), Direction.UP, spawnerPos, false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                state = State.CLICKING_DROP_ALL; ticks = delay.getValueInt() + 4;
            }
            case CLICKING_DROP_ALL -> {
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) { state = State.FINDING_SPAWNER; return; }
                var handler = screen.getScreenHandler();
                // Click "Drop All" button - in original this is a specific slot, simplified to slot 0 if it contains bone logic
                boolean clicked = false;
                for (int i = 0; i < Math.min(handler.slots.size(), 54); i++) {
                    var stack = handler.getSlot(i).getStack();
                    boolean isBone = stack.isOf(Items.BONE);
                    boolean isBoneBlock = stack.isOf(Items.BONE_BLOCK);
                    if (boneOnly.getValue() ? (isBone || isBoneBlock) : !stack.isEmpty()) {
                        // Original drops via slot click; simplified to QUICK_MOVE to player then throw would be next step
                        // For spawner GUI the "Drop All" is often a pane at slot 8 or similar; try slot 8
                        if (handler.slots.size() > 8 && handler.getSlot(8).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                            mc.interactionManager.clickSlot(handler.syncId, 8, 0, SlotActionType.PICKUP, mc.player);
                            clicked = true; break;
                        }
                    }
                }
                if (!clicked && handler.slots.size() > 8) {
                    mc.interactionManager.clickSlot(handler.syncId, 8, 0, SlotActionType.PICKUP, mc.player);
                }
                state = State.CLICKING_NEXT_PAGE; ticks = delay.getValueInt();
            }
            case CLICKING_NEXT_PAGE -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    var handler = screen.getScreenHandler();
                    // Next page arrow at slot 53 in many spawner GUIs
                    if (handler.slots.size() > 53 && handler.getSlot(53).getStack().isOf(Items.ARROW)) {
                        mc.interactionManager.clickSlot(handler.syncId, 53, 0, SlotActionType.PICKUP, mc.player);
                        state = State.CLICKING_DROP_ALL; ticks = delay.getValueInt();
                    } else {
                        mc.player.closeHandledScreen();
                        ChatUtils.info("Finished dropping from spawner");
                        setEnabled(false);
                    }
                } else {
                    setEnabled(false);
                }
            }
            default -> {}
        }
    }

    private BlockPos findNearestSpawner() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos best = null; double bestDist = Double.MAX_VALUE;
        for (int dx = -10; dx <= 10; dx++) for (int dy = -5; dy <= 5; dy++) for (int dz = -10; dz <= 10; dz++) {
            BlockPos pos = playerPos.add(dx, dy, dz);
            if (mc.world.getBlockState(pos).isOf(Blocks.SPAWNER)) {
                double d = playerPos.getSquaredDistance(pos);
                if (d < bestDist) { bestDist = d; best = pos; }
            }
        }
        return best;
    }

    private void lookAt(BlockPos pos) {
        Vec3d delta = Vec3d.ofCenter(pos).subtract(mc.player.getEyePos()).normalize();
        mc.player.setYaw((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        mc.player.setPitch((float) (-Math.toDegrees(Math.asin(delta.y))));
    }
}
