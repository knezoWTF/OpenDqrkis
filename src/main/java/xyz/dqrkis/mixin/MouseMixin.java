package xyz.dqrkis.mixin;

import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.event.EventManager;
import xyz.dqrkis.event.events.ButtonListener;
import xyz.dqrkis.event.events.MouseMoveListener;
import xyz.dqrkis.event.events.MouseUpdateListener;
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

	@Unique private double dqrkis$lastMouseX;
	@Unique private double dqrkis$lastMouseY;
	@Unique private boolean dqrkis$initialized;
	@Unique private final int[] dqrkis$buttonStates = new int[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];

	@Inject(method = "tick", at = @At("TAIL"))
	private void onMouseUpdate(CallbackInfo ci) {
		EventManager.fire(new MouseUpdateListener.MouseUpdateEvent());
		long window = client.getWindow().getHandle();
		double x = getX();
		double y = getY();

		if (!dqrkis$initialized) {
			dqrkis$initialized = true;
			dqrkis$lastMouseX = x;
			dqrkis$lastMouseY = y;

			for (int button = 0; button < dqrkis$buttonStates.length; button++) {
				dqrkis$buttonStates[button] = GLFW.glfwGetMouseButton(window, button);
			}
			return;
		}

		if (x != dqrkis$lastMouseX || y != dqrkis$lastMouseY) {
			dqrkis$lastMouseX = x;
			dqrkis$lastMouseY = y;
			EventManager.fire(new MouseMoveListener.MouseMoveEvent(window, x, y));
		}

		for (int button = 0; button < dqrkis$buttonStates.length; button++) {
			int state = GLFW.glfwGetMouseButton(window, button);
			if (state != dqrkis$buttonStates[button]) {
				dqrkis$buttonStates[button] = state;
				EventManager.fire(new ButtonListener.ButtonEvent(button, window, state));
			}
		}
	}
}
