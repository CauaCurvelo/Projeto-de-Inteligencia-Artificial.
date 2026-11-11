package br.edu.unex.sentinela.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Menu de Pausa exibido quando o jogador pressiona ESC durante a gameplay.
 */
public class PauseMenuScreen extends StackPane {

    public PauseMenuScreen(ScreenManager manager) {
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: rgba(6, 10, 8, 0.78);");

        VBox panel = UiTheme.createStandardPanel(320, 360);
        panel.setAlignment(Pos.CENTER);

        Label title = UiTheme.createTitle("JOGO PAUSADO");
        Label subtitle = UiTheme.createSubtitle("O mundo natural aguarda suas decisões.");

        Button btnResume = UiTheme.createPrimaryButton("RETOMAR", "PLAY", 250, 42);
        btnResume.setOnAction(e -> manager.resumeGame());

        Button btnSettings = UiTheme.createSecondaryButton("CONFIGURAÇÕES", "GEAR", 250, 40);
        btnSettings.setOnAction(e -> manager.navigate(ScreenState.SETTINGS));

        Button btnRestart = UiTheme.createSecondaryButton("REINICIAR PARTIDA", "CLOCK", 250, 40);
        btnRestart.setOnAction(e -> manager.restartGame());

        Button btnMainMenu = UiTheme.createSecondaryButton("MENU PRINCIPAL", "DOOR", 250, 40);
        btnMainMenu.setOnAction(e -> manager.returnToMainMenu());

        VBox buttons = new VBox(10, btnResume, btnSettings, btnRestart, btnMainMenu);
        buttons.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(title, subtitle, buttons);
        getChildren().add(panel);
    }
}
