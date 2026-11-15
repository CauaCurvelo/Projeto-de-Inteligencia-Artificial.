package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.Achievement;
import br.edu.unex.sentinela.ui.AchievementVisualCatalog.BadgeSpec;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Pop-up moderno de conquista desbloqueada exibido no canto inferior direito durante a partida.
 * Componente unmanaged de tamanho estrito que nunca distorce ou desloca o layout da janela.
 */
public class AchievementToast extends StackPane {
    public static final double CARD_WIDTH = 348.0;
    public static final double CARD_HEIGHT = 74.0;
    private static final double SLIDE_OFFSET_X = 390.0;
    private static final double DISPLAY_SECONDS = 3.8;

    private final VBox cardShell = new VBox(0);
    private final Region leftAccentBar = new Region();
    private final StackPane medallionSlot = new StackPane();
    private final Circle headerDot = new Circle(3.0);
    private final Label headerLabel = new Label("CONQUISTA DESBLOQUEADA");
    private final Label categoryPill = new Label();
    private final Label titleLabel = new Label();
    private final Label descLabel = new Label();
    private final Rectangle timerTrack = new Rectangle();
    private final Rectangle timerFill = new Rectangle();
    private final Scale timerFillScale = new Scale(1.0, 1.0, 0.0, 0.0);

    private final Queue<Achievement> queue = new LinkedList<>();
    private boolean isDisplaying = false;

    public AchievementToast() {
        setManaged(true);
        setPickOnBounds(false);
        setMouseTransparent(true);
        setVisible(false);

        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);

        cardShell.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        cardShell.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        cardShell.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        cardShell.setOpacity(0.0);
        cardShell.setTranslateX(SLIDE_OFFSET_X);

        leftAccentBar.setMinWidth(3.5);
        leftAccentBar.setPrefWidth(3.5);
        leftAccentBar.setMaxWidth(3.5);
        leftAccentBar.setMinHeight(42);
        leftAccentBar.setPrefHeight(42);
        leftAccentBar.setMaxHeight(42);

        medallionSlot.setMinSize(40, 40);
        medallionSlot.setPrefSize(40, 40);
        medallionSlot.setMaxSize(40, 40);

        headerLabel.setTextFill(Color.web(UiTheme.TEXT_SECONDARY));
        UiTypography.bind(headerLabel, UiTypography.TypoRole.MICRO);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        categoryPill.setPadding(new Insets(1, 6, 1, 6));
        UiTypography.bind(categoryPill, UiTypography.TypoRole.MICRO);

        HBox topMeta = new HBox(5, headerDot, headerLabel, spacer, categoryPill);
        topMeta.setAlignment(Pos.CENTER_LEFT);

        titleLabel.setTextFill(Color.web("#F8FAFC"));
        titleLabel.setMaxWidth(235);
        titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        UiTypography.bind(titleLabel, UiTypography.TypoRole.LABEL);

        descLabel.setTextFill(Color.web("#94A3B8"));
        descLabel.setMaxWidth(235);
        descLabel.setWrapText(false);
        descLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        UiTypography.bind(descLabel, UiTypography.TypoRole.MICRO);

        VBox textCol = new VBox(2, topMeta, titleLabel, descLabel);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        HBox bodyRow = new HBox(10, leftAccentBar, medallionSlot, textCol);
        bodyRow.setAlignment(Pos.CENTER_LEFT);
        bodyRow.setPadding(new Insets(8, 12, 6, 10));

        double timerWidth = CARD_WIDTH - 24.0;
        timerTrack.setWidth(timerWidth);
        timerTrack.setHeight(2.5);
        timerTrack.setFill(Color.web("#FFFFFF", 0.10));
        timerTrack.setArcWidth(2.5);
        timerTrack.setArcHeight(2.5);

        timerFill.setWidth(timerWidth);
        timerFill.setHeight(2.5);
        timerFill.setArcWidth(2.5);
        timerFill.setArcHeight(2.5);
        timerFill.getTransforms().setAll(timerFillScale);

