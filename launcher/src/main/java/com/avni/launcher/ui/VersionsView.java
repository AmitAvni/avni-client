package com.avni.launcher.ui;

import com.avni.launcher.core.LaunchController;
import com.avni.launcher.install.Installer;
import com.avni.launcher.model.Account;
import com.avni.launcher.model.LauncherConfig;
import com.avni.launcher.util.Http;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lunar-style version picker: a grid of version <em>series</em> cards (1.21,
 * 1.20, …) on the left, and a "Selected version" panel on the right with a
 * dropdown of the specific releases in that series plus a launch button.
 */
public class VersionsView extends HBox {

    private static final String MANIFEST =
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    private final LaunchController controller;
    private final LauncherConfig config = LauncherConfig.get();
    private final Map<String, List<String>> seriesPatches = new LinkedHashMap<>();

    private final FlowPane grid = new FlowPane(14, 14);
    private final StackPane banner = new StackPane();
    private final Label bannerLabel = new Label();
    private final Label sideTitle = new Label("Select a version");
    private final Label sideDesc = new Label();
    private final ComboBox<String> patchCombo = new ComboBox<>();
    private final ProgressBar progress = new ProgressBar(0);
    private final Label status = new Label();
    private final Button launchBtn = new Button("LAUNCH GAME");

    private String selectedSeries;

    public VersionsView(LaunchController controller) {
        this.controller = controller;
        getStyleClass().add("versions-root");
        setSpacing(18);

        Label title = new Label("Versions");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Choose a Minecraft version to launch");
        subtitle.getStyleClass().add("tagline");

        grid.setPadding(new Insets(8, 0, 0, 0));
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox left = new VBox(8, title, subtitle, scroll);
        HBox.setHgrow(left, Priority.ALWAYS);

        getChildren().addAll(left, buildSidePanel());

        Label loading = new Label("Loading versions…");
        loading.getStyleClass().add("tagline");
        grid.getChildren().add(loading);
        loadVersions();
    }

    private VBox buildSidePanel() {
        Label header = new Label("SELECTED VERSION");
        header.getStyleClass().add("side-header");

        banner.getStyleClass().add("banner");
        banner.setMinHeight(120);
        banner.setPrefHeight(120);
        bannerLabel.getStyleClass().add("banner-label");
        banner.getChildren().add(bannerLabel);

        sideTitle.getStyleClass().add("side-title");
        sideDesc.getStyleClass().add("side-desc");
        sideDesc.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label verLabel = new Label("Version");
        verLabel.getStyleClass().add("side-field");
        patchCombo.getStyleClass().add("version-combo");
        HBox.setHgrow(patchCombo, Priority.ALWAYS);
        patchCombo.setMaxWidth(Double.MAX_VALUE);
        HBox verRow = new HBox(10, verLabel, patchCombo);
        verRow.setAlignment(Pos.CENTER_LEFT);

        progress.getStyleClass().add("progress");
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setVisible(false);
        status.getStyleClass().add("status");

        launchBtn.getStyleClass().add("launch-game-btn");
        launchBtn.setMaxWidth(Double.MAX_VALUE);
        launchBtn.setOnAction(e -> launch());

        VBox panel = new VBox(12, header, banner, sideTitle, sideDesc, spacer, verRow, progress, status, launchBtn);
        panel.getStyleClass().add("side-panel");
        panel.setMinWidth(300);
        panel.setMaxWidth(300);
        return panel;
    }

    private void loadVersions() {
        Thread t = new Thread(() -> {
            try {
                JsonObject manifest = JsonParser.parseString(Http.getString(MANIFEST)).getAsJsonObject();
                // Manifest is newest-first, so insertion order gives newest series/releases first.
                Map<String, List<String>> map = new LinkedHashMap<>();
                for (JsonElement e : manifest.getAsJsonArray("versions")) {
                    JsonObject v = e.getAsJsonObject();
                    if (!v.get("type").getAsString().equals("release")) {
                        continue;
                    }
                    String id = v.get("id").getAsString();
                    String s = series(id);
                    if (supported(s)) {
                        map.computeIfAbsent(s, k -> new ArrayList<>()).add(id);
                    }
                }
                Platform.runLater(() -> {
                    seriesPatches.putAll(map);
                    buildGrid();
                    selectDefault();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    grid.getChildren().clear();
                    Label err = new Label("Couldn't load version list (offline?)");
                    err.getStyleClass().add("tagline");
                    grid.getChildren().add(err);
                });
            }
        }, "versions-load");
        t.setDaemon(true);
        t.start();
    }

