package com.avni.launcher.ui;

import com.avni.launcher.model.Account;
import com.avni.launcher.model.LauncherConfig;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Manage accounts: sign in with Microsoft, select, remove. */
public class AccountsView extends VBox {

    private final LauncherConfig config = LauncherConfig.get();
    private final VBox list = new VBox(8);
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

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(title, microsoft, scroll);
        refresh();
    }

    private void refresh() {
        list.getChildren().clear();
        if (config.accounts.isEmpty()) {
            Label empty = new Label("No accounts yet — sign in with Microsoft above.");
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

        card.getChildren().addAll(Avatar.of(account, 38), info, spacer, select, remove);
        return card;
    }
}
