package com.avni.launcher.ui;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * The Avni mark: a small isometric voxel cube (a nod to Minecraft's blocky
 * world) shaded in the aurora palette — mint top, cyan left, violet right.
 */
public final class Logo {
    private Logo() {
    }

    public static Pane mark(double w) {
        double hw = w / 2;
        double qh = w / 4;

        Polygon top = new Polygon(hw, 0, w, qh, hw, w / 2, 0, qh);
        top.setFill(Color.web("#46E8AC"));

        Polygon left = new Polygon(0, qh, hw, w / 2, hw, w, 0, 3 * qh);
        left.setFill(Color.web("#2C9FE6"));

        Polygon right = new Polygon(hw, w / 2, w, qh, w, 3 * qh, hw, w);
        right.setFill(Color.web("#7C6BF0"));

        Pane pane = new Pane(left, right, top);
        pane.setMinSize(w, w);
        pane.setPrefSize(w, w);
        pane.setMaxSize(w, w);
        return pane;
    }
}
