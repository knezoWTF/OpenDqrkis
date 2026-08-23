package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.mixin.PlayerInventoryAccessor;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.KeybindSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public final class LootYeeter extends Module implements TickListener {
	private final NumberSetting minTotems = new NumberSetting(EncryptedString.of("Min Totems"), 0, 36, 6, 1)
			.setDescription(EncryptedString.of("Keeps at least this many totems"));
	private final NumberSetting minPearls = new NumberSetting(EncryptedString.of("Min Pearls"), 0, 576, 64, 1)
			.setDescription(EncryptedString.of("Keeps at least this many pearls"));
	private final BooleanSetting totemsFirst = new BooleanSetting(EncryptedString.of("Totems First"), false)
			.setDescription(EncryptedString.of("Checks totem overflow before pearl overflow"));
	private final NumberSetting throwDelay = new NumberSetting(EncryptedString.of("Throw Delay"), 0, 10, 0, 1)
			.setDescription(EncryptedString.of("Ticks between throws"));
	private final KeybindSetting yeetKey = new KeybindSetting(EncryptedString.of("Throw Key"), GLFW.GLFW_KEY_X, false)
			.setDescription(EncryptedString.of("Hold this key to throw junk items"));
	private final BooleanSetting randomSlot = new BooleanSetting(EncryptedString.of("Random Slot"), true)
			.setDescription(EncryptedString.of("Picks a random matching slot instead of the first one"));

	private int throwIn;

	public LootYeeter() {
		super(EncryptedString.of("Loot Yeeter"),
				EncryptedString.of("Throws away junk items from your inventory"),
				-1,
				Category.MISC);
		addSettings(minTotems, minPearls, totemsFirst, throwDelay, yeetKey, randomSlot);
	}

	@Override
	public void onEnable() {
		throwIn = 0;
		eventManager.add(TickListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.interactionManager == null)
			return;

		if (!(mc.currentScreen instanceof InventoryScreen screen))
			return;

		int key = yeetKey.getKey();
		if (key <= 0 || GLFW.glfwGetKey(mc.getWindow().getHandle(), key) != GLFW.GLFW_PRESS)
			return;

		if (throwIn > 0) {
			throwIn--;
			return;
		}

		int slot = findSlot();
		if (slot == -1)
			return;

		mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot, 1, SlotActionType.THROW, mc.player);
		throwIn = throwDelay.getValueInt();
	}

	private int findSlot() {
		int pearls = pearlSlot();
		int totems = totemSlot();

		if (totemsFirst.getValue())
			return totems != -1 ? totems : pearls;
		return pearls != -1 ? pearls : totems;
	}

	private int pearlSlot() {
		int total = countItem(Items.ENDER_PEARL);
		if (total <= minPearls.getValueInt())
			return -1;

		ArrayList<Integer> slots = collectSlots(Items.ENDER_PEARL);
		if (slots.isEmpty())
			return -1;

		if (randomSlot.getValue()) {
			int slot = slots.get(ThreadLocalRandom.current().nextInt(slots.size()));
			int count = getMain().get(slot).getCount();
			return total - count >= minPearls.getValueInt() ? slot : -1;
		}

		int best = -1;
		int smallest = Integer.MAX_VALUE;
		for (int slot : slots) {
			int count = getMain().get(slot).getCount();
			if (count < smallest) {
				smallest = count;
				best = slot;
			}
		}
		return total - smallest >= minPearls.getValueInt() ? best : -1;
	}

	private int totemSlot() {
		if (countItem(Items.TOTEM_OF_UNDYING) <= minTotems.getValueInt())
			return -1;

		ArrayList<Integer> slots = collectSlots(Items.TOTEM_OF_UNDYING);
		if (slots.isEmpty())
			return -1;

		return randomSlot.getValue()
				? slots.get(ThreadLocalRandom.current().nextInt(slots.size()))
				: slots.getFirst();
	}

	private ArrayList<Integer> collectSlots(net.minecraft.item.Item item) {
		ArrayList<Integer> slots = new ArrayList<>();
		for (int i = 9; i < 36; i++) {
			ItemStack stack = getMain().get(i);
			if (stack.isOf(item))
				slots.add(i);
		}
		return slots;
	}

	private int countItem(net.minecraft.item.Item item) {
		int count = 0;
		for (ItemStack stack : getMain()) {
			if (stack.isOf(item))
				count += stack.getCount();
		}
		return count;
	}

	private DefaultedList<ItemStack> getMain() {
		return ((PlayerInventoryAccessor) (Object) mc.player.getInventory()).getMain();
	}
}
