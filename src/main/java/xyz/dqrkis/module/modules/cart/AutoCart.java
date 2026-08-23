package xyz.dqrkis.module.modules.cart;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.KeybindSetting;
import xyz.dqrkis.module.setting.ModeSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.InventoryUtils;
import xyz.dqrkis.utils.KeyUtils;
import net.minecraft.item.BowItem;
import net.minecraft.block.Blocks;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.Items;

public final class AutoCart extends Module implements TickListener {
    public enum Weapon { BOW, CROSSBOW }
    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), 1, false);
    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 10, 0, 1);
    private final BooleanSetting autoShoot = new BooleanSetting(EncryptedString.of("Auto Shoot"), false);
    private final ModeSetting<Weapon> weapon = new ModeSetting<>(EncryptedString.of("Weapon"), Weapon.BOW, Weapon.class);
    private final NumberSetting bowCharge = new NumberSetting(EncryptedString.of("Bow Charge"), 3, 20, 8, 1);
    private BlockPos railPos; private int state; private int delayTicks; private int chargeTicks;
    public AutoCart() {
        super(EncryptedString.of("Auto Cart"), EncryptedString.of("Places rail and minecart"), -1, Category.CART);
        addSettings(activateKey, delay, autoShoot, weapon, bowCharge);
    }
    @Override public void onEnable() { eventManager.add(TickListener.class, this); state=0; delayTicks=0; chargeTicks=0; railPos=null; super.onEnable(); }
    @Override public void onDisable() { eventManager.remove(TickListener.class, this); if(mc.options!=null){mc.options.useKey.setPressed(false);} railPos=null; super.onDisable(); }
    @Override public void onTick() {
        if(mc.player==null||mc.world==null||mc.currentScreen!=null) return;
        boolean pressed = KeyUtils.isKeyPressed(activateKey.getKey());
        if(!pressed && state<4){ state=0; delayTicks=0; return; }
        if(delayTicks>0){ delayTicks--; return; }
        mc.execute(this::step);
    }
    private void step(){
        int d=delay.getValueInt();
        switch(state){
            case 0->{ if(tryPlaceRail()){ if(d>0){state=1;delayTicks=d;} else state=2; } }
            case 1-> state=2;
            case 2->{ if(tryPlaceCart()){ if(d>0){state=3;delayTicks=d;} else state=autoShoot.getValue()?4:0; } else state=0; }
            case 3-> state=autoShoot.getValue()?4:0;
            case 4->{ if(selectWeapon()){ state=5; chargeTicks=0; mc.options.useKey.setPressed(true); aimAtCart(); } else state=0; }
            case 5->{ aimAtCart(); if(++chargeTicks>=bowCharge.getValueInt()){ mc.options.useKey.setPressed(false); if(mc.interactionManager!=null) mc.interactionManager.stopUsingItem(mc.player); state=0; } }
        }
    }
    private boolean tryPlaceRail(){
        if(!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType()!=HitResult.Type.BLOCK) return false;
        BlockPos place=hit.getBlockPos().offset(hit.getSide());
        if(mc.world.getBlockState(place).isOf(Blocks.RAIL)||mc.world.getBlockState(place).isOf(Blocks.POWERED_RAIL)||mc.world.getBlockState(place).isOf(Blocks.DETECTOR_RAIL)||mc.world.getBlockState(place).isOf(Blocks.ACTIVATOR_RAIL)){ railPos=place; return true; }
        if(!mc.world.getBlockState(place).isReplaceable()) return false;
        if(!InventoryUtils.hasItemInHotbar(i->i==Items.RAIL||i==Items.POWERED_RAIL||i==Items.DETECTOR_RAIL||i==Items.ACTIVATOR_RAIL)) return false;
        ActionResult r=mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if(r.isAccepted()){ mc.player.swingHand(Hand.MAIN_HAND); railPos=place; return true; }
        return false;
    }
    private boolean tryPlaceCart(){
        if(railPos==null) return false;
        if(!InventoryUtils.selectItemFromHotbar(Items.TNT_MINECART) && !InventoryUtils.selectItemFromHotbar(Items.MINECART) && !InventoryUtils.selectItemFromHotbar(Items.CHEST_MINECART)) return false;
        BlockHitResult hit=new BlockHitResult(railPos.toCenterPos(), Direction.UP, railPos, false);
        ActionResult r=mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if(r.isAccepted()){ mc.player.swingHand(Hand.MAIN_HAND); return true; }
        return false;
    }
    private boolean selectWeapon(){
        if(weapon.getMode()==Weapon.BOW) return selectBow();
        for(int i=0;i<9;i++){ var s=mc.player.getInventory().getStack(i); if(s.getItem() instanceof net.minecraft.item.CrossbowItem){ var comp=s.get(net.minecraft.component.DataComponentTypes.CHARGED_PROJECTILES); if(comp!=null && !comp.isEmpty()){ InventoryUtils.setInvSlot(i); return true; } } }
        return selectBow();
    }
    private boolean selectBow(){
        for(int i=0;i<9;i++) if(mc.player.getInventory().getStack(i).getItem() instanceof BowItem){ InventoryUtils.setInvSlot(i); return true; }
        return false;
    }
    private void aimAtCart(){
        if(railPos==null) return;
        Vec3d t=railPos.toCenterPos().add(0,1.1,0); Vec3d d=t.subtract(mc.player.getEyePos()).normalize();
        mc.player.setYaw((float)Math.toDegrees(Math.atan2(-d.x,d.z))); mc.player.setPitch((float)(-Math.toDegrees(Math.asin(d.y))));
    }
}
