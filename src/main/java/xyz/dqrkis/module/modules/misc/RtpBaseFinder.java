package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.ModeSetting;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.WorldUtils;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class RtpBaseFinder extends Module implements TickListener {
	public enum Region { Random, East, West, EuWest, EuCentral, Asia, Oceania }

	private static final float TARGET_PITCH = 85.45357F;
	private static final long STABILIZE_MS = 2500L;
	private static final float ROTATE_SPEED = 4.0F;

	private final ModeSetting<Region> region = new ModeSetting<>(EncryptedString.of("Region"), Region.Random, Region.class);

	private long worldReadyTime = -1L;
	private boolean waitingForWorld = true;
	private boolean rotating;
	private boolean sentRtpThisDescent;

	private boolean spawnerFound;
	private int chests;

	public RtpBaseFinder() {
		super(EncryptedString.of("RTP Base Finder"),
				EncryptedString.of("Finds bases by digging down"),
				-1,
				Category.MISC);
		addSettings(region);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);

		if (mc.currentScreen != null)
			mc.currentScreen.close();

		worldReadyTime = -1L;
		waitingForWorld = true;
		rotating = false;
		sentRtpThisDescent = false;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		releaseDigKeys();
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.interactionManager == null)
			return;

		if (mc.world == null) {
			worldReadyTime = -1L;
			waitingForWorld = true;
			return;
		}

		if (worldReadyTime == -1L)
			worldReadyTime = System.currentTimeMillis();

		if (!offhandHasTotem()) {
			disconnectWithReason("Totem Popped");
			return;
		}

		scanLoadedChunks();

		float pitch = mc.player.getPitch();

		if ((int) pitch == 85) {
			rotating = false;
			digOrRequestRtp();
		} else if (!rotating && stabilizedLongEnough()) {
			rotating = true;
		} else if (rotating) {
			rotateTowardsTarget();
		}
	}

	private boolean stabilizedLongEnough() {
		if (mc.world == null || worldReadyTime == -1L)
			return false;
		return System.currentTimeMillis() - worldReadyTime >= STABILIZE_MS;
	}

	private void rotateTowardsTarget() {
		float currentPitch = mc.player.getPitch();
		float diff = TARGET_PITCH - currentPitch;
		if (Math.abs(diff) <= ROTATE_SPEED) {
			mc.player.setPitch(TARGET_PITCH);
		} else {
			mc.player.setPitch(currentPitch + Math.signum(diff) * ROTATE_SPEED);
		}
	}

	private void digOrRequestRtp() {
		if (mc.player.getY() > 0.0D) {
			mc.options.attackKey.setPressed(true);
			mc.options.sneakKey.setPressed(true);
		} else {
			releaseDigKeys();

			if (!sentRtpThisDescent) {
				sendRtpCommand();
				sentRtpThisDescent = true;
				worldReadyTime = System.currentTimeMillis();
			}
		}
	}

	private void releaseDigKeys() {
		mc.options.attackKey.setPressed(false);
		mc.options.sneakKey.setPressed(false);
	}

	private void sendRtpCommand() {
		switch (region.getMode()) {
			case East -> mc.getNetworkHandler().sendChatCommand("rtp east");
			case West -> mc.getNetworkHandler().sendChatCommand("rtp west");
			case EuWest -> mc.getNetworkHandler().sendChatCommand("rtp eu west");
			case EuCentral -> mc.getNetworkHandler().sendChatCommand("rtp eu central");
			case Asia -> mc.getNetworkHandler().sendChatCommand("rtp asia");
			case Oceania -> mc.getNetworkHandler().sendChatCommand("rtp oceania");
			case Random -> mc.getNetworkHandler().sendChatCommand(switch ((int) (Math.random() * 6)) {
				case 0 -> "rtp east";
				case 1 -> "rtp west";
				case 2 -> "rtp eu west";
				case 3 -> "rtp eu central";
				case 4 -> "rtp asia";
				default -> "rtp oceania";
			});
		}
	}

	private void scanLoadedChunks() {
		chests = 0;
		int hoppers = 0;
		int dispensers = 0;
		int enderChests = 0;
		int shulkers = 0;
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
				else if (blockEntity instanceof HopperBlockEntity) hoppers++;
				else if (blockEntity instanceof DispenserBlockEntity) dispensers++;
				else if (blockEntity instanceof EnderChestBlockEntity) enderChests++;
				else if (blockEntity instanceof ShulkerBoxBlockEntity) shulkers++;
			}
		}

		if (chests >= 20)
			disconnectWithReason("Chest threshold reached");
		else if (shulkers >= 20)
			disconnectWithReason("Shulker threshold reached");
		else if (spawnerFound)
			disconnectWithReason("Spawner found");
	}

	private boolean offhandHasTotem() {
		if (mc.player == null)
			return false;

		ItemStack offHand = mc.player.getOffHandStack();
		return offHand.getItem() == Items.TOTEM_OF_UNDYING;
	}

	private void disconnectWithReason(String reason) {
		if (mc.world == null || worldReadyTime == -1L)
			return;

		if (System.currentTimeMillis() - worldReadyTime < STABILIZE_MS)
			return;

		setEnabled(false);
		if (mc.player != null && mc.player.networkHandler != null)
			mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("RTPBaseFinder | " + reason)));
	}
}
