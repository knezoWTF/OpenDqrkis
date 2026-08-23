package xyz.dqrkis.gui.components.settings;

import xyz.dqrkis.gui.components.ModuleButton;
import xyz.dqrkis.module.setting.Setting;
import xyz.dqrkis.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;

import java.awt.Color;
import java.util.Set;

/**
 * Simplified popup for EnchantListSetting (Class1916). Shows count and clears on RMB.
 * Full searchable registry grid (Class1224) is next iteration.
 */
public final class EnchantSelectorBox extends RenderableSetting {
    private final Setting<?> setting;

    public EnchantSelectorBox(ModuleButton parent, Setting<?> setting, int offset) {
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
        TextRenderer.drawString(hint, context, parentX() + parentWidth() - hintW - 4, parentY() + offset + 6, new Color(150,150,150).getRGB());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) return;
    }
}
