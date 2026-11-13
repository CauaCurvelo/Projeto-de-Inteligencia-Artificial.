package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.settings.GameSettings;
import br.edu.unex.sentinela.settings.GraphicsQuality;
import br.edu.unex.sentinela.world.WeatherType;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Desenhador especializado de figuras vetoriais da UI, emblemas, retratos, ícones 3D
 * e tipografia de jogo com contorno pesado e sombra projetada (Outlined Game Typography).
 */
public class UiIconDrawer {
    private final GraphicsContext gc;

    public UiIconDrawer(GraphicsContext gc) {
        this.gc = gc;
    }

    /**
     * Desenha texto estilo jogo moderno com sombra projetada e contorno escuro espesso (Stroke + Fill),
     * garantindo máxima legibilidade sobre qualquer fundo.
     */
    private static final Color TEXT_SHADOW_COLOR = Color.color(0, 0, 0, 0.65);
    private static final Color PANEL_SHADOW_COLOR = Color.color(0, 0, 0, 0.55);
    private static final LinearGradient PANEL_BG_GRAD = new LinearGradient(
        0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE,
        new Stop(0.0, Color.web("#1A222D", 0.92)),
        new Stop(0.5, Color.web("#111720", 0.94)),
        new Stop(1.0, Color.web("#0A0E14", 0.96))
    );

    public void drawStyledText(String text, double x, double y, Font font, Color fill, Color outline, double outlineWidth, TextAlignment align) {
        gc.save();
        gc.setFont(font);
        gc.setTextAlign(align);
        gc.setTextBaseline(VPos.CENTER);

        // Sombra projetada inferior via atlas de glifos (fillText)
        gc.setFill(TEXT_SHADOW_COLOR);
        gc.fillText(text, x + 1.5, y + 2.0);

        // Contorno em múltiplas passagens fillText (evita tesselação cara de strokeText no Prism Canvas)
        GraphicsQuality quality = GameSettings.get().getGraphicsQuality();
        if (outlineWidth > 0 && quality != GraphicsQuality.LOW) {
            double d = Math.max(1.0, Math.min(2.0, outlineWidth * 0.45));
            gc.setFill(outline);
            if (quality == GraphicsQuality.HIGH) {
                gc.fillText(text, x - d, y);
                gc.fillText(text, x + d, y);
                gc.fillText(text, x, y - d);
                gc.fillText(text, x, y + d);
            } else {
                gc.fillText(text, x - d, y - d);
                gc.fillText(text, x + d, y + d);
            }
        }

        // Preenchimento principal
        gc.setFill(fill);
        gc.fillText(text, x, y);
        gc.restore();
    }

    /**
     * Desenha uma placa/moldura estilo RPG com chanfro metálico/madeira e fundo em gradiente escuro.
     */
    public void drawGamePanel(double x, double y, double w, double h, double radius, Color borderAccent) {
        // Sombra externa profunda
        gc.setFill(PANEL_SHADOW_COLOR);
        gc.fillRoundRect(x + 3, y + 4, w, h, radius, radius);

        // Fundo em gradiente escuro rico
        gc.setFill(PANEL_BG_GRAD);
        gc.fillRoundRect(x, y, w, h, radius, radius);

        // Brilho interno superior (Bevel)
        gc.setStroke(Color.web("#FFFFFF", 0.12));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x + 1.5, y + 1.5, w - 3, h - 3, radius - 2, radius - 2);

