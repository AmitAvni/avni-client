package com.avni.client.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets us write {@link SimpleOption}'s backing value directly, skipping the
 * clamping that {@code setValue} normally applies. Used for zoom (FOV) and
 * fullbright (gamma).
 */
@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor<T> {
    @Accessor("value")
    void avni$setValue(T value);
}
