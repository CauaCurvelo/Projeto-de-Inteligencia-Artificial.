package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.Achievement;
import br.edu.unex.sentinela.progress.AchievementTracker;
import br.edu.unex.sentinela.ui.AchievementVisualCatalog.BadgeSpec;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Galeria colecionável de conquistas com anel de progresso, filtros por categoria
 * e medalhões vetoriais temáticos exclusivos por conquista.
 */
public class AchievementsScreen extends StackPane {
    private enum FilterTab {
        ALL("Todas"),
        COMBAT("Combate"),
        SURVIVAL("Sobrevivência"),
        NUTRITION("Nutrição"),
        EXPLORATION("Exploração"),
        SCORE("Pontuação");

        final String label;
        FilterTab(String label) { this.label = label; }

        boolean matches(Achievement a) {
            if (this == ALL) return true;
            String cat = a.getCategory();
            if (this == EXPLORATION) {
                return "Exploração".equalsIgnoreCase(cat) || "Furtividade".equalsIgnoreCase(cat);
            }
            return label.equalsIgnoreCase(cat);
        }
    }

    private final GridPane grid = new GridPane();
    private final List<Button> filterButtons = new ArrayList<>();
    private final AchievementTracker tracker = AchievementTracker.get();
    private FilterTab activeFilter = FilterTab.ALL;

    public AchievementsScreen(ScreenManager manager) {
        VBox panel = UiTheme.createGlassPanel(860, 580, 22, 28);
        panel.setSpacing(14);

        int unlocked = tracker.getUnlockedCount();
        int total = Achievement.values().length;

        HBox progressHero = buildProgressHero(unlocked, total);
        VBox header = UiTheme.createHeader("CONQUISTAS", progressHero, manager::navigateBack);
        HBox filterBar = buildFilterBar();

        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 10, 14, 2));

        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col, col);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        refreshGrid();

        panel.getChildren().addAll(header, filterBar, scroll);
        getChildren().add(UiTheme.createModalBackdrop(panel));
    }

    private HBox buildProgressHero(int unlocked, int total) {
        Label count = new Label(unlocked + " / " + total);
        count.setTextFill(Color.web(UiTheme.ACCENT_AMBER));
        UiTypography.bind(count, UiTypography.TypoRole.NUMERIC);

        Label subtitle = new Label("DESBLOQUEADAS");
        subtitle.setTextFill(Color.web(UiTheme.TEXT_MUTED));
        UiTypography.bind(subtitle, UiTypography.TypoRole.MICRO);

        VBox textCol = new VBox(1, count, subtitle);
        textCol.setAlignment(Pos.CENTER_LEFT);

        HBox chip = new HBox(10, UiTrophyEmblem.createProgressRing(unlocked, total, 48), textCol);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(4, 14, 4, 8));
        chip.getStyleClass().add("glass-card");
        return chip;
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMaxWidth(Region.USE_PREF_SIZE);
        bar.getStyleClass().add("pill-switcher");

        for (FilterTab tab : FilterTab.values()) {
            Button btn = new Button(tab.label.toUpperCase(Locale.ROOT));
            UiTypography.bind(btn, UiTypography.TypoRole.MICRO);
            btn.setPadding(new Insets(6, 12, 6, 12));
            btn.getStyleClass().add(tab == activeFilter ? "pill-tab-active" : "pill-tab");
            btn.setOnAction(e -> {
                activeFilter = tab;
                updateFilterTabStyles(btn);
                refreshGrid();
            });
            UiMotion.attachPressFeedback(btn, true);
            filterButtons.add(btn);
            bar.getChildren().add(btn);
        }
        return bar;
    }

    private void updateFilterTabStyles(Button selectedBtn) {
        for (Button btn : filterButtons) {
            btn.getStyleClass().removeAll("pill-tab", "pill-tab-active");
            btn.getStyleClass().add(btn == selectedBtn ? "pill-tab-active" : "pill-tab");
        }
    }

    private void refreshGrid() {
        grid.getChildren().clear();
        int index = 0;
        for (Achievement a : Achievement.values()) {
            if (!activeFilter.matches(a)) continue;
            HBox card = createAchievementCard(a);
            GridPane.setHgrow(card, Priority.ALWAYS);
            grid.add(card, index % 2, index / 2);
            index++;
        }
    }

    private HBox createAchievementCard(Achievement a) {
        boolean isUnlocked = tracker.isUnlocked(a);
        BadgeSpec spec = AchievementVisualCatalog.resolve(a);
        Color accent = Color.web(spec.getAccentHex());

        Region accentStripe = new Region();
        accentStripe.setMinWidth(3.5);
        accentStripe.setPrefWidth(3.5);
        accentStripe.setMaxWidth(3.5);
        accentStripe.setStyle("-fx-background-color: "
                + (isUnlocked ? spec.getAccentHex() : "rgba(255,255,255,0.10)")
                + "; -fx-background-radius: 4;");

        StackPane medallion = new StackPane(UiTrophyEmblem.createMedallion(spec.getEmblem(), 46, accent, isUnlocked));
        medallion.setMinSize(46, 46);
        medallion.setPrefSize(46, 46);

        Label catPill = new Label(a.getCategory().toUpperCase(Locale.ROOT));
        catPill.setPadding(new Insets(1, 7, 1, 7));
        catPill.setTextFill(Color.web(isUnlocked ? spec.getAccentHex() : UiTheme.TEXT_MUTED));
        catPill.setStyle(isUnlocked
                ? "-fx-background-color: " + UiTheme.toRgba(accent, 0.14) + "; -fx-background-radius: 5;"
                : "-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 5;");
        UiTypography.bind(catPill, UiTypography.TypoRole.MICRO);

        Label statusLbl = new Label(isUnlocked ? tracker.getUnlockDate(a) : "BLOQUEADA");
        statusLbl.setTextFill(Color.web(isUnlocked ? UiTheme.TEXT_SECONDARY : UiTheme.TEXT_MUTED));
        UiTypography.bind(statusLbl, UiTypography.TypoRole.MICRO);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox metaRow = new HBox(8, catPill, spacer, statusLbl);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(a.getTitle());
        title.setTextFill(Color.web(isUnlocked ? "#F8FAFC" : UiTheme.TEXT_SECONDARY));
        UiTypography.bind(title, UiTypography.TypoRole.LABEL);

        Label desc = new Label(a.getDescription());
        desc.setWrapText(true);
        desc.setMinHeight(Region.USE_PREF_SIZE);
        desc.setTextFill(Color.web(isUnlocked ? "#CBD5E1" : UiTheme.TEXT_MUTED));
        UiTypography.bind(desc, UiTypography.TypoRole.MICRO);

        VBox textCol = new VBox(4, metaRow, title, desc);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        HBox card = new HBox(12, accentStripe, medallion, textCol);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 14, 12, 10));
        if (isUnlocked) {
            card.setStyle(
                "-fx-background-color: linear-gradient(to right, " + UiTheme.toRgba(accent, 0.10) + ", rgba(14, 20, 18, 0.90));"
                + "-fx-background-radius: 13;"
                + "-fx-border-color: " + UiTheme.toRgba(accent, 0.36) + ";"
                + "-fx-border-width: 1;"
                + "-fx-border-radius: 13;"
            );
        } else {
            card.getStyleClass().add("glass-card-locked");
        }
        return card;
    }
}
