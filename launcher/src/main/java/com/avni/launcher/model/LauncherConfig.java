package com.avni.launcher.model;

import com.avni.launcher.core.LauncherPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Persisted launcher settings (~/.avni-client/launcher.json). */
public class LauncherConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static LauncherConfig instance;

    public List<Account> accounts = new ArrayList<>();
    public String selectedUuid = null;
    public int ramMb = 2048;
    public String version = "1.21.11";
    /** Azure application (client) ID for Microsoft sign-in (device-code flow). */
    public String azureClientId = "";

    public static LauncherConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(LauncherPaths.configFile())) {
                try (Reader r = Files.newBufferedReader(LauncherPaths.configFile())) {
                    instance = GSON.fromJson(r, LauncherConfig.class);
                }
            }
        } catch (Exception ignored) {
            // fall through to defaults
        }
        if (instance == null) {
            instance = new LauncherConfig();
        }
        if (instance.accounts == null) {
            instance.accounts = new ArrayList<>();
        }
    }

    public void save() {
        try {
            Files.createDirectories(LauncherPaths.ROOT);
            try (Writer w = Files.newBufferedWriter(LauncherPaths.configFile())) {
                GSON.toJson(this, w);
            }
        } catch (Exception ignored) {
            // best effort
        }
    }

    /** The selected account, or the first one, or null if there are none. */
    public Account selectedAccount() {
        if (accounts.isEmpty()) {
            return null;
        }
        for (Account a : accounts) {
            if (a.uuid().equals(selectedUuid)) {
                return a;
            }
        }
        return accounts.get(0);
    }

    /** Adds (or replaces by uuid) an account and selects it. */
    public Account add(Account a) {
        accounts.removeIf(x -> x.uuid().equals(a.uuid()));
        accounts.add(a);
        selectedUuid = a.uuid();
        save();
        return a;
    }

    /** Replaces an account in place (by uuid) without changing selection — for token refresh. */
    public void replace(Account a) {
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).uuid().equals(a.uuid())) {
                accounts.set(i, a);
                save();
                return;
            }
        }
    }

    public void select(Account a) {
        selectedUuid = a.uuid();
        save();
    }

    public void remove(Account a) {
        accounts.removeIf(x -> x.uuid().equals(a.uuid()));
        if (a.uuid().equals(selectedUuid)) {
            selectedUuid = accounts.isEmpty() ? null : accounts.get(0).uuid();
        }
        save();
    }
}
