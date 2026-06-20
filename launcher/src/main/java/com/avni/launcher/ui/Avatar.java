package com.avni.launcher.ui;

import com.avni.launcher.model.Account;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Builds an account avatar: a rounded square showing the coloured first
 * initial, replaced (for premium/Microsoft accounts) by the player's Minecraft
 * skin face once it loads asynchronously. Null/empty accounts keep the
 * coloured initial.
 */
public final class Avatar {

    private Avatar() {
    }

    public static StackPane of(Account account, double size) {
        String name = account == null ? "" : account.name();

        StackPane p = new StackPane();
        p.setMinSize(size, size);
        p.setMaxSize(size, size);
        p.getStyleClass().add("avatar");
        int hue = Math.abs((name.isEmpty() ? "?" : name).hashCode()) % 360;
        p.setStyle("-fx-background-color: hsb(" + hue + ", 55%, 70%);");

        Label initial = new Label(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        initial.getStyleClass().add("avatar-initial");
        p.getChildren().add(initial);

        if (account != null && account.isMicrosoft()) {
            loadFace(account.uuidNoDashes(), size, p);
        }
        return p;
    }

    private static void loadFace(String uuid, double size, StackPane target) {
        Thread t = new Thread(() -> {
            try {
                Image face = SkinService.faceFor(uuid);
                Platform.runLater(() -> {
                    ImageView iv = new ImageView(face);
                    iv.setSmooth(false); // crisp, pixelated MC head
                    iv.setFitWidth(size);
                    iv.setFitHeight(size);
                    Rectangle clip = new Rectangle(size, size);
                    clip.setArcWidth(16); // matches .avatar -fx-background-radius: 8
                    clip.setArcHeight(16);
                    iv.setClip(clip);
                    target.getChildren().add(iv);
                });
            } catch (Exception ignored) {
                // network/profile failure → keep the coloured initial
            }
        }, "skin-face-" + uuid);
        t.setDaemon(true);
        t.start();
    }
}
