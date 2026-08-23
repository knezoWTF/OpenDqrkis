package xyz.dqrkis.module.setting;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

public final class ItemSetting extends Setting<ItemSetting> {
    private Item value;
    private final Item defaultValue;

    public ItemSetting(CharSequence name, Item defaultValue) {
        super(name);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public Item getItem() {
        return value;
    }

    public void setItem(Item value) {
        this.value = value;
    }

    public Item getDefaultValue() {
        return defaultValue;
    }
}