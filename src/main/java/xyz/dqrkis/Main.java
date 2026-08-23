package xyz.dqrkis;

import net.fabricmc.api.ModInitializer;

import java.io.IOException;
import java.net.URISyntaxException;

public final class Main implements ModInitializer {
	@Override
	public void onInitialize() {
		try {
			new Dqrkis();
		} catch (InterruptedException | IOException ignored) {}
	}
}
