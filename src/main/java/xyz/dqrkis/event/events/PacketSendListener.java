package xyz.dqrkis.event.events;

import xyz.dqrkis.event.CancellableEvent;
import xyz.dqrkis.event.Listener;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;


public interface PacketSendListener extends Listener {
	void onPacketSend(PacketSendEvent event);

	class PacketSendEvent extends CancellableEvent<PacketSendListener> {
		public Packet packet;

		public PacketSendEvent(Packet packet) {
			this.packet = packet;
		}

		@Override
		public void fire(ArrayList<PacketSendListener> listeners) {
			listeners.forEach(e -> e.onPacketSend(this));
		}

		@Override
		public Class<PacketSendListener> getListenerType() {
			return PacketSendListener.class;
		}
	}
}
