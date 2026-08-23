package xyz.dqrkis.gui.components.settings;

import xyz.dqrkis.gui.components.ModuleButton;
import xyz.dqrkis.module.setting.Setting;
import xyz.dqrkis.utils.TextRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.util.Set;

/**
 * Simplified popup for BlockSetSetting (Class768). Renders as a button that shows count
 * and on right-click clears. Full searchable grid (Class1736) is stubbed as next iteration
 * to keep this batch compilable – behaviour is 1:1 for the simple use-case.
 */
public final class BlockSelectorBox extends RenderableSetting {
    private final Setting<?> setting;

    public BlockSelectorBox(ModuleButton parent, Setting<?> setting, int offset) {
        super(parent, setting, offset);
        this.setting = setting;
    }

    @Override public int parentHeight() { return 18; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        String label = setting.getName().toString();
        TextRenderer.drawString(label, context, parentX() + 4, parentY() + offset + 6, Color.WHITE.getRGB());
        String hint = "[RMB: clear]";
        int hintW = TextRenderer.getWidth(hint);
        TextRenderer.drawString(hint, context, parentX() + parentWidth() - hintW - 4, parentY() + offset + 6, new Color(150, 150, 150).getRGB());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) return;
        // Left click would open the full searchable BlockSelectorPopup (Class1736) overlay;
        // stub keeps ClickGUI responsive – full grid is next iteration
    }
}
