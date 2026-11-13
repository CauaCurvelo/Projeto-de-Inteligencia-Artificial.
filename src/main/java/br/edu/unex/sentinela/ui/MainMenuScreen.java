package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.Achievement;
import br.edu.unex.sentinela.progress.AchievementTracker;
import br.edu.unex.sentinela.progress.MatchHistoryRepository;
import br.edu.unex.sentinela.progress.PersonalRecords;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Menu Principal assimétrico sobre vidro fosco com atmosfera animada e tipografia dinâmica.
 */
public class MainMenuScreen extends StackPane {
    public MainMenuScreen(ScreenManager manager) {
        setPickOnBounds(true);
        setStyle("-fx-background-color: linear-gradient(to right, rgba(6,10,9,0.86) 0%, rgba(7,12,10,0.42) 55%, rgba(5,9,8,0.22) 100%);");

        Canvas fireflies = UiMotion.createFireflyBackdrop();
        Pane fireflyLayer = new Pane(fireflies);
        fireflyLayer.setMinSize(0, 0);
        fireflyLayer.setMouseTransparent(true);
        fireflies.widthProperty().bind(fireflyLayer.widthProperty());
        fireflies.heightProperty().bind(fireflyLayer.heightProperty());

        VBox leftRail = buildLeftRail(manager);
        HBox keyHints = buildBottomKeyHints();

        StackPane.setAlignment(leftRail, Pos.CENTER_LEFT);
        StackPane.setAlignment(keyHints, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(keyHints, new Insets(0, 28, 22, 0));

        getChildren().addAll(fireflyLayer, leftRail, keyHints);
        UiMotion.animateEntrance(leftRail);
    }

    private VBox buildLeftRail(ScreenManager manager) {
        VBox rail = new VBox(18);
        rail.setAlignment(Pos.CENTER_LEFT);
        rail.setPadding(new Insets(36, 48, 32, 44));
        UiTypography.bindScaledWidth(rail, 410);
        rail.prefHeightProperty().bind(heightProperty());
        rail.getStyleClass().add("glass-rail");

        VBox brandBlock = buildBrandBlock();
        VBox navList = buildNavigationList(manager);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        HBox statsStrip = buildCareerStatsStrip();

        rail.getChildren().addAll(brandBlock, navList, spacer, statsStrip);
        return rail;
    }

    private VBox buildBrandBlock() {
        Rectangle bar = new Rectangle(3, 14, Color.web(UiTheme.ACCENT_AMBER));
        bar.setArcWidth(3);
        bar.setArcHeight(3);

        Label eyebrow = new Label("INSTINTO SELVAGEM");
        eyebrow.setTextFill(Color.web(UiTheme.ACCENT_AMBER));
        UiTypography.bind(eyebrow, UiTypography.TypoRole.SUBHERO);

        HBox eyebrowRow = new HBox(8, bar, eyebrow);
        eyebrowRow.setAlignment(Pos.CENTER_LEFT);

        Label heroTitle = new Label("SENTINELA");
        heroTitle.setTextFill(Color.web(UiTheme.TEXT_PRIMARY));
        heroTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.72), 18, 0.25, 0, 4);");
        UiTypography.bind(heroTitle, UiTypography.TypoRole.HERO);

        VBox block = new VBox(4, eyebrowRow, heroTitle);
        block.setPadding(new Insets(8, 0, 8, 0));
        return block;
    }

    private VBox buildNavigationList(ScreenManager manager) {
        Button btnPlay = UiTheme.createPrimaryButton("JOGAR", UiBadge.Icon.PLAY);
        btnPlay.setMaxWidth(Double.MAX_VALUE);
        btnPlay.setPrefHeight(44);
        btnPlay.setOnAction(e -> manager.startGame());

        Button btnBestiary = navItem("BESTIÁRIO", UiBadge.Icon.BOOK, () -> manager.navigate(ScreenState.BESTIARY));
        Button btnAchievements = navItem("CONQUISTAS", UiBadge.Icon.TROPHY, () -> manager.navigate(ScreenState.ACHIEVEMENTS));
        Button btnHistory = navItem("HISTÓRICO", UiBadge.Icon.CHART, () -> manager.navigate(ScreenState.HISTORY));
        Button btnSettings = navItem("CONFIGURAÇÕES", UiBadge.Icon.GEAR, () -> manager.navigate(ScreenState.SETTINGS));

        Button btnExit = UiTheme.createDangerGhostButton("SAIR", UiBadge.Icon.DOOR);
        btnExit.setMaxWidth(Double.MAX_VALUE);
        btnExit.setPrefHeight(36);
        btnExit.setAlignment(Pos.CENTER_LEFT);
        btnExit.setPadding(new Insets(7, 16, 7, 16));
        btnExit.setOnAction(e -> Platform.exit());
        VBox.setMargin(btnExit, new Insets(8, 0, 0, 0));

        VBox nav = new VBox(8, btnPlay, btnBestiary, btnAchievements, btnHistory, btnSettings, btnExit);
        UiTypography.bindScaledWidth(nav, 272);
        return nav;
    }

    private Button navItem(String text, UiBadge.Icon icon, Runnable action) {
        Button btn = UiTheme.createNavButton(text, icon);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(38);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private HBox buildCareerStatsStrip() {
        PersonalRecords rec = MatchHistoryRepository.get().getPersonalRecords();
        int unlocked = AchievementTracker.get().getUnlockedCount();
        int total = Achievement.values().length;

        HBox scoreChip = UiTheme.createCompactStatChip(
                UiBadge.Icon.TROPHY, String.format("%,d", rec.getBestScore()).replace(',', '.'), UiTheme.ACCENT_AMBER);
        HBox achChip = UiTheme.createCompactStatChip(
                UiBadge.Icon.STAR, unlocked + "/" + total, UiTheme.ACCENT_EMERALD);
        HBox timeChip = UiTheme.createCompactStatChip(
                UiBadge.Icon.CLOCK, rec.getFormattedBestTime(), UiTheme.ACCENT_SKY);

        HBox row = new HBox(8, scoreChip, achChip, timeChip);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildBottomKeyHints() {
        HBox box = new HBox(16,
                UiTheme.createKeyHint("WASD", "Mover"),
                UiTheme.createKeyHint("SHIFT", "Correr"),
                UiTheme.createKeyHint("ESPAÇO", "Atacar"),
                UiTheme.createKeyHint("ESC", "Pausa")
        );
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(8, 14, 8, 14));
        box.getStyleClass().add("glass-card");
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return box;
    }
}
