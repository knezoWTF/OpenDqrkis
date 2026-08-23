package xyz.dqrkis.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = {PlayerInventory.class})
public interface PlayerInventoryAccessor {
	@Accessor("main")
	DefaultedList<ItemStack> getMain();
}
