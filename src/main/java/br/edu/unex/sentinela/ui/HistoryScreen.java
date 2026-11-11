package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.MatchHistoryRepository;
import br.edu.unex.sentinela.progress.MatchRecord;
import br.edu.unex.sentinela.progress.PersonalRecords;
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
 * Tela de Histórico de Jogos e Recordes Pessoais sem caracteres unicode quebrados
 * e com apresentação visual limpa de diário de sobrevivência.
 */
public class HistoryScreen extends StackPane {

    public HistoryScreen(ScreenManager manager) {
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: rgba(10, 16, 12, 0.88);");

        VBox panel = UiTheme.createStandardPanel(780, 540);

        Label title = UiTheme.createTitle("HISTÓRICO DE JOGOS & RECORDES");
        Label subtitle = UiTheme.createSubtitle("Estatísticas consolidadas e histórico das suas últimas corridas na selva.");

        // Cartões de recorde no topo com ícones vetoriais imunes a quebras
        HBox recordsRow = buildRecordCards();

        // Tabela/Lista de partidas
        VBox historyList = buildHistoryList();
        ScrollPane sp = new ScrollPane(historyList);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);

        Button btnBack = UiTheme.createPrimaryButton("VOLTAR", null, 200, 38);
        btnBack.setOnAction(e -> manager.navigateBack());

        panel.getChildren().addAll(title, subtitle, recordsRow, sp, btnBack);
        getChildren().add(panel);
    }

    private HBox buildRecordCards() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);

        PersonalRecords pr = MatchHistoryRepository.get().getPersonalRecords();

        row.getChildren().addAll(
            createStatCard("TROPHY", "RECORDE", String.format("%,d pts", pr.getBestScore()), "#F1C40F"),
            createStatCard("CLOCK", "SOBREVIVÊNCIA", pr.getFormattedBestTime(), "#3498DB"),
            createStatCard("SWORD", "MAIS ABATES", pr.getMaxKillsSingleRun() + " rivais", "#E74C3C"),
            createStatCard("APPLE", "MAIS MAÇÃS", pr.getMaxApplesSingleRun() + " frutos", "#2ECC71"),
            createStatCard("CHART", "TOTAL JOGOS", pr.getTotalMatchesPlayed() + " corridas", "#9B59B6")
        );

        return row;
    }

    private VBox createStatCard(String iconType, String title, String val, String colorHex) {
        VBox card = new VBox(3);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(142, 68);
        card.setStyle(UiTheme.CARD_MODERN);
        card.setPadding(new Insets(6, 8, 6, 8));

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER);
        Canvas icon = UiBadge.createBadge(iconType, 13, Color.web(colorHex));
        Label lblTitle = new Label(title);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 10));
        lblTitle.setTextFill(Color.web("#BDC3C7"));
        header.getChildren().addAll(icon, lblTitle);

        Label lblVal = new Label(val);
        lblVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblVal.setTextFill(Color.web(colorHex));

        card.getChildren().addAll(header, lblVal);
        return card;
    }

    private VBox buildHistoryList() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8));

        List<MatchRecord> list = MatchHistoryRepository.get().getHistory();
        if (list.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(40, 20, 40, 20));

            Label emptyTitle = new Label("Nenhum rastro gravado nas trilhas da selva ainda.");
            emptyTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
            emptyTitle.setTextFill(Color.web("#F1C40F"));

            Label emptyDesc = new Label("Inicie uma nova corrida pela sobrevivência para registrar suas pontuações, tempo e abates!");
            emptyDesc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
            emptyDesc.setTextFill(Color.web("#85929E"));

            emptyState.getChildren().addAll(emptyTitle, emptyDesc);
            box.getChildren().add(emptyState);
            return box;
        }

        // Cabeçalho da tabela
        HBox header = new HBox(12);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setStyle("-fx-background-color: rgba(26, 38, 30, 0.95); -fx-background-radius: 6;");
        header.getChildren().addAll(
            createHeaderCell("Data / Hora", 130),
            createHeaderCell("Tempo", 90),
            createHeaderCell("Pontuação", 100),
            createHeaderCell("Maçãs", 80),
            createHeaderCell("Abates", 80),
            createHeaderCell("Causa do Fim da Corrida", 220)
        );
        box.getChildren().add(header);

        for (MatchRecord r : list) {
            HBox row = new HBox(12);
            row.setPadding(new Insets(7, 12, 7, 12));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: rgba(28, 38, 32, 0.65); -fx-background-radius: 6;");

            row.getChildren().addAll(
                createCell(r.getTimestamp(), 130, "#BDC3C7"),
                createCell(r.getFormattedSurvivalTime(), 90, "#3498DB"),
                createCell(String.format("%,d pts", r.getScore()), 100, "#F1C40F"),
                createCell(r.getApplesEaten() + " frutos", 80, "#2ECC71"),
                createCell(r.getEnemiesDefeated() + " abates", 80, "#E74C3C"),
                createCell(r.getCauseOfDeath(), 220, "#EAECEE")
            );
            box.getChildren().add(row);
        }

        return box;
    }

    private Label createHeaderCell(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#F1C40F"));
        return l;
    }

    private Label createCell(String text, double width, String colorHex) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        l.setTextFill(Color.web(colorHex));
        return l;
    }
}
