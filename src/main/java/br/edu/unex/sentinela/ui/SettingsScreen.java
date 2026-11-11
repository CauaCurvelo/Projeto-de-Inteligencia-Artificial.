package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.audio.AudioChannel;
import br.edu.unex.sentinela.audio.AudioEngine;
import br.edu.unex.sentinela.audio.SoundId;
import br.edu.unex.sentinela.settings.GameAction;
import br.edu.unex.sentinela.settings.GameSettings;
import br.edu.unex.sentinela.settings.GraphicsQuality;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Tela de Configurações completa com abas de Gráficos, Controles (Rebinding) e Áudio Multicanal.
 */
public class SettingsScreen extends StackPane {

    public SettingsScreen(ScreenManager manager) {
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: rgba(10, 16, 12, 0.88);");

        VBox panel = UiTheme.createStandardPanel(720, 530);

        Label title = UiTheme.createTitle("CONFIGURAÇÕES");
        Label subtitle = UiTheme.createSubtitle("Ajuste a fidelidade visual, comandos e canais de áudio.");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");

        Tab tabGfx = new Tab("Gráficos", buildGraphicsTab());
        Tab tabCtrls = new Tab("Controles", buildControlsTab());
        Tab tabAudio = new Tab("Áudio Multicanal", buildAudioTab());

        tabPane.getTabs().addAll(tabGfx, tabCtrls, tabAudio);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Button btnBack = UiTheme.createPrimaryButton("VOLTAR", null, 200, 38);
        btnBack.setOnAction(e -> manager.navigateBack());

        panel.getChildren().addAll(title, subtitle, tabPane, btnBack);
        getChildren().add(panel);
    }

