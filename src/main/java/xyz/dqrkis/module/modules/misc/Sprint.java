package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.utils.EncryptedString;

public final class Sprint extends Module implements TickListener {
    public Sprint() {
        super(EncryptedString.of("Sprint"), EncryptedString.of("Keeps you sprinting at all times"), -1, Category.MISC);
    }

    @Override
    public void onEnable() {
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
        mc.player.setSprinting(mc.player.input.hasForwardMovement());
    }
}
