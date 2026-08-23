package xyz.dqrkis.mixin;

import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.event.EventManager;
import xyz.dqrkis.event.events.GameRenderListener;
import xyz.dqrkis.module.modules.misc.Freecam;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand"))
	private void onWorldRender(RenderTickCounter tickCounter, CallbackInfo ci) {
		MatrixStack matrixStack = new MatrixStack();
		EventManager.fire(new GameRenderListener.GameRenderEvent(matrixStack, tickCounter.getTickProgress(true)));
	}

	@Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
	private void onShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
		if (Dqrkis.INSTANCE.getModuleManager().getModule(Freecam.class).isEnabled())
			cir.setReturnValue(false);
	}
}