    private VBox buildGraphicsTab() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16));
        box.setAlignment(Pos.TOP_LEFT);

        GameSettings s = GameSettings.get();

        HBox qualityRow = new HBox(16);
        qualityRow.setAlignment(Pos.CENTER_LEFT);
        Label lblQ = createFieldLabel("Qualidade Visual:");
        ComboBox<GraphicsQuality> cbQ = new ComboBox<>();
        cbQ.getItems().addAll(GraphicsQuality.values());
        cbQ.setValue(s.getGraphicsQuality());
        cbQ.setOnAction(e -> s.setGraphicsQuality(cbQ.getValue()));
        qualityRow.getChildren().addAll(lblQ, cbQ);

        HBox fpsRow = new HBox(16);
        fpsRow.setAlignment(Pos.CENTER_LEFT);
        Label lblFps = createFieldLabel("Limite de FPS:");
        ComboBox<Integer> cbFps = new ComboBox<>();
        cbFps.getItems().addAll(30, 60, 120, 0);
        cbFps.setValue(s.getTargetFps());
        cbFps.setOnAction(e -> s.setTargetFps(cbFps.getValue()));
        fpsRow.getChildren().addAll(lblFps, cbFps);

        CheckBox chkCones = createCheckBox("Exibir Cones de Visão Táticos dos Predadores", s.isShowVisionCones());
        chkCones.setOnAction(e -> s.setShowVisionCones(chkCones.isSelected()));

        CheckBox chkTrails = createCheckBox("Exibir Trilhas e Rastros de Perseguição no Solo", s.isShowPathTrails());
        chkTrails.setOnAction(e -> s.setShowPathTrails(chkTrails.isSelected()));

        CheckBox chkNames = createCheckBox("Exibir Barras de Vida e Nomes dos Inimigos", s.isShowNameplates());
        chkNames.setOnAction(e -> s.setShowNameplates(chkNames.isSelected()));

        CheckBox chkShake = createCheckBox("Efeito de Tremor de Tela em Golpes Pesados", s.isScreenShake());
        chkShake.setOnAction(e -> s.setScreenShake(chkShake.isSelected()));

        box.getChildren().addAll(qualityRow, fpsRow, chkCones, chkTrails, chkNames, chkShake);
        return box;
    }

    private ScrollPane buildControlsTab() {
        VBox list = new VBox(10);
        list.setPadding(new Insets(12));
        list.setAlignment(Pos.TOP_LEFT);

        for (GameAction action : GameAction.values()) {
            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(action.getDisplayName());
            name.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            name.setTextFill(Color.web("#EAECEE"));
            name.setPrefWidth(240);

            KeyRebindButton btn = new KeyRebindButton(action, null);
            row.getChildren().addAll(name, btn);
            list.getChildren().add(row);
        }

        Button btnReset = UiTheme.createSecondaryButton("Restaurar Controles Padrão", 200, 32);
        btnReset.setOnAction(e -> {
            GameSettings.get().getKeyBindings().resetToDefaults();
            GameSettings.get().save();
            list.getChildren().stream()
                .filter(node -> node instanceof HBox)
                .map(node -> (HBox) node)
                .forEach(row -> row.getChildren().stream()
                    .filter(c -> c instanceof KeyRebindButton)
                    .forEach(c -> ((KeyRebindButton) c).updateLabel()));
        });
        list.getChildren().add(btnReset);

        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    private VBox buildAudioTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(14));
        box.setAlignment(Pos.TOP_LEFT);

        GameSettings s = GameSettings.get();

        CheckBox chkMute = createCheckBox("Silenciar Todo o Áudio (Mudo)", AudioEngine.get().isMuted());
        chkMute.setOnAction(e -> AudioEngine.get().toggleMute());
        box.getChildren().add(chkMute);

        box.getChildren().add(buildVolumeSlider("Volume Geral (Master)", AudioChannel.MASTER, s.getMasterVolume(), SoundId.UI_CLICK));
        box.getChildren().add(buildVolumeSlider("Efeitos Sonoros (Combate)", AudioChannel.SFX, s.getChannelVolume(AudioChannel.SFX), SoundId.BITE_WHOOSH));
        box.getChildren().add(buildVolumeSlider("Passos e Movimento", AudioChannel.STEPS, s.getChannelVolume(AudioChannel.STEPS), SoundId.STEP_GRASS));
        box.getChildren().add(buildVolumeSlider("Vozes e Animais", AudioChannel.CREATURES, s.getChannelVolume(AudioChannel.CREATURES), SoundId.DOG_BARK));
        box.getChildren().add(buildVolumeSlider("Ambiente da Natureza", AudioChannel.AMBIENCE, s.getChannelVolume(AudioChannel.AMBIENCE), SoundId.BIRD_CHIRP));
        box.getChildren().add(buildVolumeSlider("Clima e Chuva", AudioChannel.WEATHER, s.getChannelVolume(AudioChannel.WEATHER), SoundId.THUNDER));
        box.getChildren().add(buildVolumeSlider("Interface e Menus", AudioChannel.UI, s.getChannelVolume(AudioChannel.UI), SoundId.UI_CLICK));

        return box;
    }

    private HBox buildVolumeSlider(String label, AudioChannel ch, double initialVal, SoundId sampleSound) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setPrefWidth(190);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web("#EAECEE"));

        Slider slider = new Slider(0.0, 1.0, initialVal);
        slider.setPrefWidth(220);

        Label pct = new Label((int) (initialVal * 100) + "%");
        pct.setPrefWidth(45);
        pct.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        pct.setTextFill(Color.web("#F1C40F"));

        slider.valueProperty().addListener((obs, oldV, newV) -> {
            pct.setText((int) (newV.doubleValue() * 100) + "%");
            GameSettings.get().setChannelVolume(ch, newV.doubleValue());
        });

        // Ao soltar o mouse no slider, toca prévia do som
        slider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging && sampleSound != null) {
                AudioEngine.get().play(sampleSound, 0.7, 0.0, 1.0);
            }
        });

        row.getChildren().addAll(lbl, slider, pct);
        return row;
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        l.setTextFill(Color.web("#EAECEE"));
        l.setPrefWidth(160);
        return l;
    }

    private CheckBox createCheckBox(String text, boolean initial) {
        CheckBox chk = new CheckBox(text);
        chk.setSelected(initial);
        chk.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        chk.setTextFill(Color.web("#EAECEE"));
        return chk;
    }
}
