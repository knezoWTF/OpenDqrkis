package xyz.dqrkis.gui.components.settings;

import xyz.dqrkis.gui.components.ModuleButton;
import xyz.dqrkis.module.setting.ItemSetting;
import xyz.dqrkis.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.awt.Color;

public final class ItemSettingBox extends RenderableSetting {
    private final ItemSetting setting;

    public ItemSettingBox(ModuleButton parent, ItemSetting setting, int offset) {
        super(parent, setting, offset);
        this.setting = setting;
    }

    @Override public int parentHeight() { return 18; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        String label = setting.getName() + ": " + setting.getItem().toString().replace("minecraft:", "");
        TextRenderer.drawString(label, context, parentX() + 4, parentY() + offset + 6, Color.WHITE.getRGB());
        // Render item icon
        ItemStack stack = new ItemStack(setting.getItem());
        context.drawItem(stack, parentX() + parentWidth() - 20, parentY() + offset + 1);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) return;
        // Cycle through common items on click (simplified selector)
        // In full port this would open EnchantSelectorPopup-style grid; stub cycles for now
        if (button == 0) {
            // No-op: could open a full ItemListWidget overlay; left as next iteration
        }
    }
}
