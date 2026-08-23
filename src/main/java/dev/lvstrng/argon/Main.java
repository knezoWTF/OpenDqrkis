package dev.lvstrng.argon;

import net.fabricmc.api.ModInitializer;

import java.io.IOException;
import java.net.URISyntaxException;

public final class Main implements ModInitializer {
	@Override
	public void onInitialize() {
		try {
			new Argon();
		} catch (InterruptedException | IOException ignored) {}
	}
}
