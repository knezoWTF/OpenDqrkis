package xyz.dqrkis.module.modules.client;

import com.sun.jna.Memory;
import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.gui.ClickGui;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.*;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.Utils;

import java.io.File;

@SuppressWarnings("all")
public final class SelfDestruct extends Module {
	public static boolean destruct = false;

	private final BooleanSetting replaceMod = new BooleanSetting(EncryptedString.of("Replace Mod"), true)
			.setDescription(EncryptedString.of("Repalces the mod with the original JAR file of the ImmediatelyFast mod"));

	private final BooleanSetting saveLastModified = new BooleanSetting(EncryptedString.of("Save Last Modified"), true)
			.setDescription(EncryptedString.of("Saves the last modified date after self destruct"));

	private final StringSetting downloadURL = new StringSetting(EncryptedString.of("Replace URL"), "https://cdn.modrinth.com/data/5ZwdcRci/versions/FEOsWs1E/ImmediatelyFast-Fabric-1.2.11%2B1.20.4.jar");

	public SelfDestruct() {
		super(EncryptedString.of("Self Destruct"),
				EncryptedString.of("Removes the client from your game |Credits to lwes for deletion|"),
				-1,
				Category.CLIENT);
		addSettings(replaceMod, saveLastModified, downloadURL);
	}

	@Override
	public void onEnable() {
		destruct = true;

		Dqrkis.INSTANCE.getModuleManager().getModule(ClickGUI.class).setEnabled(false);
		setEnabled(false);

		Dqrkis.INSTANCE.getProfileManager().saveProfile();

		if (mc.currentScreen instanceof ClickGui) {
			Dqrkis.INSTANCE.guiInitialized = false;
			mc.currentScreen.close();
		}

		if (replaceMod.getValue()) {
			try {
				String modUrl = downloadURL.getValue();
				File currentJar = Utils.getCurrentJarPath();

				if (currentJar.exists())
                    Utils.replaceModFile(modUrl, Utils.getCurrentJarPath());
			} catch (Exception ignored) {}
		}

		for (Module module : Dqrkis.INSTANCE.getModuleManager().getModules()) {
			module.setEnabled(false);

			module.setName(null);
			module.setDescription(null);

			for (Setting<?> setting : module.getSettings()) {
				setting.setName(null);
				setting.setDescription(null);

				if(setting instanceof StringSetting set)
					set.setValue(null);
			}
			module.getSettings().clear();
		}

		Runtime runtime = Runtime.getRuntime();

		if (saveLastModified.getValue())
			Dqrkis.INSTANCE.resetModifiedDate();

		for (int i = 0; i <= 10; i++) {
			runtime.gc();
			runtime.runFinalization();

			try {
				Thread.sleep(100 * i);
				Memory.purge();
				Memory.disposeAll();
			} catch (InterruptedException ignored) {}
		}
	}
}