package xyz.dqrkis.event.events;

import xyz.dqrkis.event.CancellableEvent;
import xyz.dqrkis.event.Listener;

import java.util.ArrayList;

public interface BlockBreakingListener extends Listener {
	void onBlockBreaking(BlockBreakingEvent event);

	class BlockBreakingEvent extends CancellableEvent<BlockBreakingListener> {

		@Override
		public void fire(ArrayList<BlockBreakingListener> listeners) {
			listeners.forEach(e -> e.onBlockBreaking(this));
		}

		@Override
		public Class<BlockBreakingListener> getListenerType() {
			return BlockBreakingListener.class;
		}
	}
}
