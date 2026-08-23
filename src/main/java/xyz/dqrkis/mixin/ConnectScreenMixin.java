package xyz.dqrkis.mixin;

import xyz.dqrkis.module.modules.misc.AutoReconnect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

	@Inject(method = "connect", at = @At("HEAD"))
	private static void dqrkis$onConnect(Screen screen, MinecraftClient client, ServerAddress address, ServerInfo info,
	                                     boolean quickPlay, CookieStorage cookieStorage, CallbackInfo ci) {
		AutoReconnect autoReconnect = xyz.dqrkis.Dqrkis.INSTANCE.getModuleManager().getModule(AutoReconnect.class);
		if (autoReconnect != null)
			autoReconnect.recordConnection(address, info);
	}
}
