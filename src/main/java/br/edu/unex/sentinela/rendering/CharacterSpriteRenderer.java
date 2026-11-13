package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.ai.AnimalVariant;
import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/**
 * Renderizador especializado em Silhuetas Anatômicas Direcionadas (Top-Down 2.5D)
 * com animações procedurais em tempo real:
 * - Respiração (Breathing chest expansion)
 * - Ciclo de caminhada articulado (4 patas em pares diagonais ou 2 botas bípedes)
 * - Movimento sinuoso de cauda (Multi-segment tail wag)
 * - Identidade visual única e imediata para cada espécie e personagem
 */
public class CharacterSpriteRenderer {
    private final GraphicsContext gc;
    private final BossSpriteRenderer bossSpriteRenderer;

    public CharacterSpriteRenderer(GraphicsContext gc) {
        this.gc = gc;
        this.bossSpriteRenderer = new BossSpriteRenderer(gc, this);
    }

    /**
     * Renderiza a Raposa Vermelha (Player) com corpo esguio, peito branco, 4 patas pretas animadas,
     * cabeça triangular afilada, orelhas pontudas e cauda longa felpuda em múltiplos segmentos.
     */
    public void renderFox(Player player, double px, double py) {
        double angleDeg = Math.toDegrees(player.getVisualAngle());
        double walk = player.getWalkPhase();
        double breath = Math.sin(player.getBreathTimer() * 3.8) * 0.045;
        boolean moving = player.isMoving();
        double alpha = player.isHiddenInGrass() ? 0.68 : 1.0;

        double stride = moving ? Math.sin(walk) * 5.2 : 0.0;
        double bodySway = moving ? Math.cos(walk) * 1.1 : 0.0;

        gc.save();
        gc.translate(px, py);
        gc.rotate(angleDeg);

        // Sombra elíptica alongada sob o corpo da raposa
        if (!player.isSwimming()) {
            gc.setFill(Color.color(0, 0, 0, 0.38 * alpha));
            gc.fillOval(-15, -8.5, 30, 17);
        }

        // 1. As 4 Patas Pretas da Raposa (Alternando em pares diagonais na caminhada)
        if (!player.isSwimming()) {
            drawQuadrupedLegs(stride, 6.5, 6.5, 5.2, 2.8, Color.web("#1C130C", alpha), Color.web("#0D0804", alpha));
        }

        // 2. Cauda Felpuda Articulada em 3 Segmentos (Sinuosa ao correr e respirando parada)
        double tailSpeed = moving ? walk * 1.15 : player.getBreathTimer() * 2.2;
        double wag1 = Math.sin(tailSpeed) * (moving ? 3.5 : 1.5);
        double wag2 = Math.sin(tailSpeed - 0.9) * (moving ? 6.0 : 2.5);

        // Base da cauda
        gc.setFill(Color.web("#A04000", alpha));
        gc.fillOval(-16, -4.0 + wag1 * 0.4, 11, 8.0);
        // Meio volumoso da cauda
        gc.setFill(Color.web("#D35400", alpha));
        gc.fillOval(-22, -4.8 + wag1, 12, 9.6);
        // Ponta branca clássica da raposa
        gc.setFill(Color.web("#FFF8E1", alpha));
        gc.fillOval(-26, -3.5 + wag2, 7.5, 7.0);

        // 3. Tronco Esguio e Anatômico (Torso + Peito expandindo com a respiração)
        gc.scale(1.0 + breath * 0.4, 1.0 + breath);
        // Dorso externo sombreado
        gc.setFill(Color.web("#BA4A00", alpha));
        gc.fillOval(-12, -7.2 + bodySway * 0.4, 23, 14.4);
        // Pelagem laranja vibrante superior
        gc.setFill(Color.web("#E67E22", alpha));
        gc.fillOval(-10.5, -6.4 + bodySway * 0.4, 20.5, 12.8);
        // Pelagem dourada nos ombros
        gc.setFill(Color.web("#F39C12", 0.85 * alpha));
        gc.fillOval(-3, -5.5 + bodySway * 0.5, 12, 11.0);
        // Colarinho de pelo branco no peito/pescoço
        gc.setFill(Color.web("#FFF3E0", 0.90 * alpha));
        gc.fillOval(4.5, -4.8 + bodySway * 0.5, 7.5, 9.6);

        // 4. Cabeça Triangular de Raposa e Focinho Afilado
        double headBob = moving ? Math.sin(walk * 2.0) * 0.7 : 0.0;
        gc.translate(headBob, bodySway * 0.6);

        // Crânio
        gc.setFill(Color.web("#D35400", alpha));
        gc.fillOval(3.5, -6.2, 11.5, 12.4);
        gc.setFill(Color.web("#E67E22", alpha));
        gc.fillOval(4.5, -5.6, 10.5, 11.2);

        // Bochechas brancas laterais e Focinho cônico longo
        gc.setFill(Color.web("#FFF8E1", alpha));
        gc.fillPolygon(new double[]{9.0, 17.5, 9.0}, new double[]{-5.0, 0.0, 5.0}, 3);
        gc.setFill(Color.web("#E67E22", alpha));
        gc.fillPolygon(new double[]{8.0, 18.2, 8.0}, new double[]{-3.6, 0.0, 3.6}, 3);

        // Ponta do nariz preta
        gc.setFill(Color.web("#17202A", alpha));
        gc.fillOval(16.5, -1.6, 3.2, 3.2);

        // Olhos amendoados atentos
        gc.setFill(Color.web("#111111", alpha));
        gc.fillOval(10.5, -3.8, 2.6, 2.0);
        gc.fillOval(10.5, 1.8, 2.6, 2.0);
        gc.setFill(Color.web("#FFFFFF", alpha));
        gc.fillOval(11.2, -3.5, 1.0, 1.0);
        gc.fillOval(11.2, 2.1, 1.0, 1.0);

        // 5. Orelhas Triangulares Grandes (Pontudas para trás/lados com interior claro)
        // Orelha Esquerda
        gc.setFill(Color.web("#6E2C00", alpha));
        gc.fillPolygon(new double[]{6.5, 2.0, 9.0}, new double[]{-4.2, -10.5, -7.8}, 3);
        gc.setFill(Color.web("#FFE0B2", alpha));
        gc.fillPolygon(new double[]{6.8, 3.5, 8.2}, new double[]{-4.8, -9.0, -7.2}, 3);

        // Orelha Direita
        gc.setFill(Color.web("#6E2C00", alpha));
        gc.fillPolygon(new double[]{6.5, 2.0, 9.0}, new double[]{4.2, 10.5, 7.8}, 3);
        gc.setFill(Color.web("#FFE0B2", alpha));
        gc.fillPolygon(new double[]{6.8, 3.5, 8.2}, new double[]{4.8, 9.0, 7.2}, 3);

        gc.restore();
    }

