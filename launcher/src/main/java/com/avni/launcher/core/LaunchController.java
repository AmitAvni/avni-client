package com.avni.launcher.core;

import com.avni.launcher.auth.MicrosoftAuth;
import com.avni.launcher.install.Installer;
import com.avni.launcher.install.LaunchSpec;
import com.avni.launcher.launch.GameLauncher;
import com.avni.launcher.model.Account;
import com.avni.launcher.model.LauncherConfig;
import javafx.application.Platform;

/**
 * Orchestrates the real launch pipeline: install Minecraft + Fabric + the Avni
 * mod, then spawn the game.
 */
public class LaunchController {

    public interface ProgressListener {
        /** @param fraction 0..1 progress, or negative for indeterminate. */
        void onProgress(double fraction, String status);

        void onDone(boolean success, String message);
    }

    private volatile boolean running = false;

    public boolean isRunning() {
        return running;
    }

    public void launch(Account account, String mcVersion, int ramMb, ProgressListener listener) {
        if (running) {
            return;
        }
        running = true;

        Thread t = new Thread(() -> {
            try {
                // Refresh a Microsoft session so the access token is valid for online servers.
                Account acct = account;
                LauncherConfig cfg = LauncherConfig.get();
                String cid = MicrosoftAuth.clientId(cfg.azureClientId);
                if (account.isMicrosoft() && account.refreshToken() != null && MicrosoftAuth.isConfigured(cid)) {
                    try {
                        Platform.runLater(() -> listener.onProgress(-1, "Refreshing Microsoft session…"));
                        acct = MicrosoftAuth.toMinecraftAccount(
                                MicrosoftAuth.refreshToken(cid, account.refreshToken()));
                        cfg.replace(acct);
                    } catch (Exception refreshFailed) {
                        acct = account; // fall back to the stored token
                    }
                }
                final Account launchAccount = acct;

                Installer installer = new Installer(mcVersion, (fraction, message) ->
                        Platform.runLater(() -> listener.onProgress(fraction, message)));
                LaunchSpec spec = installer.install();

                Platform.runLater(() -> listener.onProgress(1.0, "Starting Minecraft…"));
                new GameLauncher().launch(spec, launchAccount, ramMb, line -> {
                    if (line.contains("Avni Client initialized")) {
                        Platform.runLater(() -> listener.onProgress(1.0, "Avni mod loaded ✓"));
                    } else if (line.contains("Sound engine started")) {
                        Platform.runLater(() -> listener.onProgress(1.0, "Minecraft running ✓"));
                    }
                });

                Platform.runLater(() -> listener.onDone(true, "Minecraft launched — have fun!"));
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                Platform.runLater(() -> listener.onDone(false, "Failed: " + msg));
            } finally {
                running = false;
            }
        }, "avni-launch");
        t.setDaemon(true);
        t.start();
    }
}
