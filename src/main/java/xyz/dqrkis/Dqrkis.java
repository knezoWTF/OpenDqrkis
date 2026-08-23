package xyz.dqrkis;

import xyz.dqrkis.event.EventManager;
import xyz.dqrkis.gui.ClickGui;
import xyz.dqrkis.gui.DqrkisClickGui;
import xyz.dqrkis.gui.TabGui;
import xyz.dqrkis.managers.FriendManager;
import xyz.dqrkis.module.ModuleManager;
import xyz.dqrkis.managers.ProfileManager;
import xyz.dqrkis.utils.rotation.RotatorManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.io.File;
import java.io.IOException;
import java.net.*;

@SuppressWarnings("all")
public final class Dqrkis {
	public RotatorManager rotatorManager;
	public ProfileManager profileManager;
	public ModuleManager moduleManager;
	public EventManager eventManager;
	public FriendManager friendManager;
	public static MinecraftClient mc;
	public String version = " b1.3";
	public static boolean BETA; //this was for beta kids but ablue never made it a reality, and you basically paid extra 10 bucks for nothing while ablue spent it all on war thunder to buy pre-historic tanks and estrogen 🤡🤡🤡
	public static Dqrkis INSTANCE;
	public boolean guiInitialized;
	public ClickGui clickGui;
	public DqrkisClickGui dqrkisClickGui;
	public TabGui tabGui;
	public Screen previousScreen = null;
	public long lastModified;
	public File dqrkisJar;

	public Dqrkis() throws InterruptedException, IOException {
		INSTANCE = this;
		this.eventManager = new EventManager();
		this.moduleManager = new ModuleManager();
		this.clickGui = new ClickGui();
		this.dqrkisClickGui = new DqrkisClickGui();
		this.tabGui = new TabGui();
		this.rotatorManager = new RotatorManager();
		this.profileManager = new ProfileManager();
		this.friendManager = new FriendManager();

		this.getProfileManager().loadProfile();
		this.setLastModified();

		this.guiInitialized = false;
		mc = MinecraftClient.getInstance();
	}

	public ProfileManager getProfileManager() {
		return profileManager;
	}

	public ModuleManager getModuleManager() {
		return moduleManager;
	}

	public FriendManager getFriendManager() {
		return friendManager;
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public ClickGui getClickGui() {
		return clickGui;
	}

	public DqrkisClickGui getDqrkisClickGui() {
		return dqrkisClickGui;
	}

	public TabGui getTabGui() {
		return tabGui;
	}

	public void resetModifiedDate() {
		this.dqrkisJar.setLastModified(lastModified);
	}

	public String getVersion() {
		return version;
	}

	public void setLastModified() {
		try {
			this.dqrkisJar = new File(Dqrkis.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			// Comment out when debugging
			this.lastModified = dqrkisJar.lastModified();
		} catch (URISyntaxException ignored) {}
	}
}