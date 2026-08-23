package xyz.dqrkis.module.setting;

import java.awt.Color;

public final class ColorSetting extends Setting<ColorSetting> {
    private Color value;
    private final Color defaultValue;

    public ColorSetting(CharSequence name, Color defaultValue) {
        super(name);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public Color getValue() { return value; }
    public void setValue(Color value) { this.value = value; }
    public Color getDefaultValue() { return defaultValue; }
    public String toHex() { return String.format("#%06X", value.getRGB() & 0xFFFFFF); }
}
