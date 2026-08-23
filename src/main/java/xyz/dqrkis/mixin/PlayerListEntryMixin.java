package xyz.dqrkis.mixin;

import xyz.dqrkis.module.modules.render.NameHider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {

	@Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
	private void dqrkis$onGetDisplayName(CallbackInfoReturnable<Text> cir) {
		NameHider nameHider = NameHider.get();
		if (nameHider == null || !nameHider.shouldHideInTab())
			return;

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null)
			return;

		PlayerListEntry self = (PlayerListEntry) (Object) this;
		if (!self.getProfile().name().equals(client.player.getName().getString()))
			return;

		Text current = cir.getReturnValue();
		if (current == null) {
			cir.setReturnValue(Text.literal(nameHider.getFakeName()));
			return;
		}

		String filtered = nameHider.filter(current.getString());
		if (!filtered.equals(current.getString()))
			cir.setReturnValue(Text.literal(filtered));
	}
}
