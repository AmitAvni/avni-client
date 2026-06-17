package com.avni.client.mixin;

import com.avni.client.hud.CpsTracker;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void avni$onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) {
            return;
        }
        if (input.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            CpsTracker.LEFT.click();
        } else if (input.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            CpsTracker.RIGHT.click();
        }
    }
}
