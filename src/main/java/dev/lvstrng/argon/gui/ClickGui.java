package dev.lvstrng.argon.gui;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.modules.client.ClickGUI;
import dev.lvstrng.argon.utils.ColorUtils;
import dev.lvstrng.argon.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dev.lvstrng.argon.Argon.mc;

public final class ClickGui extends Screen {
	public List<Window> windows = new ArrayList<>();
	public Color currentColor;
	private static final StackWalker sw = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	public ClickGui() {
		super(Text.empty());

		int offsetX = 50;
		for (Category category : Category.values()) {
			windows.add(new Window(offsetX, 50, 230, 30, category, this));
			offsetX += 250;
		}
	}

	public boolean isDraggingAlready() {
		for(Window window : windows)
			if(window.dragging)
				return true;

		return false;
	}

	@Override
	protected void setInitialFocus() {
		if (client == null) {
			return;
		}
		super.setInitialFocus();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (mc.currentScreen == this) {
			if (Argon.INSTANCE.previousScreen != null)
				Argon.INSTANCE.previousScreen.render(context, 0, 0, delta);

			if (currentColor == null)
				currentColor = new Color(0, 0, 0, 0);
			else currentColor = new Color(0, 0, 0, currentColor.getAlpha());

			if (currentColor.getAlpha() != (ClickGUI.background.getValue() ? 200 : 0))
				currentColor = ColorUtils.smoothAlphaTransition(0.05F, ClickGUI.background.getValue() ? 200 : 0, currentColor);

			if (mc.currentScreen instanceof ClickGui)
				context.fill(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight(), currentColor.getRGB());

			RenderUtils.unscaledProjection(context);
			mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
			mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
			super.render(context, mouseX, mouseY, delta);

			for (Window window : windows) {
				window.render(context, mouseX, mouseY, delta);
				window.updatePosition(mouseX, mouseY, delta);
			}

			RenderUtils.scaledProjection(context);
		}
	}

	@Override
	public boolean keyPressed(KeyInput keyInput) {
		int keyCode = keyInput.key();
		int scanCode = keyInput.scancode();
		int modifiers = keyInput.modifiers();

		for (Window window : windows)
			window.keyPressed(keyCode, scanCode, modifiers);

		return super.keyPressed(keyInput);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		double mouseX = click.x();
		double mouseY = click.y();
		int button = click.button();

		mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();

		for (Window window : windows)
			window.mouseClicked(mouseX, mouseY, button);

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		double mouseX = click.x();
		double mouseY = click.y();
		int button = click.button();

		mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();

		for (Window window : windows)
			window.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);

		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		MinecraftClient mc = MinecraftClient.getInstance();
		mouseY *= mc.getWindow().getScaleFactor();

		for (Window window : windows)
			window.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		Argon.INSTANCE.getModuleManager().getModule(ClickGUI.class).setEnabledStatus(false);
		onGuiClose();
	}

	public void onGuiClose() {
		mc.setScreenAndRender(Argon.INSTANCE.previousScreen);
		currentColor = null;

		for (Window window : windows)
			window.onGuiClose();
	}

	@Override
	public boolean mouseReleased(Click click) {
		double mouseX = click.x();
		double mouseY = click.y();
		int button = click.button();

		mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();

		for (Window window : windows)
			window.mouseReleased(mouseX, mouseY, button);

		return super.mouseReleased(click);
	}
}
