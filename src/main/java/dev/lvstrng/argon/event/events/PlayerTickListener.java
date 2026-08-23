package dev.lvstrng.argon.event.events;

import dev.lvstrng.argon.event.Event;
import dev.lvstrng.argon.event.Listener;

import java.util.ArrayList;

public interface PlayerTickListener extends Listener {
	void onPlayerTick();

	class PlayerTickEvent extends Event<PlayerTickListener> {
		@Override
		public void fire(ArrayList<PlayerTickListener> listeners) {
			listeners.forEach(PlayerTickListener::onPlayerTick);
		}

		@Override
		public Class<PlayerTickListener> getListenerType() {
			return PlayerTickListener.class;
		}
	}
}