        StackPane timerHolder = new StackPane(timerTrack, timerFill);
        timerHolder.setAlignment(Pos.CENTER_LEFT);
        timerHolder.setPadding(new Insets(0, 12, 6, 12));

        cardShell.getChildren().setAll(bodyRow, timerHolder);
        getChildren().setAll(cardShell);
    }

    public void showAchievement(Achievement achievement) {
        if (achievement == null) return;
        Runnable enqueueTask = () -> {
            queue.offer(achievement);
            processQueue();
        };
        if (Platform.isFxApplicationThread()) {
            enqueueTask.run();
        } else {
            Platform.runLater(enqueueTask);
        }
    }

    public boolean isDisplayingToast() {
        return isDisplaying || !queue.isEmpty();
    }

    private void processQueue() {
        if (isDisplaying || queue.isEmpty()) {
            if (!isDisplaying && queue.isEmpty()) {
                setVisible(false);
            }
            return;
        }
        isDisplaying = true;
        setVisible(true);

        Achievement achievement = queue.poll();
        BadgeSpec spec = AchievementVisualCatalog.resolve(achievement);
        Color accent = Color.web(spec.getAccentHex());

        applyThemeForSpec(achievement, spec, accent);

        cardShell.setTranslateX(SLIDE_OFFSET_X);
        cardShell.setOpacity(0.0);
        medallionSlot.setScaleX(0.72);
        medallionSlot.setScaleY(0.72);
        timerFillScale.setX(1.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(240), cardShell);
        fadeIn.setToValue(1.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), cardShell);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition badgePop = new ScaleTransition(Duration.millis(320), medallionSlot);
        badgePop.setToX(1.0);
        badgePop.setToY(1.0);
        badgePop.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition enter = new ParallelTransition(fadeIn, slideIn, badgePop);

        Timeline countdown = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(timerFillScale.xProperty(), 1.0)),
            new KeyFrame(Duration.seconds(DISPLAY_SECONDS), new KeyValue(timerFillScale.xProperty(), 0.0, Interpolator.LINEAR))
        );

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), cardShell);
        fadeOut.setToValue(0.0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(250), cardShell);
        slideOut.setToX(SLIDE_OFFSET_X);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
        enter.setOnFinished(e -> countdown.play());
        countdown.setOnFinished(e -> exit.play());
        exit.setOnFinished(e -> {
            isDisplaying = false;
            cardShell.setOpacity(0.0);
            cardShell.setTranslateX(SLIDE_OFFSET_X);
            processQueue();
        });
        enter.play();
    }

    private void applyThemeForSpec(Achievement a, BadgeSpec spec, Color accent) {
        String hex = spec.getAccentHex();
        String softRgba = UiTheme.toRgba(accent, 0.16);
        String borderRgba = UiTheme.toRgba(accent, 0.55);

        cardShell.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(14, 22, 19, 0.96) 0%, rgba(9, 14, 13, 0.97) 100%);"
            + "-fx-background-radius: 12;"
            + "-fx-border-color: " + borderRgba + ";"
            + "-fx-border-width: 1.2;"
            + "-fx-border-radius: 12;"
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 18, 0.25, 0, 5);"
        );

        leftAccentBar.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 2;");
        medallionSlot.getChildren().setAll(UiTrophyEmblem.createMedallion(spec.getEmblem(), 40, accent, true));
        headerDot.setFill(accent);
        headerLabel.setTextFill(accent.deriveColor(0, 0.85, 1.15, 1.0));

        categoryPill.setText(a.getCategory().toUpperCase());
        categoryPill.setTextFill(Color.web(hex));
        categoryPill.setStyle(
            "-fx-background-color: " + softRgba + ";"
            + "-fx-background-radius: 5;"
            + "-fx-border-color: " + UiTheme.toRgba(accent, 0.35) + ";"
            + "-fx-border-radius: 5;"
            + "-fx-border-width: 1;"
        );

        titleLabel.setText(a.getTitle());
        descLabel.setText(a.getDescription());
        timerFill.setFill(accent.deriveColor(0, 1.0, 1.0, 0.90));
    }
}
