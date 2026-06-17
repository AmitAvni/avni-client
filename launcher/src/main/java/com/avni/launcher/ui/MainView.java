package com.avni.launcher.ui;

import com.avni.launcher.core.LaunchController;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.List;

/**
 * Root view: a rounded, shadowed "card" with a custom title bar (the window is
 * undecorated/transparent), a left nav sidebar, and a swappable content area.
 */
public class MainView extends StackPane {

    private final Stage stage;
    private final BorderPane card = new BorderPane();
    private final StackPane content = new StackPane();
    private final LaunchController controller = new LaunchController();
    private VBox sidebar;
    private Button homeBtn;
    private Button versionsBtn;
    private Button accountsBtn;
    private Button settingsBtn;
    private double dragOffsetX;
    private double dragOffsetY;

    public MainView(Stage stage) {
        this.stage = stage;
        getStyleClass().add("root-pane");
        setPadding(new Insets(22)); // leaves room for the drop shadow

        card.getStyleClass().add("card");
        DropShadow shadow = new DropShadow(30, Color.rgb(0, 0, 0, 0.6));
        shadow.setOffsetY(10);
        card.setEffect(shadow);

        card.setTop(buildTitleBar());
        card.setLeft(buildSidebar());

        content.getStyleClass().add("content");
        content.getChildren().add(new HomeView(controller, this::showAccounts, this::showVersions));

        StackPane centerStack = new StackPane(buildAurora(), content);
        card.setCenter(centerStack);

        getChildren().add(card);
    }

    /** Signature ambient: a soft, centered aurora glow that gently breathes. */
    private Region buildAurora() {
        Region aurora = new Region();
        aurora.getStyleClass().add("aurora");
        aurora.setMouseTransparent(true);

        Timeline breathe = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(aurora.opacityProperty(), 0.7)),
                new KeyFrame(Duration.seconds(6), new KeyValue(aurora.opacityProperty(), 1.0)));
        breathe.setAutoReverse(true);
        breathe.setCycleCount(Animation.INDEFINITE);
        breathe.play();
        return aurora;
    }

    private Region buildTitleBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("titlebar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Avni Client");
        title.getStyleClass().add("titlebar-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimize = windowButton("—", () -> stage.setIconified(true), "win-min");
        Button close = windowButton("✕", stage::close, "win-close");

        bar.getChildren().addAll(title, spacer, minimize, close);

        bar.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });
        bar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
        return bar;
    }

    private Button windowButton(String glyph, Runnable action, String styleClass) {
        Button b = new Button(glyph);
        b.getStyleClass().addAll("win-btn", styleClass);
        b.setFocusTraversable(false);
        b.setOnAction(e -> action.run());
        return b;
    }

    private Region buildSidebar() {
        sidebar = new VBox(6);
        sidebar.getStyleClass().add("sidebar");

        Label wordmark = new Label("AVNI");
        wordmark.getStyleClass().add("wordmark");
        HBox brand = new HBox(10, Logo.mark(26), wordmark);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(2, 0, 16, 2));

        homeBtn = navButton("Home", Icons.HOME);
        versionsBtn = navButton("Versions", Icons.GRID);
        accountsBtn = navButton("Accounts", Icons.PERSON);
        settingsBtn = navButton("Settings", Icons.GEAR);
        setActive(homeBtn);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        homeBtn.setOnAction(e -> showHome());
        versionsBtn.setOnAction(e -> showVersions());
        accountsBtn.setOnAction(e -> showAccounts());
        settingsBtn.setOnAction(e -> showSettings());

        sidebar.getChildren().addAll(brand, homeBtn, versionsBtn, accountsBtn, settingsBtn,
                spacer, buildUserChip());
        return sidebar;
    }

    /** Bottom-of-sidebar chip showing the active account. */
    private HBox buildUserChip() {
        com.avni.launcher.model.Account account = com.avni.launcher.model.LauncherConfig.get().selectedAccount();
        String name = account == null ? "No account" : account.name();

        StackPane avatar = new StackPane();
        avatar.setMinSize(30, 30);
        avatar.setMaxSize(30, 30);
        avatar.getStyleClass().add("avatar");
        int hue = Math.abs(name.hashCode()) % 360;
        avatar.setStyle("-fx-background-color: hsb(" + hue + ", 55%, 70%);");
        Label initial = new Label(name.substring(0, 1).toUpperCase());
        initial.getStyleClass().add("avatar-initial");
        avatar.getChildren().add(initial);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("user-name");
        Label sub = new Label(account == null ? "tap Accounts" : "Offline");
        sub.getStyleClass().add("user-sub");
        VBox info = new VBox(1, nameLabel, sub);

        HBox chip = new HBox(9, avatar, info);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("user-chip");
        chip.setOnMouseClicked(e -> showAccounts());
        return chip;
    }

    private void showHome() {
        setActive(homeBtn);
        swap(new HomeView(controller, this::showAccounts, this::showVersions));
    }

    private void showVersions() {
        setActive(versionsBtn);
        swap(new VersionsView(controller));
    }

    private void showAccounts() {
        setActive(accountsBtn);
        swap(new AccountsView(this::showMicrosoftLogin));
    }

    private void showMicrosoftLogin() {
        setActive(accountsBtn);
        swap(new MicrosoftLoginView(this::showAccounts));
    }

    private void showSettings() {
        setActive(settingsBtn);
        swap(new SettingsView());
    }

    private Button navButton(String text, String iconPath) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-btn");
        b.setGraphic(Icons.of(iconPath));
        b.setGraphicTextGap(12);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFocusTraversable(false);
        return b;
    }

    private void setActive(Button active) {
        for (Button b : List.of(homeBtn, versionsBtn, accountsBtn, settingsBtn)) {
            boolean on = b == active;
            b.getStyleClass().remove("nav-active");
            if (on) {
                b.getStyleClass().add("nav-active");
            }
            if (b.getGraphic() instanceof SVGPath icon) {
                icon.setFill(Color.web(on ? "#2FE0A6" : "#8a93a6"));
            }
        }
    }

    private void swap(Region view) {
        content.getChildren().setAll(view);
        FadeTransition fade = new FadeTransition(Duration.millis(220), view);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    /** Entrance animation: fade + slight rise. */
    public void playIntro() {
        setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(420), this);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition rise = new TranslateTransition(Duration.millis(420), card);
        rise.setFromY(18);
        rise.setToY(0);
        new ParallelTransition(fade, rise).play();
    }
}