        // Borda principal temática
        gc.setStroke(borderAccent);
        gc.setLineWidth(2.2);
        gc.strokeRoundRect(x, y, w, h, radius, radius);
    }

    /**
     * Desenha o Medalhão de Retrato da Raposa no HUD (reage ao estado: camuflado, nadando, na toca ou ferido).
     */
    public void drawFoxPortraitMedallion(double cx, double cy, double radius, Player player, double animTime) {
        gc.save();

        // Sombra do medalhão
        gc.setFill(Color.color(0, 0, 0, 0.60));
        gc.fillOval(cx - radius + 2, cy - radius + 3, radius * 2, radius * 2);

        // Fundo do medalhão conforme estado da raposa
        Color bgTop = Color.web("#2E4053");
        Color bgBot = Color.web("#17202A");
        Color rimColor = Color.web("#F5B041");

        if (player.isInvulnerable()) {
            bgTop = Color.web("#922B21");
            bgBot = Color.web("#4A120D");
            rimColor = Color.web("#FF5252");
        } else if (player.isHiddenInGrass()) {
            bgTop = Color.web("#1E8449");
            bgBot = Color.web("#0E3E22");
            rimColor = Color.web("#69F0AE");
        } else if (player.isSwimming()) {
            bgTop = Color.web("#1F618D");
            bgBot = Color.web("#0E2F44");
            rimColor = Color.web("#40C4FF");
        } else if (player.isBurrowed()) {
            bgTop = Color.web("#7E5109");
            bgBot = Color.web("#3E2704");
            rimColor = Color.web("#FFD54F");
        }

        LinearGradient medallionBg = new LinearGradient(
            cx, cy - radius, cx, cy + radius, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, bgTop),
            new Stop(1.0, bgBot)
        );
        gc.setFill(medallionBg);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Cabeça da Raposa ilustrada dentro do medalhão (com leve respiração)
        double bob = Math.sin(animTime * 3.2) * 1.2;
        double fy = cy + 2 + bob;
        double s = radius / 26.0;

        gc.save();
        gc.translate(cx, fy);
        gc.scale(s, s);

        // Orelhas grandes pontudas
        gc.setFill(Color.web("#6E2C00"));
        gc.fillPolygon(new double[]{-15, -19, -5}, new double[]{-4, -22, -13}, 3);
        gc.fillPolygon(new double[]{15, 19, 5}, new double[]{-4, -22, -13}, 3);
        gc.setFill(Color.web("#FFE0B2"));
        gc.fillPolygon(new double[]{-13, -16, -7}, new double[]{-6, -18, -12}, 3);
        gc.fillPolygon(new double[]{13, 16, 7}, new double[]{-6, -18, -12}, 3);

        // Rosto triangular da raposa
        gc.setFill(Color.web("#D35400"));
        gc.fillOval(-16, -13, 32, 24);
        gc.setFill(Color.web("#E67E22"));
        gc.fillPolygon(new double[]{-16, 16, 0}, new double[]{-3, -3, 16}, 3);

        // Bochechas brancas peludas
        gc.setFill(Color.web("#FFF8E1"));
        gc.fillPolygon(new double[]{-16, -4, 0, -10}, new double[]{0, 4, 15, 11}, 4);
        gc.fillPolygon(new double[]{16, 4, 0, 10}, new double[]{0, 4, 15, 11}, 4);

        // Olhos expressivos
        gc.setFill(Color.web("#111111"));
        gc.fillOval(-10, -4, 5.5, 5.5);
        gc.fillOval(4.5, -4, 5.5, 5.5);
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(-8.8, -3.2, 2.2, 2.2);
        gc.fillOval(5.7, -3.2, 2.2, 2.2);

        // Focinho preto
        gc.setFill(Color.web("#1C2833"));
        gc.fillOval(-3.5, 11, 7, 5);

        // Se estiver camuflada, desenha uma folhinha na cabeça
        if (player.isHiddenInGrass()) {
            gc.setFill(Color.web("#00E676"));
            gc.fillOval(-4, -20, 12, 6);
        }

        gc.restore();

        if (player.isLowLife()) {
            double pulse = 0.5 + 0.5 * Math.sin(animTime * 8.0);
            gc.setStroke(Color.web("#FF1744", 0.35 + pulse * 0.45));
            gc.setLineWidth(5.5 + pulse * 2.5);
            gc.strokeOval(cx - radius - 2, cy - radius - 2, (radius + 2) * 2, (radius + 2) * 2);
        }

        // Aro metálico duplo do Medalhão
        gc.setStroke(Color.web("#000000", 0.65));
        gc.setLineWidth(4.5);
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        gc.setStroke(rimColor);
        gc.setLineWidth(3.0);
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        gc.setStroke(Color.web("#FFFFFF", 0.35));
        gc.setLineWidth(1.2);
        gc.strokeArc(cx - radius + 2, cy - radius + 2, (radius - 2) * 2, (radius - 2) * 2, 35, 110, ArcType.OPEN);

        gc.restore();
    }

    /**
     * Desenha um Coração de Vida 3D Estilizado (Cheio = Rubi brilhante, Vazio = Pedra escura engastada).
     */
    public void drawStylizedHeart(double cx, double cy, double size, boolean full, boolean lowLifePulse, double animTime) {
        gc.save();
        double scale = (full && lowLifePulse) ? 1.0 + Math.sin(animTime * 9.0) * 0.12 : 1.0;
        gc.translate(cx, cy);
        gc.scale(scale * (size / 20.0), scale * (size / 20.0));

        // Caminho geométrico do coração
        double[] hx = {0, -10, -11, -6, 0, 6, 11, 10};
        double[] hy = {9, -1, -7, -11, -6, -11, -7, -1};

        // Sombra projetada
        gc.setFill(Color.color(0, 0, 0, 0.55));
        gc.fillPolygon(hx, new double[]{11, 1, -5, -9, -4, -9, -5, 1}, 8);

        if (full) {
            LinearGradient heartGrad = new LinearGradient(
                0, -11, 0, 9, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FF5252")),
                new Stop(0.55, Color.web("#D50000")),
                new Stop(1.0, Color.web("#7F0000"))
            );
            gc.setFill(heartGrad);
            gc.fillPolygon(hx, hy, 8);

            // Contorno escuro + Borda dourada fina
            gc.setStroke(Color.web("#2B0505"));
            gc.setLineWidth(2.2);
            gc.strokePolygon(hx, hy, 8);

            // Brilho especular branco no lóbulo esquerdo
            gc.setFill(Color.web("#FFFFFF", 0.78));
            gc.fillOval(-7.5, -8.5, 4.5, 3.0);
        } else {
            // Receptáculo vazio de pedra escura
            gc.setFill(Color.web("#1C242E"));
            gc.fillPolygon(hx, hy, 8);
            gc.setStroke(Color.web("#455A64"));
            gc.setLineWidth(1.8);
            gc.strokePolygon(hx, hy, 8);
        }

        gc.restore();
    }

    /**
     * Desenha um Ícone de Coxa de Carne Assada para a Barra de Fome.
     */
    public void drawMeatHaunchIcon(double cx, double cy, double size) {
        gc.save();
        gc.translate(cx, cy);
        double s = size / 20.0;
        gc.scale(s, s);
        gc.rotate(-25);

        // Osso nas pontas
        gc.setFill(Color.web("#ECEFF1"));
        gc.fillRect(-10, -2, 20, 4);
        gc.fillOval(-12, -4.5, 5, 5);
        gc.fillOval(-12, -0.5, 5, 5);
        gc.fillOval(7, -4.5, 5, 5);
        gc.fillOval(7, -0.5, 5, 5);

        // Carne assada dourada/vermelha
        gc.setFill(Color.web("#7F1D1D"));
        gc.fillOval(-7, -7, 14, 14);
        gc.setFill(Color.web("#DC2626"));
        gc.fillOval(-6, -6.5, 12, 12);
        gc.setFill(Color.web("#FCA5A5", 0.75));
        gc.fillOval(-4, -5, 6, 4);

        gc.setStroke(Color.web("#1F0808"));
        gc.setLineWidth(1.5);
        gc.strokeOval(-7, -7, 14, 14);

        gc.restore();
    }

    /**
     * Desenha o Ícone de Sol ou Lua Crescente conforme o progresso do ciclo Dia/Noite.
     */
    public void drawSunMoonCycleIcon(double cx, double cy, double size, double cycleProgress) {
        gc.save();
        gc.translate(cx, cy);
        double s = size / 20.0;
        gc.scale(s, s);

        boolean isNight = Math.sin(cycleProgress * Math.PI) > 0.45;
        if (!isNight) {
            // Sol Dourado Radiante
            gc.setFill(Color.web("#FF8F00", 0.45));
            gc.fillOval(-10, -10, 20, 20);
            gc.setStroke(Color.web("#FFD54F"));
            gc.setLineWidth(2.0);
            for (int i = 0; i < 8; i++) {
                double a = i * (Math.PI / 4.0);
                gc.strokeLine(Math.cos(a) * 6, Math.sin(a) * 6, Math.cos(a) * 9.5, Math.sin(a) * 9.5);
            }
            gc.setFill(Color.web("#FFD54F"));
            gc.fillOval(-5.5, -5.5, 11, 11);
        } else {
            // Lua Prateada/Azulada com estrelas
            gc.setFill(Color.web("#81D4FA", 0.30));
            gc.fillOval(-9, -9, 18, 18);
            gc.setFill(Color.web("#FFF59D"));
            gc.fillOval(-6.5, -6.5, 13, 13);
            gc.setFill(Color.web("#111720"));
            gc.fillOval(-2.5, -7.5, 11, 11);
        }
        gc.restore();
    }

    /**
     * Desenha o Ícone do Clima Atual (Sol Limpo, Chuva com Nuvem, Neblina).
     */
    public void drawWeatherIcon(double cx, double cy, double size, WeatherType weather) {
        gc.save();
        gc.translate(cx, cy);
        double s = size / 20.0;
        gc.scale(s, s);

        if (weather == WeatherType.RAIN) {
            gc.setFill(Color.web("#90A4AE"));
            gc.fillOval(-8, -6, 10, 8);
            gc.fillOval(-3, -8, 11, 9);
            gc.setStroke(Color.web("#40C4FF"));
            gc.setLineWidth(1.6);
            gc.strokeLine(-4, 3, -6, 8);
            gc.strokeLine(1, 3, -1, 8);
            gc.strokeLine(5, 3, 3, 8);
        } else if (weather == WeatherType.FOG) {
            gc.setStroke(Color.web("#CFD8DC"));
            gc.setLineWidth(2.2);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.strokeLine(-8, -4, 7, -4);
            gc.strokeLine(-6, 1, 9, 1);
            gc.strokeLine(-9, 6, 5, 6);
        } else {
            gc.setFill(Color.web("#FFD54F"));
            gc.fillOval(-5, -5, 10, 10);
            gc.setFill(Color.web("#ECEFF1"));
            gc.fillOval(-2, -2, 10, 7);
        }
        gc.restore();
    }

    /**
     * Desenha um Troféu / Coroa Dourada para o Recorde e Pontuação.
     */
    public void drawCrownTrophyIcon(double cx, double cy, double size, Color mainColor) {
        gc.save();
        gc.translate(cx, cy);
        double s = size / 20.0;
        gc.scale(s, s);

        double[] px = {-8, -9, -4, 0, 4, 9, 8};
        double[] py = {6, -5, 0, -7, 0, -5, 6};

        gc.setFill(Color.color(0, 0, 0, 0.50));
        gc.fillPolygon(px, new double[]{8, -3, 2, -5, 2, -3, 8}, 7);

        gc.setFill(mainColor);
        gc.fillPolygon(px, py, 7);
        gc.setStroke(Color.web("#2E1D04"));
        gc.setLineWidth(1.5);
        gc.strokePolygon(px, py, 7);

        // Joias na coroa
        gc.setFill(Color.web("#FF1744"));
        gc.fillOval(-2, -1, 4, 4);

        gc.restore();
    }

    /**
     * Desenha um Ícone de Caveira de Caça para o contador de Abates.
     */
    public void drawSkullIcon(double cx, double cy, double size, Color tint) {
        gc.save();
        gc.translate(cx, cy);
        double s = size / 20.0;
        gc.scale(s, s);

        gc.setFill(Color.color(0, 0, 0, 0.55));
        gc.fillOval(-6.5, -6.5, 15, 14);

        gc.setFill(tint);
        gc.fillOval(-7, -7.5, 14, 12);
        gc.fillRoundRect(-4.5, 1, 9, 6, 2, 2);

        // Órbitas oculares
        gc.setFill(Color.web("#111720"));
        gc.fillOval(-5, -3.5, 3.8, 3.8);
        gc.fillOval(1.2, -3.5, 3.8, 3.8);
        gc.fillPolygon(new double[]{0, -1.5, 1.5}, new double[]{0, 3, 3}, 3);

        gc.restore();
    }

    /**
     * Desenha o Emblema Circular de Classe ao lado da Barra de Vida dos Inimigos:
     * - BEAR_BOSS: Brasão Roxo/Ouro com Coroa de Chefe
     * - HUNTER: Brasão Carmesim com Retícula de Mira
     * - HOUND / RIVAL: Brasão Selvagem com Marca de Garra/Pata
     */
    public void drawEnemyClassBadge(double cx, double cy, double radius, EnemyType type, Color stateColor) {
        gc.save();

        // Sombra do brasão
        gc.setFill(Color.color(0, 0, 0, 0.65));
        gc.fillOval(cx - radius + 1, cy - radius + 2, radius * 2, radius * 2);

        // Fundo do brasão
        Color badgeBg = (type == EnemyType.BEAR_BOSS) ? Color.web("#311B92") :
                        (type == EnemyType.HUNTER ? Color.web("#5F0F14") : Color.web("#1B2631"));
        gc.setFill(badgeBg);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Anel metálico colorido pelo estado de alerta
        gc.setStroke(type == EnemyType.BEAR_BOSS ? Color.web("#FFD54F") : stateColor);
        gc.setLineWidth(2.0);
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Ícone interno do tipo de inimigo
        if (type == EnemyType.BEAR_BOSS) {
            drawCrownTrophyIcon(cx, cy, radius * 1.35, Color.web("#FFD54F"));
        } else if (type == EnemyType.HUNTER) {
            // Mira de Caçador
            gc.setStroke(Color.web("#FF5252"));
            gc.setLineWidth(1.5);
            double r = radius * 0.55;
            gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
            gc.strokeLine(cx - r * 1.4, cy, cx + r * 1.4, cy);
            gc.strokeLine(cx, cy - r * 1.4, cx, cy + r * 1.4);
        } else {
            // Garra / Pata de Fera Selvagem
            gc.setFill(stateColor);
            gc.fillOval(cx - 2.5, cy - 0.5, 5.0, 4.2);
            gc.fillOval(cx - 4.2, cy - 4.0, 2.2, 2.8);
            gc.fillOval(cx - 1.1, cy - 5.0, 2.2, 3.0);
            gc.fillOval(cx + 2.0, cy - 4.0, 2.2, 2.8);
        }

        gc.restore();
    }

    /**
     * Desenha uma tecla física estilizada (Keycap) para dicas de controle.
     */
    public void drawKeycap(String keyLabel, double x, double y, double w, double h, Font font) {
        gc.setFill(Color.web("#0A0E14"));
        gc.fillRoundRect(x, y + 2, w, h, 6, 6);

        gc.setFill(Color.web("#263238"));
        gc.fillRoundRect(x, y, w, h, 6, 6);

        gc.setStroke(Color.web("#78909C"));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(x, y, w, h, 6, 6);

        drawStyledText(keyLabel, x + w * 0.5, y + h * 0.5, font, Color.web("#FFD54F"), Color.BLACK, 2.0, TextAlignment.CENTER);
    }

    /**
     * Desenha um ícone estilizado de raio para a barra de Sprint/Stamina.
     */
    public void drawSprintBoltIcon(double cx, double cy, double size, boolean sprinting,
                                   boolean exhausted, boolean hungerBlocked, double animTime) {
        gc.save();
        gc.translate(cx, cy);
        double pulse = sprinting ? (1.0 + 0.12 * Math.sin(animTime * 16.0)) : 1.0;
        double s = (size / 20.0) * pulse;
        gc.scale(s, s);

        double[] bx = {2.5, -6.0, 0.2, -3.0, 6.8, 0.8};
        double[] by = {-9.0, 1.0, 1.0, 9.0, -2.0, -2.0};

        gc.setFill(Color.color(0, 0, 0, 0.6));
        gc.fillPolygon(new double[]{3.5, -5.0, 1.2, -2.0, 7.8, 1.8},
                       new double[]{-7.5, 2.5, 2.5, 10.5, -0.5, -0.5}, 6);

        Color topColor;
        Color bottomColor;
        if (hungerBlocked) {
            topColor = Color.web("#FF8A65");
            bottomColor = Color.web("#B71C1C");
        } else if (exhausted) {
            double warn = 0.5 + 0.5 * Math.sin(animTime * 10.0);
            topColor = Color.web("#CFD8DC").interpolate(Color.web("#FF5252"), warn);
            bottomColor = Color.web("#546E7A");
        } else if (sprinting) {
            topColor = Color.web("#E0F7FA");
            bottomColor = Color.web("#00E5FF");
        } else {
            topColor = Color.web("#81D4FA");
            bottomColor = Color.web("#0288D1");
        }

        gc.setFill(new LinearGradient(0, -9, 0, 9, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, topColor), new Stop(1.0, bottomColor)));
        gc.fillPolygon(bx, by, 6);

        gc.setStroke(Color.web("#0A1018"));
        gc.setLineWidth(1.8);
        gc.strokePolygon(bx, by, 6);

        gc.restore();
    }

    /**
     * Desenha uma barra de medição estilizada 3D no HUD com gradiente, brilho e marcador opcional de limiar.
     */
    public void drawHudStatBar(
            double x, double y, double w, double h,
            double pct,
            Color gradTop, Color gradBottom,
            Color borderColor,
            Double thresholdMarkerPct,
            String label,
            Font labelFont,
            Color labelColor
    ) {
        double clampedPct = Math.max(0.0, Math.min(1.0, pct));

        gc.setFill(Color.web("#0A0F14", 0.92));
        gc.fillRoundRect(x, y, w, h, h, h);

        if (clampedPct > 0.0) {
            gc.save();
            gc.beginPath();
            gc.rect(x, y, w * clampedPct, h);
            gc.clip();

            gc.setFill(new LinearGradient(0, y, 0, y + h, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, gradTop), new Stop(1.0, gradBottom)));
            gc.fillRoundRect(x, y, w, h, h, h);

            gc.setFill(Color.web("#FFFFFF", 0.22));
            gc.fillRoundRect(x + 2, y + 1.5, Math.max(0, w - 4), h * 0.35, h * 0.5, h * 0.5);
            gc.restore();
        }

        if (thresholdMarkerPct != null && thresholdMarkerPct > 0.0 && thresholdMarkerPct < 1.0) {
            double mx = x + w * thresholdMarkerPct;
            gc.setStroke(Color.web("#FFF59D", 0.9));
            gc.setLineWidth(1.6);
            gc.strokeLine(mx, y + 2, mx, y + h - 2);
        }

        gc.setStroke(borderColor);
        gc.setLineWidth(1.4);
        gc.strokeRoundRect(x, y, w, h, h, h);

        drawStyledText(label, x + w * 0.5, y + h * 0.5,
                labelFont, labelColor, Color.BLACK, 2.0, TextAlignment.CENTER);
    }

    /**
     * Desenha uma vinheta carmesim sutil e pulsante nas bordas da tela durante estado de vida crítica.
     */
    public void drawLowLifeEdgeVignette(double width, double height, double animTime) {
        double pulse = 0.5 + 0.5 * Math.sin(animTime * 7.5);
        double alpha = 0.14 + pulse * 0.16;
        Color edge = Color.web("#D50000", alpha);
        Color clear = Color.web("#D50000", 0.0);
        double bandH = Math.min(72.0, height * 0.09);
        double bandW = Math.min(72.0, width * 0.07);

        gc.setFill(new LinearGradient(0, 0, 0, bandH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, edge), new Stop(1.0, clear)));
        gc.fillRect(0, 0, width, bandH);

        gc.setFill(new LinearGradient(0, height, 0, height - bandH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, edge), new Stop(1.0, clear)));
        gc.fillRect(0, height - bandH, width, bandH);

        gc.setFill(new LinearGradient(0, 0, bandW, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, edge), new Stop(1.0, clear)));
        gc.fillRect(0, 0, bandW, height);

        gc.setFill(new LinearGradient(width, 0, width - bandW, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, edge), new Stop(1.0, clear)));
        gc.fillRect(width - bandW, 0, bandW, height);
    }

    private static final Font METER_BAR_FONT = Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 9.5);

    /**
     * Desenha a barra completa de Sprint/Stamina no HUD, refletindo corrida, exaustão e bloqueio por fome.
     */
    public void drawPlayerSprintBar(Player player, double contentStartX, double barX, double y, double w, double h, double animTime) {
        double maxStam = Math.max(1.0, player.getMaxStamina());
        double pct = Math.max(0.0, Math.min(1.0, player.getStamina() / maxStam));
        boolean sprinting = player.isSprinting();
        boolean exhausted = player.isSprintExhausted();
        boolean hungerBlocked = player.isSprintBlockedByHunger();
        boolean attackAlert = player.isAttackBlockedByStaminaAlert();

        drawSprintBoltIcon(contentStartX + 10, y + h * 0.5, 18, sprinting,
                exhausted || attackAlert, hungerBlocked && !attackAlert, animTime);

        Color gradTop;
        Color gradBottom;
        Color borderColor;
        Color textColor;
        Double thresholdMarker = null;
        String label;

        int pctInt = (int) Math.round(pct * 100);
        if (attackAlert) {
            double flash = 0.5 + 0.5 * Math.sin(animTime * 28.0);
            gradTop = Color.web("#FF7043");
            gradBottom = Color.web("#8E0000");
            borderColor = Color.web("#FF1744").interpolate(Color.web("#FFD54F"), flash);
            textColor = Color.web("#FFF8E1");
            thresholdMarker = exhausted
                    ? (Player.STAMINA_EXHAUSTION_RECOVERY_THRESHOLD / maxStam)
                    : (player.getAttackStaminaCost() / maxStam);
            label = exhausted ? ("EXAUSTO " + pctInt + "%") : "SEM STAMINA!";
        } else if (hungerBlocked) {
            gradTop = Color.web("#D84315");
            gradBottom = Color.web("#5D1B09");
            borderColor = Color.web("#FF7043", 0.9);
            textColor = Color.web("#FFCCBC");
            label = "FOME • SPRINT BLOQ.";
        } else if (exhausted) {
            double warnPulse = 0.5 + 0.5 * Math.sin(animTime * 10.0);
            gradTop = Color.web("#EF5350");
            gradBottom = Color.web("#7F1D1D");
            borderColor = Color.web("#FF5252", 0.55 + warnPulse * 0.45);
            textColor = Color.web("#FFCDD2");
            thresholdMarker = Player.STAMINA_EXHAUSTION_RECOVERY_THRESHOLD / maxStam;
            label = "EXAUSTO " + pctInt + "%";
        } else if (sprinting) {
            gradTop = Color.web("#80F3FF");
            gradBottom = Color.web("#0091EA");
            borderColor = Color.web("#84FFFF", 0.95);
            textColor = Color.WHITE;
            label = "CORRENDO " + pctInt + "%";
        } else {
            gradTop = Color.web("#29B6F6");
            gradBottom = Color.web("#0277BD");
            borderColor = Color.web("#4FC3F7", 0.7);
            textColor = Color.web("#E1F5FE");
            label = "SPRINT " + pctInt + "%";
        }

        drawHudStatBar(barX, y, w, h, pct, gradTop, gradBottom, borderColor,
                thresholdMarker, label, METER_BAR_FONT, textColor);
    }
}
