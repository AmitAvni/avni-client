package com.avni.launcher.ui;

import com.avni.launcher.core.LaunchController;
import com.avni.launcher.model.Account;
import com.avni.launcher.model.LauncherConfig;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Launch screen: a hero up top and a docked launch bar at the bottom
 * (account · version · LAUNCH) with an inline progress row.
 */
public class HomeView extends BorderPane {

    private final LaunchController controller;
    private final LauncherConfig config = LauncherConfig.get();
    private final Runnable onManageAccounts;
    private final Runnable onChooseVersion;

    private final ProgressBar progress = new ProgressBar(0);
    private final Label status = new Label();
    private final HBox progressRow = new HBox(12);
    private final Button launchBtn = new Button("LAUNCH");

    public HomeView(LaunchController controller, Runnable onManageAccounts, Runnable onChooseVersion) {
        this.controller = controller;
        this.onManageAccounts = onManageAccounts;
        this.onChooseVersion = onChooseVersion;
        getStyleClass().add("home");

        setCenter(buildHero());
        setBottom(buildLaunchArea());
    }

    private VBox buildHero() {
        Label brand = new Label("AVNI CLIENT");
        brand.getStyleClass().add("brand");

        Label tagline = new Label("a fast, lightweight Minecraft client");
        tagline.getStyleClass().add("tagline");

        Label badge = new Label("Minecraft " + config.version + "   •   Fabric");
        badge.getStyleClass().add("badge");

        Label caption = new Label("WHAT'S INCLUDED");
        caption.getStyleClass().add("hero-caption");

        FlowPane features = new FlowPane(10, 10);
        features.setAlignment(Pos.CENTER);
        features.setMaxWidth(560);
        for (String f : new String[]{"Zoom", "FPS HUD", "Coordinates", "CPS counter", "Keystrokes", "Fullbright"}) {
            features.getChildren().add(featurePill(f));
        }

        VBox hero = new VBox(14, brand, tagline, badge, caption, features);
        hero.setAlignment(Pos.CENTER);
        VBox.setMargin(caption, new Insets(16, 0, 0, 0));
        return hero;
    }

    private javafx.scene.Node featurePill(String text) {
        Region dot = new Region();
        dot.setMinSize(7, 7);
        dot.setMaxSize(7, 7);
        dot.setStyle("-fx-background-color: #2FE0A6; -fx-background-radius: 4;");
        Label label = new Label(text);
        label.getStyleClass().add("feature-pill-text");
        HBox pill = new HBox(8, dot, label);
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.getStyleClass().add("feature-pill");
        return pill;
    }

    private VBox buildLaunchArea() {
        progress.getStyleClass().add("progress");
        progress.setPrefWidth(240);
        status.getStyleClass().add("status");
        progressRow.setAlignment(Pos.CENTER_LEFT);
        progressRow.getChildren().addAll(progress, status);
        progressRow.setVisible(false);
        progressRow.setManaged(false);

        launchBtn.getStyleClass().add("launch-btn");
        addHoverPulse(launchBtn);
        launchBtn.setOnAction(e -> startLaunch());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(14, accountChip(), spacer, versionPill(), launchBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("launch-bar");

        VBox area = new VBox(10, progressRow, bar);
        return area;
    }

    private HBox versionPill() {
        Label icon = new Label("◆");
        icon.getStyleClass().add("pill-icon");
        Label label = new Label(config.version);
        label.getStyleClass().add("chip-text");
        HBox pill = new HBox(8, icon, label);
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.getStyleClass().add("account-chip");
        pill.setOnMouseClicked(e -> onChooseVersion.run());
        return pill;
    }

    private HBox accountChip() {
        Account selected = config.selectedAccount();
        String name = selected == null ? "Add an account" : selected.name();

        StackPane avatar = Avatar.of(selected, 26);

        Label label = new Label(name);
        label.getStyleClass().add("chip-text");

        HBox chip = new HBox(8, avatar, label);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("account-chip");
        chip.setOnMouseClicked(e -> onManageAccounts.run());
        return chip;
    }

    private void startLaunch() {
        if (controller.isRunning()) {
            return;
        }
        Account account = config.selectedAccount();
        if (account == null) {
            status.setText("Sign in with Microsoft first");
            onManageAccounts.run();
            return;
        }

        launchBtn.setDisable(true);
        progressRow.setVisible(true);
        progressRow.setManaged(true);
        progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        status.setText("Starting…");

        controller.launch(account, config.version, config.ramMb, new LaunchController.ProgressListener() {
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

    private void addHoverPulse(Button button) {
        ScaleTransition grow = new ScaleTransition(Duration.millis(120), button);
        grow.setToX(1.05);
        grow.setToY(1.05);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(120), button);
        shrink.setToX(1.0);
        shrink.setToY(1.0);
        button.setOnMouseEntered(e -> {
            shrink.stop();
            grow.playFromStart();
        });
        button.setOnMouseExited(e -> {
            grow.stop();
            shrink.playFromStart();
        });
    }
}
