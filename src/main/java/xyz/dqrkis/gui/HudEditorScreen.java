package xyz.dqrkis.gui;

import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.module.modules.render.HUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class HudEditorScreen extends Screen {
    private HUD hud;
    private boolean dragging;
    private int dragOffsetX, dragOffsetY;
    private int hudX, hudY;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
        this.hud = Dqrkis.INSTANCE.getModuleManager().getModule(HUD.class);
        if (hud != null) {
            // Load current positions from HUD module if it has them; fallback to defaults
            hudX = 10;
            hudY = 10;
        }
    }

    @Override
    protected void init() {
        int buttonWidth = 100;
        int buttonHeight = 20;
        int centerX = width / 2;
        int bottomY = height - 30;

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), btn -> {
            savePositions();
            close();
        }).dimensions(centerX - buttonWidth - 10, bottomY, buttonWidth, buttonHeight).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> close())
                .dimensions(centerX + 10, bottomY, buttonWidth, buttonHeight).build());
        super.init();
    }

    private void savePositions() {
        // In full 1:1 port this would write to Class1619's x/y/z/A/B/C/D/E/F/G/H/I/J/K/L/M settings via .a(double)
        // For argon HUD, positions are handled via HUD module's own settings; stub preserves 1:1 flow
        if (hud != null) {
            // Persist dragged offsets if HUD exposes them; no-op for now to keep 1:1 visuals
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent background like original
        context.fill(0, 0, width, height, 0x80000000);
        super.render(context, mouseX, mouseY, delta);

        // Header
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("HUD Editor - Drag to reposition"), width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Press ESC or Cancel to exit without saving"), width / 2, 24, 0xFFAAAAAA);

        // Preview box for HUD watermark area (matches Class1619's 2x2 podzol-search-size style preview)
        int previewW = 160;
        int previewH = 20;
        int previewX = hudX;
        int previewY = hudY;

        // Draggable outline
        context.fill(previewX - 1, previewY - 1, previewX + previewW + 1, previewY + previewH + 1, 0xFFFF4444);
        context.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0xFF181820);
        context.drawText(textRenderer, Text.literal("Dqrkis | 120 FPS  45ms  play.example.net"), previewX + 6, previewY + 6, 0xFFFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Drag me"), previewX + previewW / 2 - textRenderer.getWidth("Drag me") / 2, previewY + previewH + 4, 0xFF888888, false);

        // Crosshair for alignment
        context.fill(width / 2, 0, width / 2 + 1, height, 0x20FFFFFF);
        context.fill(0, height / 2, width, height / 2 + 1, 0x20FFFFFF);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int previewW = 160;
        int previewH = 20;
        if (mouseX >= hudX && mouseX < hudX + previewW && mouseY >= hudY && mouseY < hudY + previewH) {
            dragging = true;
            dragOffsetX = (int) (mouseX - hudX);
            dragOffsetY = (int) (mouseY - hudY);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
        if (dragging) {
            hudX = (int) (click.x() - dragOffsetX);
            hudY = (int) (click.y() - dragOffsetY);
            // Clamp to screen
            hudX = Math.max(2, Math.min(width - 162, hudX));
            hudY = Math.max(2, Math.min(height - 42, hudY));
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        dragging = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean shouldPause() { return false; }
}
