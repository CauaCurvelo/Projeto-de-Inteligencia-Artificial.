package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.MatchHistoryRepository;
import br.edu.unex.sentinela.progress.MatchRecord;
import br.edu.unex.sentinela.progress.PersonalRecords;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Histórico de partidas e recordes pessoais em layout enxuto e tabular.
 */
public class HistoryScreen extends StackPane {
    public HistoryScreen(ScreenManager manager) {
        VBox panel = UiTheme.createGlassPanel(840, 560, 22, 28);
        panel.setSpacing(14);

        MatchHistoryRepository repo = MatchHistoryRepository.get();
        PersonalRecords rec = repo.getPersonalRecords();
        List<MatchRecord> history = repo.getHistory();

        VBox header = UiTheme.createHeader("HISTÓRICO", null, manager::navigateBack);
        GridPane kpis = buildKpiCards(rec);

        Node body = history.isEmpty() ? buildEmptyState() : buildHistoryTable(history);
        VBox.setVgrow(body, Priority.ALWAYS);

        panel.getChildren().addAll(header, kpis, body);
        getChildren().add(UiTheme.createModalBackdrop(panel));
    }

    private GridPane buildKpiCards(PersonalRecords rec) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            grid.getColumnConstraints().add(cc);
        }

        grid.add(kpiCard("RECORDE", String.format("%,d", rec.getBestScore()).replace(',', '.'), UiBadge.Icon.TROPHY, UiTheme.ACCENT_AMBER), 0, 0);
        grid.add(kpiCard("MELHOR TEMPO", rec.getFormattedBestTime(), UiBadge.Icon.CLOCK, UiTheme.ACCENT_SKY), 1, 0);
        grid.add(kpiCard("MAIOR ABATE", String.valueOf(rec.getMaxKillsSingleRun()), UiBadge.Icon.SWORD, UiTheme.ACCENT_CRIMSON), 2, 0);
        grid.add(kpiCard("PARTIDAS", String.valueOf(rec.getTotalMatchesPlayed()), UiBadge.Icon.CHART, UiTheme.ACCENT_EMERALD), 3, 0);
        return grid;
    }

    private VBox kpiCard(String title, String value, UiBadge.Icon icon, String accentHex) {
        Label cap = new Label(title);
        cap.setTextFill(Color.web(UiTheme.TEXT_MUTED));
        UiTypography.bind(cap, UiTypography.TypoRole.MICRO);

        HBox top = new HBox(6, UiBadge.create(icon, 13, Color.web(accentHex)), cap);
        top.setAlignment(Pos.CENTER_LEFT);

        Label val = new Label(value);
        val.setTextFill(Color.web(UiTheme.TEXT_PRIMARY));
        UiTypography.bind(val, UiTypography.TypoRole.SECTION);

        VBox card = new VBox(5, top, val);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(10, 14, 10, 14));
        return card;
    }

    private Node buildEmptyState() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("glass-card");

        Label title = new Label("Nenhuma corrida registrada");
        title.setTextFill(Color.web(UiTheme.TEXT_PRIMARY));
        UiTypography.bind(title, UiTypography.TypoRole.SECTION);

        Label hint = new Label("Complete uma partida para registrar recordes e estatísticas.");
        hint.setTextFill(Color.web(UiTheme.TEXT_SECONDARY));
        UiTypography.bind(hint, UiTypography.TypoRole.LABEL);

        box.getChildren().addAll(UiBadge.create(UiBadge.Icon.TROPHY, 34, Color.web(UiTheme.TEXT_MUTED)), title, hint);
        return box;
    }

    private Node buildHistoryTable(List<MatchRecord> records) {
        GridPane table = new GridPane();
        table.setVgap(4);
        table.setHgap(10);

        double[] widths = {18, 12, 12, 10, 10, 38};
        for (double w : widths) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(w);
            table.getColumnConstraints().add(cc);
        }

        String[] cols = {"DATA", "TEMPO", "PONTOS", "FRUTAS", "ABATES", "DESFECHO"};
        for (int c = 0; c < cols.length; c++) {
            Label h = new Label(cols[c]);
            h.setTextFill(Color.web(UiTheme.TEXT_MUTED));
            UiTypography.bind(h, UiTypography.TypoRole.MICRO);
            h.setPadding(new Insets(4, 6, 8, 6));
            table.add(h, c, 0);
        }

        int limit = Math.min(records.size(), 40);
        for (int i = 0; i < limit; i++) {
            MatchRecord r = records.get(i);
            int rowIdx = i + 1;
            String bg = (i % 2 == 0) ? "rgba(255,255,255,0.03)" : "transparent";

            addCell(table, r.getTimestamp(), 0, rowIdx, UiTheme.TEXT_SECONDARY, UiTypography.TypoRole.MICRO, bg);
            addCell(table, r.getFormattedSurvivalTime(), 1, rowIdx, UiTheme.ACCENT_SKY, UiTypography.TypoRole.NUMERIC, bg);
            addCell(table, String.valueOf(r.getScore()), 2, rowIdx, UiTheme.ACCENT_AMBER, UiTypography.TypoRole.NUMERIC, bg);
            addCell(table, String.valueOf(r.getApplesEaten()), 3, rowIdx, UiTheme.ACCENT_EMERALD, UiTypography.TypoRole.NUMERIC, bg);
            addCell(table, String.valueOf(r.getEnemiesDefeated()), 4, rowIdx, UiTheme.ACCENT_CRIMSON, UiTypography.TypoRole.NUMERIC, bg);
            addCell(table, r.getCauseOfDeath(), 5, rowIdx, UiTheme.TEXT_PRIMARY, UiTypography.TypoRole.LABEL, bg);
        }

        ScrollPane scroll = new ScrollPane(table);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private void addCell(GridPane grid, String text, int col, int row, String colorHex, UiTypography.TypoRole role, String bgCss) {
        Label lbl = new Label(text);
        lbl.setTextFill(Color.web(colorHex));
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setPadding(new Insets(7, 6, 7, 6));
        lbl.setStyle("-fx-background-color: " + bgCss + "; -fx-background-radius: 6;");
        UiTypography.bind(lbl, role);
        grid.add(lbl, col, row);
    }
}
