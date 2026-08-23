package xyz.dqrkis.event.events;

import xyz.dqrkis.event.Event;
import xyz.dqrkis.event.Listener;

import java.util.ArrayList;

public interface ShieldDisabledListener extends Listener {
	void onShieldDisabled();

	class ShieldDisabledEvent extends Event<ShieldDisabledListener> {

		@Override
		public void fire(ArrayList<ShieldDisabledListener> listeners) {
			listeners.forEach(ShieldDisabledListener::onShieldDisabled);
		}

		@Override
		public Class<ShieldDisabledListener> getListenerType() {
			return ShieldDisabledListener.class;
		}
	}
}
