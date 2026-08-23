package xyz.dqrkis.gui.components.settings;

import xyz.dqrkis.gui.components.ModuleButton;
import xyz.dqrkis.module.setting.ColorSetting;
import xyz.dqrkis.utils.RenderUtils;
import xyz.dqrkis.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public final class ColorPickerBox extends RenderableSetting {
    private final ColorSetting setting;
    private boolean pickingHue;

    public ColorPickerBox(ModuleButton parent, ColorSetting setting, int offset) {
        super(parent, setting, offset);
        this.setting = setting;
    }

    @Override public int parentHeight() { return 18; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        String label = setting.getName().toString();
        TextRenderer.drawString(label, context, parentX() + 4, parentY() + offset + 6, Color.WHITE.getRGB());

        int previewX = parentX() + parentWidth() - 42;
        int previewY = parentY() + offset + 3;
        int previewW = 34;
        int previewH = 12;
        // Preview swatch
        context.fill(previewX, previewY, previewX + previewW, previewY + previewH, setting.getValue().getRGB());
        context.fill(previewX, previewY, previewX + previewW, previewY + 1, 0xFF000000);
        context.fill(previewX, previewY + previewH - 1, previewX + previewW, previewY + previewH, 0xFF000000);
        context.fill(previewX, previewY, previewX + 1, previewY + previewH, 0xFF000000);
        context.fill(previewX + previewW - 1, previewY, previewX + previewW, previewY + previewH, 0xFF000000);
        // Hex text
        TextRenderer.drawString(setting.toHex(), context, previewX - 58, parentY() + offset + 6, new Color(180, 180, 180).getRGB());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) return;
        // Simplified: left click cycles hue, right click opens hex input (handled by future overlay)
        if (button == 0) {
            float[] hsb = Color.RGBtoHSB(setting.getValue().getRed(), setting.getValue().getGreen(), setting.getValue().getBlue(), null);
            hsb[0] = (hsb[0] + 0.08f) % 1.0f;
            setting.setValue(Color.getHSBColor(hsb[0], hsb[1], hsb[2]));
        } else if (button == 1) {
            // Cycle alpha for demo
            Color c = setting.getValue();
            int a = (c.getAlpha() + 64) % 256;
            setting.setValue(new Color(c.getRed(), c.getGreen(), c.getBlue(), a == 0 ? 255 : a));
        }
    }
}
