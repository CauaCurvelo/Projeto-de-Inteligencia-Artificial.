package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.bestiary.BestiaryUnlockManager;
import br.edu.unex.sentinela.bestiary.CreatureCatalog;
import br.edu.unex.sentinela.bestiary.CreatureInfo;
import br.edu.unex.sentinela.bestiary.CreaturePreviewRenderer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Enciclopédia de fauna com vitrine 2.5D ampliada e desbloqueio progressivo por abate.
 */
public class BestiaryScreen extends StackPane {
    private final CreaturePreviewRenderer previewRenderer = new CreaturePreviewRenderer(178, 178);
    private final List<Button> listButtons = new ArrayList<>();
    private final VBox detailCard = new VBox(14);
    private final HBox showcaseRow = new HBox(18);
    private final HBox tagStrip = new HBox(10);
    private final VBox intelContainer = new VBox(10);

    public BestiaryScreen(ScreenManager manager) {
        VBox panel = UiTheme.createGlassPanel(860, 560, 22, 26);
        panel.setSpacing(14);

        List<CreatureInfo> catalog = CreatureCatalog.getAll();
        int unlockedCount = 0;
        for (CreatureInfo c : catalog) {
            if (BestiaryUnlockManager.get().isUnlocked(c.getId())) {
                unlockedCount++;
            }
        }

        Label countChip = new Label(unlockedCount + " / " + catalog.size() + " Catalogadas");
        countChip.setPadding(new Insets(4, 10, 4, 10));
        countChip.getStyleClass().add("glass-card");
        countChip.setTextFill(Color.web(UiTheme.TEXT_SECONDARY));
        UiTypography.bind(countChip, UiTypography.TypoRole.MICRO);

        VBox header = UiTheme.createHeader("BESTIÁRIO", countChip, () -> {
            previewRenderer.stop();
            manager.navigateBack();
        });

        HBox body = new HBox(16);
        VBox.setVgrow(body, Priority.ALWAYS);

        Node listPane = buildSpeciesList(catalog);
        buildDetailCard();
        HBox.setHgrow(detailCard, Priority.ALWAYS);

        body.getChildren().addAll(listPane, detailCard);
        panel.getChildren().addAll(header, body);
        getChildren().add(UiTheme.createModalBackdrop(panel));

        if (!catalog.isEmpty()) {
            selectCreature(catalog.get(0), 0);
        }
        previewRenderer.start();
    }

