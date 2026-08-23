package xyz.dqrkis.gui;

import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.*;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class DqrkisClickGui extends Screen {
    private static final int SIDEBAR_WIDTH = 78;
    private static final int HEADER_HEIGHT = 18;
    private static final int MODULE_HEIGHT = 16;
    private static final int SETTING_HEIGHT = 14;

    private static final Color SIDEBAR_BG = new Color(18, 18, 20, 255);
    private static final Color CONTENT_BG = new Color(24, 24, 28, 255);
    private static final Color MODULE_BG = new Color(32, 32, 36, 255);
    private static final Color MODULE_ENABLED = new Color(255, 68, 68, 255);
    private static final Color MODULE_HOVER = new Color(45, 45, 50, 255);
    private static final Color CATEGORY_SELECTED = new Color(255, 68, 68, 255);
    private static final Color CATEGORY_IDLE = new Color(160, 160, 160, 255);
    private static final Color SEARCH_BG = new Color(38, 38, 42, 255);

    private Category selectedCategory;
    private final List<Category> categories = new ArrayList<>();
    private String searchQuery = "";
    private boolean searchFocused;

    private int guiLeft, guiTop, guiWidth, guiHeight;
    private int contentLeft;
    private float scroll;

    public DqrkisClickGui() {
        super(Text.literal("Dqrkis"));
        for (Category category : Category.values())
            categories.add(category);
        selectedCategory = categories.getFirst();
    }

    @Override
    protected void init() {
        guiWidth = Math.min(520, width - 40);
        guiHeight = Math.min(340, height - 40);
        guiLeft = (width - guiWidth) / 2;
        guiTop = (height - guiHeight) / 2;
        contentLeft = guiLeft + SIDEBAR_WIDTH;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        if (Dqrkis.INSTANCE.previousScreen != null)
            Dqrkis.INSTANCE.previousScreen.render(context, 0, 0, delta);
        // Single darkening pass - do not call renderBackground (would blur twice)
        context.fill(0, 0, width, height, 0x80000000);

        // Outer frame
        context.fill(guiLeft - 1, guiTop - 1, guiLeft + guiWidth + 1, guiTop + guiHeight + 1, new Color(50, 50, 55).getRGB());
        context.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, CONTENT_BG.getRGB());

        // Sidebar
        context.fill(guiLeft, guiTop, contentLeft, guiTop + guiHeight, SIDEBAR_BG.getRGB());
        // Header: "Dqrkis |" watermark like HUD
        context.drawText(textRenderer, Text.literal("Dqrkis |"), guiLeft + 8, guiTop + 6, 0xFFFFFFFF, false);
        int headerBottom = guiTop + HEADER_HEIGHT;

        // Search box in sidebar
        int searchY = headerBottom + 4;
        int searchH = 14;
        context.fill(guiLeft + 6, searchY, contentLeft - 6, searchY + searchH, SEARCH_BG.getRGB());
        int borderColor = searchFocused ? MODULE_ENABLED.getRGB() : 0xFF3A3A40;
        context.fill(guiLeft + 6, searchY, contentLeft - 6, searchY + 1, borderColor);
        context.fill(guiLeft + 6, searchY + searchH - 1, contentLeft - 6, searchY + searchH, borderColor);
        context.fill(guiLeft + 6, searchY, guiLeft + 7, searchY + searchH, borderColor);
        context.fill(contentLeft - 7, searchY, contentLeft - 6, searchY + searchH, borderColor);
        String searchDisplay = searchQuery.isEmpty() && !searchFocused ? "Search..." : searchQuery + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        context.drawText(textRenderer, Text.literal(searchDisplay), guiLeft + 10, searchY + 4, searchQuery.isEmpty() && !searchFocused ? 0xFF888888 : 0xFFFFFFFF, false);

        // Categories
        int categoryY = searchY + searchH + 8;
        for (Category category : categories) {
            boolean selected = category == selectedCategory;
            int color = selected ? CATEGORY_SELECTED.getRGB() : CATEGORY_IDLE.getRGB();
            boolean hovered = mouseX >= guiLeft && mouseX < contentLeft && mouseY >= categoryY && mouseY < categoryY + 16;
            if (hovered && !selected) color = 0xFFFFFFFF;
            context.drawText(textRenderer, Text.literal(category.name.toString()), guiLeft + 10, categoryY + 4, color, false);
            if (selected) context.fill(guiLeft, categoryY, guiLeft + 2, categoryY + 16, CATEGORY_SELECTED.getRGB());
            categoryY += 18;
        }

        // Content: modules for selected category (filtered by search)
        List<Module> modules = getVisibleModules();
        int contentWidth = guiWidth - SIDEBAR_WIDTH;
        int startY = guiTop + 6 - (int) scroll;
        int moduleY = startY;

        // Scissor content area
        context.enableScissor(contentLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight);

        for (Module module : modules) {
            if (moduleY + MODULE_HEIGHT < guiTop || moduleY > guiTop + guiHeight) { moduleY += MODULE_HEIGHT + 1; if (moduleY + getSettingsHeight(module) > guiTop) moduleY += getSettingsHeight(module); continue; }

            boolean hovered = mouseX >= contentLeft + 6 && mouseX < guiLeft + guiWidth - 6 && mouseY >= moduleY && mouseY < moduleY + MODULE_HEIGHT;
            Color bg = module.isEnabled() ? MODULE_ENABLED : (hovered ? MODULE_HOVER : MODULE_BG);
            context.fill(contentLeft + 6, moduleY, guiLeft + guiWidth - 6, moduleY + MODULE_HEIGHT, bg.getRGB());
            // Left accent
            context.fill(contentLeft + 6, moduleY, contentLeft + 8, moduleY + MODULE_HEIGHT, module.isEnabled() ? MODULE_ENABLED.brighter().getRGB() : 0xFF3A3A40);

            int textColor = module.isEnabled() ? 0xFFFFFFFF : 0xFFCCCCCC;
            context.drawText(textRenderer, Text.literal(module.getName().toString()), contentLeft + 14, moduleY + 5, textColor, false);

            // Keybind hint
            if (module.getKey() != -1) {
                String keyName = getKeyName(module.getKey());
                int keyW = textRenderer.getWidth(keyName);
                // Small pill
                int pillX = guiLeft + guiWidth - 10 - keyW - 6;
                context.fill(pillX, moduleY + 3, pillX + keyW + 6, moduleY + 13, new Color(0, 0, 0, 100).getRGB());
                context.drawText(textRenderer, Text.literal(keyName), pillX + 3, moduleY + 5, 0xFFAAAAAA, false);
            }

            // Expand indicator if has settings
            if (!module.getSettings().isEmpty()) {
                String indicator = isExpanded(module) ? "−" : "+";
                context.drawText(textRenderer, Text.literal(indicator), guiLeft + guiWidth - 18, moduleY + 5, 0xFF888888, false);
            }

            moduleY += MODULE_HEIGHT + 1;

            // Settings
            if (isExpanded(module)) {
                for (var setting : module.getSettings()) {
                    if (moduleY + SETTING_HEIGHT < guiTop || moduleY > guiTop + guiHeight) { moduleY += SETTING_HEIGHT + 1; continue; }
                    context.fill(contentLeft + 10, moduleY, guiLeft + guiWidth - 10, moduleY + SETTING_HEIGHT, new Color(28, 28, 32).getRGB());
                    renderSetting(context, setting, contentLeft + 14, moduleY + 4, mouseX, mouseY);
                    moduleY += SETTING_HEIGHT + 1;
                }
                moduleY += 2;
            }
        }

        context.disableScissor();

        // Scrollbar
        int totalContentHeight = modules.size() * (MODULE_HEIGHT + 1) + getTotalSettingsHeight(modules) + 12;
        if (totalContentHeight > guiHeight) {
            float visibleRatio = (float) guiHeight / totalContentHeight;
            int barH = Math.max(20, (int) (guiHeight * visibleRatio));
            float maxScroll = totalContentHeight - guiHeight;
            float barY = guiTop + (scroll / maxScroll) * (guiHeight - barH);
            context.fill(guiLeft + guiWidth - 3, (int) barY, guiLeft + guiWidth - 1, (int) barY + barH, 0xFF555555);
        }
    }

    private void renderSetting(DrawContext context, xyz.dqrkis.module.setting.Setting<?> setting, int x, int y, int mouseX, int mouseY) {
        var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        String name = setting.getName().toString();
        String valueStr = getSettingValueString(setting);
        int valueColor = 0xFFAAAAAA;

        if (setting instanceof BooleanSetting bs) valueColor = bs.getValue() ? MODULE_ENABLED.getRGB() : 0xFF888888;
        else if (setting instanceof NumberSetting) valueColor = 0xFFFFCC66;
        else if (setting instanceof ModeSetting<?>) valueColor = 0xFF88CCFF;

        context.drawText(textRenderer, Text.literal(name), x, y, 0xFFBBBBBB, false);
        int valueW = textRenderer.getWidth(valueStr);
        context.drawText(textRenderer, Text.literal(valueStr), contentLeft + (guiWidth - SIDEBAR_WIDTH) - valueW - 14, y, valueColor, false);
    }

    private String getSettingValueString(xyz.dqrkis.module.setting.Setting<?> setting) {
        if (setting instanceof BooleanSetting bs) return bs.getValue() ? "ON" : "OFF";
        if (setting instanceof NumberSetting ns) return String.valueOf(ns.getValue());
        if (setting instanceof ModeSetting<?> ms) return ms.getMode().toString();
        if (setting instanceof StringSetting ss) {
            String v = ss.getValue();
            return v.length() > 16 ? v.substring(0, 16) + "…" : v;
        }
        if (setting instanceof KeybindSetting ks) {
            int k = ks.getKey();
            return k == -1 ? "NONE" : getKeyName(k);
        }
        Object v = null;
        try { v = setting.getClass().getMethod("getValue").invoke(setting); } catch (Exception ignored) {}
        if (v == null) try { v = setting.getClass().getMethod("getItem").invoke(setting); } catch (Exception ignored) {}
        return v == null ? "" : v.toString().replace("minecraft:", "");
    }

    private String getKeyName(int key) {
        if (key <= 8) return switch (key) { case 0 -> "LMB"; case 1 -> "RMB"; case 2 -> "MMB"; default -> "M" + key; };
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        return name == null ? "KEY" + key : name.toUpperCase();
    }

    private List<Module> getVisibleModules() {
        List<Module> all = Dqrkis.INSTANCE.getModuleManager().getModulesInCategory(selectedCategory);
        if (searchQuery.isEmpty()) return all;
        String q = searchQuery.toLowerCase();
        List<Module> filtered = new ArrayList<>();
        for (Module m : all) if (m.getName().toString().toLowerCase().contains(q)) filtered.add(m);
        return filtered;
    }

    private boolean isExpanded(Module module) {
        // Simple: expand if module has settings and is hovered/selected via right-click toggle stored in a set
        return expandedModules.contains(module);
    }

    private final java.util.Set<Module> expandedModules = new java.util.HashSet<>();

    private int getSettingsHeight(Module module) {
        return isExpanded(module) ? module.getSettings().size() * (SETTING_HEIGHT + 1) + 2 : 0;
    }

    private int getTotalSettingsHeight(List<Module> modules) {
        int h = 0;
        for (Module m : modules) h += getSettingsHeight(m);
        return h;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        // Search focus
        int searchY = guiTop + HEADER_HEIGHT + 4;
        boolean inSearch = mouseX >= guiLeft + 6 && mouseX < contentLeft - 6 && mouseY >= searchY && mouseY < searchY + 14;
        searchFocused = inSearch;
        if (inSearch) return true;

        // Category click
        int categoryY = searchY + 14 + 8;
        for (Category category : categories) {
            if (mouseX >= guiLeft && mouseX < contentLeft && mouseY >= categoryY && mouseY < categoryY + 16) {
                selectedCategory = category;
                scroll = 0;
                return true;
            }
            categoryY += 18;
        }

        // Module click
        List<Module> modules = getVisibleModules();
        int moduleY = guiTop + 6 - (int) scroll;
        for (Module module : modules) {
            if (mouseX >= contentLeft + 6 && mouseX < guiLeft + guiWidth - 6 && mouseY >= moduleY && mouseY < moduleY + MODULE_HEIGHT) {
                if (button == 0) module.toggle();
                else if (button == 1 && !module.getSettings().isEmpty()) {
                    if (expandedModules.contains(module)) expandedModules.remove(module);
                    else expandedModules.add(module);
                } else if (button == 2) {
                    // Middle click: bind flow would go here; stub
                }
                return true;
            }
            moduleY += MODULE_HEIGHT + 1;
            if (isExpanded(module)) {
                for (var setting : module.getSettings()) {
                    if (mouseX >= contentLeft + 10 && mouseX < guiLeft + guiWidth - 10 && mouseY >= moduleY && mouseY < moduleY + SETTING_HEIGHT) {
                        handleSettingClick(setting, button, mouseX);
                        return true;
                    }
                    moduleY += SETTING_HEIGHT + 1;
                }
                moduleY += 2;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void handleSettingClick(xyz.dqrkis.module.setting.Setting<?> setting, int button, double mouseX) {
        if (setting instanceof BooleanSetting bs) {
            if (button == 0) bs.setValue(!bs.getValue());
        } else if (setting instanceof NumberSetting ns) {
            double step = ns.getIncrement();
            double delta = button == 0 ? step : -step;
            ns.setValue(ns.getValue() + delta);
        } else if (setting instanceof ModeSetting<?> ms) {
            if (button == 0) ms.cycle();
        } else if (setting instanceof KeybindSetting ks) {
            ks.setListening(!ks.isListening());
        }
        // StringSetting editing would open a text field overlay – stub
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { searchFocused = false; return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) { searchFocused = false; return true; }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        // Forward to module keybind listening
        for (Module m : Dqrkis.INSTANCE.getModuleManager().getModules()) {
            for (var s : m.getSettings()) if (s instanceof KeybindSetting ks && ks.isListening()) { ks.setKey(keyCode); ks.setListening(false); return true; }
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (searchFocused) {
            String s = charInput.asString();
            if (!s.isEmpty()) {
                char chr = s.charAt(0);
                if (chr >= 32 && chr < 127) searchQuery += chr;
            }
            return true;
        }
        return super.charTyped(charInput);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<Module> modules = getVisibleModules();
        int totalH = modules.size() * (MODULE_HEIGHT + 1) + getTotalSettingsHeight(modules) + 12;
        int maxScroll = Math.max(0, totalH - guiHeight);
        scroll = (float) Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 16));
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}
