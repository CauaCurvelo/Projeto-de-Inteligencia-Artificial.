package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.ai.AIState;
import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.world.Burrow;
import br.edu.unex.sentinela.world.Footprint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Coordena a renderização de entidades de mundo (Tocas, Frutas, Carne, Pegadas Direcionadas,
 * Indicadores de Ataque e Nameplates Estilo RPG), delegando a anatomia articulada ao CharacterSpriteRenderer
 * e os ícones/tipografia contornada ao UiIconDrawer.
 */
public class EntityRenderer {
    private final GraphicsContext gc;
    private final CharacterSpriteRenderer spriteRenderer;
    private final UiIconDrawer iconDrawer;

    private final Font nameFontNormal = Font.font("Trebuchet MS", FontWeight.EXTRA_BOLD, 12.5);
    private final Font nameFontBoss = Font.font("Trebuchet MS", FontWeight.EXTRA_BOLD, 14.5);
    private final Font stateSubFont = Font.font("Segoe UI", FontWeight.BOLD, 10.0);
    private final Font alertBadgeFont = Font.font("Impact", FontWeight.BOLD, 15.0);

    public EntityRenderer(GraphicsContext gc) {
        this.gc = gc;
        this.spriteRenderer = new CharacterSpriteRenderer(gc);
        this.iconDrawer = new UiIconDrawer(gc);
    }

    public void renderFootprint(Footprint fp, double fx, double fy, boolean debugOverlay) {
        double alpha = fp.getAlpha() * 0.52;
        gc.save();
        gc.translate(fx, fy);
        gc.rotate(Math.toDegrees(fp.getAngle()));

        gc.setFill(Color.web("#23170D", alpha));

        switch (fp.getStyle()) {
            case BOOT:
                gc.fillRoundRect(-3.0, -1.8, 3.8, 3.6, 1.5, 1.5);
                gc.fillRoundRect(1.2, -2.0, 3.5, 4.0, 2.0, 2.0);
                break;

            case HOOF:
                gc.fillPolygon(new double[]{-2.0, 3.0, -1.0}, new double[]{-2.2, -0.5, -0.3}, 3);
                gc.fillPolygon(new double[]{-2.0, 3.0, -1.0}, new double[]{2.2, 0.5, 0.3}, 3);
                break;

            case PAW:
            default:
                gc.fillOval(-2.2, -2.0, 3.2, 4.0);
                gc.fillOval(1.5, -2.6, 1.8, 1.4);
                gc.fillOval(2.2, -0.7, 1.9, 1.4);
                gc.fillOval(1.5, 1.2, 1.8, 1.4);
                break;
        }

        gc.restore();

        if (debugOverlay && fp.isPlayerOrigin()) {
            gc.setFill(Color.web("#CE93D8", fp.getAlpha() * 0.18));
            gc.fillOval(fx - 4.5, fy - 4.5, 9, 9);
        }
    }

    public void renderBurrow(Burrow b, double bx, double by) {
        gc.setFill(Color.color(0.02, 0.04, 0.01, 0.55));
        gc.fillOval(bx - 16, by - 12, 34, 30);

        gc.setFill(Color.web("#5C3D22"));
        gc.fillOval(bx - 15, by - 14, 30, 28);
        gc.setFill(Color.web("#785230"));
        gc.fillOval(bx - 14, by - 15, 26, 23);

        gc.setFill(Color.web("#2E1D0E"));
        gc.fillOval(bx - 10, by - 10, 20, 20);

        gc.setFill(Color.web("#0D0804"));
        gc.fillOval(bx - 8, by - 8, 16, 16);
    }

