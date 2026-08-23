package xyz.dqrkis.module;

import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.event.events.ButtonListener;
import xyz.dqrkis.module.modules.cart.*;
import xyz.dqrkis.module.modules.client.ClickGUI;
import xyz.dqrkis.module.modules.client.Friends;
import xyz.dqrkis.module.modules.client.SelfDestruct;
import xyz.dqrkis.module.modules.combat.*;
import xyz.dqrkis.module.modules.misc.*;
import xyz.dqrkis.module.modules.render.*;
import xyz.dqrkis.module.setting.KeybindSetting;
import xyz.dqrkis.utils.EncryptedString;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager implements ButtonListener {
	private final List<Module> modules = new ArrayList<>();

	public ModuleManager() {
		addModules();
		addKeybinds();
	}

	public void addModules() {
		//Combat
		add(new AimAssist());
		add(new AnchorMacro());
		add(new AutoCrystal());
		add(new AutoDoubleHand());
		add(new AutoHitCrystal());
		add(new AutoInventoryTotem());
		add(new TriggerBot());
		add(new AutoPot());
		add(new AutoPotRefill());
		add(new AutoWTap());
		add(new CrystalOptimizer());
		add(new DoubleAnchor());
		add(new HoverTotem());
		add(new NoMissDelay());
		add(new ShieldDisabler());
		add(new TotemOffhand());
		add(new AutoJumpReset());
		add(new AutoWeb());
		add(new AntiWeb());
		add(new HitboxExpand());
		add(new MaceSwap());
		add(new QuickStrike());
		add(new TotemPopHit());
		add(new StunSlam());
		add(new Macro198());
		add(new AutoDTap());
		add(new AutoMace());
		add(new AutoLava());
		add(new SafeCart());
		add(new AutoCart());
		add(new CartTrap());
		add(new ShopBuyer());
		add(new CrateBuyer());
		add(new AutoSell());
		add(new AhSell());
		add(new LegitTridentFly());
		add(new AntiTrap());
		add(new AutoReconnect());
add(new SilentHomeSetter());
		add(new LightFinder());
		add(new RtpBaseFinder());
		add(new SpawnerProtect());
		add(new AutoTreeFarmer());
		add(new TunnelBaseFinder());
		add(new AuctionSniper());
		add(new AutoShulker());
		add(new AutoBoneOrder());
		add(new SpawnerDropper());

		//Misc
		add(new Prevent());
		add(new AutoXP());
		add(new NoJumpDelay());
		add(new PingSpoof());
		add(new FakeLag());
		add(new AutoClicker());
		add(new KeyPearl());
		add(new NoBreakDelay());
		add(new Freecam());
		add(new PackSpoof());
		add(new Sprint());
		add(new LootYeeter());
		add(new AntiAfk());
		add(new FastPlace());

		//Render
		add(new HUD());
		add(new NoBounce());
		add(new PlayerESP());
		add(new StorageEsp());
		add(new TargetHud());
		add(new Glow());
		add(new HideScoreboard());
		add(new FakeScoreboard());
		add(new NameHider());

		//Client
		add(new ClickGUI());
		add(new Friends());
		add(new SelfDestruct());
	}

	public List<Module> getEnabledModules() {
		return modules.stream()
				.filter(Module::isEnabled)
				.toList();
	}


	public List<Module> getModules() {
		return modules;
	}

	public void addKeybinds() {
		Dqrkis.INSTANCE.getEventManager().add(ButtonListener.class, this);

		for (Module module : modules)
			module.addSetting(new KeybindSetting(EncryptedString.of("Keybind"), module.getKey(), true).setDescription(EncryptedString.of("Key to enabled the module")));
	}

	public List<Module> getModulesInCategory(Category category) {
		return modules.stream()
				.filter(module -> module.getCategory() == category)
				.toList();
	}

	@SuppressWarnings("unchecked")
	public <T extends Module> T getModule(Class<T> moduleClass) {
		return (T) modules.stream()
				.filter(moduleClass::isInstance)
				.findFirst()
				.orElse(null);
	}

	public void add(Module module) {
		modules.add(module);
	}

	@Override
	public void onButtonPress(ButtonEvent event) {
		if(!SelfDestruct.destruct) {
			modules.forEach(module -> {
				if(module.getKey() == event.button && event.action == GLFW.GLFW_PRESS)
					module.toggle();
			});
		}
	}
}