    /**
     * Despacha o desenho anatômico de cada inimigo de acordo com seu Tipo e Espécie (Skin).
     */
    public void renderEnemySprite(Enemy enemy, double ex, double ey) {
        if (enemy.getType() == EnemyType.BEAR_BOSS) {
            bossSpriteRenderer.renderBossSprite(enemy, ex, ey);
            return;
        }

        double angleDeg = Math.toDegrees(enemy.getFaceAngle());
        double walk = enemy.getWalkPhase();
        double breath = Math.sin(enemy.getBreathTimer() * 3.4) * 0.045;
        boolean moving = enemy.isMoving();

        gc.save();
        gc.translate(ex, ey);
        gc.rotate(angleDeg);

        EnemyType type = enemy.getType();
        EnemySkin skin = enemy.getSkin();
        AnimalVariant variant = enemy.getVariant();
        EnemyPalette palette = EnemyPalette.of(skin, variant);

        if (type == EnemyType.HUNTER) {
            drawBipedHunter(walk, breath, moving, palette);
        } else if (type == EnemyType.HOUND) {
            drawTrackerHound(walk, breath, moving, palette);
        } else if (type == EnemyType.RIVAL) {
            if (skin == EnemySkin.WOLF) {
                drawTimberWolf(walk, breath, moving, palette);
            } else if (skin == EnemySkin.BADGER) {
                drawFierceBadger(walk, breath, moving, palette);
            } else {
                drawWildBoar(walk, breath, moving, palette, variant == AnimalVariant.JUNGLE);
            }
        }

        gc.restore();
    }

