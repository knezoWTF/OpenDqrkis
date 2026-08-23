package xyz.dqrkis.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class ItemUtils {
	private ItemUtils() {}

	public static boolean hasEnchant(ItemStack stack, String idPart) {
		return stack.getEnchantments().getEnchantments().stream()
				.anyMatch(entry -> entry.getIdAsString().contains(idPart));
	}

	public static boolean isMace(ItemStack stack) {
		return stack.isOf(Items.MACE);
	}
}