    private Node buildSpeciesList(List<CreatureInfo> catalog) {
        VBox listBox = new VBox(6);
        listBox.setPadding(new Insets(2, 6, 2, 2));

        for (int i = 0; i < catalog.size(); i++) {
            CreatureInfo info = catalog.get(i);
            final int index = i;
            boolean unlocked = BestiaryUnlockManager.get().isUnlocked(info.getId());

            String buttonText = unlocked ? info.getName() : "??? (Não Catalogada)";
            Button row = new Button(buttonText);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setGraphic(new Circle(4, unlocked ? roleColor(info) : Color.web("#475569")));
            row.setGraphicTextGap(10);
            row.getStyleClass().add("species-row");
            if (!unlocked) {
                row.setTextFill(Color.web("#94A3B8"));
            }
            UiTypography.bind(row, UiTypography.TypoRole.LABEL);
            UiMotion.attachPressFeedback(row, true);
            row.setOnAction(e -> selectCreature(info, index));

            listButtons.add(row);
            listBox.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        UiTypography.bindScaledWidth(scroll, 236);
        return scroll;
    }

    private void buildDetailCard() {
        detailCard.getStyleClass().add("glass-card");
        detailCard.setPadding(new Insets(18, 20, 18, 20));
        showcaseRow.setAlignment(Pos.CENTER_LEFT);
        tagStrip.setAlignment(Pos.CENTER_LEFT);
        detailCard.getChildren().addAll(showcaseRow, UiTheme.createDivider(), tagStrip, UiTheme.createDivider(), intelContainer);
    }

    private void selectCreature(CreatureInfo info, int activeIdx) {
        for (int i = 0; i < listButtons.size(); i++) {
            Button btn = listButtons.get(i);
            btn.getStyleClass().remove("species-row-active");
            if (!btn.getStyleClass().contains("species-row")) {
                btn.getStyleClass().add("species-row");
            }
            if (i == activeIdx) {
                btn.getStyleClass().remove("species-row");
                btn.getStyleClass().add("species-row-active");
            }
        }
        previewRenderer.setCreature(info);
        populateDetails(info);
    }

    private void populateDetails(CreatureInfo info) {
        boolean unlocked = BestiaryUnlockManager.get().isUnlocked(info.getId());

        if (!unlocked) {
            Label nameLbl = new Label("Criatura Não Catalogada");
            nameLbl.setTextFill(Color.web("#94A3B8"));
            UiTypography.bind(nameLbl, UiTypography.TypoRole.TITLE);

            Label rolePill = new Label("SILHUETA BLOQUEADA");
            rolePill.setPadding(new Insets(3, 9, 3, 9));
            rolePill.setTextFill(Color.web("#64748B"));
            rolePill.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 99;");
            UiTypography.bind(rolePill, UiTypography.TypoRole.MICRO);

            HBox titleRow = new HBox(10, nameLbl, rolePill);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            Label hpBadge = new Label("♥  ? HP");
            hpBadge.setTextFill(Color.web("#64748B"));
            UiTypography.bind(hpBadge, UiTypography.TypoRole.NUMERIC);

            VBox statsColumn = new VBox(9,
                    titleRow,
                    createMetricRow("Velocidade", 0, 5, "#475569"),
                    createMetricRow("Ameaça", 0, 5, "#475569"),
                    hpBadge
            );
            HBox.setHgrow(statsColumn, Priority.ALWAYS);
            showcaseRow.getChildren().setAll(previewRenderer, statsColumn);

            tagStrip.getChildren().setAll(
                    attributeChip("DIETA", "Desconhecida"),
                    attributeChip("HABITAT", "Desconhecido"),
                    attributeChip("ATAQUE", "Desconhecido")
            );

            intelContainer.getChildren().setAll(
                    intelBlock("STATUS DE DESCOBERTA",
                        "Esta criatura ainda não foi derrotada pelo jogador na natureza selvagem. Encontre-a nos biomas e derrote ao menos um exemplar para desvendar sua anatomia, atributos, pontos fracos e comportamento no Bestiário.",
                        UiTheme.TEXT_MUTED),
                    intelBlock("DICA DE RASTREAMENTO",
                        "Explore tocas, matas fechadas, rios, brejos pantanosos, encostas rochosas e a tundra nevada à procura de rastros e pegadas.",
                        UiTheme.ACCENT_SKY)
            );
            return;
        }

        Color accent = roleColor(info);

        Label nameLbl = new Label(info.getName());
        nameLbl.setTextFill(Color.web(UiTheme.TEXT_PRIMARY));
        UiTypography.bind(nameLbl, UiTypography.TypoRole.TITLE);

        Label rolePill = new Label(roleBadge(info));
        rolePill.setPadding(new Insets(3, 9, 3, 9));
        rolePill.setTextFill(accent);
        rolePill.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 99;");
        UiTypography.bind(rolePill, UiTypography.TypoRole.MICRO);

        HBox titleRow = new HBox(10, nameLbl, rolePill);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label hpBadge = new Label("♥  " + info.getHealthPoints() + " HP");
        hpBadge.setTextFill(Color.web(UiTheme.ACCENT_CRIMSON));
        UiTypography.bind(hpBadge, UiTypography.TypoRole.NUMERIC);

        VBox statsColumn = new VBox(9,
                titleRow,
                createMetricRow("Velocidade", info.getSpeedStars(), 5, UiTheme.ACCENT_SKY),
                createMetricRow("Ameaça", info.getDangerStars(), 5, info.getDangerStars() >= 4 ? UiTheme.ACCENT_CRIMSON : UiTheme.ACCENT_AMBER),
                hpBadge
        );
        HBox.setHgrow(statsColumn, Priority.ALWAYS);
        showcaseRow.getChildren().setAll(previewRenderer, statsColumn);

        tagStrip.getChildren().setAll(
                attributeChip("DIETA", info.getDiet()),
                attributeChip("HABITAT", info.getHabitat()),
                attributeChip("ATAQUE", info.getAttackType())
        );

        intelContainer.getChildren().setAll(
                intelBlock("COMPORTAMENTO", info.getBehaviorDescription(), UiTheme.ACCENT_AMBER),
                intelBlock("TÁTICA", info.getSurvivalTip(), UiTheme.ACCENT_EMERALD)
        );
    }

    private HBox createMetricRow(String labelText, int value, int max, String colorHex) {
        Label lbl = new Label(labelText);
        lbl.setPrefWidth(82);
        lbl.setTextFill(Color.web(UiTheme.TEXT_SECONDARY));
        UiTypography.bind(lbl, UiTypography.TypoRole.LABEL);

        HBox segments = new HBox(5);
        segments.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < max; i++) {
            Rectangle seg = new Rectangle(24, 6);
            seg.setArcWidth(4);
            seg.setArcHeight(4);
            seg.setFill(i < value ? Color.web(colorHex) : Color.web("#FFFFFF", 0.10));
            segments.getChildren().add(seg);
        }

        HBox row = new HBox(10, lbl, segments);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox attributeChip(String title, String val) {
        Label key = new Label(title);
        key.setTextFill(Color.web(UiTheme.TEXT_MUTED));
        UiTypography.bind(key, UiTypography.TypoRole.MICRO);

        Label value = new Label(val);
        value.setTextFill(Color.web(UiTheme.TEXT_PRIMARY));
        value.setWrapText(true);
        value.setMinHeight(Region.USE_PREF_SIZE);
        UiTypography.bind(value, UiTypography.TypoRole.LABEL);

        VBox chip = new VBox(3, key, value);
        chip.getStyleClass().add("glass-card");
        chip.setPadding(new Insets(9, 12, 9, 12));
        HBox.setHgrow(chip, Priority.ALWAYS);
        chip.setMaxWidth(Double.MAX_VALUE);
        return chip;
    }

    private VBox intelBlock(String heading, String body, String accentHex) {
        Label h = new Label(heading);
        h.setTextFill(Color.web(accentHex));
        UiTypography.bind(h, UiTypography.TypoRole.MICRO);

        Label b = new Label(body);
        b.setTextFill(Color.web(UiTheme.TEXT_PRIMARY));
        b.setWrapText(true);
        b.setMinHeight(Region.USE_PREF_SIZE);
        UiTypography.bind(b, UiTypography.TypoRole.BODY);

        VBox box = new VBox(4, h, b);
        box.getStyleClass().add("glass-card");
        box.setPadding(new Insets(10, 14, 10, 14));
        return box;
    }

    private static Color roleColor(CreatureInfo info) {
        if (info.isPassive()) return Color.web("#38BDF8");
        if (info.getType() == null) return Color.web(UiTheme.ACCENT_EMERALD);
        if (info.getType() == EnemyType.BEAR_BOSS) return Color.web(UiTheme.ACCENT_CRIMSON);
        if (info.getType() == EnemyType.HUNTER) return Color.web(UiTheme.ACCENT_AMBER);
        return Color.web(UiTheme.ACCENT_AMBER);
    }

    private static String roleBadge(CreatureInfo info) {
        if (info.isPassive()) return "PRESA SILVESTRE";
        if (info.getType() == null) return "SENTINELA · VOCÊ";
        if (info.getType() == EnemyType.BEAR_BOSS) return "CHEFE ALFA";
        if (info.getType() == EnemyType.HUNTER) return "PATRULHEIRO";
        return "FAUNA RIVAL";
    }

    public void stopPreview() {
        previewRenderer.stop();
    }

    public void startPreview() {
        previewRenderer.start();
    }
}
