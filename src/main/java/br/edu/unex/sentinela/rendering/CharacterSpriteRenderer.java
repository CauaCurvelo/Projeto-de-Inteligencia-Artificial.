package br.edu.unex.sentinela.rendering;

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

    public CharacterSpriteRenderer(GraphicsContext gc) {
        this.gc = gc;
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
        double angleDeg = Math.toDegrees(enemy.getFaceAngle());
        double walk = enemy.getWalkPhase();
        double breath = Math.sin(enemy.getBreathTimer() * 3.4) * 0.045;
        boolean moving = enemy.isMoving();

        gc.save();
        gc.translate(ex, ey);
        gc.rotate(angleDeg);

        EnemyType type = enemy.getType();
        EnemySkin skin = enemy.getSkin();

        if (type == EnemyType.HUNTER) {
            drawBipedHunter(walk, breath, moving);
        } else if (type == EnemyType.HOUND) {
            drawTrackerHound(walk, breath, moving);
        } else if (type == EnemyType.RIVAL) {
            if (skin == EnemySkin.WOLF) {
                drawTimberWolf(walk, breath, moving);
            } else if (skin == EnemySkin.BADGER) {
                drawFierceBadger(walk, breath, moving);
            } else {
                drawWildBoar(walk, breath, moving);
            }
        } else if (type == EnemyType.BEAR_BOSS) {
            if (skin == EnemySkin.CROCODILE) {
                drawRiverCrocodile(walk, breath, moving);
            } else if (skin == EnemySkin.PANTHER) {
                drawShadowPanther(walk, breath, moving);
            } else {
                drawAlphaGrizzlyBear(walk, breath, moving);
            }
        }

        gc.restore();
    }

    /**
     * CAÇADOR HUMANO (Bípede): Botas alternando marcha, mochila de couro, sobretudo de caça,
     * braços segurando arco recurvo longo com flecha pronta e chapéu de aba larga.
     */
    private void drawBipedHunter(double walk, double breath, boolean moving) {
        double stride = moving ? Math.sin(walk) * 5.8 : 0.0;
        double torsoTwist = moving ? Math.cos(walk) * 3.0 : 0.0;

        // Sombra no solo
        gc.setFill(Color.color(0, 0, 0, 0.42));
        gc.fillOval(-11, -11, 22, 22);

        // 1. Botas de Couro Marchando (Bípede: Esquerda e Direita opostas)
        gc.setFill(Color.web("#2C1B10"));
        gc.fillRoundRect(-3 + stride, -8.5, 7.5, 4.5, 3, 3);
        gc.fillRoundRect(-3 - stride, 4.0, 7.5, 4.5, 3, 3);

        // 2. Mochila de Caça nas Costas (Aljava com flechas)
        gc.setFill(Color.web("#4E342E"));
        gc.fillRoundRect(-11.5, -6.5, 6.5, 13.0, 4, 4);
        gc.setStroke(Color.web("#D7CCC8"));
        gc.setLineWidth(1.2);
        gc.strokeLine(-10, -4, -13, -6);
        gc.strokeLine(-10, -2, -13, -4);

        // 3. Ombros Largos e Casaco de Caçador (Com respiração e rotação de tronco)
        gc.save();
        gc.rotate(torsoTwist);
        gc.scale(1.0 + breath * 0.5, 1.0 + breath);

        // Casaco Carmesim/Couro Escuro
        gc.setFill(Color.web("#5A1818"));
        gc.fillRoundRect(-7.5, -10.5, 15.0, 21.0, 9, 9);
        gc.setFill(Color.web("#8E2424"));
        gc.fillRoundRect(-6.0, -9.2, 13.0, 18.4, 8, 8);

        // Braços estendidos segurando o Arco
        gc.setFill(Color.web("#6E1C1C"));
        gc.fillRoundRect(2.0, -9.5, 10.5, 4.2, 3, 3); // Braço esquerdo
        gc.fillRoundRect(1.0, 5.3, 8.5, 4.2, 3, 3);   // Braço direito puxando a corda
        // Mãos com luvas
        gc.setFill(Color.web("#D7CCC8"));
        gc.fillOval(11.0, -9.0, 3.8, 3.8);
        gc.fillOval(7.5, 5.2, 3.8, 3.8);

        // 4. Arco Longo Recurvo de Madeira e Flecha com Ponta de Aço
        gc.setStroke(Color.web("#D4AC0D"));
        gc.setLineWidth(2.4);
        gc.strokeArc(6.5, -11.5, 11.0, 23.0, -75, 150, ArcType.OPEN);
        // Corda tensionada e Flecha
        gc.setStroke(Color.web("#ECEFF1", 0.85));
        gc.setLineWidth(1.0);
        gc.strokeLine(10.5, -10.0, 7.5, 0.0);
        gc.strokeLine(7.5, 0.0, 10.5, 10.0);
        // Haste da flecha e ponta metálica
        gc.setStroke(Color.web("#8D6E63"));
        gc.setLineWidth(1.6);
        gc.strokeLine(7.5, 0.0, 20.0, 0.0);
        gc.setFill(Color.web("#CFD8DC"));
        gc.fillPolygon(new double[]{18.5, 23.0, 18.5}, new double[]{-2.2, 0.0, 2.2}, 3);

        // 5. Chapéu de Caçador de Aba Larga (Slouch Hat)
        gc.setFill(Color.web("#2E1B0E"));
        gc.fillOval(-6.5, -8.0, 13.5, 16.0);
        // Faixa vermelha da copa do chapéu
        gc.setFill(Color.web("#C0392B"));
        gc.fillOval(-4.2, -5.5, 9.0, 11.0);
        // Copa central iluminada
        gc.setFill(Color.web("#4E342E"));
        gc.fillOval(-3.5, -4.8, 7.6, 9.6);
        // Pena branca presa na lateral do chapéu
        gc.setStroke(Color.web("#F5F5F5"));
        gc.setLineWidth(1.4);
        gc.strokeLine(-1.0, -6.5, -6.5, -8.5);

        gc.restore();
    }

    /**
     * CÃO RASTREADOR (Sabujo / Bloodhound): Corpo esguio e musculoso, coleira cravada vermelha,
     * orelhas longas caídas nas laterais, focinho quadrado farejador e cauda fina erguida.
     */
    private void drawTrackerHound(double walk, double breath, boolean moving) {
        double stride = moving ? Math.sin(walk) * 5.5 : 0.0;
        double wag = Math.sin(moving ? walk * 1.5 : breath * 10) * 3.5;

        gc.setFill(Color.color(0, 0, 0, 0.38));
        gc.fillOval(-14, -8, 28, 16);

        drawQuadrupedLegs(stride, 7.0, 6.2, 5.5, 3.0, Color.web("#3B1E47"), Color.web("#23112B"));

        // Cauda fina curvada de cão de caça
        gc.setStroke(Color.web("#4A235A"));
        gc.setLineWidth(3.0);
        gc.strokeLine(-11, 0, -19, wag);

        // Corpo atlético (Garupa estreita + Peito forte)
        gc.scale(1.0 + breath * 0.4, 1.0 + breath);
        gc.setFill(Color.web("#4A235A"));
        gc.fillOval(-12.5, -6.5, 22, 13);
        gc.setFill(Color.web("#6C3483"));
        gc.fillOval(-5.5, -7.2, 15.5, 14.4);
        // Mancha dorsal escura de sabujo
        gc.setFill(Color.web("#2E1236"));
        gc.fillOval(-10, -4.5, 14, 9);

        // Coleira Vermelha com Cravos Metálicos (Identificador claro de Cão de Caça)
        gc.setStroke(Color.web("#E74C3C"));
        gc.setLineWidth(2.8);
        gc.strokeArc(3.5, -6.8, 6.0, 13.6, -70, 140, ArcType.OPEN);
        gc.setFill(Color.web("#F1C40F"));
        gc.fillOval(8.5, -1.5, 2.5, 3.0);

        // Cabeça e Focinho Quadrado Farejador
        gc.setFill(Color.web("#5B2C6F"));
        gc.fillOval(5.0, -5.8, 10.5, 11.6);
        gc.setFill(Color.web("#4A235A"));
        gc.fillRoundRect(11.5, -3.6, 7.5, 7.2, 3, 3);
        gc.setFill(Color.web("#111111"));
        gc.fillOval(17.0, -2.0, 3.0, 4.0);

        // Orelhas Longas Caídas (Floppy Hound Ears balançando nas laterais)
        double earSwing = moving ? Math.sin(walk) * 1.2 : 0.0;
        gc.setFill(Color.web("#260E2E"));
        gc.fillOval(5.5 + earSwing, -9.8, 7.5, 4.8);
        gc.fillOval(5.5 - earSwing, 5.0, 7.5, 4.8);

        // Olhos amarelos ferozes
        gc.setFill(Color.web("#FFEB3B"));
        gc.fillOval(11.0, -3.6, 2.2, 2.0);
        gc.fillOval(11.0, 1.6, 2.2, 2.0);
    }

    /**
     * LOBO DA FLORESTA (WOLF): Juba cinza-aço volumosa nos ombros, orelhas afiadas,
     * focinho comprido cinzento e cauda felpuda baixa.
     */
    private void drawTimberWolf(double walk, double breath, boolean moving) {
        double stride = moving ? Math.sin(walk) * 5.6 : 0.0;
        double wag = Math.sin(moving ? walk : breath * 8) * 2.5;

        gc.setFill(Color.color(0, 0, 0, 0.40));
        gc.fillOval(-15, -8.5, 30, 17);

        drawQuadrupedLegs(stride, 7.5, 6.8, 5.8, 3.2, Color.web("#263238"), Color.web("#37474F"));

        // Cauda espessa de lobo
        gc.setFill(Color.web("#37474F"));
        gc.fillOval(-21, -3.5 + wag * 0.5, 12, 7.0);
        gc.setFill(Color.web("#263238"));
        gc.fillOval(-24, -2.5 + wag, 6, 5.0);

        // Tronco e Juba Eriçada nos Ombros
        gc.scale(1.0 + breath * 0.4, 1.0 + breath);
        gc.setFill(Color.web("#37474F"));
        gc.fillOval(-13, -7.0, 23, 14);
        // Juba prateada/cinza-claro nos ombros (distingue imediatamente o Lobo)
        gc.setFill(Color.web("#78909C"));
        gc.fillPolygon(new double[]{-4, 8, 10, 8, -4, -7}, new double[]{-8.5, -7.5, 0, 7.5, 8.5, 0}, 6);
        gc.setFill(Color.web("#546E7A"));
        gc.fillOval(-9, -5.8, 18, 11.6);

        // Cabeça e Focinho Longo
        gc.setFill(Color.web("#455A64"));
        gc.fillOval(4.5, -6.0, 11, 12);
        gc.setFill(Color.web("#607D8B"));
        gc.fillPolygon(new double[]{9.5, 19.5, 9.5}, new double[]{-4.2, 0.0, 4.2}, 3);
        gc.setFill(Color.web("#111111"));
        gc.fillOval(17.5, -1.6, 3.0, 3.2);

        // Orelhas pontudas cinza-escuro
        gc.setFill(Color.web("#263238"));
        gc.fillPolygon(new double[]{6.5, 2.5, 9.5}, new double[]{-4.2, -9.5, -7.2}, 3);
        gc.fillPolygon(new double[]{6.5, 2.5, 9.5}, new double[]{4.2, 9.5, 7.2}, 3);

        // Olhos âmbar brilhantes
        gc.setFill(Color.web("#FFC107"));
        gc.fillOval(11.0, -3.6, 2.4, 2.0);
        gc.fillOval(11.0, 1.6, 2.4, 2.0);
    }

    /**
     * JAVALI SELVAGEM (BOAR): Corpo robusto em barril, crina dorsal escura, focinho achatado
     * e duas presas brancas curvas salientes (Tusks).
     */
    private void drawWildBoar(double walk, double breath, boolean moving) {
        double stride = moving ? Math.sin(walk) * 4.5 : 0.0;

        gc.setFill(Color.color(0, 0, 0, 0.44));
        gc.fillOval(-15, -10, 30, 20);

        // Patas curtas com cascos pretos
        drawQuadrupedLegs(stride, 6.5, 8.2, 5.0, 3.4, Color.web("#271914"), Color.web("#1A100C"));

        // Rabinho curto de javali
        gc.setStroke(Color.web("#3E2723"));
        gc.setLineWidth(2.0);
        gc.strokeLine(-12, 0, -16, Math.sin(walk * 2) * 2.5);

        // Corpo em formato de barril pesado
        gc.scale(1.0 + breath * 0.5, 1.0 + breath);
        gc.setFill(Color.web("#4E342E"));
        gc.fillOval(-13, -9.8, 25, 19.6);
        gc.setFill(Color.web("#6D4C41"));
        gc.fillOval(-10, -8.5, 21, 17.0);

        // Crina Dorsal Preta Eriçada ao longo da coluna vertebral
        gc.setFill(Color.web("#21120E"));
        gc.fillOval(-10, -2.5, 20, 5.0);

        // Cabeça Cônica Pesada e Focinho Achatado
        gc.setFill(Color.web("#4E342E"));
        gc.fillOval(5.0, -6.8, 11.5, 13.6);
        // Presas Brancas Curvas Grandes (Tusks laterais)
        gc.setFill(Color.web("#FFF8E1"));
        gc.fillPolygon(new double[]{12.0, 18.5, 14.0}, new double[]{-4.5, -8.5, -2.5}, 3);
        gc.fillPolygon(new double[]{12.0, 18.5, 14.0}, new double[]{4.5, 8.5, 2.5}, 3);

        // Disco do focinho de porco do mato
        gc.setFill(Color.web("#8D6E63"));
        gc.fillRoundRect(14.0, -3.8, 4.0, 7.6, 3, 3);
        gc.setFill(Color.web("#271914"));
        gc.fillOval(16.0, -2.4, 1.5, 1.5);
        gc.fillOval(16.0, 0.9, 1.5, 1.5);

        // Orelhas pequenas laterais
        gc.setFill(Color.web("#3E2723"));
        gc.fillOval(5.5, -9.5, 5.5, 3.5);
        gc.fillOval(5.5, 6.0, 5.5, 3.5);
    }

    /**
     * TEXUGO FEROZ (BADGER): Corpo baixo e achatado, garras brancas de escavação
     * e as icônicas listras brancas e pretas longitudinais da cabeça ao dorso.
     */
    private void drawFierceBadger(double walk, double breath, boolean moving) {
        double stride = moving ? Math.sin(walk) * 4.2 : 0.0;

        gc.setFill(Color.color(0, 0, 0, 0.42));
        gc.fillOval(-14, -9.5, 28, 19);

        // Patas fortes com garras claras
        drawQuadrupedLegs(stride, 6.5, 7.8, 5.5, 3.5, Color.web("#1C262B"), Color.web("#CFD8DC"));

        // Cauda curta e grossa
        gc.setFill(Color.web("#37474F"));
        gc.fillOval(-16, -3.5, 8, 7.0);

        // Corpo largo e rasteiro cinza-carvão
        gc.scale(1.0 + breath * 0.4, 1.0 + breath);
        gc.setFill(Color.web("#263238"));
        gc.fillOval(-12, -8.8, 23, 17.6);
        gc.setFill(Color.web("#455A64"));
        gc.fillOval(-10, -7.5, 19, 15.0);

        // Cabeça Preta com Listras Brancas Longitudinais Inconfundíveis
        gc.setFill(Color.web("#11171A"));
        gc.fillPolygon(new double[]{3.0, 17.5, 3.0}, new double[]{-6.8, 0.0, 6.8}, 3);
        gc.fillOval(2.0, -6.2, 11.0, 12.4);

        // Listras brancas faciais do Texugo
        gc.setStroke(Color.web("#FFFFFF"));
        gc.setLineWidth(2.4);
        gc.strokeLine(2.5, -3.2, 14.5, -1.6);
        gc.strokeLine(2.5, 3.2, 14.5, 1.6);

        // Focinho
        gc.setFill(Color.web("#000000"));
        gc.fillOval(15.2, -1.8, 3.2, 3.6);
    }

    /**
     * CHEFE: URSO PARDO ALFA (BEAR_BOSS): Silhueta colossal, patas enormes com garras,
     * corcunda muscular nos ombros, cabeça maciça e orelhas redondas.
     */
    private void drawAlphaGrizzlyBear(double walk, double breath, boolean moving) {
        double stride = moving ? Math.sin(walk) * 6.5 : 0.0;
        double sway = moving ? Math.cos(walk) * 1.6 : 0.0;

        // Sombra pesada
        gc.setFill(Color.color(0, 0, 0, 0.46));
        gc.fillOval(-22, -15, 44, 30);

        // 4 Patas Colossais de Urso com Garras
        drawQuadrupedLegs(stride, 10.5, 12.0, 9.0, 5.5, Color.web("#2B1707"), Color.web("#D7CCC8"));

        // Rabo curto redondo
        gc.setFill(Color.web("#38200D"));
        gc.fillOval(-22, -4.0, 7, 8);

        // Corpo Maciço e Corcunda Muscular (Shoulder Hump)
        gc.scale(1.0 + breath * 0.5, 1.0 + breath);
        gc.setFill(Color.web("#38200D"));
        gc.fillOval(-19, -14.0 + sway * 0.4, 36, 28.0);
        gc.setFill(Color.web("#4E2C12"));
        gc.fillOval(-16, -12.5 + sway * 0.4, 31, 25.0);
        // Corcunda dos ombros iluminada
        gc.setFill(Color.web("#6E411E"));
        gc.fillOval(-4, -11.5 + sway * 0.5, 19, 23.0);

        // Cabeça Larga de Urso Pardo
        gc.setFill(Color.web("#4A2910"));
        gc.fillOval(7.0, -9.5 + sway * 0.6, 15.0, 19.0);
        // Focinho caramelo claro
        gc.setFill(Color.web("#8D5B36"));
        gc.fillRoundRect(16.5, -5.0 + sway * 0.6, 8.5, 10.0, 5, 5);
        gc.setFill(Color.web("#140A03"));
        gc.fillOval(22.5, -2.5 + sway * 0.6, 3.8, 5.0);

        // Orelhas Redondas de Urso
        gc.setFill(Color.web("#2B1707"));
        gc.fillOval(8.5, -12.5 + sway * 0.6, 6.5, 6.5);
        gc.fillOval(8.5, 6.0 + sway * 0.6, 6.5, 6.5);

        // Olhos vermelhos de predador alfa
        gc.setFill(Color.web("#FF3D00"));
        gc.fillOval(16.5, -5.2 + sway * 0.6, 2.8, 2.4);
        gc.fillOval(16.5, 2.8 + sway * 0.6, 2.8, 2.4);
    }

    /**
     * CHEFE: PANTERA NEGRA (PANTHER): Corpo felino comprido e sinuoso, omoplatas marcadas,
     * cauda longa chicoteando em "S" e olhos dourados brilhantes.
     */
    private void drawShadowPanther(double walk, double breath, boolean moving) {
        double stride = moving ? Math.sin(walk) * 6.8 : 0.0;
        double spineCurve = moving ? Math.cos(walk) * 1.8 : 0.0;
        double tailWhip = Math.sin(moving ? walk * 1.2 : breath * 9) * 6.5;

        gc.setFill(Color.color(0, 0, 0, 0.44));
        gc.fillOval(-20, -10, 40, 20);

        // 4 Patas Felinas Musculosas
        drawQuadrupedLegs(stride, 10.5, 8.2, 7.5, 3.8, Color.web("#121212"), Color.web("#2C2C2C"));

        // Cauda Felina Longa em "S"
        gc.setStroke(Color.web("#141414"));
        gc.setLineWidth(4.2);
        gc.strokeLine(-15, 0, -24, tailWhip * 0.5);
        gc.strokeLine(-24, tailWhip * 0.5, -31, -tailWhip * 0.4);

        // Corpo Felino Alongado e Flexível (Cintura fina, ombros fortes)
        gc.scale(1.0 + breath * 0.4, 1.0 + breath);
        gc.setFill(Color.web("#101014"));
        gc.fillOval(-17, -7.8 + spineCurve * 0.3, 24, 15.6); // Quadril
        gc.fillOval(-6, -9.2 + spineCurve * 0.5, 21, 18.4);  // Peito/Ombros
        // Reflexo azul-obsidiana acetinado nos músculos
        gc.setFill(Color.web("#263238", 0.75));
        gc.fillOval(-12, -5.5 + spineCurve * 0.4, 15, 11.0);
        gc.fillOval(-3, -6.8 + spineCurve * 0.5, 14, 13.6);

        // Cabeça Felina Arredondada e Mandíbula
        gc.setFill(Color.web("#141418"));
        gc.fillOval(9.0, -7.2 + spineCurve * 0.6, 12.5, 14.4);
        gc.setFill(Color.web("#1F1F24"));
        gc.fillRoundRect(16.5, -4.5 + spineCurve * 0.6, 6.0, 9.0, 4, 4);

        // Orelhas curtas arredondadas de pantera
        gc.setFill(Color.web("#08080A"));
        gc.fillOval(10.0, -9.2 + spineCurve * 0.6, 5.0, 4.5);
        gc.fillOval(10.0, 4.7 + spineCurve * 0.6, 5.0, 4.5);

        // Olhos Amarelo-Ouro Luminosos de Predador Noturno
        gc.setFill(Color.web("#FFD600"));
        gc.fillOval(16.0, -4.5 + spineCurve * 0.6, 3.0, 2.4);
        gc.fillOval(16.0, 2.1 + spineCurve * 0.6, 3.0, 2.4);
    }

    /**
     * CHEFE: CROCODILO DO RIO (CROCODILE): Corpo reptiliano comprido com patas abertas para os lados,
     * placas dorsais serrilhadas, cauda articulada ondulante e mandíbula longa cheia de dentes.
     */
    private void drawRiverCrocodile(double walk, double breath, boolean moving) {
        double stride = moving ? Math.sin(walk) * 5.0 : 0.0;
        double tailSway = Math.sin(moving ? walk * 1.4 : breath * 8) * 5.5;

        gc.setFill(Color.color(0, 0, 0, 0.42));
        gc.fillOval(-26, -12, 52, 24);

        // 4 Patas Reptilianas Abertas para as Laterais (Sprawling Reptile Legs)
        gc.setFill(Color.web("#1B4D20"));
        gc.fillOval(6 + stride, -13.5, 7.5, 5.0);
        gc.fillOval(6 - stride, 8.5, 7.5, 5.0);
        gc.fillOval(-11 - stride, -13.0, 7.5, 5.0);
        gc.fillOval(-11 + stride, 8.0, 7.5, 5.0);

        // Cauda Escamosa Articulada em 3 Segmentos Ondulantes
        gc.setFill(Color.web("#143D19"));
        gc.fillOval(-24, -5.5 + tailSway * 0.3, 13, 11.0);
        gc.fillOval(-32, -4.0 + tailSway * 0.7, 11, 8.0);
        gc.fillPolygon(new double[]{-29, -40, -29}, new double[]{-3.0 + tailSway * 0.7, tailSway * 1.1, 3.0 + tailSway * 0.7}, 3);

        // Torso Couraçado Alongado
        gc.scale(1.0 + breath * 0.3, 1.0 + breath);
        gc.setFill(Color.web("#143D19"));
        gc.fillOval(-16, -9.5, 32, 19.0);
        gc.setFill(Color.web("#2E7D32"));
        gc.fillOval(-13, -7.8, 27, 15.6);

        // Fileira Dupla de Escamas Serrilhadas (Osteodermos no dorso)
        gc.setFill(Color.web("#0D2910"));
        for (int i = -12; i <= 8; i += 5) {
            gc.fillRect(i, -4.2, 3.2, 2.4);
            gc.fillRect(i, 1.8, 3.2, 2.4);
        }

        // Mandíbula Longa em "U" com Dentes Brancos Expostos nas Laterais
        gc.setFill(Color.web("#1B5E20"));
        gc.fillRoundRect(11.0, -6.2, 16.5, 12.4, 5, 5);
        gc.setFill(Color.web("#388E3C"));
        gc.fillRoundRect(12.0, -4.8, 14.5, 9.6, 4, 4);

        // Dentes brancos laterais
        gc.setFill(Color.web("#FFFDE7"));
        for (int d = 14; d <= 23; d += 3) {
            gc.fillOval(d, -6.6, 2.0, 2.0);
            gc.fillOval(d, 4.6, 2.0, 2.0);
        }

        // Narinas na ponta do focinho e Olhos reptilianos amarelos
        gc.setFill(Color.web("#0A1F0D"));
        gc.fillOval(24.5, -3.2, 2.0, 1.8);
        gc.fillOval(24.5, 1.4, 2.0, 1.8);

        gc.setFill(Color.web("#FFEA00"));
        gc.fillOval(11.5, -5.5, 3.0, 2.4);
        gc.fillOval(11.5, 3.1, 3.0, 2.4);
    }

    /**
     * Utilitário para desenhar as 4 patas articuladas de quadrúpedes com marcha diagonal realista.
     */
    private void drawQuadrupedLegs(double stride, double frontX, double sideY, double legLen, double legW, Color legColor, Color pawColor) {
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
