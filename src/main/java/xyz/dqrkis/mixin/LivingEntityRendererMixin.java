package xyz.dqrkis.mixin;

import xyz.dqrkis.module.modules.render.NameHider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

	@Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
	private void dqrkis$onHasLabel(LivingEntity entity, double cameraDistance, CallbackInfoReturnable<Boolean> cir) {
		if (!(entity instanceof PlayerEntity))
			return;

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || entity != client.player)
			return;

		NameHider nameHider = NameHider.get();
		if (nameHider != null && nameHider.shouldHideNametag())
			cir.setReturnValue(false);
	}
}
