package xyz.dqrkis.mixin;

import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.module.modules.combat.HitboxExpand;
import xyz.dqrkis.module.modules.render.Glow;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

	@Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
	private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof LivingEntity) {
			Glow glow = Dqrkis.INSTANCE.getModuleManager().getModule(Glow.class);
			if (glow != null && glow.isEnabled())
				cir.setReturnValue(true);
		}
	}

	@Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
	private void onGetTargetingMargin(CallbackInfoReturnable<Float> cir) {
		HitboxExpand hitboxExpand = Dqrkis.INSTANCE.getModuleManager().getModule(HitboxExpand.class);
		if (hitboxExpand == null || !hitboxExpand.isEnabled())
			return;

		float expand = hitboxExpand.getExpand((Entity) (Object) this);
		if (expand > 0.0F)
			cir.setReturnValue(cir.getReturnValueF() + expand);
	}
}
