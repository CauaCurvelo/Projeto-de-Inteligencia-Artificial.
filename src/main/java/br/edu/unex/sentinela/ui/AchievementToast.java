package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.Achievement;
import br.edu.unex.sentinela.ui.AchievementVisualCatalog.BadgeSpec;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
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
 * Usa medalhão vetorial exclusivo por conquista, acento categórico e barra de expiração animada.
 */
public class AchievementToast extends StackPane {
    private static final double SLIDE_OFFSET_X = 390.0;
    private static final double DISPLAY_SECONDS = 3.8;

    private final VBox cardShell = new VBox(0);
    private final Region leftAccentBar = new Region();
    private final StackPane medallionSlot = new StackPane();
    private final Circle headerDot = new Circle(3.2);
    private final Label headerLabel = new Label("CONQUISTA DESBLOQUEADA");
    private final Label categoryPill = new Label();
    private final Label titleLabel = new Label();
    private final Label descLabel = new Label();
    private final Rectangle timerTrack = new Rectangle(310, 2.5);
    private final Rectangle timerFill = new Rectangle(310, 2.5);
    private final Scale timerFillScale = new Scale(1.0, 1.0, 0.0, 0.0);

    private final Queue<Achievement> queue = new LinkedList<>();
    private boolean isDisplaying = false;

    public AchievementToast() {
        setPickOnBounds(false);
        setMouseTransparent(true);
        setAlignment(Pos.BOTTOM_RIGHT);
        setPadding(new Insets(0, 22, 22, 0));

        cardShell.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cardShell.setPrefWidth(332);
        cardShell.setOpacity(0.0);
        cardShell.setTranslateX(SLIDE_OFFSET_X);

        leftAccentBar.setMinWidth(4);
        leftAccentBar.setPrefWidth(4);
        leftAccentBar.setMaxWidth(4);

        medallionSlot.setMinSize(44, 44);
        medallionSlot.setPrefSize(44, 44);

        headerLabel.setTextFill(Color.web(UiTheme.TEXT_SECONDARY));
        UiTypography.bind(headerLabel, UiTypography.TypoRole.MICRO);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        categoryPill.setPadding(new Insets(1, 7, 1, 7));
        UiTypography.bind(categoryPill, UiTypography.TypoRole.MICRO);

        HBox topMeta = new HBox(6, headerDot, headerLabel, spacer, categoryPill);
        topMeta.setAlignment(Pos.CENTER_LEFT);

        titleLabel.setTextFill(Color.web("#F8FAFC"));
        UiTypography.bind(titleLabel, UiTypography.TypoRole.LABEL);

        descLabel.setTextFill(Color.web("#CBD5E1"));
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(246);
        UiTypography.bind(descLabel, UiTypography.TypoRole.MICRO);

        VBox textCol = new VBox(3, topMeta, titleLabel, descLabel);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        HBox bodyRow = new HBox(12, leftAccentBar, medallionSlot, textCol);
        bodyRow.setAlignment(Pos.CENTER_LEFT);
        bodyRow.setPadding(new Insets(11, 14, 10, 10));

        timerTrack.setFill(Color.web("#FFFFFF", 0.08));
        timerTrack.setArcWidth(2);
        timerTrack.setArcHeight(2);
        timerTrack.widthProperty().bind(cardShell.widthProperty().subtract(24));

        timerFill.setArcWidth(2);
        timerFill.setArcHeight(2);
        timerFill.widthProperty().bind(timerTrack.widthProperty());
        timerFill.getTransforms().add(timerFillScale);

        StackPane timerHolder = new StackPane(timerTrack, timerFill);
        timerHolder.setAlignment(Pos.CENTER_LEFT);
        timerHolder.setPadding(new Insets(0, 12, 6, 12));

        cardShell.getChildren().addAll(bodyRow, timerHolder);
        getChildren().add(cardShell);
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

    private void processQueue() {
        if (isDisplaying || queue.isEmpty()) return;
        isDisplaying = true;

        Achievement achievement = queue.poll();
        BadgeSpec spec = AchievementVisualCatalog.resolve(achievement);
        Color accent = Color.web(spec.getAccentHex());

        applyThemeForSpec(achievement, spec, accent);

        cardShell.setTranslateX(SLIDE_OFFSET_X);
        cardShell.setOpacity(0.0);
        medallionSlot.setScaleX(0.74);
        medallionSlot.setScaleY(0.74);
        timerFillScale.setX(1.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), cardShell);
        fadeIn.setToValue(1.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), cardShell);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition badgePop = new ScaleTransition(Duration.millis(340), medallionSlot);
        badgePop.setToX(1.0);
        badgePop.setToY(1.0);
        badgePop.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition enter = new ParallelTransition(fadeIn, slideIn, badgePop);

        Timeline countdown = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(timerFillScale.xProperty(), 1.0)),
            new KeyFrame(Duration.seconds(DISPLAY_SECONDS), new KeyValue(timerFillScale.xProperty(), 0.0, Interpolator.LINEAR))
        );

        FadeTransition fadeOut = new FadeTransition(Duration.millis(280), cardShell);
        fadeOut.setToValue(0.0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(280), cardShell);
        slideOut.setToX(SLIDE_OFFSET_X);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
        enter.setOnFinished(e -> countdown.play());
        countdown.setOnFinished(e -> exit.play());
        exit.setOnFinished(e -> {
            isDisplaying = false;
            processQueue();
        });
        enter.play();
    }

    private void applyThemeForSpec(Achievement a, BadgeSpec spec, Color accent) {
        String hex = spec.getAccentHex();
        String softRgba = UiTheme.toRgba(accent, 0.14);
        String borderRgba = UiTheme.toRgba(accent, 0.48);

        cardShell.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(16, 24, 21, 0.96), rgba(9, 13, 12, 0.96));"
            + "-fx-background-radius: 13;"
            + "-fx-border-color: " + borderRgba + ";"
            + "-fx-border-width: 1.1;"
            + "-fx-border-radius: 13;"
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.72), 20, 0.25, 0, 6);"
        );

        leftAccentBar.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 4;");
        medallionSlot.getChildren().setAll(UiTrophyEmblem.createMedallion(spec.getEmblem(), 44, accent, true));
        headerDot.setFill(accent);
        headerLabel.setTextFill(accent.deriveColor(0, 0.85, 1.12, 1.0));

        categoryPill.setText(a.getCategory().toUpperCase());
        categoryPill.setTextFill(Color.web(hex));
        categoryPill.setStyle(
            "-fx-background-color: " + softRgba + ";"
            + "-fx-background-radius: 6;"
            + "-fx-border-color: " + UiTheme.toRgba(accent, 0.30) + ";"
            + "-fx-border-radius: 6;"
            + "-fx-border-width: 1;"
        );

        titleLabel.setText(a.getTitle());
        descLabel.setText(a.getDescription());
        timerFill.setFill(accent.deriveColor(0, 1.0, 1.0, 0.85));
    }
}
