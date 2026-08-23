package dev.lvstrng.argon.utils;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class FakeInvScreen extends InventoryScreen {
	public FakeInvScreen(PlayerEntity player) {
		super(player);
	}

	@Override
	protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubleClick) {
		return false;
	}
}
