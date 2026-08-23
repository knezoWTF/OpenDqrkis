package dev.lvstrng.argon.module.modules.render;

import dev.lvstrng.argon.event.events.GameRenderListener;
import dev.lvstrng.argon.event.events.HudListener;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.modules.client.ClickGUI;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.ModeSetting;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.ColorUtils;
import dev.lvstrng.argon.utils.EncryptedString;
import dev.lvstrng.argon.utils.ProjectionUtils;
import dev.lvstrng.argon.utils.RenderUtils;
import dev.lvstrng.argon.utils.Utils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public final class PlayerESP extends Module implements GameRenderListener, HudListener {
	public enum Mode {
		TwoD, ThreeD
	}

	public final ModeSetting<Mode> mode = new ModeSetting<>(EncryptedString.of("Mode"), Mode.ThreeD, Mode.class);
	private final NumberSetting alpha = new NumberSetting(EncryptedString.of("Alpha"), 0, 255, 100, 1);
	private final NumberSetting width = new NumberSetting(EncryptedString.of("Line width"), 1, 10, 1, 1);
	private final BooleanSetting tracers = new BooleanSetting(EncryptedString.of("Tracers"), false)
			.setDescription(EncryptedString.of("Draws a line from your player to the other"));
	private final BooleanSetting threeDOutline = new BooleanSetting(EncryptedString.of("3D box outline"), false);
	private final BooleanSetting twoDOutline = new BooleanSetting(EncryptedString.of("2D Outline"), false);

	public PlayerESP() {
		super(EncryptedString.of("Player ESP"),
				EncryptedString.of("Renders players through walls"),
				-1,
				Category.RENDER);
		addSettings(alpha, mode, threeDOutline, twoDOutline, width, tracers);
	}

	@Override
	public void onEnable() {
		eventManager.add(GameRenderListener.class, this);
		eventManager.add(HudListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(GameRenderListener.class, this);
		eventManager.remove(HudListener.class, this);
		super.onDisable();
	}

	@Override
	public void onGameRender(GameRenderEvent event) {
		if (mc.world == null || mc.player == null) {
			return;
		}

		Camera camera = mc.gameRenderer.getCamera();
		if (camera == null) {
			return;
		}

		event.matrices.push();
		Vec3d cameraPos = camera.getCameraPos();
		event.matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
		event.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
		event.matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

		for (PlayerEntity player : mc.world.getPlayers()) {
			if (!shouldRender(player)) {
				continue;
			}

			Box box = getRenderBox(player, event.delta).expand(0.02);
			if (mode.isMode(Mode.ThreeD)) {
				RenderUtils.renderFilledBox(
						event.matrices,
						(float) box.minX,
						(float) box.minY,
						(float) box.minZ,
						(float) box.maxX,
						(float) box.maxY,
						(float) box.maxZ,
						getColor(alpha.getValueInt()).brighter()
				);
				if (threeDOutline.getValue())
					RenderUtils.renderBoxOutline(event.matrices, box, getColor(255), width.getValueInt());
			}

			if (tracers.getValue() && mc.crosshairTarget != null) {
				RenderUtils.renderLine(event.matrices, Utils.getMainColor(255, 1), mc.crosshairTarget.getPos(), player.getLerpedPos(RenderUtils.tickProgress()));
			}
		}

		event.matrices.pop();
	}

	@Override
	public void onRenderHud(HudEvent event) {
		if (!mode.isMode(Mode.TwoD) || mc.world == null || mc.player == null) {
			return;
		}

		for (PlayerEntity player : mc.world.getPlayers()) {
			if (!shouldRender(player)) {
				continue;
			}

			ScreenBounds bounds = projectBounds(getRenderBox(player, event.delta));
			if (bounds == null || !bounds.isValid()) {
				continue;
			}

			Color outlineColor = getColor(255);
			int thickness = Math.max(1, width.getValueInt());
			if (twoDOutline.getValue()) {
				Color borderColor = new Color(0, 0, 0, Math.min(255, outlineColor.getAlpha()));
				drawOutline(event.context, bounds, thickness + 1, borderColor);
			}
			drawOutline(event.context, bounds, thickness, outlineColor);
		}
	}

	private boolean shouldRender(Entity entity) {
		return entity instanceof PlayerEntity player && player != mc.player && player.isAlive();
	}

	private Box getRenderBox(PlayerEntity player, float tickDelta) {
		double x = MathHelper.lerp(tickDelta, player.lastX, player.getX());
		double y = MathHelper.lerp(tickDelta, player.lastY, player.getY());
		double z = MathHelper.lerp(tickDelta, player.lastZ, player.getZ());
		return player.getBoundingBox().offset(x - player.getX(), y - player.getY(), z - player.getZ());
	}

	private ScreenBounds projectBounds(Box box) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		int visibleCorners = 0;

		for (double x : new double[]{box.minX, box.maxX}) {
			for (double y : new double[]{box.minY, box.maxY}) {
				for (double z : new double[]{box.minZ, box.maxZ}) {
					ProjectionUtils.ProjectedPoint projected = ProjectionUtils.project(new Vec3d(x, y, z));
					if (projected == null) {
						continue;
					}

					visibleCorners++;
					minX = Math.min(minX, projected.x());
					minY = Math.min(minY, projected.y());
					maxX = Math.max(maxX, projected.x());
					maxY = Math.max(maxY, projected.y());
				}
			}
		}

		if (visibleCorners == 0) {
			return null;
		}

		return new ScreenBounds((int) Math.floor(minX), (int) Math.floor(minY), (int) Math.ceil(maxX), (int) Math.ceil(maxY));
	}

	private void drawOutline(DrawContext context, ScreenBounds bounds, int thickness, Color color) {
		int packedColor = color.getRGB();
		context.fill(bounds.minX, bounds.minY, bounds.maxX, bounds.minY + thickness, packedColor);
		context.fill(bounds.minX, bounds.maxY - thickness, bounds.maxX, bounds.maxY, packedColor);
		context.fill(bounds.minX, bounds.minY + thickness, bounds.minX + thickness, bounds.maxY - thickness, packedColor);
		context.fill(bounds.maxX - thickness, bounds.minY + thickness, bounds.maxX, bounds.maxY - thickness, packedColor);
	}

	private Color getColor(int alpha) {
		int red = ClickGUI.red.getValueInt();
		int green = ClickGUI.green.getValueInt();
		int blue = ClickGUI.blue.getValueInt();

		if (ClickGUI.rainbow.getValue())
			return ColorUtils.getBreathingRGBColor(1, alpha);
		else
			return new Color(red, green, blue, alpha);
	}

	private record ScreenBounds(int minX, int minY, int maxX, int maxY) {
		private boolean isValid() {
			return maxX > minX && maxY > minY;
		}
	}
}
