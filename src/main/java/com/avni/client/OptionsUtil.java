package com.avni.client;

import com.avni.client.mixin.SimpleOptionAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Zoom and fullbright both work by writing the backing {@code value} field of a
 * {@link SimpleOption} directly (via {@link SimpleOptionAccessor}), bypassing the
 * normal value clamping. The original value is remembered so it can be restored.
 */
public final class OptionsUtil {
    private static Integer savedFov = null;
    private static Double savedGamma = null;

    private OptionsUtil() {
    }

    @SuppressWarnings("unchecked")
    private static void setValue(SimpleOption<?> option, Object value) {
        ((SimpleOptionAccessor<Object>) (Object) option).avni$setValue(value);
    }

    public static void startZoom(MinecraftClient client, int divisor) {
        SimpleOption<Integer> fov = client.options.getFov();
        if (savedFov == null) {
            savedFov = fov.getValue();
        }
        int zoomed = Math.max(1, savedFov / Math.max(1, divisor));
        setValue(fov, zoomed);
    }

    public static void stopZoom(MinecraftClient client) {
        if (savedFov != null) {
            setValue(client.options.getFov(), savedFov);
            savedFov = null;
        }
    }

    /** Idempotent — safe to call every tick. */
    public static void setFullbright(MinecraftClient client, boolean on) {
        SimpleOption<Double> gamma = client.options.getGamma();
        if (on) {
            if (savedGamma == null) {
                savedGamma = gamma.getValue();
            }
            setValue(gamma, 15.0);
        } else if (savedGamma != null) {
            setValue(gamma, savedGamma);
            savedGamma = null;
        }
    }
}
