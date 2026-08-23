package dev.lvstrng.argon.utils;

import dev.lvstrng.argon.font.Fonts;
import dev.lvstrng.argon.module.modules.client.ClickGUI;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

import static dev.lvstrng.argon.Argon.mc;


public final class TextRenderer {

	public static void drawString(CharSequence string, DrawContext context, int x, int y, int color) {
		boolean custom = ClickGUI.customFont.getValue();
		if (custom)
			Fonts.QUICKSAND.drawString(context, string, x, y - 8, color);
		else drawMinecraftText(string, context, x, y, color);
	}

	public static int getWidth(CharSequence string) {
		boolean custom = ClickGUI.customFont.getValue();
		if (custom)
			return Fonts.QUICKSAND.getStringWidth(string);
		else return mc.textRenderer.getWidth(string.toString()) * 2;
	}

	public static void drawCenteredString(CharSequence string, DrawContext context, int x, int y, int color) {
		boolean custom = ClickGUI.customFont.getValue();
		if (custom)
			Fonts.QUICKSAND.drawString(context, string, (x - (Fonts.QUICKSAND.getStringWidth(string) / 2)), y - 8, color);
		else drawCenteredMinecraftText(string, context, x, y, color);
	}

	public static void drawLargeString(CharSequence string, DrawContext context, int x, int y, int color) {
		boolean custom = ClickGUI.customFont.getValue();
		if (custom) {
			Matrix3x2fStack matrices = context.getMatrices();
			matrices.pushMatrix();

			matrices.scale(1.4f, 1.4f);
			Fonts.QUICKSAND.drawString(context, string, x, y - 8, color);

			matrices.popMatrix();
		} else
			drawLargerMinecraftText(string, context, x, y, color);
	}

	public static void drawMinecraftText(CharSequence string, DrawContext context, int x, int y, int color) {
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();

		matrices.scale(2f, 2f);
		context.drawText(mc.textRenderer, string.toString(), x / 2, y / 2, color, false);

		matrices.popMatrix();
	}

	public static void drawLargerMinecraftText(CharSequence string, DrawContext context, int x, int y, int color) {
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();

		matrices.scale(3f, 3f);
		context.drawText(mc.textRenderer, string.toString(), x / 3, y / 3, color, false);

		matrices.popMatrix();
	}

	public static void drawCenteredMinecraftText(CharSequence string, DrawContext context, int x, int y, int color) {
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();

		matrices.scale(2f, 2f);
		context.drawText(mc.textRenderer, string.toString(), (x / 2) - (mc.textRenderer.getWidth(string.toString()) / 2), y / 2, color, false);

		matrices.popMatrix();
	}
}
