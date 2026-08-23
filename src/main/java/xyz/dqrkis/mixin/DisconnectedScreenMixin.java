package xyz.dqrkis.mixin;

import xyz.dqrkis.module.modules.misc.AutoReconnect;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
	@Shadow @Final private DirectionalLayoutWidget grid;

	@Unique
	private ButtonWidget dqrkis$reconnectButton;

	@Unique
	private double dqrkis$countdownTicks;

	protected DisconnectedScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;refreshPositions()V",
			shift = At.Shift.BEFORE))
	private void dqrkis$addReconnectButtons(CallbackInfo ci) {
		AutoReconnect autoReconnect = xyz.dqrkis.Dqrkis.INSTANCE.getModuleManager().getModule(AutoReconnect.class);
		if (autoReconnect == null || !autoReconnect.hasServer() || autoReconnect.shouldHideButtons())
			return;

		dqrkis$countdownTicks = autoReconnect.getDelaySeconds() * 20.0D;

		dqrkis$reconnectButton = ButtonWidget.builder(Text.literal(dqrkis$buttonText(autoReconnect)), button -> dqrkis$tryConnecting()).build();
		grid.add(dqrkis$reconnectButton);
		grid.add(ButtonWidget.builder(
						Text.literal(dqrkis$toggleText(autoReconnect)),
						button -> {
							autoReconnect.toggle();
							button.setMessage(Text.literal(dqrkis$toggleText(autoReconnect)));
							if (dqrkis$reconnectButton != null)
								dqrkis$reconnectButton.setMessage(Text.literal(dqrkis$buttonText(autoReconnect)));
							dqrkis$countdownTicks = autoReconnect.getDelaySeconds() * 20.0D;
						})
				.build());
	}

	@Override
	public void tick() {
		AutoReconnect autoReconnect = xyz.dqrkis.Dqrkis.INSTANCE.getModuleManager().getModule(AutoReconnect.class);
		if (autoReconnect == null || !autoReconnect.isEnabled() || !autoReconnect.hasServer())
			return;

		if (dqrkis$countdownTicks <= 0.0D) {
			dqrkis$tryConnecting();
		} else {
			dqrkis$countdownTicks--;
			if (dqrkis$reconnectButton != null)
				dqrkis$reconnectButton.setMessage(Text.literal(dqrkis$buttonText(autoReconnect)));
		}
	}

	@Unique
	private String dqrkis$buttonText(AutoReconnect autoReconnect) {
		String text = "Reconnect";
		if (autoReconnect.isEnabled())
			text += String.format(" (%.1f)", dqrkis$countdownTicks / 20.0D);
		return text;
	}

	@Unique
	private String dqrkis$toggleText(AutoReconnect autoReconnect) {
		return "Auto Reconnect: " + (autoReconnect.isEnabled() ? "§aON" : "§cOFF");
	}

	@Unique
	private void dqrkis$tryConnecting() {
		AutoReconnect autoReconnect = xyz.dqrkis.Dqrkis.INSTANCE.getModuleManager().getModule(AutoReconnect.class);
		if (autoReconnect != null && autoReconnect.hasServer())
			ConnectScreen.connect(new TitleScreen(), this.client, autoReconnect.serverAddress, autoReconnect.serverInfo, false, null);
	}
}
