package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.mixin.ClientPlayerInteractionManagerAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AutoTreeFarmer extends Module implements TickListener {
    private enum TreeState { SEARCHING, PLANTING, BONEMEALING, MINING, WAIT }

    private static final int RADIUS = 16;

    private final BooleanSetting hotbarRefill = new BooleanSetting(EncryptedString.of("Hotbar Refill"), true);
    private final NumberSetting saplingCount = new NumberSetting(EncryptedString.of("Sapling Count"), 1, 10, 4, 1);
    private final NumberSetting boneMealCount = new NumberSetting(EncryptedString.of("Bone Meal Count"), 1, 10, 5, 1);

    private TreeState state = TreeState.SEARCHING;
    private final List<BlockPos> saplingSpots = new ArrayList<>();
    private BlockPos farmCenter = null;
    private int spotIndex = 0;
    private int savedSlot = -1;

    public AutoTreeFarmer() {
        super(EncryptedString.of("Auto Tree Farmer"),
                EncryptedString.of("AFK farms 2x2 spruce podzol patches with auto refill"),
                -1,
                Category.MISC);
        addSettings(hotbarRefill, saplingCount, boneMealCount);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        releaseKeys();
        if (savedSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(savedSlot);
            syncSlot();
            savedSlot = -1;
        }
        resetState();
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null)
            return;

        if (hotbarRefill.getValue())
            refillHotbar();

        switch (state) {
            case SEARCHING -> searchForPodzol();
            case PLANTING -> plantSaplings();
            case BONEMEALING -> boneMealSaplings();
            case MINING -> mineTrees();
            case WAIT -> waitCycle();
        }
    }

    private void resetState() {
        state = TreeState.SEARCHING;
        farmCenter = null;
        saplingSpots.clear();
        spotIndex = 0;
    }

    private void releaseKeys() {
        if (mc.options == null) return;
        mc.options.useKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }

    private void searchForPodzol() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dy = -2; dy <= 0; dy++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    BlockPos candidate = findPodzol2x2(playerPos.add(dx, dy, dz));
                    if (candidate != null) {
                        double dist = playerPos.getSquaredDistance(candidate);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = candidate;
                        }
                    }
                }
            }
        }

        if (best == null)
            return;

        farmCenter = best;
        saplingSpots.clear();
        for (int dx = 0; dx < 2; dx++)
            for (int dz = 0; dz < 2; dz++)
                saplingSpots.add(farmCenter.add(dx, 1, dz));

        saplingSpots.sort(Comparator.comparingDouble(pos -> -mc.player.getBlockPos().getSquaredDistance(pos)));
        spotIndex = 0;
        state = TreeState.PLANTING;
        ChatUtils.info("Found 2x2 podzol at " + farmCenter.toShortString());
    }

    private BlockPos findPodzol2x2(BlockPos at) {
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                BlockPos base = at.add(dx, 0, dz);
                boolean valid = true;
                for (int x = 0; x < 2 && valid; x++)
                    for (int z = 0; z < 2 && valid; z++)
                        if (!mc.world.getBlockState(base.add(x, 0, z)).isOf(Blocks.PODZOL))
                            valid = false;
                if (valid) return base;
            }
        }
        return null;
    }

    private void plantSaplings() {
        if (spotIndex >= saplingSpots.size()) {
            spotIndex = 0;
            state = TreeState.BONEMEALING;
            return;
        }

        BlockPos pos = saplingSpots.get(spotIndex);
        if (!mc.world.getBlockState(pos).isAir()) {
            spotIndex++;
            return;
        }

        int saplingSlot = findSlotMatching(Items.SPRUCE_SAPLING);
        if (saplingSlot == -1) {
            ChatUtils.error("No spruce saplings!");
            state = TreeState.SEARCHING;
            return;
        }

        saveSlotOnce();
        mc.player.getInventory().setSelectedSlot(saplingSlot);
        syncSlot();

        lookAt(pos);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
        spotIndex++;

        if (spotIndex >= saplingSpots.size()) {
            spotIndex = 0;
            state = TreeState.BONEMEALING;
        }
    }

    private void boneMealSaplings() {
        int boneMealSlot = findSlotMatching(Items.BONE_MEAL);
        if (boneMealSlot == -1) {
            ChatUtils.error("No bone meal!");
            state = TreeState.SEARCHING;
            return;
        }

        if (anyTreeGrown()) {
            mc.options.useKey.setPressed(false);
            state = TreeState.MINING;
            ChatUtils.info("Tree grown! Starting to mine...");
            return;
        }

        saveSlotOnce();
        mc.player.getInventory().setSelectedSlot(boneMealSlot);
        syncSlot();

        BlockPos target = farmCenter.add(0, 1, 0);
        lookAt(target);

        mc.options.useKey.setPressed(true);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(target), Direction.UP, target, false));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void mineTrees() {
        mc.options.useKey.setPressed(false);

        BlockPos topLog = farmCenter.add(0, 1, 0);
        if (mc.world.getBlockState(topLog).getBlock() != Blocks.SPRUCE_LOG) {
            releaseDigKeys();
            state = TreeState.WAIT;
            return;
        }

        int axeSlot = findAxeSlot();
        if (axeSlot == -1) {
            ChatUtils.error("No axe found!");
            state = TreeState.SEARCHING;
            return;
        }

        saveSlotOnce();
        mc.player.getInventory().setSelectedSlot(axeSlot);
        syncSlot();

        lookAt(topLog);
        mc.options.attackKey.setPressed(true);
        mc.interactionManager.attackBlock(farmCenter.add(0, 1, 0), Direction.DOWN);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void waitCycle() {
        releaseDigKeys();
        state = TreeState.SEARCHING;
        farmCenter = null;
        saplingSpots.clear();
        spotIndex = 0;
    }

    private boolean anyTreeGrown() {
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                Block above = mc.world.getBlockState(farmCenter.add(dx, 1, dz)).getBlock();
                if (above != Blocks.SPRUCE_SAPLING && above != Blocks.AIR)
                    return true;
            }
        }
        return false;
    }

    private void releaseDigKeys() {
        mc.options.useKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
    }

    private void refillHotbar() {
        ensureHotbarHas(Items.BONE_MEAL, boneMealCount.getValueInt());
        ensureHotbarHas(Items.SPRUCE_SAPLING, saplingCount.getValueInt());
    }

    private void ensureHotbarHas(net.minecraft.item.Item item, int minCount) {
        if (countInInventory(item) >= minCount)
            return;

        int source = -1;
        for (int i = 9; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                source = i;
                break;
            }
        }
        if (source == -1)
            return;

        int dest = findHotbarSlotWith(item);
        if (dest == -1)
            dest = findEmptyHotbarSlot();
        if (dest == -1)
            return;

        final int syncId = mc.player.playerScreenHandler.syncId;
        final int src = source;
        final int dst = dest;
        mc.execute(() -> mc.interactionManager.clickSlot(syncId, src, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player));
        mc.execute(() -> mc.interactionManager.clickSlot(syncId, dst, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player));
        mc.execute(() -> mc.interactionManager.clickSlot(syncId, src, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player));
    }

    private int countInInventory(net.minecraft.item.Item item) {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(item))
                count += stack.getCount();
        }
        return count;
    }

    private int findHotbarSlotWith(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item))
                return i;
        }
        return -1;
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty())
                return i;
        }
        return -1;
    }

    private int findSlotMatching(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item))
                return i;
        }
        return -1;
    }

    private int findAxeSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem)
                return i;
        }
        return -1;
    }

    private void saveSlotOnce() {
        if (savedSlot == -1)
            savedSlot = mc.player.getInventory().getSelectedSlot();
    }

    private void lookAt(BlockPos target) {
        Vec3d delta = Vec3d.ofCenter(target).subtract(mc.player.getEyePos()).normalize();
        mc.player.setYaw((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        mc.player.setPitch((float) (-Math.toDegrees(Math.asin(delta.y))));
    }

    private void syncSlot() {
        if (mc.interactionManager != null)
            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).syncSlot();
    }
}