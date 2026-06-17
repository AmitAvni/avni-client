package com.avni.launcher.ui;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/** Small filled vector icons (Material-style, 24×24 paths) for the sidebar. */
public final class Icons {
    public static final String HOME =
            "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z";
    public static final String PERSON =
            "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z";
    public static final String GRID =
            "M3 3h8v8H3zM13 3h8v8h-8zM3 13h8v8H3zM13 13h8v8h-8z";
    public static final String GEAR =
            "M19.43 12.98c.04-.32.07-.64.07-.98s-.03-.66-.07-.98l2.11-1.65c.19-.15.24-.42.12-.64"
            + "l-2-3.46c-.12-.22-.39-.3-.61-.22l-2.49 1c-.52-.4-1.08-.73-1.69-.98l-.38-2.65"
            + "C14.46 2.18 14.25 2 14 2h-4c-.25 0-.46.18-.49.42l-.38 2.65c-.61.25-1.17.59-1.69.98"
            + "l-2.49-1c-.23-.09-.49 0-.61.22l-2 3.46c-.13.22-.07.49.12.64l2.11 1.65"
            + "c-.04.32-.07.65-.07.98s.03.66.07.98l-2.11 1.65c-.19.15-.24.42-.12.64l2 3.46"
            + "c.12.22.39.3.61.22l2.49-1c.52.4 1.08.73 1.69.98l.38 2.65c.03.24.24.42.49.42h4"
            + "c.25 0 .46-.18.49-.42l.38-2.65c.61-.25 1.17-.59 1.69-.98l2.49 1c.23.09.49 0 .61-.22"
            + "l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.65zM12 15.5c-1.93 0-3.5-1.57-3.5-3.5"
            + "s1.57-3.5 3.5-3.5 3.5 1.57 3.5 3.5-1.57 3.5-3.5 3.5z";

    private Icons() {
    }

    public static SVGPath of(String content) {
        SVGPath p = new SVGPath();
        p.setContent(content);
        p.setScaleX(0.75);
        p.setScaleY(0.75);
        p.setFill(Color.web("#aab2c0"));
        return p;
    }
}
