package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.world.World;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

/**
 * Tela interativa de Fim de Jogo (Obsidian Glass) exibida ao morrer,
 * oferecendo botões explícitos de "Jogar de Novo" e "Menu Principal".
 */
public class GameOverScreen extends StackPane {
    public GameOverScreen(ScreenManager manager, World world) {
        VBox panel = UiTheme.createGlassPanel(500, Region.USE_PREF_SIZE, 22, 28);
        panel.setSpacing(14);
        panel.setAlignment(Pos.TOP_CENTER);

        Rectangle accentBar = new Rectangle(260, 3.5);
        accentBar.setArcWidth(4);
        accentBar.setArcHeight(4);
        accentBar.setFill(Color.web(UiTheme.ACCENT_CRIMSON));

        StackPane skull = new StackPane(UiTrophyEmblem.createSkullMedallion(48, Color.web(UiTheme.ACCENT_CRIMSON)));

        Label title = new Label("INSTINTO INTERROMPIDO");
        title.setTextFill(Color.web("#F8FAFC"));
        UiTypography.bind(title, UiTypography.TypoRole.TITLE);

        String reasonText = (world.getGameOverReason() != null && !world.getGameOverReason().isBlank())
                ? world.getGameOverReason()
                : "Sua jornada na selva chegou ao fim.";
        Label reason = new Label(reasonText);
        reason.setTextFill(Color.web(UiTheme.TEXT_SECONDARY));
        reason.setWrapText(true);
        reason.setTextAlignment(TextAlignment.CENTER);
        reason.setMaxWidth(420);
        UiTypography.bind(reason, UiTypography.TypoRole.BODY);

        HBox metricsStrip = buildMetricsStrip(world);

        int apples = world.getApplesEaten();
        int kills = world.getEnemiesDefeated();
        String summaryText = String.format("%d %s   ·   %d %s",
                apples, apples == 1 ? "Fruta" : "Frutas",
                kills, kills == 1 ? "Abate" : "Abates");
        Label summary = new Label(summaryText);
        summary.setTextFill(Color.web("#CBD5E1"));
        UiTypography.bind(summary, UiTypography.TypoRole.LABEL);

        Button btnPlayAgain = UiTheme.createPrimaryButton("JOGAR DE NOVO", UiBadge.Icon.PLAY);
        btnPlayAgain.setMaxWidth(Double.MAX_VALUE);
        btnPlayAgain.setPrefHeight(44);
        btnPlayAgain.setOnAction(e -> manager.startGame());
        HBox.setHgrow(btnPlayAgain, Priority.ALWAYS);

        Button btnMainMenu = UiTheme.createSecondaryButton("MENU PRINCIPAL", UiBadge.Icon.DOOR);
        btnMainMenu.setMaxWidth(Double.MAX_VALUE);
        btnMainMenu.setPrefHeight(44);
        btnMainMenu.setOnAction(e -> manager.returnToMainMenu());
        HBox.setHgrow(btnMainMenu, Priority.ALWAYS);

        HBox actions = new HBox(12, btnPlayAgain, btnMainMenu);
        actions.setAlignment(Pos.CENTER);
        actions.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(actions, new Insets(4, 0, 0, 0));

        panel.getChildren().addAll(
                accentBar, skull, title, reason,
                UiTheme.createDivider(), metricsStrip, summary, actions
        );

        getChildren().add(UiTheme.createModalBackdrop(panel));
    }

    private HBox buildMetricsStrip(World world) {
        int score = world.getScore();
        int bestScore = world.getHighScoreManager().getBestScore();
        boolean isNewRecord = score > 0 && score >= bestScore;

        VBox scoreCol = buildMetricColumn(
                isNewRecord ? "NOVO RECORDE" : "PONTOS",
                String.valueOf(score),
                isNewRecord ? UiTheme.ACCENT_EMERALD : UiTheme.TEXT_SECONDARY,
                UiTheme.ACCENT_AMBER,
                false
        );
        VBox recordCol = buildMetricColumn(
                "RECORDE",
                String.valueOf(bestScore),
                UiTheme.TEXT_SECONDARY,
                "#F8FAFC",
                true
        );

        int totalSecs = Math.max(0, (int) world.getSurvivalTime());
        String timeVal = String.format("%02d:%02d · D%d", totalSecs / 60, totalSecs % 60, world.getDayCycleNumber());
        VBox timeCol = buildMetricColumn(
                "TEMPO",
                timeVal,
                UiTheme.TEXT_SECONDARY,
                UiTheme.ACCENT_SKY,
                false
        );

        HBox strip = new HBox(0, scoreCol, createVerticalDivider(), recordCol, createVerticalDivider(), timeCol);
        strip.setAlignment(Pos.CENTER);
        strip.setPadding(new Insets(12, 8, 12, 8));
        strip.setMaxWidth(Double.MAX_VALUE);
        strip.getStyleClass().add("glass-card");
        return strip;
    }

    private VBox buildMetricColumn(String labelText, String valueText, String labelHex, String valHex, boolean withCrown) {
        HBox labelRow = new HBox(5);
        labelRow.setAlignment(Pos.CENTER);
        if (withCrown) {
            labelRow.getChildren().add(UiBadge.create(UiBadge.Icon.STAR, 11, Color.web(UiTheme.ACCENT_AMBER)));
        }
        Label lbl = new Label(labelText);
        lbl.setTextFill(Color.web(labelHex));
        UiTypography.bind(lbl, UiTypography.TypoRole.MICRO);
        labelRow.getChildren().add(lbl);

        Label val = new Label(valueText);
        val.setTextFill(Color.web(valHex));
        UiTypography.bind(val, UiTypography.TypoRole.NUMERIC);

        VBox col = new VBox(4, labelRow, val);
        col.setAlignment(Pos.CENTER);
        col.setMinWidth(120);
        HBox.setHgrow(col, Priority.ALWAYS);
        return col;
    }

    private Region createVerticalDivider() {
        Region sep = new Region();
        sep.setMinWidth(1);
        sep.setPrefWidth(1);
        sep.setMaxWidth(1);
        sep.setPrefHeight(36);
        sep.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08);");
        return sep;
    }
}
