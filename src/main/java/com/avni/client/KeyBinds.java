package com.avni.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeyBinds {
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("avni-client", "main"));

    public static KeyBinding zoom;
    public static KeyBinding fullbright;
    public static KeyBinding toggleHud;
    public static KeyBinding config;
    public static KeyBinding waypoints;

    private KeyBinds() {
    }

    public static void register() {
        zoom = reg("key.avni-client.zoom", GLFW.GLFW_KEY_C);
        fullbright = reg("key.avni-client.fullbright", GLFW.GLFW_KEY_G);
        toggleHud = reg("key.avni-client.toggle_hud", GLFW.GLFW_KEY_H);
        config = reg("key.avni-client.config", GLFW.GLFW_KEY_RIGHT_SHIFT);
        waypoints = reg("key.avni-client.waypoints", GLFW.GLFW_KEY_B);
    }

    private static KeyBinding reg(String translationKey, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(translationKey, InputUtil.Type.KEYSYM, code, CATEGORY));
    }
}
