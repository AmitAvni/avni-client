package com.avni.launcher;

import com.avni.launcher.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Entry point for the Avni Client desktop launcher.
 */
public class AvniLauncher extends Application {

    @Override
    public void start(Stage stage) {
        loadFonts();
        MainView root = new MainView(stage);

        Scene scene = new Scene(root, 980, 620);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
                getClass().getResource("/com/avni/launcher/app.css").toExternalForm());

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Avni Client");
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();

        root.playIntro();
    }

    private void loadFonts() {
        for (String weight : new String[]{"Regular", "Medium", "SemiBold", "Bold"}) {
            var in = getClass().getResourceAsStream("/com/avni/launcher/fonts/ChakraPetch-" + weight + ".ttf");
            if (in != null) {
                Font.loadFont(in, 12);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
