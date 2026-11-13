package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.audio.AudioChannel;
import br.edu.unex.sentinela.audio.AudioEngine;
import br.edu.unex.sentinela.settings.GameAction;
import br.edu.unex.sentinela.settings.GameSettings;
import br.edu.unex.sentinela.settings.GraphicsQuality;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
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
import java.util.function.Consumer;

/**
 * Tela de Configurações modular com switcher segmentado (VÍDEO | TECLAS | ÁUDIO),
 * toggle switches modernos e rótulos enxutos.
 */
public class SettingsScreen extends StackPane {
    private final StackPane tabContent = new StackPane();
    private final List<Button> pillButtons = new ArrayList<>();
    private final List<KeyRebindButton> rebindButtons = new ArrayList<>();

    public SettingsScreen(ScreenManager manager) {
        VBox panel = UiTheme.createGlassPanel(740, 505, 22, 28);
        panel.setSpacing(14);

        VBox header = UiTheme.createHeader("CONFIGURAÇÕES", null, manager::navigateBack);

        Node videoPane = buildGraphicsPane();
        Node controlsPane = buildControlsPane();
        Node audioPane = buildAudioPane();

        HBox switcher = new HBox(4);
        switcher.getStyleClass().add("pill-switcher");
        switcher.setMaxWidth(Region.USE_PREF_SIZE);

        Button tabVideo = createPillTab("VÍDEO", videoPane);
        Button tabControls = createPillTab("TECLAS", controlsPane);
        Button tabAudio = createPillTab("ÁUDIO", audioPane);
        switcher.getChildren().addAll(tabVideo, tabControls, tabAudio);

        VBox.setVgrow(tabContent, Priority.ALWAYS);
        selectTab(tabVideo, videoPane);

        panel.getChildren().addAll(header, switcher, tabContent);
        getChildren().add(UiTheme.createModalBackdrop(panel));
    }

    private Button createPillTab(String label, Node pane) {
        Button btn = new Button(label);
        btn.getStyleClass().add("pill-tab");
        UiTypography.bind(btn, UiTypography.TypoRole.ACTION);
        btn.setOnAction(e -> selectTab(btn, pane));
        UiMotion.attachPressFeedback(btn, true);
        pillButtons.add(btn);
        return btn;
    }

    private void selectTab(Button activeBtn, Node content) {
        for (Button b : pillButtons) {
            b.getStyleClass().remove("pill-tab-active");
            if (!b.getStyleClass().contains("pill-tab")) {
                b.getStyleClass().add("pill-tab");
            }
        }
        activeBtn.getStyleClass().remove("pill-tab");
        activeBtn.getStyleClass().add("pill-tab-active");
        tabContent.getChildren().setAll(content);
        UiMotion.animateEntrance(content);
    }

    private Node buildGraphicsPane() {
        GameSettings s = GameSettings.get();

        ComboBox<GraphicsQuality> cbQuality = new ComboBox<>();
        cbQuality.getItems().addAll(GraphicsQuality.values());
        cbQuality.setValue(s.getGraphicsQuality());
        cbQuality.setPrefWidth(160);
        UiTypography.bindComboBox(cbQuality, UiTypography.TypoRole.LABEL, SettingsScreen::qualityShortName);
        cbQuality.setOnAction(e -> s.setGraphicsQuality(cbQuality.getValue()));

        ComboBox<Integer> cbFps = new ComboBox<>();
        cbFps.getItems().addAll(30, 60, 120, 0);
        cbFps.setValue(s.getTargetFps());
        cbFps.setPrefWidth(160);
        UiTypography.bindComboBox(cbFps, UiTypography.TypoRole.LABEL, fps -> fps == 0 ? "Ilimitado" : fps + " FPS");
        cbFps.setOnAction(e -> s.setTargetFps(cbFps.getValue()));

        VBox performanceCard = createSectionCard(
                settingRow("Qualidade Visual", cbQuality),
                settingRow("Limite de FPS", cbFps)
        );

        VBox visualCard = createSectionCard(
                switchRow("Cones de Visão", s.isShowVisionCones(), s::setShowVisionCones),
                switchRow("Trilhas no Solo", s.isShowPathTrails(), s::setShowPathTrails),
                switchRow("Barras de Vida", s.isShowNameplates(), s::setShowNameplates),
                switchRow("Tremor de Tela", s.isScreenShake(), s::setScreenShake)
        );

        VBox layout = new VBox(12, performanceCard, visualCard);
        layout.setPadding(new Insets(4, 0, 0, 0));
        return layout;
    }

