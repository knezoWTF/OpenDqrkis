package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.ItemUseListener;
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
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class AutoDTap extends Module implements TickListener, ItemUseListener {
	private final KeybindSetting macroKey = new KeybindSetting(EncryptedString.of("Macro Key"), GLFW.GLFW_MOUSE_BUTTON_LEFT, false)
			.setDescription(EncryptedString.of("Hold this button to run the macro"));
	private final NumberSetting placeDelay = new NumberSetting(EncryptedString.of("Place Delay"), 0, 20, 3, 1);
	private final NumberSetting breakDelay = new NumberSetting(EncryptedString.of("Break Delay"), 0, 20, 3, 1);
	private final NumberSetting placeChance = new NumberSetting(EncryptedString.of("Place Chance %"), 0, 100, 100, 1);
	private final NumberSetting attackChance = new NumberSetting(EncryptedString.of("Attack Chance %"), 0, 100, 100, 1);
	private final BooleanSetting antiWeakness = new BooleanSetting(EncryptedString.of("Anti Weakness"), false)
			.setDescription(EncryptedString.of("Pauses while weak and an enemy recently died nearby"));
	private final BooleanSetting allEntities = new BooleanSetting(EncryptedString.of("All Entities"), false)
			.setDescription(EncryptedString.of("Attacks any entity in the crosshair, not just crystals and slimes"));
	private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), false);
	private final BooleanSetting breakBlocks = new BooleanSetting(EncryptedString.of("Break Blocks"), false)
			.setDescription(EncryptedString.of("Mines the block you are pointing at"));
	private final BooleanSetting swapForWeakness = new BooleanSetting(EncryptedString.of("Swap For Weakness"), false)
			.setDescription(EncryptedString.of("Swaps to a sword while attacking while weak"));
	private final NumberSetting attackWindow = new NumberSetting(EncryptedString.of("Attack Window"), 0, 100, 20, 1)
			.setDescription(EncryptedString.of("Ticks a position stays limited after two placements"));
	private final NumberSetting placeLimitDelay = new NumberSetting(EncryptedString.of("Place Limit Delay"), 0, 100, 10, 1)
			.setDescription(EncryptedString.of("Extra delay once a position reaches its place limit"));

	private static final int MAX_PLACES_PER_POS = 2;

	private int placeIn;
	private int breakIn;
	public boolean active;
	private final Map<BlockPos, Integer> placeCounts = new HashMap<>();
	private final Map<BlockPos, Integer> limitWindows = new HashMap<>();

	public AutoDTap() {
		super(EncryptedString.of("Auto DTap"),
				EncryptedString.of("Places and breaks crystals for you with limits"),
				-1,
				Category.COMBAT);
		addSettings(macroKey, placeDelay, breakDelay, placeChance, attackChance, antiWeakness,
				allEntities, clickSimulation, breakBlocks, swapForWeakness, attackWindow, placeLimitDelay);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(ItemUseListener.class, this);
		resetState();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(ItemUseListener.class, this);
		placeCounts.clear();
		limitWindows.clear();
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.interactionManager == null)
			return;

		boolean placeWasReady = placeIn == 0;
		boolean breakWasCooling = breakIn != 0;

		if (placeIn > 0)
			placeIn--;

		if (breakIn > 0)
			breakIn--;

		if (antiWeakness.getValue() && WorldUtils.isDeadBodyNearby())
			return;

		ThreadLocalRandom random = ThreadLocalRandom.current();
		int roll = random.nextInt(1, 101);

		limitWindows.entrySet().removeIf(entry -> {
			int ticks = entry.getValue() - 1;
			if (ticks <= 0) {
				placeCounts.remove(entry.getKey());
				return true;
			}
			entry.setValue(ticks);
			return false;
		});

		if (mc.player.isUsingItem())
			return;

		if (breakBlocks.getValue() && isComboingNearby())
			return;

		if (macroKey.getKey() != -1 && !KeyUtils.isKeyPressed(macroKey.getKey())) {
			resetState();
			return;
		}

		active = true;

		if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL)
			return;

		HitResult hitResult = mc.crosshairTarget;
		if (!(hitResult instanceof BlockHitResult blockHit) || hitResult.getType() != HitResult.Type.BLOCK)
			return;

		BlockPos pos = blockHit.getBlockPos();

		handleCrystalPlace(blockHit, pos, placeWasReady, roll, random);
		handleBlockBreak(blockHit, pos, !breakWasCooling, placeWasReady, breakWasCooling, roll, random);
		handleMissSwing(!breakWasCooling, roll, random);

		handleEntityAttack(!breakWasCooling, random);
	}

	private void handleCrystalPlace(BlockHitResult blockHit, BlockPos pos, boolean placeReady, int roll, ThreadLocalRandom random) {
		if (!placeReady || roll > placeChance.getValueInt())
			return;

		if (!isSupport(pos) || !BlockUtils.canPlaceBlockClient(pos) || limitWindows.containsKey(pos))
			return;

		int places = placeCounts.getOrDefault(pos, 0);
		if (places >= MAX_PLACES_PER_POS)
			return;

		if (clickSimulation.getValue())
			MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

		WorldUtils.placeBlock(blockHit, true);

		placeCounts.put(pos, places + 1);
		if (places + 1 >= MAX_PLACES_PER_POS)
			limitWindows.put(pos, placeLimitDelay.getValueInt());

		placeIn = placeDelay.getValueInt();
	}

	private void handleBlockBreak(BlockHitResult blockHit, BlockPos pos, boolean breakReady,
	                              boolean placeReady, boolean breakWasCooling, int roll, ThreadLocalRandom random) {
		if (!breakBlocks.getValue())
			return;

		if (!breakReady || roll > attackChance.getValueInt())
			return;

		if (clickSimulation.getValue() && (!isSupport(pos) || BlockUtils.canPlaceBlockClient(pos)))
			MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

		mc.interactionManager.attackBlock(pos, blockHit.getSide());
		mc.player.swingHand(Hand.MAIN_HAND);
		mc.interactionManager.updateBlockBreakingProgress(pos, blockHit.getSide());
		breakIn = breakDelay.getValueInt();

		if (placeReady && roll <= placeChance.getValueInt() && breakWasCooling && clickSimulation.getValue())
			MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
	}

	private void handleMissSwing(boolean breakReady, int roll, ThreadLocalRandom random) {
		if (mc.crosshairTarget.getType() != HitResult.Type.MISS || !breakBlocks.getValue())
			return;

		if (!breakReady || roll > attackChance.getValueInt())
			return;

		if (mc.interactionManager.hasLimitedAttackSpeed())
			((MinecraftClientAccessor) mc).setAttackCooldown(10);

		if (clickSimulation.getValue())
			MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

		mc.player.swingHand(Hand.MAIN_HAND);
		breakIn = breakDelay.getValueInt();
	}

	private void handleEntityAttack(boolean breakReady, ThreadLocalRandom random) {
		if (!(mc.crosshairTarget instanceof EntityHitResult entityHit))
			return;

		int roll = random.nextInt(1, 101);
		if (!breakReady || roll > attackChance.getValueInt())
			return;

		Entity entity = entityHit.getEntity();
		if (!allEntities.getValue() && !(entity instanceof EndCrystalEntity) && !(entity instanceof SlimeEntity))
			return;

		int previousSlot = mc.player.getInventory().getSelectedSlot();
		boolean needsSword = (entity instanceof EndCrystalEntity || entity instanceof SlimeEntity)
				&& swapForWeakness.getValue()
				&& isWeakWithoutStrength();

		if (needsSword)
			InventoryUtils.selectSword();

		if (clickSimulation.getValue())
			MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

		WorldUtils.hitEntity(entity, true);
		breakIn = breakDelay.getValueInt();

		if (needsSword)
			InventoryUtils.setInvSlot(previousSlot);
	}

	@Override
	public void onItemUse(ItemUseEvent event) {
		if (mc.player == null || mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL)
			return;

		if (!(mc.crosshairTarget instanceof BlockHitResult blockHit))
			return;

		if (blockHit.getType() != HitResult.Type.BLOCK)
			return;

		BlockPos support = blockHit.getBlockPos();
		if (isSupport(support))
			event.cancel();
	}

	private boolean isWeakWithoutStrength() {
		StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
		StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
		return weakness != null && (strength == null || strength.getAmplifier() <= weakness.getAmplifier());
	}

	private boolean isComboingNearby() {
		return mc.world.getPlayers().parallelStream()
				.filter(player -> player != mc.player)
				.filter(player -> player.squaredDistanceTo(mc.player) < 36.0D)
				.filter(player -> player.getLastAttacker() == null)
				.filter(player -> !player.isOnGround())
				.anyMatch(player -> player.hurtTime >= 2)
				&& !(mc.player.getAttacking() instanceof net.minecraft.entity.player.PlayerEntity);
	}

	private boolean isSupport(BlockPos pos) {
		Block block = mc.world.getBlockState(pos).getBlock();
		return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
	}

	private void resetState() {
		placeIn = 0;
		breakIn = 0;
		active = false;
	}
}
