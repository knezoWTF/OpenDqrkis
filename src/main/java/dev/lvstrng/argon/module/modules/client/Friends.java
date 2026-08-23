package dev.lvstrng.argon.module.modules.client;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.event.events.AttackListener;
import dev.lvstrng.argon.event.events.ButtonListener;
import dev.lvstrng.argon.event.events.HudListener;
import dev.lvstrng.argon.managers.FriendManager;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.KeybindSetting;
import dev.lvstrng.argon.utils.EncryptedString;
import dev.lvstrng.argon.utils.RenderUtils;
import dev.lvstrng.argon.utils.TextRenderer;
import dev.lvstrng.argon.utils.WorldUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public final class Friends extends Module implements ButtonListener, AttackListener, HudListener {
    private final KeybindSetting addFriendKey = new KeybindSetting(EncryptedString.of("Friend Key"), GLFW.GLFW_MOUSE_BUTTON_MIDDLE, false)
            .setDescription(EncryptedString.of("Key to add/remove friends"));
    public final BooleanSetting antiAttack = new BooleanSetting(EncryptedString.of("Anti-Attack"), false)
            .setDescription(EncryptedString.of("Doesn't let you hit friends"));
    public final BooleanSetting disableAimAssist = new BooleanSetting(EncryptedString.of("Anti-Aim"), false)
            .setDescription(EncryptedString.of("Disables aim assist for friends"));
    public final BooleanSetting friendStatus = new BooleanSetting(EncryptedString.of("Friend Status"), false)
            .setDescription(EncryptedString.of("Tells you if you're aiming at a friend or not"));

    private FriendManager manager;

    public Friends() {
        super(EncryptedString.of("Friends"), EncryptedString.of("This module makes it so you can't do certain stuff if you have a player friended!"), -1, Category.CLIENT);
        addSettings(addFriendKey, antiAttack, disableAimAssist, friendStatus);
        setKey(-1);
    }

    @Override
    public void onEnable() {
        manager = Argon.INSTANCE.getFriendManager();

        eventManager.add(ButtonListener.class, this);
        eventManager.add(AttackListener.class, this);
        eventManager.add(HudListener.class, this);

        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(ButtonListener.class, this);
        eventManager.remove(AttackListener.class, this);
        eventManager.remove(HudListener.class, this);

        super.onDisable();
    }

    @Override
    public void onButtonPress(ButtonEvent event) {
        if(mc.player == null)
            return;

        if(mc.currentScreen != null)
            return;

        if(mc.crosshairTarget instanceof EntityHitResult hitResult) {
            Entity entity = hitResult.getEntity();

            if(entity instanceof PlayerEntity player) {
                if (event.button == addFriendKey.getKey() && event.action == GLFW.GLFW_PRESS) {
                    if(!manager.isFriend(player))
                        manager.addFriend(player);
                    else manager.removeFriend(player);
                }
            }
        }
    }

    @Override
    public void onAttack(AttackEvent event) {
        if(!antiAttack.getValue())
            return;

        if(manager.isAimingOverFriend())
            event.cancel();
    }

    @Override
    public void onRenderHud(HudEvent event) {
        if(!friendStatus.getValue())
            return;

        DrawContext context = event.context;
        RenderUtils.unscaledProjection(context);
        if(WorldUtils.getHitResult(100) instanceof EntityHitResult hitResult) {
            Entity entity = hitResult.getEntity();

            if(entity instanceof PlayerEntity player) {
                if(manager.isFriend(player)) {
                    TextRenderer.drawCenteredString(EncryptedString.of("Player is friend"), context, (mc.getWindow().getWidth() / 2), (mc.getWindow().getHeight() / 2) + 25, Color.GREEN.getRGB());
                }
            }
        }
        RenderUtils.scaledProjection(context);
    }
}
