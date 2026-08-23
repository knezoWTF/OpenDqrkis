package xyz.dqrkis.utils;

import xyz.dqrkis.Dqrkis;
import net.minecraft.text.Text;

public final class ChatUtils {
	private ChatUtils() {}

	public static void info(String message) {
		send("§b[Dqrkis]§f " + message);
	}

	public static void error(String message) {
		send("§c[Dqrkis]§f " + message);
	}

	private static void send(String message) {
		if (Dqrkis.mc != null && Dqrkis.mc.player != null)
			Dqrkis.mc.player.sendMessage(Text.literal(message), false);
	}
}