    public void drawBerryBush(double fx, double fy, double sunOffsetX, double sunOffsetY) {
        gc.setFill(Color.color(0.02, 0.06, 0.02, 0.45));
        gc.fillOval(fx - 14 + sunOffsetX * 0.45, fy - 6 + sunOffsetY * 0.45, 30, 20);

        gc.setFill(Color.color(0.01, 0.04, 0.01, 0.65));
        gc.fillOval(fx - 12, fy, 24, 12);

        gc.setFill(Color.web("#143D14"));
        gc.fillOval(fx - 14, fy - 13, 28, 25);

        gc.setFill(Color.web("#235E23"));
        gc.fillOval(fx - 12, fy - 14, 23, 21);
        gc.fillOval(fx - 7, fy - 11, 20, 19);

        gc.setFill(Color.web("#388E3C"));
        gc.fillOval(fx - 10, fy - 13, 16, 15);
        gc.setFill(Color.web("#4CAF50", 0.75));
        gc.fillOval(fx - 8, fy - 12, 10, 9);

        drawSingleBerry(fx - 6, fy - 5);
        drawSingleBerry(fx + 3, fy - 4);
        drawSingleBerry(fx - 1, fy + 2);
    }

    private void drawSingleBerry(double bx, double by) {
        gc.setFill(Color.color(0.05, 0.12, 0.05, 0.65));
        gc.fillOval(bx - 2.5, by - 1.5, 6.5, 6.5);
        gc.setFill(Color.web("#B71C1C"));
        gc.fillOval(bx - 3, by - 3, 6, 6);
        gc.setFill(Color.web("#E53935"));
        gc.fillOval(bx - 2.5, by - 2.8, 4.8, 4.8);
        gc.setFill(Color.web("#FFFFFF", 0.90));
        gc.fillOval(bx - 1.5, by - 2.0, 1.8, 1.8);
    }

    public void drawMeatDrop(double mx, double my) {
        gc.setFill(Color.color(0.0, 0.0, 0.0, 0.48));
        gc.fillOval(mx - 10, my - 3, 21, 12);

        gc.setFill(Color.web("#CFD8DC"));
        gc.fillOval(mx - 11, my - 3, 5, 6);
        gc.fillOval(mx + 6, my - 3, 5, 6);
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(mx - 10.5, my - 2.5, 3.5, 4);
        gc.fillOval(mx + 6.5, my - 2.5, 3.5, 4);

        gc.setFill(Color.web("#6E1200"));
        gc.fillOval(mx - 8, my - 6, 16, 12);
        gc.setFill(Color.web("#C62828"));
        gc.fillOval(mx - 7, my - 6, 14, 10);
        gc.setFill(Color.web("#EF5350", 0.75));
        gc.fillOval(mx - 5, my - 5, 9, 5);
    }

    public void drawPlayerFox(Player player, double px, double py) {
        if (player.isBurrowed()) {
            gc.setFill(Color.web("#F39C12", 0.45));
            gc.fillOval(px - 11, py - 11, 22, 22);
            gc.setStroke(Color.web("#FFD54F", 0.80));
            gc.setLineWidth(1.5);
            gc.strokeOval(px - 11, py - 11, 22, 22);
            return;
        }

        if (player.isInvulnerable()) {
            if ((System.currentTimeMillis() / 110) % 2 == 0) {
                gc.setStroke(Color.web("#FF1744", 0.85));
                gc.setLineWidth(2.0);
                gc.strokeOval(px - 18, py - 18, 36, 36);
            }
        }

        if (player.isSwimming()) {
            double ripple = (System.currentTimeMillis() % 900) / 900.0;
            gc.setStroke(Color.web("#81D4FA", 0.78 * (1.0 - ripple)));
            gc.setLineWidth(1.6);
            double rw = 15 + ripple * 9;
            gc.strokeOval(px - rw, py - rw * 0.85, rw * 2, rw * 1.7);
        } else if (player.isHiddenInGrass()) {
            gc.save();
            gc.setStroke(Color.web("#69F0AE", 0.65));
            gc.setLineWidth(1.5);
            gc.setLineDashes(5.0, 4.0);
            gc.strokeOval(px - 16, py - 12, 32, 24);
            gc.restore();
        }

        spriteRenderer.renderFox(player, px, py);
    }

    public void drawClawSlash(Player player, double px, double py) {
        double angle = player.getVisualAngle();
        double slashX = px + Math.cos(angle) * 20;
        double slashY = py + Math.sin(angle) * 20;

        gc.setStroke(Color.web("#FFF275", 0.90));
        gc.setLineWidth(3.5);
        gc.strokeArc(slashX - 16, slashY - 16, 32, 32, -Math.toDegrees(angle) - 50, 100, ArcType.OPEN);

        gc.setStroke(Color.web("#FFFFFF"));
        gc.setLineWidth(2.0);
        gc.strokeArc(slashX - 14, slashY - 14, 28, 28, -Math.toDegrees(angle) - 40, 80, ArcType.OPEN);
    }

