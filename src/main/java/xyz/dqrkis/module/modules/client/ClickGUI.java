package xyz.dqrkis.module.modules.client;

import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.event.events.PacketReceiveListener;
import xyz.dqrkis.gui.ClickGui;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.MinMaxSetting;
import xyz.dqrkis.module.setting.ModeSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import org.lwjgl.glfw.GLFW;

public final class ClickGUI extends Module implements PacketReceiveListener {
	public static final NumberSetting red = new NumberSetting(EncryptedString.of("Red"), 0, 255, 255, 1);
	public static final NumberSetting green = new NumberSetting(EncryptedString.of("Green"), 0, 255, 0, 1);
	public static final NumberSetting blue = new NumberSetting(EncryptedString.of("Blue"), 0, 255, 50, 1);

	public static final NumberSetting alphaWindow = new NumberSetting(EncryptedString.of("Window Alpha"), 0, 255, 170, 1);

	public static final BooleanSetting breathing = new BooleanSetting(EncryptedString.of("Breathing"), true)
			.setDescription(EncryptedString.of("Color breathing effect (only with rainbow off)"));
	public static final BooleanSetting rainbow = new BooleanSetting(EncryptedString.of("Rainbow"), true)
			.setDescription(EncryptedString.of("Enables LGBTQ mode"));

	public static final BooleanSetting background = new BooleanSetting(EncryptedString.of("Background"), false).setDescription(EncryptedString.of("Renders the background of the Click Gui"));
	public static final BooleanSetting customFont = new BooleanSetting(EncryptedString.of("Custom Font"), true);

	private final BooleanSetting preventClose = new BooleanSetting(EncryptedString.of("Prevent Close"), true)
			.setDescription(EncryptedString.of("For servers with freeze plugins that don't let you open the GUI"));

	public static final NumberSetting roundQuads = new NumberSetting(EncryptedString.of("Roundness"), 1, 10, 5, 1);
	public static final ModeSetting<AnimationMode> animationMode = new ModeSetting<>(EncryptedString.of("Animations"), AnimationMode.Normal, AnimationMode.class);
	public static final BooleanSetting antiAliasing = new BooleanSetting(EncryptedString.of("MSAA"), true)
			.setDescription(EncryptedString.of("Anti Aliasing | This can impact performance if you're using tracers but gives them a smoother look |"));

	public enum AnimationMode {
		Normal, Positive, Off;
	}

	public ClickGUI() {
		super(EncryptedString.of("Dqrkis"),
				EncryptedString.of("Settings for the client"),
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				Category.CLIENT);

		addSettings(red, green, blue, alphaWindow, breathing, rainbow, background, preventClose, roundQuads, animationMode, antiAliasing);
	}

	@Override
	public void onEnable() {
		eventManager.add(PacketReceiveListener.class, this);
		Dqrkis.INSTANCE.previousScreen = mc.currentScreen;

		if (Dqrkis.INSTANCE.clickGui != null) {
			mc.setScreenAndRender(Dqrkis.INSTANCE.clickGui);
		} else if (mc.currentScreen instanceof InventoryScreen) {
			Dqrkis.INSTANCE.guiInitialized = true;
		}

		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(PacketReceiveListener.class, this);

		if (mc.currentScreen instanceof ClickGui) {
			Dqrkis.INSTANCE.clickGui.close();
			mc.setScreenAndRender(Dqrkis.INSTANCE.previousScreen);
			Dqrkis.INSTANCE.clickGui.onGuiClose();
		} else if (mc.currentScreen instanceof InventoryScreen) {
			Dqrkis.INSTANCE.guiInitialized = false;
		}

		super.onDisable();
	}


	@Override
	public void onPacketReceive(PacketReceiveEvent event) {
		if (Dqrkis.INSTANCE.guiInitialized) {
			if (event.packet instanceof OpenScreenS2CPacket) {
				if (preventClose.getValue())
					event.cancel();
			}
		}
	}
}