package dev.lvstrng.argon.module.modules.misc;

import dev.lvstrng.argon.event.events.TickListener;
import dev.lvstrng.argon.mixin.MinecraftClientAccessor;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.ModeSetting;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.EncryptedString;
import dev.lvstrng.argon.utils.MathUtils;
import dev.lvstrng.argon.utils.MouseSimulation;
import dev.lvstrng.argon.utils.TimerUtils;
import dev.lvstrng.argon.utils.WorldUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;


public final class AutoClicker extends Module implements TickListener {
	private final BooleanSetting onlyWeapon = new BooleanSetting(EncryptedString.of("Only Weapon"), true)
			.setDescription(EncryptedString.of("Only left clicks with weapon in hand"));
	private final BooleanSetting onlyBlocks = new BooleanSetting(EncryptedString.of("Only Blocks"), true)
			.setDescription(EncryptedString.of("Only right clicks blocks"));
	private final BooleanSetting onClick = new BooleanSetting(EncryptedString.of("On Click"), true);

	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 1000, 0, 1);
	private final NumberSetting chance = new NumberSetting(EncryptedString.of("Chance"), 0, 100, 100, 1);
	private final ModeSetting<Mode> mode = new ModeSetting<>(EncryptedString.of("Actions"), Mode.All, Mode.class);
	private final TimerUtils timer = new TimerUtils();

	public enum Mode {
		All, Left, Right
	}

	public AutoClicker() {
		super(EncryptedString.of("Auto Clicker"),
				EncryptedString.of("Automatically clicks for you"),
				-1,
				Category.MISC);

		addSettings(onlyWeapon, onClick, delay, chance, mode);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		timer.reset();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}

	//using this cuz its faster/instant
	@Override
	public void onTick() {
		if (mc.player == null)
			return;

		if (mc.currentScreen != null)
			return;

		if (mc.crosshairTarget == null)
			return;

		if (timer.delay(delay.getValueFloat()) && chance.getValueInt() >= MathUtils.randomInt(1, 100)) {
			if (mode.isMode(Mode.Left)) {
				performLeftClick();
			}

			if (mode.isMode(Mode.Right)) {
				performRightClick();
			}

			if (mode.isMode(Mode.All)) {
				performLeftClick();
				performRightClick();
			}
		}
	}

	private void performRightClick() {
		Item mainhand = mc.player.getMainHandStack().getItem();
		Item offhand = mc.player.getOffHandStack().getItem();

		if (mainhand.getComponents().contains(DataComponentTypes.FOOD))
			return;

		if (offhand.getComponents().contains(DataComponentTypes.FOOD))
			return;

		if (mainhand instanceof RangedWeaponItem || offhand instanceof RangedWeaponItem)
			return;

		if (onClick.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS)
			return;

		MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

		((MinecraftClientAccessor) mc).invokeDoItemUse();
		timer.reset();
	}

	private void performLeftClick() {
		Item mainhand = mc.player.getMainHandStack().getItem();
		Item offhand = mc.player.getOffHandStack().getItem();

		if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK)
			return;

		if (mc.player.isUsingItem())
			return;

		if (onlyWeapon.getValue() && !WorldUtils.isWeapon(mc.player.getMainHandStack()))
			return;

		if (onClick.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
			return;

		MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

		((MinecraftClientAccessor) mc).invokeDoAttack();
		timer.reset();
	}
}