    public void renderEnemy(Enemy enemy, double ex, double ey) {
        double angle = enemy.getFaceAngle();

        if (enemy.isFlashing()) {
            gc.setFill(Color.web("#FFFFFF", 0.85));
            gc.fillOval(ex - 20, ey - 20, 40, 40);
        }

        if (enemy.isWindup() || enemy.isLunging() || enemy.getState() == AIState.AIMING) {
            renderUniqueAttackTelegraph(enemy, ex, ey, angle);
        }

        spriteRenderer.renderEnemySprite(enemy, ex, ey);

        if (br.edu.unex.sentinela.settings.GameSettings.get().isShowNameplates()) {
            renderRpgEnemyNameplate(enemy, ex, ey);
        }
    }

    /**
     * Renderiza o novo Nameplate estilo Jogo RPG de Ação:
     * - Tipografia desenhada com contorno preto espesso e preenchimento vibrante (super legível sem caixote preto pesado)
     * - Barra de Vida larga e chanfrada com divisórias de blocos de HP
     * - Brasão Circular de Classe na esquerda da barra (Coroa de Chefe, Mira de Caçador ou Pata de Fera)
     * - Pílula compacta de Estado Mental e Escudo de Alerta (!) flutuante
     */
    private void renderRpgEnemyNameplate(Enemy enemy, double ex, double ey) {
        AIState state = enemy.getState();
        Color stateAccent = TacticalAuraRenderer.resolveTacticalStateColor(state);
        boolean isBoss = enemy.getType() == EnemyType.BEAR_BOSS;
        boolean isAlert = state == AIState.CHASE || state == AIState.AIMING ||
                          state == AIState.FIGHTING_ENEMY || state == AIState.TERRITORIAL_ATTACK;
        boolean isSuspicious = state == AIState.SUSPICIOUS || state == AIState.ALERT || state == AIState.SNIFFING_TRACK;

        String skinName = enemy.getSkin() != null ? enemy.getSkin().getDisplayName() : enemy.getType().name();
        String stateDesc = state.getDescription().toUpperCase();

        // Posicionamento vertical acima do sprite
        double baseOffsetY = isBoss ? 36.0 : 28.0;
        double barW = isBoss ? 78.0 : 54.0;
        double barH = isBoss ? 9.5 : 7.5;
        double barX = ex - barW * 0.5 + 4; // Leve offset para acomodar o brasão na esquerda
        double barY = ey - baseOffsetY;

        // 1. Sombra e Moldura da Barra de Vida Chanfrada
        gc.setFill(Color.color(0, 0, 0, 0.70));
        gc.fillRoundRect(barX - 2, barY - 1.5, barW + 4, barH + 4, 6, 6);

        gc.setFill(Color.web("#1C0D12"));
        gc.fillRoundRect(barX, barY, barW, barH, 5, 5);

        // 2. Preenchimento em Gradiente da Barra de HP
        double hpPct = Math.max(0.0, Math.min(1.0, (double) enemy.getHp() / enemy.getMaxHp()));
        LinearGradient hpGrad = isBoss
            ? new LinearGradient(barX, barY, barX, barY + barH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#E040FB")),
                new Stop(0.5, Color.web("#9C27B0")),
                new Stop(1.0, Color.web("#4A148C")))
            : new LinearGradient(barX, barY, barX, barY + barH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FF5252")),
                new Stop(0.5, Color.web("#D50000")),
                new Stop(1.0, Color.web("#7F0000")));

        gc.setFill(hpGrad);
        gc.fillRoundRect(barX, barY, barW * hpPct, barH, 4, 4);

        // Brilho especular no topo da barra
        gc.setFill(Color.web("#FFFFFF", 0.32));
        gc.fillRoundRect(barX + 1, barY + 0.8, Math.max(0, barW * hpPct - 2), barH * 0.42, 3, 3);

        // 3. Divisórias de Segmentos de HP (Ticks verticais estilo RPG para enxergar quantos golpes faltam)
        int maxHp = enemy.getMaxHp();
        if (maxHp > 1 && maxHp <= 12) {
            gc.setStroke(Color.color(0, 0, 0, 0.65));
            gc.setLineWidth(1.2);
            double segW = barW / maxHp;
            for (int i = 1; i < maxHp; i++) {
                double sx = barX + i * segW;
                gc.strokeLine(sx, barY + 1, sx, barY + barH - 1);
            }
        }

        // Borda metálica da barra de HP
        gc.setStroke(isBoss ? Color.web("#FFD54F", 0.85) : stateAccent.deriveColor(0, 1.0, 1.0, 0.75));
        gc.setLineWidth(1.4);
        gc.strokeRoundRect(barX - 0.5, barY - 0.5, barW + 1, barH + 1, 5, 5);

        // 4. Brasão Circular de Classe na Ponta Esquerda da Barra
        double badgeRadius = isBoss ? 9.5 : 7.8;
        iconDrawer.drawEnemyClassBadge(barX - 2, barY + barH * 0.5, badgeRadius, enemy.getType(), stateAccent);

        // 5. Nome do Personagem / Animal com Fonte Contornada Desenhada (Acima da Barra)
        Color nameFill = isBoss ? Color.web("#FFD54F") : (isAlert ? Color.web("#FF8A80") : Color.web("#FFFFFF"));
        Font activeNameFont = isBoss ? nameFontBoss : nameFontNormal;
        iconDrawer.drawStyledText(
            skinName,
            ex + 2,
            barY - 9.5,
            activeNameFont,
            nameFill,
            Color.web("#080B10"),
            3.4,
            TextAlignment.CENTER
        );

        // 6. Subtítulo Dinâmico do Estado Mental (Abaixo ou Acima com Destaque)
        Color stateTextFill = isAlert ? Color.web("#FF5252") : (isSuspicious ? Color.web("#FFD54F") : Color.web("#CFD8DC"));
        iconDrawer.drawStyledText(
            "‹ " + stateDesc + " ›",
            ex + 2,
            barY + barH + 8.0,
            stateSubFont,
            stateTextFill,
            Color.web("#05070A"),
            2.6,
            TextAlignment.CENTER
        );

        // 7. Insígnia de Alerta Pulsante (! / ?) quando o inimigo detecta ou persegue a raposa
        if (isAlert || isSuspicious) {
            double bounce = Math.sin(System.currentTimeMillis() * 0.014) * 2.5;
            double shieldY = barY - 26.0 + bounce;

            // Losango / Escudo de Alerta Tático
            gc.save();
            gc.translate(ex + 2, shieldY);
            double r = isBoss ? 10.5 : 9.0;
            double[] dx = {0, r, 0, -r};
            double[] dy = {-r * 1.15, 0, r * 1.15, 0};

            gc.setFill(Color.color(0, 0, 0, 0.65));
            gc.fillPolygon(dx, new double[]{-r * 1.15 + 2, 2, r * 1.15 + 2, 2}, 4);

            gc.setFill(isAlert ? Color.web("#D50000") : Color.web("#FF8F00"));
            gc.fillPolygon(dx, dy, 4);

            gc.setStroke(Color.web("#FFF8E1"));
            gc.setLineWidth(1.6);
            gc.strokePolygon(dx, dy, 4);
            gc.restore();

            iconDrawer.drawStyledText(
                isAlert ? "!" : "?",
                ex + 2,
                shieldY,
                alertBadgeFont,
                Color.WHITE,
                Color.BLACK,
                2.5,
                TextAlignment.CENTER
            );
        }
    }