    private void drawBipedHunter(double walk, double breath, boolean moving, EnemyPalette p) {
        double stride = moving ? Math.sin(walk) * 5.8 : 0.0;
        double torsoTwist = moving ? Math.cos(walk) * 3.0 : 0.0;

        gc.setFill(Color.color(0, 0, 0, 0.42));
        gc.fillOval(-11, -11, 22, 22);

        gc.setFill(Color.web("#2C1B10"));
        gc.fillRoundRect(-3 + stride, -8.5, 7.5, 4.5, 3, 3);
        gc.fillRoundRect(-3 - stride, 4.0, 7.5, 4.5, 3, 3);

        gc.setFill(p.dark);
        gc.fillRoundRect(-11.5, -6.5, 6.5, 13.0, 4, 4);
        gc.setStroke(Color.web("#D7CCC8"));
        gc.setLineWidth(1.2);
        gc.strokeLine(-10, -4, -13, -6);
        gc.strokeLine(-10, -2, -13, -4);

        gc.save();
        gc.rotate(torsoTwist);
        gc.scale(1.0 + breath * 0.5, 1.0 + breath);

        gc.setFill(p.secondary);
        gc.fillRoundRect(-7.5, -10.5, 15.0, 21.0, 9, 9);
        gc.setFill(p.primary);
        gc.fillRoundRect(-6.0, -9.2, 13.0, 18.4, 8, 8);

        gc.setFill(p.secondary.deriveColor(0, 1.0, 1.15, 1.0));
        gc.fillRoundRect(2.0, -9.5, 10.5, 4.2, 3, 3);
        gc.fillRoundRect(1.0, 5.3, 8.5, 4.2, 3, 3);
        gc.setFill(p.eye);
        gc.fillOval(11.0, -9.0, 3.8, 3.8);
        gc.fillOval(7.5, 5.2, 3.8, 3.8);

        gc.setStroke(Color.web("#D4AC0D"));
        gc.setLineWidth(2.4);
        gc.strokeArc(6.5, -11.5, 11.0, 23.0, -75, 150, ArcType.OPEN);
        gc.setStroke(Color.web("#ECEFF1", 0.85));
        gc.setLineWidth(1.0);
        gc.strokeLine(10.5, -10.0, 7.5, 0.0);
        gc.strokeLine(7.5, 0.0, 10.5, 10.0);
        gc.setStroke(Color.web("#8D6E63"));
        gc.setLineWidth(1.6);
        gc.strokeLine(7.5, 0.0, 20.0, 0.0);
        gc.setFill(Color.web("#CFD8DC"));
        gc.fillPolygon(new double[]{18.5, 23.0, 18.5}, new double[]{-2.2, 0.0, 2.2}, 3);

        gc.setFill(p.dark.darker());
        gc.fillOval(-6.5, -8.0, 13.5, 16.0);
        gc.setFill(p.trim);
        gc.fillOval(-4.2, -5.5, 9.0, 11.0);
        gc.setFill(p.accent);
        gc.fillOval(-3.5, -4.8, 7.6, 9.6);
        gc.setStroke(Color.web("#F5F5F5"));
        gc.setLineWidth(1.4);
        gc.strokeLine(-1.0, -6.5, -6.5, -8.5);

        gc.restore();
    }

    private void drawTrackerHound(double walk, double breath, boolean moving, EnemyPalette p) {
        double stride = moving ? Math.sin(walk) * 5.5 : 0.0;
        double wag = Math.sin(moving ? walk * 1.5 : breath * 10) * 3.5;

        gc.setFill(Color.color(0, 0, 0, 0.38));
        gc.fillOval(-14, -8, 28, 16);

        drawQuadrupedLegs(stride, 7.0, 6.2, 5.5, 3.0, p.secondary, p.dark);

        gc.setStroke(p.secondary);
        gc.setLineWidth(3.0);
        gc.strokeLine(-11, 0, -19, wag);

        gc.scale(1.0 + breath * 0.4, 1.0 + breath);
        gc.setFill(p.secondary);
        gc.fillOval(-12.5, -6.5, 22, 13);
        gc.setFill(p.primary);
        gc.fillOval(-5.5, -7.2, 15.5, 14.4);
        gc.setFill(p.accent);
        gc.fillOval(-10, -4.5, 14, 9);

        gc.setStroke(p.trim);
        gc.setLineWidth(2.8);
        gc.strokeArc(3.5, -6.8, 6.0, 13.6, -70, 140, ArcType.OPEN);
        gc.setFill(Color.web("#F1C40F"));
        gc.fillOval(8.5, -1.5, 2.5, 3.0);

        gc.setFill(p.primary);
        gc.fillOval(5.0, -5.8, 10.5, 11.6);
        gc.setFill(p.secondary);
        gc.fillRoundRect(11.5, -3.6, 7.5, 7.2, 3, 3);
        gc.setFill(Color.web("#111111"));
        gc.fillOval(17.0, -2.0, 3.0, 4.0);

        double earSwing = moving ? Math.sin(walk) * 1.2 : 0.0;
        gc.setFill(p.dark);
        gc.fillOval(5.5 + earSwing, -9.8, 7.5, 4.8);
        gc.fillOval(5.5 - earSwing, 5.0, 7.5, 4.8);

        gc.setFill(p.eye);
        gc.fillOval(11.0, -3.6, 2.2, 2.0);
        gc.fillOval(11.0, 1.6, 2.2, 2.0);
    }

