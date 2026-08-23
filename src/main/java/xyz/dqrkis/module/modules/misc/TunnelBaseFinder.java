package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.ModeSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class TunnelBaseFinder extends Module implements TickListener {
    public enum MiningStyle { CRAWL, STANDING, AMETHYST }

    private static final int SCAN_INTERVAL_TICKS = 40;
    private static final long MIN_SESSION_MS = 5000L;

    private final ModeSetting<MiningStyle> miningStyle = new ModeSetting<>(EncryptedString.of("Mining Style"), MiningStyle.AMETHYST, MiningStyle.class);
    private final BooleanSetting spawnerCritical = new BooleanSetting(EncryptedString.of("Spawner Critical"), false);
    private final BooleanSetting humanize = new BooleanSetting(EncryptedString.of("Humanize"), true);
    private final NumberSetting delayRandomness = new NumberSetting(EncryptedString.of("Delay Randomness"), 0, 10, 3, 1);

    private Direction tunnelDirection = Direction.NORTH;
    private int scanCounter;
    private int breakDelayTicks;
    private boolean miningPaused;
    private long sessionStartMs = -1L;
    private boolean waitingForStabilize = true;

    private int chests, shulkers, pistons;
    private boolean spawnerFound;

    public TunnelBaseFinder() {
        super(EncryptedString.of("Tunnel Base Finder"),
                EncryptedString.of("Digs in tunnels until you find a base"),
                -1,
                Category.MISC);
        addSettings(miningStyle, spawnerCritical, humanize, delayRandomness);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        tunnelDirection = mc.player != null ? mc.player.getHorizontalFacing() : Direction.NORTH;
        scanCounter = 0;
        breakDelayTicks = 0;
        sessionStartMs = -1L;
        waitingForStabilize = true;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        releaseKeys();
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null)
            return;

        if (mc.world == null || mc.world.getRegistryManager() == null) {
            sessionStartMs = -1L;
            waitingForStabilize = true;
            return;
        }

        if (sessionStartMs == -1L)
            sessionStartMs = System.currentTimeMillis();
        else if (waitingForStabilize
                && System.currentTimeMillis() - sessionStartMs >= MIN_SESSION_MS)
            waitingForStabilize = false;

        if (!offhandHasTotem()) {
            disconnectWithReason("Totem Popped");
            return;
        }

        if (++scanCounter >= SCAN_INTERVAL_TICKS) {
            scanCounter = 0;
            scanLoadedChunks();
        }

        digStep();
    }

    private void digStep() {
        if (!holdingPickaxe()) {
            releaseKeys();
            return;
        }

        BlockPos feet = mc.player.getBlockPos();
        BlockPos head = feet.add(0, 1, 0);

        if (isDiggable(mc.world.getBlockState(head).getBlock())) {
            lookAtBlock(head);
            holdAttack(true);
            pauseBetweenBreaks();
            return;
        }

        if (isDiggable(mc.world.getBlockState(feet).getBlock())) {
            lookAtBlock(feet);
            holdAttack(true);
            pauseBetweenBreaks();
            return;
        }

        holdAttack(false);

        BlockPos ahead = feet.offset(tunnelDirection);
        BlockPos aheadDown = ahead.add(0, -1, 0);
        BlockState belowAheadState = mc.world.getBlockState(aheadDown);
        Block belowAheadBlock = belowAheadState.getBlock();
        BlockState atAheadState = mc.world.getBlockState(ahead);
        Block atAheadBlock = atAheadState.getBlock();

        if (atAheadState.isAir() && !belowAheadState.isAir()) {
            faceDirection(tunnelDirection);
            mc.options.forwardKey.setPressed(true);
        } else {
            mc.options.forwardKey.setPressed(false);
            if (!belowAheadState.isAir())
                tunnelDirection = tunnelDirection.rotateYClockwise();
        }
    }

    private void holdAttack(boolean pressed) {
        if (pressed && breakDelayTicks > 0) {
            breakDelayTicks--;
            mc.options.attackKey.setPressed(false);
            return;
        }
        mc.options.attackKey.setPressed(pressed);
    }

    private void pauseBetweenBreaks() {
        if (!humanize.getValue())
            return;
        int base = 3;
        int variance = delayRandomness.getValueInt();
        breakDelayTicks = Math.max(1, base + (int) (Math.random() * variance * 2) - variance);
    }

    private boolean isDiggable(Block block) {
        return !block.getDefaultState().isAir()
                && block != Blocks.BEDROCK
                && block != Blocks.LAVA
                && block != Blocks.WATER;
    }

    private boolean holdingPickaxe() {
        return mc.player.getMainHandStack().getItem().toString().contains("pickaxe");
    }

    private void lookAtBlock(BlockPos pos) {
        Vec3d delta = Vec3d.ofCenter(pos).subtract(mc.player.getEyePos()).normalize();
        mc.player.setYaw((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        mc.player.setPitch((float) (-Math.toDegrees(Math.asin(delta.y))));
    }

    private void faceDirection(Direction direction) {
        float yaw = switch (direction) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> mc.player.getYaw();
        };
        mc.player.setYaw(yaw);
        mc.player.setPitch(0.0F);
    }

    private void scanLoadedChunks() {
        chests = 0;
        shulkers = 0;
        pistons = 0;
        spawnerFound = false;

        for (var chunk : WorldUtils.getLoadedChunks().toList()) {
            for (BlockPos pos : chunk.getBlockEntityPositions()) {
                BlockEntity blockEntity = mc.world.getBlockEntity(pos);
                if (blockEntity == null)
                    continue;

                if (blockEntity instanceof MobSpawnerBlockEntity)
                    spawnerFound = true;

                if (blockEntity.getPos().getY() > 0)
                    continue;

                if (blockEntity instanceof ChestBlockEntity) chests++;
                else if (blockEntity instanceof ShulkerBoxBlockEntity) shulkers++;
                else if (blockEntity instanceof PistonBlockEntity) pistons++;
            }
        }

        if (chests >= 35)
            disconnectWithReason("BASE");
        else if (shulkers >= 20)
            disconnectWithReason("Shulker threshold reached");
        else if (spawnerFound && spawnerCritical.getValue())
            disconnectWithReason("Spawner found");
    }

    private boolean offhandHasTotem() {
        return mc.player != null && mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
    }

    private void disconnectWithReason(String reason) {
        setEnabled(false);
        releaseKeys();
        if (mc.player != null && mc.player.networkHandler != null)
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("TunnelBaseFinder | " + reason)));
    }

    private void releaseKeys() {
        if (mc.options == null) return;
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }
}