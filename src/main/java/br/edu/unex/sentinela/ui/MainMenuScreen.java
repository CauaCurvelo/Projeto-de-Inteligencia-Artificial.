package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.AchievementTracker;
import br.edu.unex.sentinela.progress.MatchHistoryRepository;
import br.edu.unex.sentinela.progress.PersonalRecords;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Menu Principal estilizado em Dark Fantasy e estética selvagem de jogo de aventura,
 * com ícones vetoriais imunes a quebras de fonte e resumo de carreira em tempo real.
 */
public class MainMenuScreen extends StackPane {

    public MainMenuScreen(ScreenManager manager) {
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: radial-gradient(center 50% 45%, radius 65%, rgba(14, 22, 16, 0.82), rgba(6, 10, 8, 0.96));");

        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMaxWidth(680);

        // Brasão de Título
        VBox titleBox = new VBox(6);
        titleBox.setAlignment(Pos.CENTER);

        Label title = new Label("INSTINTO SELVAGEM");
        title.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 48));
        title.setTextFill(Color.web("#F1C40F"));
        title.setEffect(new DropShadow(24, Color.rgb(241, 196, 15, 0.65)));

        Label subtitle = new Label("—  SENTINELA DA FLORESTA  •  SOBREVIVÊNCIA & FURTIVIDADE  —");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        subtitle.setTextFill(Color.web("#BDC3C7"));
        subtitle.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.9)));

        titleBox.getChildren().addAll(title, subtitle);

        // Menu com Botões Chanfrados e Ícones Vetoriais Garantidos
        Button btnPlay = UiTheme.createPrimaryButton("INICIAR JOGO", "PLAY", 300, 48);
        btnPlay.setOnAction(e -> manager.startGame());

        Button btnBestiary = UiTheme.createSecondaryButton("BESTIÁRIO & FAUNA", "BOOK", 300, 42);
        btnBestiary.setOnAction(e -> manager.navigate(ScreenState.BESTIARY));

        Button btnAchievements = UiTheme.createSecondaryButton("CONQUISTAS", "TROPHY", 300, 42);
        btnAchievements.setOnAction(e -> manager.navigate(ScreenState.ACHIEVEMENTS));

        Button btnHistory = UiTheme.createSecondaryButton("HISTÓRICO & RECORDES", "CHART", 300, 42);
        btnHistory.setOnAction(e -> manager.navigate(ScreenState.HISTORY));

        Button btnSettings = UiTheme.createSecondaryButton("CONFIGURAÇÕES", "GEAR", 300, 42);
        btnSettings.setOnAction(e -> manager.navigate(ScreenState.SETTINGS));

        Button btnExit = UiTheme.createSecondaryButton("SAIR DO JOGO", "DOOR", 300, 38);
        btnExit.setOnAction(e -> Platform.exit());

        VBox btnBox = new VBox(10, btnPlay, btnBestiary, btnAchievements, btnHistory, btnSettings, btnExit);
        btnBox.setAlignment(Pos.CENTER);

        // Resumo de Carreira Rápido no rodapé do menu
        HBox summaryBadge = buildQuickStatsBadge();

        Label footer = new Label("Controles: WASD para Mover  •  Shift Correr  •  Espaço Atacar  •  ESC Pausa");
        footer.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        footer.setTextFill(Color.web("#7F8C8D"));

        contentBox.getChildren().addAll(titleBox, btnBox, summaryBadge, footer);
        getChildren().add(contentBox);
    }

    private HBox buildQuickStatsBadge() {
        HBox badge = new HBox(20);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(8, 18, 8, 18));
        badge.setMaxWidth(480);
        badge.setStyle(UiTheme.CARD_MODERN);

        PersonalRecords pr = MatchHistoryRepository.get().getPersonalRecords();
        int achievementsCount = AchievementTracker.get().getUnlockedCount();

        Label l1 = new Label("Recorde: " + String.format("%,d", pr.getBestScore()) + " pts");
        l1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        l1.setTextFill(Color.web("#F1C40F"));

        Label l2 = new Label("Conquistas: " + achievementsCount + " / 14");
        l2.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        l2.setTextFill(Color.web("#2ECC71"));

        Label l3 = new Label("Sobrevivência: " + pr.getFormattedBestTime());
        l3.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        l3.setTextFill(Color.web("#3498DB"));

        badge.getChildren().addAll(l1, l2, l3);
        return badge;
    }
}
