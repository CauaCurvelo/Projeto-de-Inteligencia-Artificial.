package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.Achievement;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Notificação de conquista (Toast) elegante em Dark Glassmorphism e ouro antigo,
 * com insígnia vetorial imune a quebras de fonte e animação suave com interpolação de física.
 */
public class AchievementToast extends StackPane {
    private final Queue<Achievement> queue = new ArrayDeque<>();
    private boolean isDisplaying = false;

    private final HBox toastBox;
    private final Canvas iconCanvas;
    private final Label titleLabel;
    private final Label descLabel;

    public AchievementToast() {
        setAlignment(Pos.TOP_RIGHT);
        setPadding(new Insets(20, 24, 0, 0));
        setMouseTransparent(true);
        setPickOnBounds(false);

        toastBox = new HBox(14);
        toastBox.setAlignment(Pos.CENTER_LEFT);
        toastBox.setPrefWidth(360);
        toastBox.setMaxWidth(360);
        toastBox.setPadding(new Insets(10, 16, 10, 14));
        toastBox.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(28, 40, 32, 0.98), rgba(16, 24, 18, 0.95));" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: linear-gradient(to right, #F1C40F, #9A7D0A);" +
            "-fx-border-width: 1.8;" +
            "-fx-border-radius: 10;"
        );

        DropShadow glow = new DropShadow(20, Color.rgb(241, 196, 15, 0.5));
        toastBox.setEffect(glow);

        // Insígnia vetorial de ouro desenhada em canvas (sem emojis quebrados)
        iconCanvas = new Canvas(42, 42);
        drawTrophyMedallion(iconCanvas.getGraphicsContext2D(), 42);

        VBox textCol = new VBox(2);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label headerLabel = new Label("CONQUISTA DESBLOQUEADA");
        headerLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 10));
        headerLabel.setTextFill(Color.web("#F1C40F"));
        headerLabel.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.8)));

        titleLabel = new Label("Título da Conquista");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#FFFFFF"));

        descLabel = new Label("Descrição da conquista");
        descLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        descLabel.setTextFill(Color.web("#BDC3C7"));
        descLabel.setWrapText(true);

        textCol.getChildren().addAll(headerLabel, titleLabel, descLabel);
        toastBox.getChildren().addAll(iconCanvas, textCol);

        toastBox.setOpacity(0);
        toastBox.setTranslateX(420); // Garante que começa 100% fora da tela

        getChildren().add(toastBox);
    }

    private void drawTrophyMedallion(GraphicsContext gc, double s) {
        gc.clearRect(0, 0, s, s);

        // Fundo do medalhão dourado com gradiente radial
        RadialGradient medalBg = new RadialGradient(
            0, 0, s * 0.4, s * 0.4, s * 0.5, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#FFF2A1")),
            new Stop(0.6, Color.web("#F1C40F")),
            new Stop(1.0, Color.web("#9A7D0A"))
        );
        gc.setFill(medalBg);
        gc.fillOval(2, 2, s - 4, s - 4);

        // Borda dupla em latão escuro
        gc.setStroke(Color.web("#5B4706"));
        gc.setLineWidth(1.8);
        gc.strokeOval(2, 2, s - 4, s - 4);
        gc.strokeOval(5, 5, s - 10, s - 10);

        // Taça de troféu escura gravada no centro
        gc.setFill(Color.web("#221A04"));
        gc.fillRoundRect(s * 0.3, s * 0.25, s * 0.4, s * 0.35, 3, 3);
        gc.fillRect(s * 0.44, s * 0.58, s * 0.12, s * 0.16);
        gc.fillRoundRect(s * 0.32, s * 0.72, s * 0.36, s * 0.1, 2, 2);
    }

    public void showAchievement(Achievement achievement) {
        if (achievement == null) return;
        Platform.runLater(() -> {
            queue.offer(achievement);
            processQueue();
        });
    }

    private void processQueue() {
        if (isDisplaying || queue.isEmpty()) return;
        isDisplaying = true;

        Achievement a = queue.poll();
        titleLabel.setText(a.getTitle());
        descLabel.setText(a.getDescription());

        toastBox.setTranslateX(420);
        toastBox.setOpacity(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), toastBox);
        fadeIn.setToValue(1.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(380), toastBox);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition enter = new ParallelTransition(fadeIn, slideIn);

        PauseTransition pause = new PauseTransition(Duration.millis(4500));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toastBox);
        fadeOut.setToValue(0.0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), toastBox);
        slideOut.setToX(420);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);

        SequentialTransition sequence = new SequentialTransition(enter, pause, exit);
        sequence.setOnFinished(e -> {
            isDisplaying = false;
            processQueue();
        });
        sequence.play();
    }
}