    private Node buildControlsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);

        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col, col);

        GameAction[] actions = GameAction.values();
        int half = (actions.length + 1) / 2;
        for (int i = 0; i < actions.length; i++) {
            GameAction act = actions[i];
            KeyRebindButton btn = new KeyRebindButton(act, this::refreshAllKeyButtons);
            rebindButtons.add(btn);

            HBox row = settingRow(act.getDisplayName(), btn);
            row.getStyleClass().add("glass-card");
            row.setPadding(new Insets(8, 12, 8, 14));
            GridPane.setHgrow(row, Priority.ALWAYS);

            grid.add(row, i / half, i % half);
        }

        Button btnReset = UiTheme.createGhostButton("Restaurar Padrão");
        btnReset.setOnAction(e -> {
            GameSettings.get().getKeyBindings().resetToDefaults();
            GameSettings.get().save();
            refreshAllKeyButtons();
        });

        HBox footer = new HBox(btnReset);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(14, grid, footer);
        layout.setPadding(new Insets(4, 0, 0, 0));
        return layout;
    }

    private Node buildAudioPane() {
        GameSettings s = GameSettings.get();

        ToggleButton muteToggle = UiTheme.createToggleSwitch(AudioEngine.get().isMuted(), m -> AudioEngine.get().toggleMute());
        VBox muteCard = createSectionCard(settingRow("Silenciar Áudio", muteToggle));

        VBox slidersCard = new VBox(10);
        slidersCard.getStyleClass().add("glass-card");
        slidersCard.setPadding(new Insets(14, 18, 14, 18));
        slidersCard.getChildren().addAll(
                channelSlider("Geral", AudioChannel.MASTER, s.getMasterVolume()),
                channelSlider("Trilha Sonora", AudioChannel.MUSIC, s.getChannelVolume(AudioChannel.MUSIC)),
                channelSlider("Combate", AudioChannel.SFX, s.getChannelVolume(AudioChannel.SFX)),
                channelSlider("Passos", AudioChannel.STEPS, s.getChannelVolume(AudioChannel.STEPS)),
                channelSlider("Criaturas", AudioChannel.CREATURES, s.getChannelVolume(AudioChannel.CREATURES)),
                channelSlider("Ambiente", AudioChannel.AMBIENCE, s.getChannelVolume(AudioChannel.AMBIENCE)),
                channelSlider("Clima", AudioChannel.WEATHER, s.getChannelVolume(AudioChannel.WEATHER)),
                channelSlider("Interface", AudioChannel.UI, s.getChannelVolume(AudioChannel.UI))
        );

        ScrollPane scroll = new ScrollPane(slidersCard);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox layout = new VBox(10, muteCard, scroll);
        layout.setPadding(new Insets(4, 0, 0, 0));
        return layout;
    }

    private VBox createSectionCard(Node... rows) {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(12, 18, 12, 18));
        for (int i = 0; i < rows.length; i++) {
            card.getChildren().add(rows[i]);
            if (i < rows.length - 1) card.getChildren().add(UiTheme.createDivider());
        }
        return card;
    }

    private HBox switchRow(String title, boolean initial, Consumer<Boolean> onToggle) {
        return settingRow(title, UiTheme.createToggleSwitch(initial, onToggle));
    }

    private HBox settingRow(String title, Node control) {
        Label lbl = new Label(title);
        lbl.setTextFill(Color.web(UiTheme.TEXT_PRIMARY));
        UiTypography.bind(lbl, UiTypography.TypoRole.BODY);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, lbl, spacer, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox channelSlider(String label, AudioChannel ch, double initial) {
        Label lbl = new Label(label);
        lbl.setPrefWidth(120);
        lbl.setTextFill(Color.web(UiTheme.TEXT_PRIMARY));
        UiTypography.bind(lbl, UiTypography.TypoRole.LABEL);

        Slider slider = new Slider(0.0, 1.0, initial);
        HBox.setHgrow(slider, Priority.ALWAYS);

        Label pct = new Label(Math.round(initial * 100) + "%");
        pct.setMinWidth(46);
        pct.setAlignment(Pos.CENTER_RIGHT);
        pct.setTextFill(Color.web(UiTheme.ACCENT_AMBER));
        UiTypography.bind(pct, UiTypography.TypoRole.NUMERIC);

        slider.valueProperty().addListener((obs, o, v) -> {
            double val = v.doubleValue();
            pct.setText(Math.round(val * 100) + "%");
            GameSettings.get().setChannelVolume(ch, val);
        });

        HBox row = new HBox(14, lbl, slider, pct);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void refreshAllKeyButtons() {
        for (KeyRebindButton btn : rebindButtons) {
            btn.refreshText();
        }
    }

    private static String qualityShortName(GraphicsQuality q) {
        return switch (q) {
            case LOW -> "Baixa";
            case MEDIUM -> "Média";
            case HIGH -> "Alta";
        };
    }
}
