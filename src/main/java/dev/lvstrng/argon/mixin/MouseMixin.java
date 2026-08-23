package dev.lvstrng.argon.mixin;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.event.EventManager;
import dev.lvstrng.argon.event.events.ButtonListener;
import dev.lvstrng.argon.event.events.MouseMoveListener;
import dev.lvstrng.argon.event.events.MouseUpdateListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
	@Shadow @Final private MinecraftClient client;
	@Shadow public abstract double getX();
	@Shadow public abstract double getY();

	@Unique private double argon$lastMouseX;
	@Unique private double argon$lastMouseY;
	@Unique private boolean argon$initialized;
	@Unique private final int[] argon$buttonStates = new int[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];

	@Inject(method = "tick", at = @At("TAIL"))
	private void onMouseUpdate(CallbackInfo ci) {
		EventManager.fire(new MouseUpdateListener.MouseUpdateEvent());
		long window = client.getWindow().getHandle();
		double x = getX();
		double y = getY();

		if (!argon$initialized) {
			argon$initialized = true;
			argon$lastMouseX = x;
			argon$lastMouseY = y;

			for (int button = 0; button < argon$buttonStates.length; button++) {
				argon$buttonStates[button] = GLFW.glfwGetMouseButton(window, button);
			}
			return;
		}

		if (x != argon$lastMouseX || y != argon$lastMouseY) {
			argon$lastMouseX = x;
			argon$lastMouseY = y;
			EventManager.fire(new MouseMoveListener.MouseMoveEvent(window, x, y));
		}

		for (int button = 0; button < argon$buttonStates.length; button++) {
			int state = GLFW.glfwGetMouseButton(window, button);
			if (state != argon$buttonStates[button]) {
				argon$buttonStates[button] = state;
				EventManager.fire(new ButtonListener.ButtonEvent(button, window, state));
			}
		}
	}
}
