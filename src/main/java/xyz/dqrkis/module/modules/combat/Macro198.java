package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.mixin.MinecraftClientAccessor;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.KeybindSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.BlockUtils;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.InventoryUtils;
import xyz.dqrkis.utils.KeyUtils;
import xyz.dqrkis.utils.MouseSimulation;
import xyz.dqrkis.utils.WorldUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public final class Macro198 extends Module implements TickListener {
	private enum State { IDLE, PLACE_OBI, WAIT_OBI, PLACE_CRYSTAL, BREAK_CRYSTAL }

	private final KeybindSetting macroKey = new KeybindSetting(EncryptedString.of("Macro Key"), GLFW.GLFW_MOUSE_BUTTON_MIDDLE, false)
			.setDescription(EncryptedString.of("Hold this button to run the macro"));
	private final BooleanSetting autoBreak = new BooleanSetting(EncryptedString.of("Auto Break"), true)
			.setDescription(EncryptedString.of("Breaks placed crystals automatically"));
	private final NumberSetting placeDelay = new NumberSetting(EncryptedString.of("Place Delay"), 0, 20, 0, 1);
	private final NumberSetting breakDelay = new NumberSetting(EncryptedString.of("Break Delay"), 0, 20, 0, 1);
	private final NumberSetting placeChance = new NumberSetting(EncryptedString.of("Place Chance %"), 0, 100, 100, 1);
	private final NumberSetting attackChance = new NumberSetting(EncryptedString.of("Attack Chance %"), 0, 100, 100, 1);
	private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), false);
	private final BooleanSetting airSwing = new BooleanSetting(EncryptedString.of("Air Swing"), false)
			.setDescription(EncryptedString.of("Swings at air when no crystal is in range"));
	private final NumberSetting airSwingChance = new NumberSetting(EncryptedString.of("Air Swing Chance %"), 0, 100, 20, 1);

	private static final int MAX_PLACES_PER_POS = 2;

	private int placeIn;
	private int breakIn;
	private State state = State.IDLE;
	private BlockPos targetPos;

	public Macro198() {
		super(EncryptedString.of("Macro 198"),
				EncryptedString.of("Combat macro for 1.9.8 style gameplay"),
				-1,
				Category.COMBAT);
		addSettings(macroKey, autoBreak, placeDelay, breakDelay, placeChance, attackChance, clickSimulation, airSwing, airSwingChance);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		resetState();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.interactionManager == null)
			return;

		if (!KeyUtils.isKeyPressed(macroKey.getKey())) {
			resetState();
			return;
		}

		if (placeIn > 0)
			placeIn--;

		if (breakIn > 0)
			breakIn--;

		attackNearbyCrystals();

		State previous = state;
		for (int i = 0; i < 4 && state == previous; i++) {
			switch (state) {
				case IDLE -> scanForPlacement();
				case PLACE_OBI -> placeObsidian();
				case WAIT_OBI -> waitForObsidian();
				case PLACE_CRYSTAL -> placeCrystal();
				case BREAK_CRYSTAL -> finishCycle();
			}
			previous = state;
		}
	}

	private void resetState() {
		placeIn = 0;
		breakIn = 0;
		state = State.IDLE;
		targetPos = null;
	}

	private void scanForPlacement() {
		HitResult hit = mc.player.raycast(4.5D, 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK)
			return;

		BlockPos pos = blockHit.getBlockPos();
		if (mc.world.getBlockState(pos).isAir())
			return;

		if (mc.player.getEyePos().distanceTo(blockHit.getPos()) > 4.5D)
			return;

		if (blockHit.getSide() != Direction.UP)
			return;

		if (isSupport(mc.world.getBlockState(pos).getBlock())) {
			targetPos = pos;
			state = State.PLACE_CRYSTAL;
		} else {
			targetPos = pos.up();
			state = State.PLACE_OBI;
		}
	}

	private void placeObsidian() {
		if (placeIn > 0)
			return;

		ThreadLocalRandom random = ThreadLocalRandom.current();
		if (random.nextInt(1, 101) > placeChance.getValueInt())
			return;

		HitResult hit = mc.player.raycast(4.5D, 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			state = State.IDLE;
			return;
		}

		if (mc.player.getEyePos().distanceTo(blockHit.getPos()) > 4.5D) {
			state = State.IDLE;
			return;
		}

		InventoryUtils.selectItemFromHotbar(Items.OBSIDIAN);
		if (clickSimulation.getValue())
			MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

		WorldUtils.placeBlock(blockHit, true);
		placeIn = placeDelay.getValueInt();
		state = State.WAIT_OBI;
	}

	private void waitForObsidian() {
		if (isSupport(mc.world.getBlockState(targetPos).getBlock())) {
			state = State.PLACE_CRYSTAL;
		} else if (isSupport(mc.world.getBlockState(targetPos.down()).getBlock())) {
			targetPos = targetPos.down();
			state = State.PLACE_CRYSTAL;
		} else {
			state = State.IDLE;
		}
	}

	private void placeCrystal() {
		if (placeIn > 0)
			return;

		if (!isSupport(mc.world.getBlockState(targetPos).getBlock())) {
			state = State.IDLE;
			return;
		}

		if (!BlockUtils.canPlaceBlockClient(targetPos)) {
			state = State.BREAK_CRYSTAL;
			return;
		}

		ThreadLocalRandom random = ThreadLocalRandom.current();
		if (random.nextInt(1, 101) > placeChance.getValueInt())
			return;

		InventoryUtils.selectItemFromHotbar(Items.END_CRYSTAL);
		BlockHitResult hit = new BlockHitResult(
				new Vec3d(targetPos.getX() + 0.5D, targetPos.getY() + 1.0D, targetPos.getZ() + 0.5D),
				Direction.UP, targetPos, false);

		if (clickSimulation.getValue())
			MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

		WorldUtils.placeBlock(hit, true);
		placeIn = placeDelay.getValueInt();
		state = State.BREAK_CRYSTAL;
	}

	private void finishCycle() {
		if (autoBreak.getValue())
			state = State.IDLE;
		else
			state = isSupport(mc.world.getBlockState(targetPos).getBlock()) ? State.PLACE_CRYSTAL : State.IDLE;

		if (airSwing.getValue() && breakIn == 0
				&& mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.MISS
				&& ThreadLocalRandom.current().nextInt(1, 101) <= attackChance.getValueInt()) {
			((MinecraftClientAccessor) mc).setAttackCooldown(10);

			if (clickSimulation.getValue())
				MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

			mc.player.swingHand(Hand.MAIN_HAND);
			breakIn = breakDelay.getValueInt();
		}
	}

	private void attackNearbyCrystals() {
		if (breakIn > 0)
			return;

		ThreadLocalRandom random = ThreadLocalRandom.current();
		if (random.nextInt(1, 101) > attackChance.getValueInt())
			return;

		if (mc.crosshairTarget instanceof EntityHitResult entityHit
				&& entityHit.getEntity() instanceof EndCrystalEntity crystal) {
			if (clickSimulation.getValue())
				MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

			WorldUtils.hitEntity(crystal, true);
			breakIn = breakDelay.getValueInt();
			return;
		}

		for (Entity entity : mc.world.getEntities()) {
			if (entity instanceof EndCrystalEntity && entity.distanceTo(mc.player) < 9.0D) {
				if (clickSimulation.getValue())
					MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

				mc.interactionManager.attackEntity(mc.player, entity);
				mc.player.swingHand(Hand.MAIN_HAND);
				breakIn = breakDelay.getValueInt();
				return;
			}
		}
	}

	private static boolean isSupport(net.minecraft.block.Block block) {
		return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
	}
}
