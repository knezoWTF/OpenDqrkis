package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.KeybindSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.module.setting.StringSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.DiscordWebhook;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.InventoryUtils;
import xyz.dqrkis.utils.KeyUtils;
import xyz.dqrkis.utils.RotationUtils;
import xyz.dqrkis.utils.WorldUtils;
import xyz.dqrkis.mixin.ClientPlayerInteractionManagerAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SpawnerProtect extends Module implements TickListener {
    public enum State {
        CHECKING,
        FINDSPAWNER,
        MINING,
        FINDENDERCHEST,
        OPENENDERCHEST,
        DUMPINVENTORY
    }

    private final BooleanSetting fastMode = new BooleanSetting(EncryptedString.of("Fast Mode"), true);
    private final NumberSetting emergencyDistance = new NumberSetting(EncryptedString.of("Emergency Distance"), 1, 50, 5, 0.5);
    private final BooleanSetting webhookEnabled = new BooleanSetting(EncryptedString.of("Webhook"), false);
    private final StringSetting webhookUrl = new StringSetting(EncryptedString.of("Webhook URL"), "");
    private final BooleanSetting selfPing = new BooleanSetting(EncryptedString.of("Self Ping"), false);
    private final StringSetting discordId = new StringSetting(EncryptedString.of("Discord ID"), "");

    private State state = State.CHECKING;
    private final List<BlockPos> foundSpawners = new ArrayList<>();
    private int scanRadius = 10;
    private int currentSpawnerIndex = 0;
    private int miningTicks = 0;
    private int miningStage = 0;
    private int enderChestOpenTicks = 0;
    private int dumpSlot = 0;
    private boolean playerDetected = false;
    private boolean rotationComplete = false;
    private int rotationWaitTicks = 0;
    private int dumpProgress = 0;
    private int dumpInventoryTicks = 0;
    private int restartCooldown = 0;
    private int spawnScanCooldown = 0;
    private int recheckSpawnerTicks = 0;
    private int resumeCooldown = 0;
    private boolean spawnerRecheck = false;
    private boolean rotationDone = false;
    private int noPickaxeTicks = 0;
    private BlockPos originalPos;
    private boolean positionChanged = false;
    private boolean isResuming = false;
    private int resumeWait = 0;
    private BlockPos enderChestPos;
    private boolean isLookingAtChest = false;
    private int chestOpenWait = 0;
    private int dumpSlotIndex = 0;
    private int dumpWaitTicks = 0;
    private int spawnerVerifiedTicks = 0;
    private boolean spawnerVerified = false;
    private int attackTicks = 0;
    private int verificationWait = 0;
    private boolean pickaxeSwapped = false;
    private int originalSlot = -1;
    private boolean spawnerListCleared = false;

    private final BooleanSetting fastModeSetting = fastMode;
    private final NumberSetting emergencyDistSetting = emergencyDistance;
    private final BooleanSetting webhookEnabledSetting = webhookEnabled;
    private final StringSetting webhookUrlSetting = webhookUrl;
    private final BooleanSetting selfPingSetting = selfPing;
    private final StringSetting discordIdSetting = discordId;

    public SpawnerProtect() {
        super(EncryptedString.of("Spawner Protect"),
                EncryptedString.of("Breaks all spawners around you when players are nearby and dumps your inventory in an e-chest"),
                -1,
                Category.MISC);
        addSettings(fastMode, emergencyDistance, webhookEnabled, webhookUrl, selfPing, discordId);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        resetState();
        if (mc.player != null) {
            originalPos = mc.player.getBlockPos();
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        if (mc.options.sneakKey.isPressed()) {
            mc.options.sneakKey.setPressed(false);
        }
        if (mc.options.attackKey.isPressed()) {
            mc.options.attackKey.setPressed(false);
        }
        if (mc.options.useKey.isPressed()) {
            mc.options.useKey.setPressed(false);
        }
        restoreOriginalSlot();
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        checkRestart();

        if (mc.currentScreen != null) {
            return;
        }

        if (mc.player.isSpectator()) {
            return;
        }

        // Sneak while in active states
        if (state == State.CHECKING || state == State.FINDSPAWNER || state == State.MINING) {
            mc.player.setSneaking(true);
            mc.options.sneakKey.setPressed(true);
        }

        // Check for nearby players (emergency distance)
        checkNearbyPlayers();

        // State machine
        switch (state) {
            case CHECKING -> checkState();
            case FINDSPAWNER -> findSpawners();
            case MINING -> mineSpawners();
            case FINDENDERCHEST -> findEnderChest();
            case OPENENDERCHEST -> openEnderChest();
            case DUMPINVENTORY -> dumpInventory();
        }

        // Resume cooldown
        if (resumeCooldown > 0) {
            resumeCooldown--;
        }
    }

    private void resetState() {
        state = State.CHECKING;
        foundSpawners.clear();
        spawnerRecheck = false;
        rotationDone = false;
        attackTicks = 0;
        verificationWait = 0;
        pickaxeSwapped = false;
        originalSlot = -1;
        spawnerListCleared = false;
        spawnerVerified = false;
        enderChestPos = null;
        isLookingAtChest = false;
        chestOpenWait = 0;
        dumpSlotIndex = 0;
        dumpWaitTicks = 0;
        spawnerVerifiedTicks = 0;
        spawnerVerified = false;
        attackTicks = 0;
        verificationWait = 0;
        pickaxeSwapped = false;
        originalSlot = -1;
        spawnerListCleared = false;
        spawnerVerified = false;
        enderChestPos = null;
        isLookingAtChest = false;
        chestOpenWait = 0;
        dumpSlotIndex = 0;
        dumpWaitTicks = 0;
    }

    private void checkRestart() {
        if (mc.player != null && originalPos != null) {
            BlockPos currentPos = mc.player.getBlockPos();
            if (!currentPos.equals(originalPos) && !positionChanged) {
                positionChanged = true;
                isResuming = true;
                resumeWait = 40;
                sendChat("Restart Detected");
                state = State.CHECKING;
                foundSpawners.clear();
                rotationDone = false;
                if (originalSlot != -1) {
                    mc.player.getInventory().setSelectedSlot(originalSlot);
                    syncSlot();
                    originalSlot = -1;
                }
                mc.options.attackKey.setPressed(false);
            }
            originalPos = mc.player.getBlockPos();

            if (resumeCooldown > 0) {
                resumeCooldown--;
            }
        }
    }

    private void checkState() {
        if (mc.player == null) return;

        int pickaxeSlot = findSilkTouchPickaxe();
        if (pickaxeSlot == -1) {
            ChatUtils.error("Please Get A Silk Touch Pickaxe!");
            setEnabled(false);
            return;
        }

        if (mc.currentScreen != null) {
            mc.execute(() -> {
                if (mc.currentScreen != null) {
                    mc.currentScreen.close();
                }
            });
        }

        int slot = findSilkTouchPickaxe();
        if (slot != -1) {
            originalSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(slot);
            syncSlot();
        }

        playerDetected = false;
        rotationDone = false;
        rotationWaitTicks = 0;
        state = State.FINDSPAWNER;
        spawnerRecheck = false;
        rotationDone = false;
        attackTicks = 0;
        verificationWait = 0;
        pickaxeSwapped = false;
        originalSlot = -1;
        spawnerListCleared = false;
        isLookingAtChest = false;
        chestOpenWait = 0;
        dumpSlotIndex = 0;
        dumpWaitTicks = 0;
        if (mc.player != null) {
            BlockPos pos = mc.player.getBlockPos();
        }
    }

    private void findSpawners() {
        foundSpawners.clear();
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = 10; // fixed scan radius

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = mc.player.getBlockPos().add(dx, dy, dz);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.SPAWNER) {
                        foundSpawners.add(pos);
                    }
                }
            }
        }

        if (foundSpawners.isEmpty()) {
            sendChat("No Spawners Found");
            state = State.CHECKING;
        } else {
            sendChat("Found " + foundSpawners.size() + " Spawners");
            currentSpawnerIndex = 0;
            spawnerRecheck = false;
            rotationDone = false;
            state = State.MINING;
        }
    }

    private void mineSpawners() {
        if (currentSpawnerIndex >= foundSpawners.size()) {
            sendChat("Mined Spawners");
            if (originalSlot != -1) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
                syncSlot();
                originalSlot = -1;
            }
            mc.options.attackKey.setPressed(false);
            state = State.FINDENDERCHEST;
            foundSpawners.clear();
            currentSpawnerIndex = 0;
            return;
        }

        BlockPos target = foundSpawners.get(currentSpawnerIndex);
        Block block = mc.world.getBlockState(target).getBlock();

        if (block != Blocks.SPAWNER && !spawnerRecheck) {
            spawnerRecheck = true;
            spawnerVerifiedTicks = 0;
            mc.options.attackKey.setPressed(false);
        } else if (spawnerRecheck) {
            spawnerVerifiedTicks++;
            if (spawnerVerifiedTicks < 10) {
                return;
            }
            Block blockCheck = mc.world.getBlockState(target).getBlock();
            if (blockCheck != Blocks.SPAWNER) {
                sendChat("Spawner " + (currentSpawnerIndex + 1) + " Already Broken, Moving To Next");
                currentSpawnerIndex++;
                spawnerRecheck = false;
                spawnerVerified = false;
                spawnerVerifiedTicks = 0;
                attackTicks = 0;
                rotationDone = false;
                return;
            } else {
                int spawnerNum = currentSpawnerIndex + 1;
                int total = foundSpawners.size();
                sendChat("Spawner " + spawnerNum + " Detected After Verification");
                spawnerRecheck = false;
                spawnerVerified = false;
                spawnerVerifiedTicks = 0;
            }
        }

        if (!rotationDone && originalSlot == -1) {
            int pickaxeSlot = findSilkTouchPickaxe();
            if (pickaxeSlot == -1) {
                sendError("No Silk Touch Pickaxe Found In Hotbar!");
                state = State.CHECKING;
                return;
            }
            originalSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(pickaxeSlot);
            syncSlot();
            pickaxeSwapped = true;
        }

        Vec3d targetVec = Vec3d.ofCenter(target);
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d diff = targetVec.subtract(eyePos).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
        float pitch = (float) (-Math.toDegrees(Math.asin(diff.y)));

        if (!rotationDone) {
            mc.options.attackKey.setPressed(false);
            if (!RotationUtils.isRotating()) {
                RotationUtils.rotateTo(yaw, pitch, () -> {
                    rotationDone = true;
                    rotationWaitTicks = 0;
                });
            }
        } else {
            attackTicks++;
            mc.options.attackKey.setPressed(true);
            if (attackTicks % 5 == 0) {
                Block checkBlock = mc.world.getBlockState(target).getBlock();
                if (block != Blocks.SPAWNER) {
                    int mined = currentSpawnerIndex + 1;
                    int total = foundSpawners.size();
                    sendChat("Mined Spawner " + mined + "/" + total);
                    mc.options.attackKey.setPressed(false);
                    currentSpawnerIndex++;
                    rotationDone = false;
                    attackTicks = 0;
                }
            }
        }
    }

    private void findEnderChest() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos found = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -10; dy <= 10; dy++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos pos = mc.player.getBlockPos().add(dx, dy, dz);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
                        double dist = playerPos.getSquaredDistance(pos);
                        if (dist < bestDist) {
                            bestDist = dist;
                            enderChestPos = pos;
                        }
                    }
                }
            }
        }

        if (enderChestPos == null) {
            sendError("No E-Chest Found!");
            state = State.CHECKING;
        } else {
            sendChat("Found E-Chest at " + enderChestPos.toShortString());
            mc.player.setSneaking(false);
            mc.options.sneakKey.setPressed(false);
            state = State.OPENENDERCHEST;
        }
    }

    private void openEnderChest() {
        if (enderChestPos == null) {
            state = State.CHECKING;
            return;
        }

        if (mc.world.getBlockState(enderChestPos).getBlock() != Blocks.ENDER_CHEST) {
            sendError("E-Chest vanished!");
            state = State.CHECKING;
            return;
        }

        if (!isLookingAtChest && !rotationDone) {
            Vec3d target = Vec3d.ofCenter(enderChestPos);
            Vec3d eyePos = mc.player.getEyePos();
            Vec3d diff = target.subtract(eyePos).normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
            float pitch = (float) (-Math.toDegrees(Math.asin(diff.y)));

            if (!RotationUtils.isRotating()) {
                RotationUtils.rotateTo(yaw, pitch, () -> {
                    rotationDone = true;
                    chestOpenWait = 10;
                });
            }
        } else if (rotationDone) {
            if (chestOpenWait > 0) {
                chestOpenWait--;
            } else {
                BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(enderChestPos), Direction.UP, enderChestPos, false);
                ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                if (result.isAccepted()) {
                    isLookingAtChest = true;
                    chestOpenWait = 10;
                    state = State.DUMPINVENTORY;
                    dumpProgress = 0;
                    dumpWaitTicks = 0;
                } else {
                    rotationDone = false;
                }
            }
        }
    }

    private void dumpInventory() {
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler != null && handler != mc.player.playerScreenHandler) {
            if (dumpProgress < 36) {
                int slot = 36 + dumpProgress; // start after hotbar
                if (slot < mc.player.playerScreenHandler.slots.size()) {
                    mc.execute(() -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player));
                    dumpProgress++;
                } else {
                    mc.execute(() -> {
                        if (mc.currentScreen != null) {
                            mc.player.closeHandledScreen();
                        }
                    });
                    sendChat("Dumped Inventory");
                    sendChat("Spawners Saved!");
                    enderChestPos = null;
                    isLookingAtChest = false;
                    state = State.CHECKING;
                }
            } else {
                mc.execute(() -> {
                    if (mc.currentScreen != null) {
                        mc.player.closeHandledScreen();
                    }
                });
                sendChat("Dumped Inventory");
                sendChat("Spawners Saved!");
                enderChestPos = null;
                isLookingAtChest = false;
                state = State.CHECKING;
            }
        } else {
            if (!isLookingAtChest) {
                sendError("E-Chest Not Opened");
                isLookingAtChest = false;
                state = State.OPENENDERCHEST;
            }
        }
    }

    private void checkNearbyPlayers() {
        if (emergencyDistSetting.getValue() > 0) {
            double range = emergencyDistSetting.getValue();
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || player.isSpectator()) continue;
                if (player.getName().getString().equalsIgnoreCase("venom")) continue;

                double dist = mc.player.distanceTo(player);
                if (dist <= range) {
                    sendEmergencyAlert(player.getName().getString(), dist);
                    if (webhookEnabledSetting.getValue() && !webhookUrlSetting.getValue().trim().isEmpty()) {
                        String ping = "";
                        if (selfPingSetting.getValue() && !discordIdSetting.getValue().trim().isEmpty()) {
                            ping = "<@" + discordIdSetting.getValue().trim() + "> ";
                        }
                        new DiscordWebhook(webhookUrlSetting.getValue())
                                .title("Player Detected!")
                                .description("A player was detected near spawners")
                                .attach(Path.of("")) // no attachment
                                .sendAsync();
                    }
                    playerDetected = true;
                    return;
                }
            }
        }
    }

    private void sendEmergencyAlert(String playerName, double distance) {
        String msg = String.format("Emergency Distance Triggered! Player: %s (%.1f blocks)", playerName, distance);
        sendChat(msg);
    }

    private int findSilkTouchPickaxe() {
        if (mc.player == null) return -1;
        RegistryEntry<Enchantment> silkTouch;
        try {
            silkTouch = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        } catch (Exception e) {
            return -1;
        }
        if (silkTouch == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.DIAMOND_PICKAXE) || stack.isOf(Items.NETHERITE_PICKAXE) || stack.isOf(Items.IRON_PICKAXE)) {
                if (EnchantmentHelper.getLevel(silkTouch, stack) > 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void syncSlot() {
        if (mc.interactionManager != null) {
            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).syncSlot();
        }
    }

    private void sendChat(String message) {
        ChatUtils.info(message);
    }

    private void sendError(String message) {
        ChatUtils.error(message);
    }

    private void restoreOriginalSlot() {
        if (originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            syncSlot();
            originalSlot = -1;
        }
    }
}