    private void buildGrid() {
        grid.getChildren().clear();
        for (String s : seriesPatches.keySet()) {
            grid.getChildren().add(card(s));
        }
    }

    private Node card(String series) {
        StackPane card = new StackPane();
        card.getStyleClass().add("version-card");
        if (series.equals(selectedSeries)) {
            card.getStyleClass().add("version-selected");
        }
        card.setMinSize(232, 108);
        card.setPrefSize(232, 108);
        card.setMaxSize(232, 108);
        card.setStyle(gradient(series, 42, 20));

        Label name = new Label(series);
        name.getStyleClass().add("version-name");
        card.getChildren().add(name);

        card.setOnMouseClicked(e -> selectSeries(series));
        return card;
    }

    private void selectDefault() {
        String cur = config.version != null ? series(config.version) : null;
        if (cur != null && seriesPatches.containsKey(cur)) {
            selectSeries(cur);
        } else if (!seriesPatches.isEmpty()) {
            selectSeries(seriesPatches.keySet().iterator().next());
        }
    }

    private void selectSeries(String series) {
        selectedSeries = series;
        buildGrid();

        List<String> patches = seriesPatches.getOrDefault(series, List.of());
        patchCombo.getItems().setAll(patches);
        if (config.version != null && patches.contains(config.version)) {
            patchCombo.getSelectionModel().select(config.version);
        } else {
            patchCombo.getSelectionModel().selectFirst();
        }

        sideTitle.setText("Minecraft " + series);
        bannerLabel.setText(series);
        banner.setStyle(gradient(series, 45, 22));
        boolean hasAvni = patches.contains(Installer.AVNI_MC_VERSION);
        sideDesc.setText(hasAvni
                ? "The " + series + " series. Includes 1.21.11 — the version Avni Client's "
                  + "in-game features (zoom, HUD, fullbright…) are built for."
                : "The " + series + " series of Minecraft. Pick a specific release below, then launch. "
                  + "Avni in-game features are available on 1.21.11.");
    }

    private void launch() {
        if (controller.isRunning()) {
            return;
        }
        String version = patchCombo.getValue();
        if (version == null) {
            return;
        }
        config.version = version;
        config.save();
        Account account = config.selectedAccount();
        if (account == null) {
            account = config.addOffline("Player");
        }

        launchBtn.setDisable(true);
        progress.setVisible(true);
        progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        status.setText("Starting…");

        controller.launch(account, version, config.ramMb, new LaunchController.ProgressListener() {
            @Override
            public void onProgress(double fraction, String text) {
                progress.setProgress(fraction < 0 ? ProgressIndicator.INDETERMINATE_PROGRESS : fraction);
                status.setText(text);
            }

            @Override
            public void onDone(boolean success, String message) {
                progress.setProgress(success ? 1.0 : 0.0);
                status.setText(message);
                launchBtn.setDisable(false);
            }
        });
    }

    private static String gradient(String key, int b1, int b2) {
        int h1 = Math.abs(key.hashCode()) % 360;
        int h2 = (h1 + 38) % 360;
        return "-fx-background-color: linear-gradient(to bottom right, "
                + "hsb(" + h1 + ", 45%, " + b1 + "%), hsb(" + h2 + ", 55%, " + b2 + "%));";
    }

    /**
     * Groups a version into its series. Year-based versions (26.2, 26.1.1) group
     * by the year (26); classic versions (1.21.11) group by minor (1.21).
     */
    private static String series(String id) {
        String[] p = id.split("\\.");
        if (p[0].equals("1") && p.length >= 2) {
            return p[0] + "." + p[1];
        }
        return p[0];
    }

    /** Fabric supports classic 1.14+ and all year-based versions. */
    private static boolean supported(String series) {
        try {
            if (series.startsWith("1.")) {
                return Integer.parseInt(series.substring(2)) >= 14;
            }
            return Integer.parseInt(series) >= 14; // year-based (26, 27, …)
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
