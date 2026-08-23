package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;

import java.util.HashSet;
import java.util.Set;

public final class AntiTrap extends Module implements TickListener {
	private final Set<Entity> hiddenEntities = new HashSet<>();

	public AntiTrap() {
		super(EncryptedString.of("Anti Trap"),
				EncryptedString.of("Helps you escape traps by removing certain entities."),
				-1,
				Category.MISC);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);

		for (Entity entity : new HashSet<>(hiddenEntities)) {
			if (entity != null && !entity.isRemoved() && mc.world != null)
				setHidden(entity, false);
		}
		hiddenEntities.clear();
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.world == null || mc.player == null)
			return;

		for (Entity entity : mc.world.getEntities()) {
			if (entity == null || entity.isRemoved())
				continue;

			if (isTrapEntity(entity)) {
				if (!hiddenEntities.contains(entity)) {
					setHidden(entity, true);
					hiddenEntities.add(entity);
				}
			} else if (hiddenEntities.contains(entity)) {
				setHidden(entity, false);
				hiddenEntities.remove(entity);
			}
		}

		hiddenEntities.removeIf(entity -> entity == null || entity.isRemoved());
	}

	private boolean isTrapEntity(Entity entity) {
		if (entity instanceof ArmorStandEntity || entity instanceof ItemFrameEntity)
			return true;

		EntityType<?> type = entity.getType();
		return type == EntityType.ARMOR_STAND || type == EntityType.ITEM_FRAME
				|| type == EntityType.GLOW_ITEM_FRAME || type == EntityType.CHEST_MINECART;
	}

	private void setHidden(Entity entity, boolean hidden) {
		if (entity.isRemoved())
			return;

		entity.setInvisible(hidden);
	}
}