    private void drawTimberWolf(double walk, double breath, boolean moving, EnemyPalette p) {
        double stride = moving ? Math.sin(walk) * 5.6 : 0.0;
        double wag = Math.sin(moving ? walk : breath * 8) * 2.5;

        gc.setFill(Color.color(0, 0, 0, 0.40));
        gc.fillOval(-15, -8.5, 30, 17);

        drawQuadrupedLegs(stride, 7.5, 6.8, 5.8, 3.2, p.dark, p.secondary);

        gc.setFill(p.secondary);
        gc.fillOval(-21, -3.5 + wag * 0.5, 12, 7.0);
        gc.setFill(p.dark);
        gc.fillOval(-24, -2.5 + wag, 6, 5.0);

        gc.scale(1.0 + breath * 0.4, 1.0 + breath);
        gc.setFill(p.secondary);
        gc.fillOval(-13, -7.0, 23, 14);
        gc.setFill(p.accent);
        gc.fillPolygon(new double[]{-4, 8, 10, 8, -4, -7}, new double[]{-8.5, -7.5, 0, 7.5, 8.5, 0}, 6);
        gc.setFill(p.primary);
        gc.fillOval(-9, -5.8, 18, 11.6);

        gc.setFill(p.trim);
        gc.fillOval(4.5, -6.0, 11, 12);
        gc.setFill(p.primary.deriveColor(0, 0.9, 1.12, 1.0));
        gc.fillPolygon(new double[]{9.5, 19.5, 9.5}, new double[]{-4.2, 0.0, 4.2}, 3);
        gc.setFill(Color.web("#111111"));
        gc.fillOval(17.5, -1.6, 3.0, 3.2);

        gc.setFill(p.dark);
        gc.fillPolygon(new double[]{6.5, 2.5, 9.5}, new double[]{-4.2, -9.5, -7.2}, 3);
        gc.fillPolygon(new double[]{6.5, 2.5, 9.5}, new double[]{4.2, 9.5, 7.2}, 3);

        gc.setFill(p.eye);
        gc.fillOval(11.0, -3.6, 2.4, 2.0);
        gc.fillOval(11.0, 1.6, 2.4, 2.0);
    }

