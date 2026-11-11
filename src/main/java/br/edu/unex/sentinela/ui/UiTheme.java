package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.audio.AudioEngine;
import br.edu.unex.sentinela.audio.SoundId;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;

/**
 * Utilitários e temas visuais estilo RPG Dark Fantasy / Sobrevivência Selvagem.
 */
public final class UiTheme {
    public static final String PANEL_MODERN = 
        "-fx-background-color: linear-gradient(to bottom, rgba(22, 32, 25, 0.96), rgba(12, 18, 14, 0.98));" +
        "-fx-background-radius: 12;" +
        "-fx-border-color: linear-gradient(to bottom, #F1C40F, #7D6608);" +
        "-fx-border-width: 2.0;" +
        "-fx-border-radius: 12;";

    public static final String CARD_MODERN = 
        "-fx-background-color: linear-gradient(to bottom, rgba(32, 44, 35, 0.85), rgba(18, 26, 20, 0.9));" +
        "-fx-background-radius: 8;" +
        "-fx-border-color: rgba(212, 172, 13, 0.4);" +
        "-fx-border-width: 1.2;" +
        "-fx-border-radius: 8;";

    public static final String CARD_UNLOCKED = 
        "-fx-background-color: linear-gradient(to bottom, rgba(36, 62, 42, 0.95), rgba(20, 38, 25, 0.95));" +
        "-fx-background-radius: 8;" +
        "-fx-border-color: #F1C40F;" +
        "-fx-border-width: 1.8;" +
        "-fx-border-radius: 8;";

    public static final String CARD_LOCKED = 
        "-fx-background-color: rgba(18, 24, 20, 0.6);" +
        "-fx-background-radius: 8;" +
        "-fx-border-color: rgba(90, 105, 95, 0.3);" +
        "-fx-border-width: 1.0;" +
        "-fx-border-radius: 8;";

    private UiTheme() {}

    public static void applyGlobalTheme(Scene scene) {
        if (scene == null) return;
        URL cssUrl = UiTheme.class.getResource("/ui/game_theme.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    public static Button createPrimaryButton(String text, double width, double height) {
        return createPrimaryButton(text, null, width, height);
    }

    public static Button createPrimaryButton(String text, String badgeType, double width, double height) {
        Button btn = new Button(text);
        btn.setPrefSize(width, height);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        btn.setAlignment(Pos.CENTER);

        if (badgeType != null && !badgeType.isEmpty()) {
            Canvas icon = UiBadge.createBadge(badgeType, 18, Color.rgb(20, 24, 20));
            btn.setGraphic(icon);
            btn.setGraphicTextGap(10);
        }

        String normal = 
            "-fx-background-color: linear-gradient(to bottom, #F4D03F, #B7950B);" +
            "-fx-text-fill: #141C16;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #FFF2A1 #7D6608 #7D6608 #FFF2A1;" +
            "-fx-border-width: 2.0;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;";

        String hover = 
            "-fx-background-color: linear-gradient(to bottom, #FFF, #F1C40F, #D4AC0D);" +
            "-fx-text-fill: #0A0E0B;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #FFFFFF;" +
            "-fx-border-width: 2.0;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;";

        btn.setStyle(normal);
        btn.setEffect(new DropShadow(12, Color.rgb(241, 196, 15, 0.4)));

        btn.setOnMouseEntered(e -> {
            btn.setStyle(hover);
            btn.setEffect(new DropShadow(16, Color.rgb(241, 196, 15, 0.7)));
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(normal);
            btn.setEffect(new DropShadow(12, Color.rgb(241, 196, 15, 0.4)));
        });

        btn.setOnAction(e -> AudioEngine.get().play(SoundId.UI_CLICK, 0.7, 0.0, 1.0));
        return btn;
    }

    public static Button createSecondaryButton(String text, double width, double height) {
        return createSecondaryButton(text, null, width, height);
    }

    public static Button createSecondaryButton(String text, String badgeType, double width, double height) {
        Button btn = new Button(text);
        btn.setPrefSize(width, height);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btn.setAlignment(Pos.CENTER);

        if (badgeType != null && !badgeType.isEmpty()) {
            Canvas icon = UiBadge.createBadge(badgeType, 16, Color.web("#F1C40F"));
            btn.setGraphic(icon);
            btn.setGraphicTextGap(10);
        }

        String normal = 
            "-fx-background-color: linear-gradient(to bottom, rgba(38, 52, 42, 0.85), rgba(20, 28, 22, 0.9));" +
            "-fx-text-fill: #EAECEE;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(212, 172, 13, 0.45);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;";

        String hover = 
            "-fx-background-color: linear-gradient(to bottom, rgba(58, 80, 64, 0.95), rgba(30, 42, 34, 0.95));" +
            "-fx-text-fill: #F1C40F;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #F1C40F;" +
            "-fx-border-width: 1.8;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;";

        btn.setStyle(normal);
        btn.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.6)));

        btn.setOnMouseEntered(e -> {
            btn.setStyle(hover);
            btn.setEffect(new DropShadow(12, Color.rgb(241, 196, 15, 0.4)));
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(normal);
            btn.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.6)));
        });

        btn.setOnAction(e -> AudioEngine.get().play(SoundId.UI_CLICK, 0.5, 0.0, 1.0));
        return btn;
    }

    public static Label createTitle(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 26));
        lbl.setTextFill(Color.web("#F1C40F"));
        lbl.setEffect(new DropShadow(10, Color.rgb(241, 196, 15, 0.5)));
        return lbl;
    }

    public static Label createSubtitle(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        lbl.setTextFill(Color.web("#A6ACAF"));
        return lbl;
    }

    public static VBox createStandardPanel(double width, double height) {
        VBox box = new VBox(14);
        box.setMaxSize(width, height);
        box.setPrefSize(width, height);
        box.setStyle(PANEL_MODERN);
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.TOP_CENTER);
        box.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.9)));
        return box;
    }
}