    private void renderUniqueAttackTelegraph(Enemy enemy, double ex, double ey, double angle) {
        EnemyType type = enemy.getType();
        EnemySkin skin = enemy.getSkin();

        if (type == EnemyType.BEAR_BOSS) {
            if (skin == EnemySkin.CROCODILE) {
                double mouthX = ex + Math.cos(angle) * 24;
                double mouthY = ey + Math.sin(angle) * 24;
                gc.setStroke(Color.web("#00E676", 0.90));
                gc.setLineWidth(3.0);
                gc.strokeArc(mouthX - 16, mouthY - 16, 32, 32, -Math.toDegrees(angle) - 50, 100, ArcType.OPEN);
            } else if (skin == EnemySkin.PANTHER) {
                gc.setStroke(Color.web("#FFD700", 0.90));
                gc.setLineWidth(2.5);
                double slashX = ex + Math.cos(angle) * 22;
                double slashY = ey + Math.sin(angle) * 22;
                gc.strokeLine(slashX - 10, slashY - 10, slashX + 10, slashY + 10);
                gc.strokeLine(slashX - 6, slashY - 14, slashX + 14, slashY + 6);
                gc.strokeLine(slashX - 14, slashY - 6, slashX + 6, slashY + 14);
            } else {
                gc.setStroke(Color.web("#E67E22", 0.95));
                gc.setLineWidth(3.5);
                gc.strokeOval(ex - 28, ey - 28, 56, 56);
                gc.setStroke(Color.web("#D35400", 0.60));
                gc.strokeOval(ex - 36, ey - 36, 72, 72);
            }
        } else if (type == EnemyType.HOUND) {
            double biteX = ex + Math.cos(angle) * 18;
            double biteY = ey + Math.sin(angle) * 18;
            gc.setStroke(Color.web("#FF1744", 0.90));
            gc.setLineWidth(2.5);
            gc.strokeArc(biteX - 12, biteY - 12, 24, 24, -Math.toDegrees(angle) - 45, 90, ArcType.OPEN);
        } else if (type == EnemyType.RIVAL) {
            if (skin == EnemySkin.BOAR) {
                double chargeLen = 70.0;
                double targetX = ex + Math.cos(angle) * chargeLen;
                double targetY = ey + Math.sin(angle) * chargeLen;

                LinearGradient chargeGrad = new LinearGradient(
                    ex, ey, targetX, targetY, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#FF1744", 0.15)),
                    new Stop(1.0, Color.web("#FF1744", 0.85))
                );
                gc.setStroke(chargeGrad);
                gc.setLineWidth(12.0);
                gc.strokeLine(ex, ey, targetX, targetY);

                gc.setStroke(Color.web("#FF5252", 0.95));
                gc.setLineWidth(2.0);
                gc.strokeLine(ex, ey, targetX, targetY);

                double arrowAngle1 = angle + Math.PI * 0.82;
                double arrowAngle2 = angle - Math.PI * 0.82;
                gc.strokeLine(targetX, targetY, targetX + Math.cos(arrowAngle1) * 9, targetY + Math.sin(arrowAngle1) * 9);
                gc.strokeLine(targetX, targetY, targetX + Math.cos(arrowAngle2) * 9, targetY + Math.sin(arrowAngle2) * 9);
            } else {
                gc.setStroke(Color.web("#FF9100", 0.75));
                gc.setLineWidth(2.0);
                gc.strokeOval(ex - 18, ey - 18, 36, 36);
            }
        } else {
            double aimLen = 220.0;
            double aimEndX = ex + Math.cos(angle) * aimLen;
            double aimEndY = ey + Math.sin(angle) * aimLen;

            LinearGradient laserGrad = new LinearGradient(
                ex, ey, aimEndX, aimEndY, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FF1744", 0.85)),
                new Stop(0.75, Color.web("#FF1744", 0.45)),
                new Stop(1.0, Color.web("#FF1744", 0.08))
            );

            gc.setStroke(Color.web("#FF1744", 0.18));
            gc.setLineWidth(5.0);
            gc.strokeLine(ex, ey, aimEndX, aimEndY);

            gc.setStroke(laserGrad);
            gc.setLineWidth(1.6);
            gc.strokeLine(ex, ey, aimEndX, aimEndY);

            gc.setStroke(Color.web("#FF5252", 0.90));
            gc.setLineWidth(1.4);
            gc.strokeOval(aimEndX - 6, aimEndY - 6, 12, 12);
            gc.fillOval(aimEndX - 1.5, aimEndY - 1.5, 3, 3);
        }
    }
}
