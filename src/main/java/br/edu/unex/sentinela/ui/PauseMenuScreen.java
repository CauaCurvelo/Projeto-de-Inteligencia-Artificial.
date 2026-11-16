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
 * Overlay compacto de Pausa acessível via ESC durante a partida,
 * agora com acesso direto ao Bestiário e à Galeria de Conquistas.
 */
public class PauseMenuScreen extends StackPane {
    public PauseMenuScreen(ScreenManager manager) {
        VBox panel = UiTheme.createGlassPanel(350, Region.USE_PREF_SIZE, 22, 24);
        panel.setSpacing(9);
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
        btnResume.setPrefHeight(40);
        btnResume.setOnAction(e -> manager.resumeGame());

        Button btnBestiary = UiTheme.createSecondaryButton("BESTIÁRIO", UiBadge.Icon.BOOK);
        btnBestiary.setMaxWidth(Double.MAX_VALUE);
        btnBestiary.setPrefHeight(36);
        btnBestiary.setOnAction(e -> manager.navigate(ScreenState.BESTIARY));

        Button btnAchievements = UiTheme.createSecondaryButton("CONQUISTAS", UiBadge.Icon.TROPHY);
        btnAchievements.setMaxWidth(Double.MAX_VALUE);
        btnAchievements.setPrefHeight(36);
        btnAchievements.setOnAction(e -> manager.navigate(ScreenState.ACHIEVEMENTS));

        Button btnSettings = UiTheme.createSecondaryButton("CONFIGURAÇÕES", UiBadge.Icon.GEAR);
        btnSettings.setMaxWidth(Double.MAX_VALUE);
        btnSettings.setPrefHeight(36);
        btnSettings.setOnAction(e -> manager.navigate(ScreenState.SETTINGS));

        Button btnRestart = UiTheme.createSecondaryButton("REINICIAR", UiBadge.Icon.CLOCK);
        btnRestart.setMaxWidth(Double.MAX_VALUE);
        btnRestart.setPrefHeight(36);
        btnRestart.setOnAction(e -> manager.restartGame());

        Button btnMainMenu = UiTheme.createDangerGhostButton("MENU PRINCIPAL", UiBadge.Icon.DOOR);
        btnMainMenu.setMaxWidth(Double.MAX_VALUE);
        btnMainMenu.setPrefHeight(34);
        btnMainMenu.setOnAction(e -> manager.returnToMainMenu());
        VBox.setMargin(btnMainMenu, new Insets(3, 0, 0, 0));

        HBox footer = new HBox(UiTheme.createKeyHint("ESC", "Retomar"));
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(3, 0, 0, 0));

        panel.getChildren().addAll(
            headerRow,
            UiTheme.createDivider(),
            btnResume,
            btnBestiary,
            btnAchievements,
            btnSettings,
            btnRestart,
            btnMainMenu,
            footer
        );
        getChildren().add(UiTheme.createModalBackdrop(panel));
    }
}
