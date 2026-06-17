package com.avni.client;

import com.avni.client.gui.AvniConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvniClient implements ClientModInitializer {
    public static final String MOD_ID = "avni-client";
    public static final Logger LOGGER = LoggerFactory.getLogger("Avni Client");

    private boolean zooming = false;

    @Override
    public void onInitializeClient() {
        AvniConfig.load();
        KeyBinds.register();
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
        LOGGER.info("Avni Client initialized");
    }

    private void onEndTick(MinecraftClient client) {
        AvniConfig cfg = AvniConfig.get();

        while (KeyBinds.fullbright.wasPressed()) {
            cfg.fullbright = !cfg.fullbright;
            cfg.save();
        }
        while (KeyBinds.toggleHud.wasPressed()) {
            cfg.hudEnabled = !cfg.hudEnabled;
            cfg.save();
        }
        while (KeyBinds.config.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new AvniConfigScreen(null));
            }
        }
        while (KeyBinds.waypoints.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new com.avni.client.gui.WaypointsScreen(null));
            }
        }

        // Hold-to-zoom: only while no screen is open.
        boolean wantZoom = KeyBinds.zoom.isPressed() && client.currentScreen == null;
        if (wantZoom && !zooming) {
            OptionsUtil.startZoom(client, cfg.zoomDivisor);
            zooming = true;
        } else if (!wantZoom && zooming) {
            OptionsUtil.stopZoom(client);
            zooming = false;
        }

        OptionsUtil.setFullbright(client, cfg.fullbright);
    }
}
