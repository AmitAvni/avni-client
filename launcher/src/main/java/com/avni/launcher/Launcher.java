package com.avni.launcher;

/**
 * Plain entry point used by the packaged (jpackage) build.
 *
 * <p>When JavaFX is on the classpath rather than the module path, launching a
 * main class that {@code extends Application} fails with "JavaFX runtime
 * components are missing". Delegating from a class that does <em>not</em> extend
 * {@link javafx.application.Application} sidesteps that, so the bundled app can
 * start without a module-path setup.
 */
public final class Launcher {
    public static void main(String[] args) {
        AvniLauncher.main(args);
    }
}
