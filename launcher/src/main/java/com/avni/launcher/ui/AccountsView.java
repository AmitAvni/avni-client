package com.avni.launcher.ui;

import com.avni.launcher.model.Account;
import com.avni.launcher.model.LauncherConfig;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Manage offline accounts: add, select, remove. */
public class AccountsView extends VBox {

    private final LauncherConfig config = LauncherConfig.get();
    private final VBox list = new VBox(8);
    private final TextField nameField = new TextField();
    private final Runnable onMicrosoftLogin;

    public AccountsView(Runnable onMicrosoftLogin) {
        this.onMicrosoftLogin = onMicrosoftLogin;
        getStyleClass().add("page");
        setSpacing(14);

        Label title = new Label("Accounts");
        title.getStyleClass().add("page-title");

        Button microsoft = new Button("Sign in with Microsoft");
        microsoft.getStyleClass().add("accent-btn");
        microsoft.setOnAction(e -> onMicrosoftLogin.run());

        nameField.setPromptText("New offline username");
        nameField.getStyleClass().add("text-field-dark");
        nameField.setPrefWidth(220);
        nameField.setOnAction(e -> addAccount());

        Button add = new Button("Add offline");
        add.getStyleClass().add("ghost-btn");
        add.setOnAction(e -> addAccount());

        HBox addRow = new HBox(10, nameField, add);
        addRow.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(title, microsoft, addRow, scroll);
        refresh();
    }

    private void addAccount() {
        String name = nameField.getText().trim();
        if (!name.isEmpty()) {
            config.addOffline(name);
            nameField.clear();
            refresh();
        }
    }

    private void refresh() {
        list.getChildren().clear();
        if (config.accounts.isEmpty()) {
            Label empty = new Label("No accounts yet — add an offline username above.");
            empty.getStyleClass().add("tagline");
            list.getChildren().add(empty);
            return;
        }
        Account selected = config.selectedAccount();
        for (Account a : config.accounts) {
            boolean isSelected = selected != null && selected.uuid().equals(a.uuid());
            list.getChildren().add(accountCard(a, isSelected));
        }
    }

    private Node accountCard(Account account, boolean selected) {
        HBox card = new HBox(12);
        card.getStyleClass().add("account-card");
        if (selected) {
            card.getStyleClass().add("account-selected");
        }
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(2);
        Label name = new Label(account.name());
        name.getStyleClass().add("account-name");
        Label type = new Label(account.isMicrosoft() ? "Microsoft" : "Offline");
        type.getStyleClass().add("account-type");
        info.getChildren().addAll(name, type);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button select = new Button(selected ? "Selected" : "Select");
        select.getStyleClass().add("ghost-btn");
        select.setDisable(selected);
        select.setOnAction(e -> {
            config.select(account);
            refresh();
        });

        Button remove = new Button("✕");
        remove.getStyleClass().add("remove-btn");
        remove.setOnAction(e -> {
            config.remove(account);
            refresh();
        });

        card.getChildren().addAll(avatar(account.name()), info, spacer, select, remove);
        return card;
    }

    private StackPane avatar(String name) {
        StackPane p = new StackPane();
        p.setMinSize(38, 38);
        p.setMaxSize(38, 38);
        p.getStyleClass().add("avatar");
        int hue = Math.abs(name.hashCode()) % 360;
        p.setStyle("-fx-background-color: hsb(" + hue + ", 55%, 70%);");
        Label initial = new Label(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        initial.getStyleClass().add("avatar-initial");
        p.getChildren().add(initial);
        return p;
    }
}
