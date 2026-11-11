package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.world.World;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Renderizador do HUD Temático Estilo RPG / Aventura Selvagem:
 * - Escala responsiva proporcional à resolução da tela (uiScale)
 * - Medalhão ilustrado da Raposa reativo ao estado (Camuflada, Nadando, Toca, Ferida)
 * - Corações de Vida 3D vetoriais com pulsação de perigo
 * - Barra de Fome chanfrada com ícone de Coxa de Carne e divisórias
 * - Banner Central de Pontuação e Recorde com Coroa/Troféu Dourado
 * - Painel Direito de Estatísticas com Relógio Solar/Lunar, Caveira de Abates e Teclas Físicas (Keycaps)
 */
public class HudRenderer {
    private final GraphicsContext gc;
    private final UiIconDrawer iconDrawer;

    private final Font labelFont = Font.font("Trebuchet MS", FontWeight.EXTRA_BOLD, 12.5);
    private final Font statusBadgeFont = Font.font("Trebuchet MS", FontWeight.EXTRA_BOLD, 11.5);
    private final Font scoreMainFont = Font.font("Impact", FontWeight.NORMAL, 24.0);
    private final Font scoreSubFont = Font.font("Trebuchet MS", FontWeight.BOLD, 12.5);
    private final Font statValueFont = Font.font("Trebuchet MS", FontWeight.EXTRA_BOLD, 14.5);
    private final Font keycapFont = Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 10.0);
    private final Font hintTextFont = Font.font("Segoe UI", FontWeight.BOLD, 11.0);
    private final Font gameOverTitleFont = Font.font("Impact", FontWeight.NORMAL, 46.0);
    private final Font gameOverBodyFont = Font.font("Trebuchet MS", FontWeight.BOLD, 16.0);

    public HudRenderer(GraphicsContext gc) {
        this.gc = gc;
        this.iconDrawer = new UiIconDrawer(gc);
    }

    public void renderInfiniteHUD(World world, InputManager inputManager, double width, double height) {
        Player player = world.getPlayer();
        double animTime = world.getSurvivalTime();
        double cycleProgress = world.getCycleProgressTimer() / world.getCycleDuration();

        // Escala proporcional suave para telas maiores ou Fullscreen (F11)
        double uiScale = Math.max(1.0, Math.min(width / 960.0, height / 640.0) * 1.05);
        uiScale = Math.min(uiScale, 1.45);

        // ====================================================================
        // 1. PAINEL DO JOGADOR (CANTO SUPERIOR ESQUERDO - MEDALHÃO + BARRAS RPG)
        // ====================================================================
        gc.save();
        gc.scale(uiScale, uiScale);

        double panelX = 20;
        double panelY = 16;
        double panelW = 315;
        double panelH = 92;

        Color leftBorderColor = player.isInvulnerable() ? Color.web("#FF5252") :
                                (player.isHiddenInGrass() ? Color.web("#69F0AE") : Color.web("#D4AC0D", 0.85));
        iconDrawer.drawGamePanel(panelX + 26, panelY, panelW - 26, panelH, 16, leftBorderColor);

        // Medalhão Ilustrado da Raposa à esquerda sobrepondo a borda do painel
        double medX = panelX + 34;
        double medY = panelY + panelH * 0.5;
        iconDrawer.drawFoxPortraitMedallion(medX, medY, 34, player, animTime);

        // Linha 1: Corações de Vida 3D + Badge do Dia/Ciclo Solar
        double contentStartX = panelX + 78;
        boolean lowLife = player.getLives() <= 1;

        for (int i = 0; i < player.getMaxLives(); i++) {
            double hx = contentStartX + 11 + i * 25;
            double hy = panelY + 23;
            iconDrawer.drawStylizedHeart(hx, hy, 21, i < player.getLives(), lowLife, animTime);
        }

        // Mostrador de Dia + Sol/Lua no canto direito do painel do jogador
        double dayBadgeX = panelX + panelW - 82;
        double dayBadgeY = panelY + 11;
        gc.setFill(Color.web("#080B10", 0.75));
        gc.fillRoundRect(dayBadgeX, dayBadgeY, 70, 24, 10, 10);
        gc.setStroke(Color.web("#FFD54F", 0.55));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(dayBadgeX, dayBadgeY, 70, 24, 10, 10);

        iconDrawer.drawSunMoonCycleIcon(dayBadgeX + 15, dayBadgeY + 12, 16, cycleProgress);
        iconDrawer.drawStyledText("DIA " + world.getDayCycleNumber(), dayBadgeX + 46, dayBadgeY + 12,
                labelFont, Color.web("#FFD54F"), Color.BLACK, 2.5, TextAlignment.CENTER);

        // Linha 2: Barra de Fome / Energia com Ícone de Coxa de Carne Assada
        double barX = contentStartX + 24;
        double barY = panelY + 44;
        double barW = 200;
        double barH = 15;
        double hungerPct = Math.max(0.0, Math.min(1.0, player.getHunger() / player.getMaxHunger()));

        iconDrawer.drawMeatHaunchIcon(contentStartX + 10, barY + barH * 0.5, 20);

        // Fundo da barra de fome
        gc.setFill(Color.web("#080B0F"));
        gc.fillRoundRect(barX - 1.5, barY - 1.5, barW + 3, barH + 3, 8, 8);

        // Gradiente dinâmico de fome
        LinearGradient hungerGrad;
        if (hungerPct > 0.45) {
            hungerGrad = new LinearGradient(barX, barY, barX, barY + barH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#69F0AE")),
                new Stop(0.5, Color.web("#00C853")),
                new Stop(1.0, Color.web("#1B5E20")));
        } else if (hungerPct > 0.22) {
            hungerGrad = new LinearGradient(barX, barY, barX, barY + barH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FFD54F")),
                new Stop(0.5, Color.web("#FF8F00")),
                new Stop(1.0, Color.web("#E65100")));
        } else {
            hungerGrad = new LinearGradient(barX, barY, barX, barY + barH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FF5252")),
                new Stop(0.5, Color.web("#D50000")),
                new Stop(1.0, Color.web("#7F0000")));
        }

        gc.setFill(hungerGrad);
        gc.fillRoundRect(barX, barY, barW * hungerPct, barH, 6, 6);

        // Brilho superior na barra de fome
        gc.setFill(Color.web("#FFFFFF", 0.28));
        gc.fillRoundRect(barX + 2, barY + 1, Math.max(0, barW * hungerPct - 4), barH * 0.42, 4, 4);

        // Divisórias de 25% na barra de fome
        gc.setStroke(Color.color(0, 0, 0, 0.55));
        gc.setLineWidth(1.2);
        for (int s = 1; s < 4; s++) {
            double sx = barX + (barW * 0.25 * s);
            gc.strokeLine(sx, barY + 1, sx, barY + barH - 1);
        }

        // Moldura metálica da barra de fome
        gc.setStroke(Color.web("#B0BEC5", 0.70));
        gc.setLineWidth(1.4);
        gc.strokeRoundRect(barX, barY, barW, barH, 6, 6);

        // Texto percentual centralizado na barra de fome
        int hungerInt = (int) Math.ceil(player.getHunger());
        iconDrawer.drawStyledText("NUTRIÇÃO " + hungerInt + "%", barX + barW * 0.5, barY + barH * 0.5,
                Font.font("Trebuchet MS", FontWeight.EXTRA_BOLD, 10.5), Color.WHITE, Color.BLACK, 2.4, TextAlignment.CENTER);

        // Linha 3: Badges de Status Furtivo & Clima Ilustrado
        double statusY = panelY + 75;
        String stealthStatus;
        Color stealthColor;
        if (player.isBurrowed()) {
            stealthStatus = "ABRIGO NA TOCA";
            stealthColor = Color.web("#FFD54F");
        } else if (player.isSwimming()) {
            stealthStatus = "NADANDO NO RIO";
            stealthColor = Color.web("#40C4FF");
        } else if (player.isHiddenInGrass()) {
            stealthStatus = "FURTIVO NA MATA";
            stealthColor = Color.web("#69F0AE");
        } else if (player.isSprinting()) {
            stealthStatus = "CORRIDA RÁPIDA";
            stealthColor = Color.web("#FF8A65");
        } else {
            stealthStatus = "EXPLORANDO";
            stealthColor = Color.web("#CFD8DC");
        }

        // Indicador luminoso de status
        gc.setFill(stealthColor);
        gc.fillOval(contentStartX, statusY - 4, 8, 8);
        iconDrawer.drawStyledText(stealthStatus, contentStartX + 13, statusY,
                statusBadgeFont, stealthColor, Color.BLACK, 2.4, TextAlignment.LEFT);

        // Ícone + Nome do Clima à direita
        double weatherX = panelX + panelW - 95;
        iconDrawer.drawWeatherIcon(weatherX, statusY, 17, world.getCurrentWeather());
        iconDrawer.drawStyledText(world.getCurrentWeather().getDisplayName().toUpperCase(), weatherX + 13, statusY,
                statusBadgeFont, Color.web("#90CAF9"), Color.BLACK, 2.4, TextAlignment.LEFT);

        gc.restore();

        // ====================================================================
        // 2. BANNER CENTRAL DE PONTUAÇÃO E RECORDE (BRASÃO DE TROFÉU)
        // ====================================================================
        gc.save();
        gc.translate(width * 0.5, 0);
        gc.scale(uiScale, uiScale);

        double bannerW = 270;
        double bannerH = 66;
        double bannerX = -bannerW * 0.5;
        double bannerY = 14;

        iconDrawer.drawGamePanel(bannerX, bannerY, bannerW, bannerH, 16, Color.web("#FFD54F", 0.88));

        // Detalhe ornamental no topo do placar
        gc.setFill(Color.web("#FFD54F"));
        gc.fillRoundRect(-45, bannerY - 2, 90, 4, 2, 2);

        // Pontuação Principal Grande com Fonte Impact Contornada
        iconDrawer.drawStyledText(
            String.format("%06d PTS", world.getScore()),
            0,
            bannerY + 24,
            scoreMainFont,
            Color.web("#FFF176"),
            Color.web("#1F1200"),
            3.8,
            TextAlignment.CENTER
        );

        // Faixa Inferior de Recorde com Coroa Dourada Desenhada
        gc.setFill(Color.web("#06090E", 0.70));
        gc.fillRoundRect(bannerX + 24, bannerY + 41, bannerW - 48, 19, 8, 8);

        iconDrawer.drawCrownTrophyIcon(-62, bannerY + 50, 15, Color.web("#FFB300"));
        iconDrawer.drawStyledText(
            String.format("RECORDE: %06d", world.getHighScoreManager().getBestScore()),
            8,
            bannerY + 50.5,
            scoreSubFont,
            Color.web("#FFB74D"),
            Color.BLACK,
            2.4,
            TextAlignment.CENTER
        );

        gc.restore();

        // ====================================================================
        // 3. PAINEL DE CAÇA E COMANDOS (CANTO SUPERIOR DIREITO - ÍCONES + KEYCAPS)
        // ====================================================================
        gc.save();
        gc.translate(width, 0);
        gc.scale(uiScale, uiScale);

        double rightW = 235;
        double rightH = 92;
        double rightX = -rightW - 18;
        double rightY = 16;

        iconDrawer.drawGamePanel(rightX, rightY, rightW, rightH, 16, Color.web("#78909C", 0.75));

        int mins = (int) world.getSurvivalTime() / 60;
        int secs = (int) world.getSurvivalTime() % 60;

        // Bloco 1: Tempo de Sobrevivência + Abates (Com Caveira desenhada)
        double statRowY = rightY + 21;
        // Badge Tempo
        gc.setFill(Color.web("#090D12", 0.75));
        gc.fillRoundRect(rightX + 10, rightY + 9, 104, 25, 8, 8);
        gc.setFill(Color.web("#4FC3F7"));
        gc.fillOval(rightX + 17, statRowY - 6, 12, 12);
        gc.setStroke(Color.web("#090D12"));
        gc.setLineWidth(1.5);
        gc.strokeLine(rightX + 23, statRowY - 4, rightX + 23, statRowY);
        gc.strokeLine(rightX + 23, statRowY, rightX + 26, statRowY);

        iconDrawer.drawStyledText(String.format("%02d:%02d", mins, secs), rightX + 68, statRowY,
                statValueFont, Color.web("#E1F5FE"), Color.BLACK, 2.6, TextAlignment.CENTER);

        // Badge Abates com Caveira
        gc.setFill(Color.web("#090D12", 0.75));
        gc.fillRoundRect(rightX + 120, rightY + 9, 105, 25, 8, 8);
        iconDrawer.drawSkullIcon(rightX + 135, statRowY, 16, Color.web("#FF5252"));
        iconDrawer.drawStyledText(world.getEnemiesDefeated() + " ABATES", rightX + 182, statRowY,
                 Font.font("Trebuchet MS", FontWeight.EXTRA_BOLD, 12.5), Color.web("#FF8A80"), Color.BLACK, 2.6, TextAlignment.CENTER);

        // Bloco 2: Teclas de Comando Desenhadas (Keycaps)
        double keyRow1Y = rightY + 42;
        iconDrawer.drawKeycap("ESPAÇO", rightX + 12, keyRow1Y, 54, 18, keycapFont);
        iconDrawer.drawStyledText("Morder / Atacar", rightX + 74, keyRow1Y + 9,
                hintTextFont, Color.web("#ECEFF1"), Color.BLACK, 2.2, TextAlignment.LEFT);

        double keyRow2Y = rightY + 66;
        iconDrawer.drawKeycap("H", rightX + 12, keyRow2Y, 24, 18, keycapFont);
        iconDrawer.drawStyledText("Auras", rightX + 42, keyRow2Y + 9,
                hintTextFont, Color.web("#B0BEC5"), Color.BLACK, 2.2, TextAlignment.LEFT);

        iconDrawer.drawKeycap("+ / -", rightX + 92, keyRow2Y, 36, 18, keycapFont);
        iconDrawer.drawStyledText("Zoom Câmera", rightX + 134, keyRow2Y + 9,
                hintTextFont, Color.web("#B0BEC5"), Color.BLACK, 2.2, TextAlignment.LEFT);

        gc.restore();

        // ====================================================================
        // 4. TELA DE FIM DE JOGO (MODAL CENTRAL ESTILIZADO)
        // ====================================================================
        if (world.isGameOver()) {
            renderGameOverModal(world, width, height, uiScale);
        }
    }

    private void renderGameOverModal(World world, double width, double height, double uiScale) {
        gc.setFill(Color.color(0.02, 0.03, 0.05, 0.86));
        gc.fillRect(0, 0, width, height);

        gc.save();
        gc.translate(width * 0.5, height * 0.5);
        gc.scale(uiScale, uiScale);

        double cardW = 480;
        double cardH = 300;
        double cardX = -cardW * 0.5;
        double cardY = -cardH * 0.5;

        iconDrawer.drawGamePanel(cardX, cardY, cardW, cardH, 22, Color.web("#FF2A55", 0.90));

        // Ícone de Caveira Grande no Topo do Modal
        iconDrawer.drawSkullIcon(0, cardY + 38, 36, Color.web("#FF5252"));

        iconDrawer.drawStyledText(
            "INSTINTO INTERROMPIDO",
            0,
            cardY + 80,
            gameOverTitleFont,
            Color.web("#FF5252"),
            Color.BLACK,
            4.5,
            TextAlignment.CENTER
        );

        iconDrawer.drawStyledText(
            world.getGameOverReason(),
            0,
            cardY + 118,
            gameOverBodyFont,
            Color.web("#ECEFF1"),
            Color.BLACK,
            2.8,
            TextAlignment.CENTER
        );

        // Caixa interna de resumo da partida
        gc.setFill(Color.web("#070A0F", 0.80));
        gc.fillRoundRect(cardX + 36, cardY + 142, cardW - 72, 86, 14, 14);
        gc.setStroke(Color.web("#FFD54F", 0.45));
        gc.setLineWidth(1.4);
        gc.strokeRoundRect(cardX + 36, cardY + 142, cardW - 72, 86, 14, 14);

        iconDrawer.drawCrownTrophyIcon(-135, cardY + 166, 22, Color.web("#FFD54F"));
        iconDrawer.drawStyledText(
            "PONTUAÇÃO FINAL: " + world.getScore() + "   |   RECORDE: " + world.getHighScoreManager().getBestScore(),
            12,
            cardY + 166,
            Font.font("Trebuchet MS", FontWeight.EXTRA_BOLD, 15.5),
            Color.web("#FFD54F"),
            Color.BLACK,
            2.8,
            TextAlignment.CENTER
        );

        int mins = (int) world.getSurvivalTime() / 60;
        int secs = (int) world.getSurvivalTime() % 60;
        String statsSummary = String.format("Sobreviveu %02d:%02d (Dia %d)  •  %d Frutas  •  %d Predadores Abatidos",
                mins, secs, world.getDayCycleNumber(), world.getApplesEaten(), world.getEnemiesDefeated());

        iconDrawer.drawStyledText(
            statsSummary,
            0,
            cardY + 202,
            Font.font("Segoe UI", FontWeight.BOLD, 13.5),
            Color.web("#B0BEC5"),
            Color.BLACK,
            2.4,
            TextAlignment.CENTER
        );

        // Botão Pulsante de Reiniciar
        double pulse = 1.0 + Math.sin(System.currentTimeMillis() * 0.008) * 0.04;
        gc.save();
        gc.translate(0, cardY + 258);
        gc.scale(pulse, pulse);

        gc.setFill(Color.web("#00C853", 0.25));
        gc.fillRoundRect(-155, -18, 310, 36, 18, 18);
        gc.setStroke(Color.web("#69F0AE"));
        gc.setLineWidth(2.0);
        gc.strokeRoundRect(-155, -18, 310, 36, 18, 18);

        iconDrawer.drawStyledText(
            "PRESSIONE [R] PARA RENASCER NA FLORESTA",
            0,
            0,
            Font.font("Trebuchet MS", FontWeight.EXTRA_BOLD, 14.0),
            Color.web("#69F0AE"),
            Color.BLACK,
            2.8,
            TextAlignment.CENTER
        );
        gc.restore();

        gc.restore();
    }
}
