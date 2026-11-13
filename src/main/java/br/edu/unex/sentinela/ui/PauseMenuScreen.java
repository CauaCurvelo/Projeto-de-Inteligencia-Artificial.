package br.edu.unex.sentinela.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Overlay compacto de Pausa acessível via ESC durante a partida.
 */
public class PauseMenuScreen extends StackPane {
    public PauseMenuScreen(ScreenManager manager) {
        VBox panel = UiTheme.createGlassPanel(340, Region.USE_PREF_SIZE, 26, 26);
        panel.setSpacing(14);
        panel.setAlignment(Pos.TOP_CENTER);

        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("PAUSA");
        title.setTextFill(Color.web(UiTheme.TEXT_PRIMARY));
        UiTypography.bind(title, UiTypography.TypoRole.TITLE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(title, spacer, UiTheme.createKeycapBadge("ESC"));

        Button btnResume = UiTheme.createPrimaryButton("CONTINUAR", UiBadge.Icon.PLAY);
        btnResume.setMaxWidth(Double.MAX_VALUE);
        btnResume.setPrefHeight(42);
        btnResume.setOnAction(e -> manager.resumeGame());

        Button btnSettings = UiTheme.createSecondaryButton("CONFIGURAÇÕES", UiBadge.Icon.GEAR);
        btnSettings.setMaxWidth(Double.MAX_VALUE);
        btnSettings.setPrefHeight(38);
        btnSettings.setOnAction(e -> manager.navigate(ScreenState.SETTINGS));

        Button btnRestart = UiTheme.createSecondaryButton("REINICIAR", UiBadge.Icon.CLOCK);
        btnRestart.setMaxWidth(Double.MAX_VALUE);
        btnRestart.setPrefHeight(38);
        btnRestart.setOnAction(e -> manager.restartGame());

        Button btnMainMenu = UiTheme.createDangerGhostButton("MENU PRINCIPAL", UiBadge.Icon.DOOR);
        btnMainMenu.setMaxWidth(Double.MAX_VALUE);
        btnMainMenu.setPrefHeight(36);
        btnMainMenu.setOnAction(e -> manager.returnToMainMenu());
        VBox.setMargin(btnMainMenu, new Insets(4, 0, 0, 0));

        HBox footer = new HBox(UiTheme.createKeyHint("ESC", "Retomar"));
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(4, 0, 0, 0));

        panel.getChildren().addAll(headerRow, UiTheme.createDivider(), btnResume, btnSettings, btnRestart, btnMainMenu, footer);
        getChildren().add(UiTheme.createModalBackdrop(panel));
    }
}
