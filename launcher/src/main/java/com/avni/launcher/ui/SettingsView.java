package com.avni.launcher.ui;

import com.avni.launcher.core.LauncherPaths;
import com.avni.launcher.model.LauncherConfig;
import com.avni.launcher.model.ModSettings;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** RAM allocation, game directory, and in-game mod feature toggles. */
public class SettingsView extends VBox {

    private final LauncherConfig config = LauncherConfig.get();
    private final ModSettings mod = ModSettings.load();

    public SettingsView() {
        getStyleClass().add("page");
        setSpacing(16);

        Label title = new Label("Settings");
        title.getStyleClass().add("page-title");

        VBox sections = new VBox(18, memorySection(), gameDirSection(), featuresSection());

        ScrollPane scroll = new ScrollPane(sections);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(title, scroll);
    }

    private VBox memorySection() {
        Label value = new Label();
        Runnable update = () -> value.setText(config.ramMb + " MB  ("
                + String.format("%.1f", config.ramMb / 1024.0) + " GB)");

        Slider slider = new Slider(1024, 8192, config.ramMb);
        slider.setBlockIncrement(512);
        slider.setMajorTickUnit(1024);
        slider.setPrefWidth(340);
        slider.getStyleClass().add("ram-slider");
        slider.valueProperty().addListener((o, ov, nv) -> {
            config.ramMb = (int) (Math.round(nv.doubleValue() / 512.0) * 512);
            update.run();
        });
        slider.setOnMouseReleased(e -> config.save());
        update.run();

        return section("Memory allocation", value, slider);
    }

    private VBox gameDirSection() {
        Label dir = new Label(LauncherPaths.gameDir().toString());
        dir.getStyleClass().add("mono");
        return section("Game directory", dir);
    }

    private VBox featuresSection() {
        VBox toggles = new VBox(10,
                toggle("HUD", mod.hudEnabled, v -> mod.hudEnabled = v),
                toggle("FPS counter", mod.fps, v -> mod.fps = v),
                toggle("Coordinates", mod.coords, v -> mod.coords = v),
                toggle("CPS counter", mod.cps, v -> mod.cps = v),
                toggle("Keystrokes overlay", mod.keystrokes, v -> mod.keystrokes = v),
                toggle("Watermark", mod.watermark, v -> mod.watermark = v),
                toggle("Fullbright", mod.fullbright, v -> mod.fullbright = v));
        return section("In-game features", toggles);
    }

    private CheckBox toggle(String label, boolean value, Consumer<Boolean> setter) {
        CheckBox cb = new CheckBox(label);
        cb.setSelected(value);
        cb.getStyleClass().add("feature-toggle");
        cb.selectedProperty().addListener((o, ov, nv) -> {
            setter.accept(nv);
            mod.save();
        });
        return cb;
    }

    private VBox section(String heading, Node... nodes) {
        VBox box = new VBox(10);
        box.getStyleClass().add("section");
        Label t = new Label(heading);
        t.getStyleClass().add("section-title");
        box.getChildren().add(t);
        box.getChildren().addAll(nodes);
        return box;
    }
}
