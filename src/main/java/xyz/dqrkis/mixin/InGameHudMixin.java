package xyz.dqrkis.mixin;

import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.event.EventManager;
import xyz.dqrkis.event.events.HudListener;
import xyz.dqrkis.module.modules.render.HideScoreboard;
import xyz.dqrkis.module.modules.misc.SilentHomeSetter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void onRenderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		HudListener.HudEvent event = new HudListener.HudEvent(context, tickCounter.getTickProgress(true));

		EventManager.fire(event);
	}

	@Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
	private void dqrkis$onSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
		SilentHomeSetter silentHomeSetter = Dqrkis.INSTANCE.getModuleManager().getModule(SilentHomeSetter.class);
		if (silentHomeSetter != null && silentHomeSetter.isEnabled() && silentHomeSetter.isHidingOverlayMessage()
				&& message.getString().toLowerCase().contains("home"))
			ci.cancel();
	}

	@Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
	private void dqrkis$onRenderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		HideScoreboard hideScoreboard = Dqrkis.INSTANCE.getModuleManager().getModule(HideScoreboard.class);
		if (hideScoreboard != null && hideScoreboard.isEnabled())
			ci.cancel();
	}

	@Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true)
	private void dqrkis$onRenderScoreboardSidebarObjective(DrawContext context, net.minecraft.scoreboard.ScoreboardObjective objective, CallbackInfo ci) {
		HideScoreboard hideScoreboard = Dqrkis.INSTANCE.getModuleManager().getModule(HideScoreboard.class);
		if (hideScoreboard != null && hideScoreboard.isEnabled())
			ci.cancel();
	}
}
