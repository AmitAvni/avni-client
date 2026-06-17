package com.avni.launcher.ui;

import com.avni.launcher.auth.MicrosoftAuth;
import com.avni.launcher.model.Account;
import com.avni.launcher.model.LauncherConfig;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Microsoft sign-in flow: one-time client-ID setup, then device-code login. */
public class MicrosoftLoginView extends VBox {

    private final LauncherConfig config = LauncherConfig.get();
    private final Runnable onClose;
    private final Label title = new Label("Sign in with Microsoft");
    private final Label status = new Label();

    public MicrosoftLoginView(Runnable onClose) {
        this.onClose = onClose;
        getStyleClass().add("page");
        setSpacing(14);
        title.getStyleClass().add("page-title");
        status.getStyleClass().add("status");

        getChildren().add(title);
        if (MicrosoftAuth.isConfigured(clientId())) {
            buildSignIn();
        } else {
            buildSetup();
        }
    }

    private String clientId() {
        return MicrosoftAuth.clientId(config.azureClientId);
    }

    /** Only shown when this build hasn't had a client id baked in yet (developer step). */
    private void buildSetup() {
        Label info = new Label("""
                Microsoft sign-in isn't set up in this build yet.

                This is a ONE-TIME step for whoever ships Avni — players never do this.
                Register a free app at portal.azure.com (Personal Microsoft accounts,
                no redirect URI, "Allow public client flows" = Yes), then paste the
                Application (client) ID into MicrosoftAuth.CLIENT_ID and rebuild — or
                paste it below to try it now.""");
        info.getStyleClass().add("tagline");
        info.setWrapText(true);

        TextField field = new TextField();
        field.setPromptText("Application (client) ID");
        field.getStyleClass().add("text-field-dark");
        field.setMaxWidth(380);

        Button save = new Button("Use this ID");
        save.getStyleClass().add("accent-btn");
        save.setOnAction(e -> {
            String id = field.getText().trim();
            if (!id.isEmpty()) {
                config.azureClientId = id;
                config.save();
                getChildren().setAll(title);
                buildSignIn();
            }
        });
        Button back = new Button("Back");
        back.getStyleClass().add("ghost-btn");
        back.setOnAction(e -> onClose.run());

        getChildren().addAll(info, field, new HBox(10, save, back));
    }

    private void buildSignIn() {
        Label hint = new Label("Click Sign in, then approve in the browser page that opens.");
        hint.getStyleClass().add("tagline");

        VBox codeBox = new VBox(6);

        Button start = new Button("Sign in");
        start.getStyleClass().add("accent-btn");
        Button back = new Button("Back");
        back.getStyleClass().add("ghost-btn");
        back.setOnAction(e -> onClose.run());
        start.setOnAction(e -> {
            start.setDisable(true);
            startFlow(codeBox);
        });

        HBox buttons = new HBox(10, start, back);
        buttons.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(hint, buttons, codeBox, status);
    }

    private void startFlow(VBox codeBox) {
        status.setText("Requesting a sign-in code…");
        String clientId = clientId();
        Thread t = new Thread(() -> {
            try {
                MicrosoftAuth.DeviceCode dc = MicrosoftAuth.requestDeviceCode(clientId);
                Platform.runLater(() -> {
                    Label inst = new Label("Go to " + dc.verificationUri() + " and enter:");
                    inst.getStyleClass().add("tagline");
                    Label code = new Label(dc.userCode());
                    code.getStyleClass().add("device-code");
                    Button open = ghost("Open page", () -> openBrowser(dc.verificationUri()));
                    Button copy = ghost("Copy code", () -> copy(dc.userCode()));
                    codeBox.getChildren().setAll(inst, code, new HBox(10, open, copy));
                    status.setText("Waiting for you to approve…");
                });
                openBrowser(dc.verificationUri());

                Account account = MicrosoftAuth.toMinecraftAccount(
                        MicrosoftAuth.pollForToken(clientId, dc));
                config.add(account);
                Platform.runLater(() -> status.setText("Signed in as " + account.name() + "  ✓"));
                Thread.sleep(900);
                Platform.runLater(onClose);
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Platform.runLater(() -> status.setText("Failed: " + msg));
            }
        }, "ms-login");
        t.setDaemon(true);
        t.start();
    }

    private Button ghost(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("ghost-btn");
        b.setOnAction(e -> action.run());
        return b;
    }

    private void openBrowser(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                return;
            }
        } catch (Throwable ignored) {
            // fall through to a per-OS launcher
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Throwable ignored) {
            // user can open the URL manually
        }
    }

    private void copy(String s) {
        ClipboardContent c = new ClipboardContent();
        c.putString(s);
        Clipboard.getSystemClipboard().setContent(c);
    }
}
