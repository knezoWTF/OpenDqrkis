package dev.lvstrng.argon.module.modules.render;

import dev.lvstrng.argon.event.events.GameRenderListener;
import dev.lvstrng.argon.event.events.PacketReceiveListener;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.EncryptedString;
import dev.lvstrng.argon.utils.RenderUtils;
import dev.lvstrng.argon.utils.WorldUtils;
import net.minecraft.block.entity.*;
import net.minecraft.client.render.Camera;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.*;

public final class StorageEsp extends Module implements GameRenderListener, PacketReceiveListener {
	private final NumberSetting alpha = new NumberSetting(EncryptedString.of("Alpha"), 1, 255, 125, 1);
	private final BooleanSetting donutBypass = new BooleanSetting(EncryptedString.of("Donut Bypass"), false);
	private final BooleanSetting tracers = new BooleanSetting(EncryptedString.of("Tracers"), false)
			.setDescription(EncryptedString.of("Draws a line from your player to the storage block"));

	public StorageEsp() {
		super(EncryptedString.of("Storage ESP"),
				EncryptedString.of("Renders storage blocks through walls"),
				-1,
				Category.RENDER);
		addSettings(donutBypass, alpha, tracers);
	}

	@Override
	public void onEnable() {
		eventManager.add(PacketReceiveListener.class, this);
		eventManager.add(GameRenderListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(PacketReceiveListener.class, this);
		eventManager.remove(GameRenderListener.class, this);
		super.onDisable();
	}

	@Override
	public void onGameRender(GameRenderEvent event) {
		renderStorages(event);
	}

	private Color getColor(BlockEntity blockEntity, int a) {
		if (blockEntity instanceof TrappedChestBlockEntity) {
			return new Color(200, 91, 0, a);
		} else if (blockEntity instanceof ChestBlockEntity) {
			return new Color(156, 91, 0, a);
		} else if (blockEntity instanceof EnderChestBlockEntity) {
			return new Color(117, 0, 255, a);
		} else if (blockEntity instanceof MobSpawnerBlockEntity) {
			return new Color(138, 126, 166, a);
		} else if (blockEntity instanceof ShulkerBoxBlockEntity) {
			return new Color(134, 0, 158, a);
		} else if (blockEntity instanceof FurnaceBlockEntity) {
			return new Color(125, 125, 125, a);
		} else if (blockEntity instanceof BarrelBlockEntity) {
			return new Color(255, 140, 140, a);
		} else if (blockEntity instanceof EnchantingTableBlockEntity) {
			return new Color(80, 80, 255, a);
		} else return new Color(255, 255, 255, 0);
	}

	private void renderStorages(GameRenderEvent event) {
		Camera cam = mc.gameRenderer.getCamera();
		if (cam == null) {
			return;
		}

		event.matrices.push();
		Vec3d cameraPos = cam.getCameraPos();
		event.matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
		event.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180.0F));
		event.matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

		for (WorldChunk chunk : WorldUtils.getLoadedChunks().toList()) {
			for (BlockPos blockPos : chunk.getBlockEntityPositions()) {
				BlockEntity blockEntity = mc.world.getBlockEntity(blockPos);
				if (blockEntity == null) {
					continue;
				}

				RenderUtils.renderFilledBox(event.matrices, blockPos.getX() + 0.1F, blockPos.getY() + 0.05F, blockPos.getZ() + 0.1F, blockPos.getX() + 0.9F, blockPos.getY() + 0.85F, blockPos.getZ() + 0.9F, getColor(blockEntity, alpha.getValueInt()));

				Vec3d center = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
				if (tracers.getValue() && mc.crosshairTarget != null) {
					RenderUtils.renderLine(
							event.matrices,
							getColor(blockEntity, 255),
							mc.crosshairTarget.getPos(),
							center
					);
				}
			}
		}

		event.matrices.pop();
	}

	@Override
	public void onPacketReceive(PacketReceiveEvent event) {
		if (donutBypass.getValue()) {
			if (event.packet instanceof ChunkDeltaUpdateS2CPacket) {
				event.cancel();
			}
		}
	}
}
