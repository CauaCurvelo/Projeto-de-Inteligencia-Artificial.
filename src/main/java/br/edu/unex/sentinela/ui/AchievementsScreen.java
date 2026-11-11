package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.Achievement;
import br.edu.unex.sentinela.progress.AchievementTracker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/**
 * Tela de Conquistas e Galeria de Troféus do Jogador.
 */
public class AchievementsScreen extends StackPane {

    public AchievementsScreen(ScreenManager manager) {
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: rgba(10, 16, 12, 0.88);");

        VBox panel = UiTheme.createStandardPanel(780, 540);

        Label title = UiTheme.createTitle("CONQUISTAS DA SELVA");
        Label subtitle = UiTheme.createSubtitle("Desafios de sobrevivência, bravura e maestria ecológica.");

        // Barra de progresso geral
        int total = Achievement.values().length;
        int unlockedCount = AchievementTracker.get().getUnlockedCount();
        double progress = total > 0 ? (double) unlockedCount / total : 0;

        Label progressLabel = new Label(String.format("Progresso: %d de %d Conquistas (%d%%)", unlockedCount, total, (int)(progress * 100)));
        progressLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        progressLabel.setTextFill(Color.web("#F1C40F"));

        ProgressBar bar = new ProgressBar(progress);
        bar.setPrefWidth(540);
        bar.setPrefHeight(12);
        bar.setStyle("-fx-accent: #F1C40F;");

        VBox progressBox = new VBox(6, progressLabel, bar);
        progressBox.setAlignment(Pos.CENTER);

        // Grade de Conquistas em ScrollPane
        FlowPane grid = new FlowPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        grid.setAlignment(Pos.TOP_CENTER);

        for (Achievement a : Achievement.values()) {
            grid.getChildren().add(buildAchievementCard(a));
        }

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);

        Button btnBack = UiTheme.createPrimaryButton("VOLTAR", null, 200, 38);
        btnBack.setOnAction(e -> manager.navigateBack());

        panel.getChildren().addAll(title, subtitle, progressBox, sp, btnBack);
        getChildren().add(panel);
    }

    private VBox buildAchievementCard(Achievement a) {
        boolean isUnlocked = AchievementTracker.get().isUnlocked(a);

        VBox card = new VBox(6);
        card.setPrefSize(232, 115);
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(isUnlocked ? UiTheme.CARD_UNLOCKED : UiTheme.CARD_LOCKED);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Ícone vetorial desenhado diretamente em canvas (imune a emojis quebrados)
        javafx.scene.canvas.Canvas icon = UiBadge.createBadge(
            isUnlocked ? "TROPHY" : "LOCK", 22,
            isUnlocked ? Color.web("#F1C40F") : Color.web("#7F8C8D")
        );

        VBox titleCol = new VBox(1);
        Label name = new Label(a.getTitle());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        name.setTextFill(isUnlocked ? Color.web("#F1C40F") : Color.web("#95A5A6"));

        Label cat = new Label(a.getCategory());
        cat.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 10));
        cat.setTextFill(isUnlocked ? Color.web("#2ECC71") : Color.web("#7F8C8D"));

        titleCol.getChildren().addAll(name, cat);
        topRow.getChildren().addAll(icon, titleCol);

        Label desc = new Label(a.getDescription());
        desc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        desc.setTextFill(isUnlocked ? Color.web("#EAECEE") : Color.web("#7F8C8D"));
        desc.setWrapText(true);
        VBox.setVgrow(desc, Priority.ALWAYS);

        Label status = new Label(isUnlocked ? "Conquistado em " + AchievementTracker.get().getUnlockDate(a) : "Desafio Bloqueado");
        status.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 10));
        status.setTextFill(isUnlocked ? Color.web("#F39C12") : Color.web("#566573"));

        card.getChildren().addAll(topRow, desc, status);
        return card;
    }
}
