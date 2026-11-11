package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.bestiary.CreatureCatalog;
import br.edu.unex.sentinela.bestiary.CreatureInfo;
import br.edu.unex.sentinela.bestiary.CreaturePreviewRenderer;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Tela de Bestiário e Enciclopédia da Fauna com manequim animado 2.5D em tempo real
 * e ficha técnica sem emojis quebrados.
 */
public class BestiaryScreen extends StackPane {
    private final Canvas previewCanvas = new Canvas(200, 200);
    private final CreaturePreviewRenderer previewRenderer;
    private CreatureInfo selectedCreature;
    private AnimationTimer previewTimer;

    // Campos da ficha técnica
    private final Label nameLabel = new Label();
    private final Label titleLabel = new Label();
    private final Label dietLabel = new Label();
    private final Label habitatLabel = new Label();
    private final Label attackLabel = new Label();
    private final Label statsLabel = new Label();
    private final Label behaviorLabel = new Label();
    private final Label tipLabel = new Label();

    public BestiaryScreen(ScreenManager manager) {
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: rgba(10, 16, 12, 0.88);");

        previewRenderer = new CreaturePreviewRenderer(previewCanvas);

        VBox panel = UiTheme.createStandardPanel(820, 560);

        Label title = UiTheme.createTitle("BESTIÁRIO & FAUNA SELVAGEM");
        Label subtitle = UiTheme.createSubtitle("Enciclopédia viva de espécies, comportamentos de IA e táticas de sobrevivência.");

        HBox splitView = new HBox(16);
        VBox.setVgrow(splitView, Priority.ALWAYS);

        VBox listCol = buildCreatureList();
        VBox detailsCol = buildDetailsPanel();
        HBox.setHgrow(detailsCol, Priority.ALWAYS);

        splitView.getChildren().addAll(listCol, detailsCol);

        Button btnBack = UiTheme.createPrimaryButton("VOLTAR", null, 200, 38);
        btnBack.setOnAction(e -> {
            stopPreview();
            manager.navigateBack();
        });

        panel.getChildren().addAll(title, subtitle, splitView, btnBack);
        getChildren().add(panel);

        // Seleciona a primeira criatura por padrão (Raposa)
        selectCreature(CreatureCatalog.getAll().get(0));
        startPreview();
    }

    private VBox buildCreatureList() {
        VBox box = new VBox(6);
        box.setPrefWidth(240);
        box.setPadding(new Insets(4));

        List<CreatureInfo> all = CreatureCatalog.getAll();
        for (CreatureInfo c : all) {
            Button btn = new Button(c.getName());
            btn.setPrefSize(230, 36);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            btn.setStyle(
                "-fx-background-color: linear-gradient(to right, rgba(30, 42, 34, 0.85), rgba(18, 26, 20, 0.85));" +
                "-fx-text-fill: #EAECEE;" +
                "-fx-border-color: rgba(212, 172, 13, 0.35);" +
                "-fx-border-width: 1.2;" +
                "-fx-background-radius: 6;" +
                "-fx-border-radius: 6;" +
                "-fx-cursor: hand;"
            );

            btn.setOnAction(e -> selectCreature(c));
            box.getChildren().add(btn);
        }

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setPrefWidth(240);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return new VBox(sp);
    }

    private VBox buildDetailsPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14));
        box.setStyle(UiTheme.CARD_MODERN);
        box.setAlignment(Pos.TOP_LEFT);

        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleCol = new VBox(4);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 22));
        nameLabel.setTextFill(Color.web("#F1C40F"));

        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        titleLabel.setTextFill(Color.web("#2ECC71"));

        statsLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        statsLabel.setTextFill(Color.web("#E67E22"));

        titleCol.getChildren().addAll(nameLabel, titleLabel, statsLabel);
        topRow.getChildren().addAll(previewCanvas, titleCol);

        VBox dataRows = new VBox(6);
        dataRows.getChildren().addAll(
            createDataRow("Dieta:", dietLabel),
            createDataRow("Habitat:", habitatLabel),
            createDataRow("Tipo de Ataque:", attackLabel)
        );

        Label lblBehTitle = new Label("Comportamento e Inteligência Artificial:");
        lblBehTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblBehTitle.setTextFill(Color.web("#F1C40F"));

        behaviorLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        behaviorLabel.setTextFill(Color.web("#BDC3C7"));
        behaviorLabel.setWrapText(true);

        Label lblTipTitle = new Label("Dica Tática de Sobrevivência:");
        lblTipTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblTipTitle.setTextFill(Color.web("#3498DB"));

        tipLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        tipLabel.setTextFill(Color.web("#EAECEE"));
        tipLabel.setWrapText(true);

        box.getChildren().addAll(topRow, dataRows, lblBehTitle, behaviorLabel, lblTipTitle, tipLabel);
        return box;
    }

    private HBox createDataRow(String prefix, Label valueLabel) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label p = new Label(prefix);
        p.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        p.setTextFill(Color.web("#EAECEE"));
        p.setPrefWidth(110);

        valueLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        valueLabel.setTextFill(Color.web("#BDC3C7"));

        row.getChildren().addAll(p, valueLabel);
        return row;
    }

    private void selectCreature(CreatureInfo c) {
        this.selectedCreature = c;
        nameLabel.setText(c.getName());
        titleLabel.setText(c.getTitle());
        dietLabel.setText(c.getDiet());
        habitatLabel.setText(c.getHabitat());
        attackLabel.setText(c.getAttackType());

        // Barras de status segmentadas estilo PIP bar
        String speedBars = formatPipBar(c.getSpeedStars(), 5);
        String dangerBars = formatPipBar(c.getDangerStars(), 5);
        statsLabel.setText(String.format("Velocidade: %s   |   Ameaça: %s   |   Vida: %d HP", speedBars, dangerBars, c.getHealthPoints()));

        behaviorLabel.setText(c.getBehaviorDescription());
        tipLabel.setText(c.getSurvivalTip());
    }

    private String formatPipBar(int filled, int total) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filled; i++) sb.append("█");
        for (int i = filled; i < total; i++) sb.append("░");
        return sb.toString();
    }

    public void startPreview() {
        if (previewTimer != null) previewTimer.stop();
        previewTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double time = now / 1_000_000_000.0;
                previewRenderer.render(selectedCreature, time);
            }
        };
        previewTimer.start();
    }

    public void stopPreview() {
        if (previewTimer != null) {
            previewTimer.stop();
            previewTimer = null;
        }
    }
}
