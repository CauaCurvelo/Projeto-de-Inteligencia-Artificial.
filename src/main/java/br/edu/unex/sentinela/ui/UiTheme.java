package br.edu.unex.sentinela.ui;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Design System moderno (Botanical Obsidian Glass + Warm Amber).
 * Fornece tokens cromáticos e fábricas enxutas com fontes dinâmicas via UiTypography.
 */
public final class UiTheme {
    public static final String ACCENT_AMBER = "#E8B948";
    public static final String ACCENT_EMERALD = "#34D399";
    public static final String ACCENT_CRIMSON = "#F87171";
    public static final String ACCENT_SKY = "#38BDF8";
    public static final String TEXT_PRIMARY = "#F1F5F9";
    public static final String TEXT_SECONDARY = "#94A3B8";
    public static final String TEXT_MUTED = "#64748B";

    private static final String THEME_CSS_PATH = "/ui/game_theme.css";

    private UiTheme() {}

    public static void applyGlobalTheme(Scene scene) {
        if (scene == null) return;
        UiTypography.attachToScene(scene);
        try {
            var url = UiTheme.class.getResource(THEME_CSS_PATH);
            if (url != null) {
                String cssUrl = url.toExternalForm();
                if (!scene.getStylesheets().contains(cssUrl)) {
                    scene.getStylesheets().add(cssUrl);
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Painel central Obsidian Glass com entrada animada e borda hairline sutil.
     */
    public static VBox createGlassPanel(double maxWidth, double maxHeight, double paddingV, double paddingH) {
        VBox panel = new VBox(16);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPadding(new Insets(paddingV, paddingH, paddingV, paddingH));
        panel.getStyleClass().add("glass-panel");
        UiTypography.bindScaledPanel(panel, maxWidth, maxHeight);
        UiMotion.animateEntrance(panel);
        return panel;
    }

    /**
     * Backdrop escuro sutil atrás de menus modais.
     */
    public static StackPane createModalBackdrop(Node content) {
        StackPane backdrop = new StackPane(content);
        backdrop.setAlignment(Pos.CENTER);
        backdrop.setPadding(new Insets(24));
        backdrop.setStyle("-fx-background-color: rgba(5, 8, 7, 0.72);");
        return backdrop;
    }

    /**
     * Cabeçalho minimalista padrão: Título dinâmico + chip opcional + botão Voltar discreto.
     */
    public static VBox createHeader(String titleText, Node rightAccessory, Runnable onBack) {
        HBox topRow = new HBox(14);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(titleText);
        title.setTextFill(Color.web(TEXT_PRIMARY));
        UiTypography.bind(title, UiTypography.TypoRole.TITLE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(title, spacer);
        if (rightAccessory != null) {
            topRow.getChildren().add(rightAccessory);
        }
        if (onBack != null) {
            Button back = createGhostButton("← VOLTAR");
            back.setOnAction(e -> onBack.run());
            topRow.getChildren().add(back);
        }

        Region divider = new Region();
        divider.getStyleClass().add("divider-line");

        VBox header = new VBox(12, topRow, divider);
        header.setMaxWidth(Double.MAX_VALUE);
        return header;
    }

    public static Button createPrimaryButton(String text, UiBadge.Icon icon) {
        Button btn = new Button(text);
        btn.getStyleClass().add("primary-cta-btn");
        UiTypography.bind(btn, UiTypography.TypoRole.ACTION);
        if (icon != null) {
            btn.setGraphic(UiBadge.create(icon, 14, Color.web("#0B0F0D")));
            btn.setGraphicTextGap(10);
        }
        UiMotion.attachPressFeedback(btn, true);
        return btn;
    }

    public static Button createNavButton(String text, UiBadge.Icon icon) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-rail-btn");
        UiTypography.bind(btn, UiTypography.TypoRole.ACTION);
        if (icon != null) {
            btn.setGraphic(UiBadge.create(icon, 14, Color.web(ACCENT_AMBER)));
            btn.setGraphicTextGap(12);
        }
        UiMotion.attachHoverSlide(btn, 6);
        UiMotion.attachPressFeedback(btn, true);
        return btn;
    }

    public static Button createSecondaryButton(String text, UiBadge.Icon icon) {
        Button btn = new Button(text);
        btn.getStyleClass().add("secondary-btn");
        UiTypography.bind(btn, UiTypography.TypoRole.ACTION);
        if (icon != null) {
            btn.setGraphic(UiBadge.create(icon, 13, Color.web(ACCENT_AMBER)));
            btn.setGraphicTextGap(10);
        }
        UiMotion.attachPressFeedback(btn, true);
        return btn;
    }

    public static Button createGhostButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("ghost-btn");
        UiTypography.bind(btn, UiTypography.TypoRole.LABEL);
        UiMotion.attachPressFeedback(btn, true);
        return btn;
    }

    public static Button createDangerGhostButton(String text, UiBadge.Icon icon) {
        Button btn = new Button(text);
        btn.getStyleClass().add("danger-ghost-btn");
        UiTypography.bind(btn, UiTypography.TypoRole.ACTION);
        if (icon != null) {
            btn.setGraphic(UiBadge.create(icon, 13, Color.web(TEXT_SECONDARY)));
            btn.setGraphicTextGap(10);
        }
        UiMotion.attachPressFeedback(btn, true);
        return btn;
    }

    public static Label createKeycapBadge(String keyText) {
        Label lbl = new Label(keyText);
        lbl.getStyleClass().add("keycap-badge");
        UiTypography.bind(lbl, UiTypography.TypoRole.KEYCAP);
        return lbl;
    }

    public static HBox createKeyHint(String keyText, String actionText) {
        Label action = new Label(actionText);
        action.setTextFill(Color.web(TEXT_SECONDARY));
        UiTypography.bind(action, UiTypography.TypoRole.MICRO);
        HBox box = new HBox(6, createKeycapBadge(keyText), action);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static HBox createCompactStatChip(UiBadge.Icon icon, String value, String accentHex) {
        Label val = new Label(value);
        val.setTextFill(Color.web(TEXT_PRIMARY));
        UiTypography.bind(val, UiTypography.TypoRole.NUMERIC);

        HBox chip = new HBox(7, UiBadge.create(icon, 13, Color.web(accentHex)), val);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(5, 11, 5, 10));
        chip.getStyleClass().add("glass-card");
        return chip;
    }

    /**
     * Toggle Switch moderno (cápsula deslizante animada) em vez de checkboxes antigos.
     */
    public static ToggleButton createToggleSwitch(boolean initial, Consumer<Boolean> onToggle) {
        ToggleButton toggle = new ToggleButton();
        toggle.setSelected(initial);
        toggle.setMinSize(42, 22);
        toggle.setPrefSize(42, 22);
        toggle.setMaxSize(42, 22);
        toggle.setPadding(Insets.EMPTY);
        toggle.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;");

        Rectangle track = new Rectangle(42, 22);
        track.setArcWidth(22);
        track.setArcHeight(22);

        Circle thumb = new Circle(8, Color.web("#F8FAFC"));
        StackPane visual = new StackPane(track, thumb);
        visual.setAlignment(Pos.CENTER_LEFT);
        thumb.setTranslateX(initial ? 23 : 3);
        updateSwitchColors(track, initial);

        toggle.setGraphic(visual);
        toggle.selectedProperty().addListener((obs, oldV, selected) -> {
            updateSwitchColors(track, selected);
            TranslateTransition tt = new TranslateTransition(Duration.millis(120), thumb);
            tt.setToX(selected ? 23 : 3);
            tt.play();
            if (onToggle != null) onToggle.accept(selected);
        });
        UiMotion.attachPressFeedback(toggle, true);
        return toggle;
    }

    private static void updateSwitchColors(Rectangle track, boolean selected) {
        track.setFill(selected ? Color.web(ACCENT_EMERALD, 0.85) : Color.web("#FFFFFF", 0.14));
        track.setStroke(selected ? Color.web(ACCENT_EMERALD) : Color.web("#FFFFFF", 0.18));
        track.setStrokeWidth(1.0);
    }

    public static Region createDivider() {
        Region line = new Region();
        line.getStyleClass().add("divider-line");
        return line;
    }

    public static String toRgba(Color c, double alpha) {
        return String.format(java.util.Locale.ROOT, "rgba(%d,%d,%d,%.2f)",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255),
                alpha);
    }
}
