package xyz.dqrkis.gui;

import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class TabGui extends Screen {
    private static final int TAB_WIDTH = 78;
    private static final int TAB_HEIGHT = 14;
    private static final int MODULE_WIDTH = 96;
    private static final int SETTING_WIDTH = 110;

    private static final Color BG = new Color(18, 18, 20, 240);
    private static final Color SELECTED = new Color(255, 68, 68, 255);
    private static final Color HOVER = new Color(255, 255, 255, 255);
    private static final Color IDLE = new Color(160, 160, 160, 255);
    private static final Color ENABLED = new Color(255, 68, 68, 255);

    private int selectedCategory = 0;
    private int selectedModule = 0;
    private int selectedSetting = -1; // -1 = module list, >=0 = setting list
    private final List<Category> categories = new ArrayList<>();

    public TabGui() {
        super(Text.literal("TabGui"));
        categories.addAll(List.of(Category.values()));
    }

    @Override
    protected void init() {
        selectedCategory = 0;
        selectedModule = 0;
        selectedSetting = -1;
    }

    private List<Module> getModules(Category category) {
        return Dqrkis.INSTANCE.getModuleManager().getModulesInCategory(category);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        int baseX = 6;
        int baseY = 18;

        // Categories column
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            boolean sel = i == selectedCategory && selectedSetting == -1 && getSelectedModuleIndex() == -1;
            // Actually: category selected when selectedSetting==-1 and we're in category level? Simplified: category is selected when selectedCategory==i
            boolean catSel = i == selectedCategory;
            int y = baseY + i * (TAB_HEIGHT + 1);
            context.fill(baseX, y, baseX + TAB_WIDTH, y + TAB_HEIGHT, catSel ? SELECTED.getRGB() : BG.getRGB());
            if (catSel) context.fill(baseX, y, baseX + 2, y + TAB_HEIGHT, SELECTED.brighter().getRGB());
            int color = catSel ? 0xFFFFFFFF : IDLE.getRGB();
            context.drawText(textRenderer, Text.literal(cat.name.toString()), baseX + 6, y + 4, color, false);
        }

        // Modules column (to the right of categories)
        Category cat = categories.get(selectedCategory);
        List<Module> modules = getModules(cat);
        int modX = baseX + TAB_WIDTH + 2;
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            boolean sel = i == selectedModule && selectedSetting == -1;
            int y = baseY + i * (TAB_HEIGHT + 1);
            Color bg = m.isEnabled() ? ENABLED : BG;
            if (sel) bg = SELECTED;
            context.fill(modX, y, modX + MODULE_WIDTH, y + TAB_HEIGHT, bg.getRGB());
            if (sel) context.fill(modX, y, modX + 2, y + TAB_HEIGHT, Color.WHITE.getRGB());
            int color = m.isEnabled() ? 0xFFFFFFFF : (sel ? 0xFFFFFFFF : IDLE.getRGB());
            String name = m.getName().toString();
            if (textRenderer.getWidth(name) > MODULE_WIDTH - 10) name = textRenderer.trimToWidth(name, MODULE_WIDTH - 12);
            context.drawText(textRenderer, Text.literal(name), modX + 4, y + 4, color, false);
            if (!m.getSettings().isEmpty()) {
                context.drawText(textRenderer, Text.literal(sel ? ">" : "…"), modX + MODULE_WIDTH - 10, y + 4, 0xFF888888, false);
            }
        }

        // Settings column (to the right of modules, if expanded)
        if (selectedSetting != -1 || (selectedModule >= 0 && selectedModule < modules.size() && !modules.get(selectedModule).getSettings().isEmpty())) {
            int activeModuleIdx = selectedModule >= 0 && selectedModule < modules.size() ? selectedModule : 0;
            if (activeModuleIdx < modules.size()) {
                Module active = modules.get(activeModuleIdx);
                int settingX = modX + MODULE_WIDTH + 2;
                List<?> settings = active.getSettings();
                for (int i = 0; i < settings.size(); i++) {
                    boolean sel = i == selectedSetting;
                    int y = baseY + i * (TAB_HEIGHT + 1);
                    context.fill(settingX, y, settingX + SETTING_WIDTH, y + TAB_HEIGHT, sel ? SELECTED.getRGB() : BG.getRGB());
                    if (sel) context.fill(settingX, y, settingX + 2, y + TAB_HEIGHT, Color.WHITE.getRGB());
                    String label = getSettingDisplay(active.getSettings().get(i));
                    if (textRenderer.getWidth(label) > SETTING_WIDTH - 8) label = textRenderer.trimToWidth(label, SETTING_WIDTH - 10);
                    context.drawText(textRenderer, Text.literal(label), settingX + 4, y + 4, sel ? 0xFFFFFFFF : IDLE.getRGB(), false);
                }
            }
        }

        // Hint
        context.drawText(textRenderer, Text.literal("TabGUI — Arrows:Navigate  Enter:Toggle/Edit  ESC:Close"), baseX, height - 12, 0xFF666666, false);
    }

    private String getSettingDisplay(Object setting) {
        if (setting instanceof BooleanSetting bs) return bs.getName() + ": " + (bs.getValue() ? "ON" : "OFF");
        if (setting instanceof NumberSetting ns) return ns.getName() + ": " + ns.getValue();
        if (setting instanceof ModeSetting<?> ms) return ms.getName() + ": " + ms.getMode();
        if (setting instanceof KeybindSetting ks) return ks.getName() + ": " + (ks.getKey() == -1 ? "NONE" : ks.getKey());
        if (setting instanceof StringSetting ss) {
            String v = ss.getValue();
            return ss.getName() + ": " + (v.length() > 8 ? v.substring(0, 8) + "…" : v);
        }
        return setting.toString();
    }

    private int getSelectedModuleIndex() { return selectedModule; }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        int baseX = 6;
        int baseY = 18;
        // Category click
        for (int i = 0; i < categories.size(); i++) {
            int y = baseY + i * (TAB_HEIGHT + 1);
            if (mouseX >= baseX && mouseX < baseX + TAB_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT) {
                selectedCategory = i;
                selectedModule = 0;
                selectedSetting = -1;
                return true;
            }
        }
        // Module click
        Category cat = categories.get(selectedCategory);
        List<Module> modules = getModules(cat);
        int modX = baseX + TAB_WIDTH + 2;
        for (int i = 0; i < modules.size(); i++) {
            int y = baseY + i * (TAB_HEIGHT + 1);
            if (mouseX >= modX && mouseX < modX + MODULE_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT) {
                selectedModule = i;
                selectedSetting = -1;
                if (button == 0) modules.get(i).toggle();
                else if (button == 1 && !modules.get(i).getSettings().isEmpty()) selectedSetting = 0;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        Category cat = categories.get(selectedCategory);
        List<Module> modules = getModules(cat);

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (selectedSetting != -1) { selectedSetting = -1; return true; }
            if (selectedModule != 0 || selectedCategory != 0) { close(); return true; }
            close(); return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            if (selectedSetting != -1) selectedSetting = Math.max(-1, selectedSetting - 1);
            else if (!modules.isEmpty() && selectedModule >= 0) selectedModule = Math.max(0, selectedModule - 1);
            else selectedCategory = Math.max(0, selectedCategory - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            if (selectedSetting != -1) {
                Module m = modules.get(selectedModule);
                selectedSetting = Math.min(m.getSettings().size() - 1, selectedSetting + 1);
            } else if (!modules.isEmpty() && selectedModule >= 0) selectedModule = Math.min(modules.size() - 1, selectedModule + 1);
            else selectedCategory = Math.min(categories.size() - 1, selectedCategory + 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT) {
            if (selectedSetting != -1) { selectedSetting = -1; return true; }
            // Collapse module list back to category
            selectedModule = -1; return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (selectedSetting == -1 && !modules.isEmpty() && selectedModule >= 0) {
                Module m = modules.get(selectedModule);
                if (!m.getSettings().isEmpty()) { selectedSetting = 0; return true; }
                m.toggle(); return true;
            }
            if (selectedSetting != -1) {
                Module m = modules.get(selectedModule);
                var s = m.getSettings().get(selectedSetting);
                if (s instanceof BooleanSetting bs) bs.setValue(!bs.getValue());
                else if (s instanceof NumberSetting ns) ns.setValue(ns.getValue() + ns.getIncrement());
                else if (s instanceof ModeSetting<?> ms) ms.cycle();
                else if (s instanceof KeybindSetting ks) ks.setListening(!ks.isListening());
                return true;
            }
            if (selectedModule == -1) { selectedModule = 0; return true; }
        }
        // Toggle module with Enter when in module list and no settings
        if (key == GLFW.GLFW_KEY_ENTER && selectedSetting == -1 && !modules.isEmpty()) {
            modules.get(selectedModule).toggle(); return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean shouldPause() { return false; }
}