    private void drawWildBoar(double walk, double breath, boolean moving, EnemyPalette p, boolean spottedCoat) {
        double stride = moving ? Math.sin(walk) * 4.5 : 0.0;

        gc.setFill(Color.color(0, 0, 0, 0.44));
        gc.fillOval(-15, -10, 30, 20);

        drawQuadrupedLegs(stride, 6.5, 8.2, 5.0, 3.4, p.dark, p.dark.darker());

        gc.setStroke(p.secondary);
        gc.setLineWidth(2.0);
        gc.strokeLine(-12, 0, -16, Math.sin(walk * 2) * 2.5);

        gc.scale(1.0 + breath * 0.5, 1.0 + breath);
        gc.setFill(p.secondary);
        gc.fillOval(-13, -9.8, 25, 19.6);
        gc.setFill(p.primary);
        gc.fillOval(-10, -8.5, 21, 17.0);

        if (spottedCoat) {
            gc.setFill(Color.web("#F5DEB3", 0.72));
            gc.fillOval(-7.5, -6.0, 3.5, 2.8);
            gc.fillOval(-2.0, 3.4, 3.8, 2.8);
            gc.fillOval(1.5, -5.8, 3.2, 2.6);
        }

        gc.setFill(p.accent);
        gc.fillOval(-10, -2.5, 20, 5.0);

        gc.setFill(p.secondary);
        gc.fillOval(5.0, -6.8, 11.5, 13.6);
        gc.setFill(p.trim);
        gc.fillPolygon(new double[]{12.0, 18.5, 14.0}, new double[]{-4.5, -8.5, -2.5}, 3);
        gc.fillPolygon(new double[]{12.0, 18.5, 14.0}, new double[]{4.5, 8.5, 2.5}, 3);

        gc.setFill(p.eye);
        gc.fillRoundRect(14.0, -3.8, 4.0, 7.6, 3, 3);
        gc.setFill(p.dark);
        gc.fillOval(16.0, -2.4, 1.5, 1.5);
        gc.fillOval(16.0, 0.9, 1.5, 1.5);

        gc.setFill(p.secondary.darker());
        gc.fillOval(5.5, -9.5, 5.5, 3.5);
        gc.fillOval(5.5, 6.0, 5.5, 3.5);
    }

    private void drawFierceBadger(double walk, double breath, boolean moving, EnemyPalette p) {
        double stride = moving ? Math.sin(walk) * 4.2 : 0.0;

        gc.setFill(Color.color(0, 0, 0, 0.42));
        gc.fillOval(-14, -9.5, 28, 19);

        drawQuadrupedLegs(stride, 6.5, 7.8, 5.5, 3.5, p.dark, p.eye);

        gc.setFill(p.secondary);
        gc.fillOval(-16, -3.5, 8, 7.0);

        gc.scale(1.0 + breath * 0.4, 1.0 + breath);
        gc.setFill(p.secondary);
        gc.fillOval(-12, -8.8, 23, 17.6);
        gc.setFill(p.primary);
        gc.fillOval(-10, -7.5, 19, 15.0);

        gc.setFill(p.accent);
        gc.fillPolygon(new double[]{3.0, 17.5, 3.0}, new double[]{-6.8, 0.0, 6.8}, 3);
        gc.fillOval(2.0, -6.2, 11.0, 12.4);

        gc.setStroke(p.trim);
        gc.setLineWidth(2.4);
        gc.strokeLine(2.5, -3.2, 14.5, -1.6);
        gc.strokeLine(2.5, 3.2, 14.5, 1.6);

        gc.setFill(Color.web("#000000"));
        gc.fillOval(15.2, -1.8, 3.2, 3.6);
    }

    /**
     * Utilitário compartilhado para desenhar as 4 patas articuladas de quadrúpedes com marcha diagonal realista.
     */
    void drawQuadrupedLegs(double stride, double frontX, double sideY, double legLen, double legW, Color legColor, Color pawColor) {
        // Par Diagonal 1: Dianteira Esquerda (+stride) & Traseira Direita (+stride)
        // Par Diagonal 2: Dianteira Direita (-stride) & Traseira Esquerda (-stride)
        double flX = frontX + stride;
        double frX = frontX - stride;
        double blX = -frontX - stride;
        double brX = -frontX + stride;

        gc.setFill(legColor);
        gc.fillRoundRect(flX - legLen * 0.5, -sideY - legW * 0.5, legLen, legW, 3, 3);
        gc.fillRoundRect(frX - legLen * 0.5, sideY - legW * 0.5, legLen, legW, 3, 3);
        gc.fillRoundRect(blX - legLen * 0.5, -sideY - legW * 0.5, legLen, legW, 3, 3);
        gc.fillRoundRect(brX - legLen * 0.5, sideY - legW * 0.5, legLen, legW, 3, 3);

        // Pontas das patas / garras / cascos
        gc.setFill(pawColor);
        gc.fillOval(flX + legLen * 0.25, -sideY - legW * 0.45, legW * 0.9, legW * 0.9);
        gc.fillOval(frX + legLen * 0.25, sideY - legW * 0.45, legW * 0.9, legW * 0.9);
        gc.fillOval(blX + legLen * 0.25, -sideY - legW * 0.45, legW * 0.9, legW * 0.9);
        gc.fillOval(brX + legLen * 0.25, sideY - legW * 0.45, legW * 0.9, legW * 0.9);
    }
}
