package xyz.dqrkis.mixin;

import xyz.dqrkis.module.modules.render.NameHider;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public class ChatHudMixin {

	@ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/message/MessageSignatureData;Lnet/minecraft/chat/MessageIndicator;)V",
			at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private Text dqrkis$modifyChatMessage(Text message) {
		NameHider nameHider = NameHider.get();
		if (nameHider == null || !nameHider.shouldHideInChat())
			return message;

		String filtered = nameHider.filter(message.getString());
		return filtered.equals(message.getString()) ? message : Text.literal(filtered);
	}
}
