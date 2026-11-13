package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.entity.PassiveAnimal;
import br.edu.unex.sentinela.entity.PassiveAnimalType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Renderizador de alta fidelidade visual para a fauna silvestre passiva (Top-Down 2.5D).
 * Alinhado rigorosamente à anatomia, iluminação e pipeline gráfico de CharacterSpriteRenderer:
 * - Rotação fluida 360° no vetor visual (visualAngle)
 * - Sombras de contato elípticas e atenuação de altitude (hopHeight e flightHeight)
 * - Volume anatômico com iluminação dorsal, ventre sombreado e detalhes de pelagem/penas/pele
 * - Ciclos procedurais de locomoção (galope, saltos, debicar e envergadura de asas no voo)
 * - Olhos vivos com reflexo especular e feições faciais detalhadas
 */
public class PassiveAnimalRenderer {
    private final GraphicsContext gc;

    public PassiveAnimalRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    public void renderPassiveAnimal(PassiveAnimal animal, double ax, double ay) {
        if (animal == null || animal.isDespawned()) return;

        double hop = animal.getHopHeight();
        double fly = animal.getFlightHeight();
        double totalAltitude = hop + fly;
        double angleDeg = Math.toDegrees(animal.getVisualAngle());
        double walk = animal.getWalkPhase();
        double breath = Math.sin(animal.getBreathTimer() * 3.6) * 0.045;
        boolean moving = animal.isMoving();
        boolean fleeing = animal.getState() == PassiveAnimal.State.FLEEING;
        boolean alert = animal.getState() == PassiveAnimal.State.ALERT;
        PassiveAnimalType type = animal.getType();

        // 1. Sombra Projetada no Solo (Permanece no chão em ax, ay)
        double shadowFactor = Math.max(0.18, 1.0 - totalAltitude / 80.0);
        double shadowAlpha = Math.max(0.08, 0.38 * shadowFactor);

        gc.save();
        gc.translate(ax, ay);
        gc.rotate(angleDeg);

        gc.setFill(Color.color(0.02, 0.04, 0.02, shadowAlpha));
        double sw = (type == PassiveAnimalType.DEER ? 32.0 :
                    (type == PassiveAnimalType.RABBIT ? 19.0 :
                    (type == PassiveAnimalType.FROG ? 14.0 : 12.0))) * shadowFactor;
        double sh = (type == PassiveAnimalType.DEER ? 15.0 :
                    (type == PassiveAnimalType.RABBIT ? 10.5 :
                    (type == PassiveAnimalType.FROG ? 11.0 : 7.5))) * shadowFactor;
        gc.fillOval(-sw * 0.5, -sh * 0.5, sw, sh);
        gc.restore();

        // 2. Corpo do Animal Elevado no Espaço 2.5D (ax, ay - totalAltitude)
        gc.save();
        gc.translate(ax, ay - totalAltitude);
        gc.rotate(angleDeg);

        switch (type) {
            case RABBIT:
                drawRabbitTopDown(walk, breath, hop, moving, fleeing, alert);
                break;
            case DEER:
                drawDeerTopDown(walk, breath, moving, fleeing, alert);
                break;
            case FROG:
                drawFrogTopDown(walk, breath, hop, moving, alert, animal.getBreathTimer());
                break;
            case BIRD:
                drawBirdTopDown(walk, breath, fly, moving, animal.getState() == PassiveAnimal.State.FLYING_AWAY, animal.getPeckTimer());
                break;
        }

        gc.restore();
    }

    private void drawRabbitTopDown(double walk, double breath, double hop, boolean moving, boolean fleeing, boolean alert) {
        double earTwitch = Math.sin(breath * 15.0) * (alert ? 2.5 : 0.8);
        double tailWag = moving ? Math.sin(walk * 1.5) * 1.5 : 0.0;

        // 1. Patas Traseiras Potentes de Leporídeo (Nas laterais traseiras, estendendo no pulo)
        double rearLegExt = (hop > 0.5 || fleeing) ? Math.max(0, Math.sin(walk)) * 4.5 : 0.0;
        gc.setFill(Color.web("#5D4037"));
        // Coxas traseiras musculosas
        gc.fillOval(-8.5, -6.8, 8.5, 4.5);
        gc.fillOval(-8.5, 2.3, 8.5, 4.5);
        // Pés traseiros compridos com pelugem mais clara
        gc.setFill(Color.web("#8D6E63"));
        gc.fillRoundRect(-10.5 - rearLegExt, -7.2, 7.5, 3.2, 2, 2);
        gc.fillRoundRect(-10.5 - rearLegExt, 4.0, 7.5, 3.2, 2, 2);
        // Pontinhas das patas traseiras (alvas)
        gc.setFill(Color.web("#D7CCC8"));
        gc.fillOval(-11.5 - rearLegExt, -6.8, 2.5, 2.5);
        gc.fillOval(-11.5 - rearLegExt, 4.3, 2.5, 2.5);

        // 2. Patas Dianteiras Delicadas (Mais juntas sob o peito)
        gc.setFill(Color.web("#6D4C41"));
        double frontStride = moving ? Math.cos(walk) * 2.2 : 0.0;
        gc.fillRoundRect(2.5 + frontStride, -4.2, 4.5, 2.4, 2, 2);
        gc.fillRoundRect(2.5 - frontStride, 1.8, 4.5, 2.4, 2, 2);

        // 3. Rabinho Pompom de Algodão (Cottontail)
        gc.setFill(Color.web("#BDBDBD"));
        gc.fillOval(-11.0, -2.8 + tailWag, 5.0, 5.6);
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(-11.8, -2.4 + tailWag, 4.8, 4.8);

        // 4. Tronco Volumoso em Pêra / Gota (Costas largas nos quadris afinando nos ombros)
        gc.scale(1.0 + breath * 0.35, 1.0 + breath * 0.7);
        // Sombra dorsal profunda
        gc.setFill(Color.web("#4E342E"));
        gc.fillOval(-8.5, -5.5, 15.5, 11.0);
        // Pelagem castanho-avelã silvestre
        gc.setFill(Color.web("#795548"));
        gc.fillOval(-8.0, -5.0, 14.5, 10.0);
        // Dorso superior fulvo dourado
        gc.setFill(Color.web("#8D6E63"));
        gc.fillOval(-6.5, -4.0, 12.0, 8.0);
        // Faixa espinhal suave
        gc.setFill(Color.web("#A1887F", 0.75));
        gc.fillOval(-5.0, -2.2, 9.5, 4.4);

        // 5. Cabeça Oval com Bochechas Fofas
        gc.setFill(Color.web("#6D4C41"));
        gc.fillOval(3.0, -4.2, 8.0, 8.4);
        gc.setFill(Color.web("#8D6E63"));
        gc.fillOval(3.8, -3.8, 7.2, 7.6);

        // Focinho e bigodes
        gc.setFill(Color.web("#D7CCC8"));
        gc.fillOval(8.0, -2.5, 3.8, 5.0);
        gc.setFill(Color.web("#3E2723"));
        gc.fillOval(10.5, -1.2, 1.8, 2.4);

        // Bigodinhos sutis
        gc.setStroke(Color.web("#ECEFF1", 0.65));
        gc.setLineWidth(0.6);
        gc.strokeLine(9.5, -2.0, 13.5, -5.0);
        gc.strokeLine(9.5, -1.0, 14.0, -2.5);
        gc.strokeLine(9.5, 2.0, 13.5, 5.0);
        gc.strokeLine(9.5, 1.0, 14.0, 2.5);

        // Olhos escuros brilhantes nas laterais
        gc.setFill(Color.web("#1A1A1A"));
        gc.fillOval(6.0, -3.8, 2.2, 1.8);
        gc.fillOval(6.0, 2.0, 2.2, 1.8);
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(6.6, -3.4, 0.8, 0.8);
        gc.fillOval(6.6, 2.4, 0.8, 0.8);

        // 6. Orelhas Longas Características (Apontadas para trás e ligeiramente para fora)
        // Orelha Esquerda
        double earAngleL = -0.35 + earTwitch * 0.05;
        gc.save();
        gc.translate(4.5, -2.5);
        gc.rotate(Math.toDegrees(earAngleL));
        gc.setFill(Color.web("#4E342E"));
        gc.fillOval(-9.5, -2.2, 10.5, 3.8);
        gc.setFill(Color.web("#795548"));
        gc.fillOval(-9.0, -1.8, 9.5, 3.0);
        gc.setFill(Color.web("#FFCDD2", 0.85));
        gc.fillOval(-8.0, -1.3, 7.5, 2.0);
        gc.setFill(Color.web("#212121"));
        gc.fillOval(-10.0, -1.6, 2.5, 2.4);
        gc.restore();

        // Orelha Direita
        double earAngleR = 0.35 - earTwitch * 0.05;
        gc.save();
        gc.translate(4.5, 2.5);
        gc.rotate(Math.toDegrees(earAngleR));
        gc.setFill(Color.web("#4E342E"));
        gc.fillOval(-9.5, -1.6, 10.5, 3.8);
        gc.setFill(Color.web("#795548"));
        gc.fillOval(-9.0, -1.2, 9.5, 3.0);
        gc.setFill(Color.web("#FFCDD2", 0.85));
        gc.fillOval(-8.0, -0.7, 7.5, 2.0);
        gc.setFill(Color.web("#212121"));
        gc.fillOval(-10.0, -0.8, 2.5, 2.4);
        gc.restore();
    }

    private void drawDeerTopDown(double walk, double breath, boolean moving, boolean fleeing, boolean alert) {
        double stride = moving ? Math.sin(walk) * 6.2 : 0.0;
        double neckSway = moving ? Math.cos(walk) * 1.5 : 0.0;
        double tailAlert = fleeing ? Math.sin(walk * 2.0) * 2.5 : 0.0;

        // 1. As 4 Pernas Longas e Cascos Bipartidos (Marcha diagonal esbelta)
        double frontX = 8.5;
        double sideY = 6.2;
        double legLen = 5.8;
        double legW = 2.6;

        double flX = frontX + stride;
        double frX = frontX - stride;
        double blX = -frontX - stride;
        double brX = -frontX + stride;

        gc.setFill(Color.web("#5C3317"));
        gc.fillRoundRect(flX - legLen * 0.5, -sideY - legW * 0.5, legLen, legW, 2, 2);
        gc.fillRoundRect(frX - legLen * 0.5, sideY - legW * 0.5, legLen, legW, 2, 2);
        gc.fillRoundRect(blX - legLen * 0.5, -sideY - legW * 0.5, legLen, legW, 2, 2);
        gc.fillRoundRect(brX - legLen * 0.5, sideY - legW * 0.5, legLen, legW, 2, 2);

        // Cascos pretos
        gc.setFill(Color.web("#1A1A1A"));
        gc.fillOval(flX + legLen * 0.25, -sideY - legW * 0.45, legW * 0.9, legW * 0.9);
        gc.fillOval(frX + legLen * 0.25, sideY - legW * 0.45, legW * 0.9, legW * 0.9);
        gc.fillOval(blX + legLen * 0.25, -sideY - legW * 0.45, legW * 0.9, legW * 0.9);
        gc.fillOval(brX + legLen * 0.25, sideY - legW * 0.45, legW * 0.9, legW * 0.9);

        // 2. Traseira & Cauda de Alarme (Rump patch branco dos cervídeos)
        gc.setFill(Color.web("#FFFDE7"));
        gc.fillOval(-14.5, -4.5, 5.0, 9.0);
        gc.setFill(Color.web("#8D5B28"));
        gc.fillOval(-16.5, -2.5 + tailAlert * 0.4, 5.0, 5.0);
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(-17.5, -1.8 + tailAlert, 4.0, 3.6);

        // 3. Tronco Musculoso e Elegante de Cervo
        gc.scale(1.0 + breath * 0.35, 1.0 + breath * 0.6);
        gc.setFill(Color.web("#6E3A07"));
        gc.fillOval(-13.5, -7.5, 25.0, 15.0);
        gc.setFill(Color.web("#9E581A"));
        gc.fillOval(-12.5, -6.8, 23.5, 13.6);
        gc.setFill(Color.web("#B87333", 0.90));
        gc.fillOval(-10.0, -5.2, 19.0, 10.4);

        // Pintas suaves de cervo nos flancos
        gc.setFill(Color.web("#FFF8E1", 0.60));
        gc.fillOval(-8.5, -4.2, 1.8, 1.4);
        gc.fillOval(-5.5, -4.8, 1.8, 1.4);
        gc.fillOval(-8.5, 2.8, 1.8, 1.4);
        gc.fillOval(-5.5, 3.4, 1.8, 1.4);
        gc.fillOval(-2.5, -3.8, 1.6, 1.2);
        gc.fillOval(-2.5, 2.6, 1.6, 1.2);

        // 4. Pescoço Longo Esbelto e Cabeça Altiva
        gc.setFill(Color.web("#8D5B28"));
        gc.fillPolygon(new double[]{4.0, 13.0, 13.0, 4.0},
                       new double[]{-4.8, -3.5 + neckSway, 3.5 + neckSway, 4.8}, 4);

        gc.translate(neckSway * 0.3, 0);
        gc.setFill(Color.web("#A0522D"));
        gc.fillOval(10.5, -4.2, 9.5, 8.4);
        gc.setFill(Color.web("#C48246"));
        gc.fillPolygon(new double[]{16.0, 22.5, 16.0}, new double[]{-2.8, 0.0, 2.8}, 3);
        gc.setFill(Color.web("#1A1A1A"));
        gc.fillOval(21.2, -1.2, 2.2, 2.4);

        // Olhos amendoados de cervo nas laterais
        gc.setFill(Color.web("#1A1A1A"));
        gc.fillOval(13.8, -3.8, 2.6, 2.0);
        gc.fillOval(13.8, 1.8, 2.6, 2.0);
        gc.setFill(Color.web("#D4AC0D", 0.85));
        gc.fillOval(14.2, -3.4, 1.5, 1.2);
        gc.fillOval(14.2, 2.2, 1.5, 1.2);
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(14.6, -3.2, 0.7, 0.7);
        gc.fillOval(14.6, 2.4, 0.7, 0.7);

        // Orelhas de Veludo Longas
        gc.setFill(Color.web("#5C3317"));
        gc.fillOval(9.5, -8.2, 6.2, 3.4);
        gc.fillOval(9.5, 4.8, 6.2, 3.4);
        gc.setFill(Color.web("#FFF3E0", 0.85));
        gc.fillOval(10.2, -7.5, 4.8, 2.2);
        gc.fillOval(10.2, 5.3, 4.8, 2.2);

        // 5. Galhada Ramificada Majestosa (Antlers 2.5D)
        gc.setStroke(Color.web("#8D6E63"));
        gc.setLineWidth(1.8);
        gc.strokeLine(13.0, -2.5, 9.5, -9.5);
        gc.strokeLine(9.5, -9.5, 13.5, -14.5);
        gc.strokeLine(11.0, -6.5, 14.5, -8.5);
        gc.strokeLine(11.5, -12.0, 15.5, -12.5);

        gc.strokeLine(13.0, 2.5, 9.5, 9.5);
        gc.strokeLine(9.5, 9.5, 13.5, 14.5);
        gc.strokeLine(11.0, 6.5, 14.5, 8.5);
        gc.strokeLine(11.5, 12.0, 15.5, 12.5);

        gc.setStroke(Color.web("#EFEBE9"));
        gc.setLineWidth(1.0);
        gc.strokeLine(12.5, -13.5, 13.5, -14.5);
        gc.strokeLine(12.5, 13.5, 13.5, 14.5);
    }

    private void drawFrogTopDown(double walk, double breath, double hop, boolean moving, boolean alert, double breathTimer) {
        boolean inHop = hop > 0.4 || moving;
        double throatPulse = 0.5 + 0.5 * Math.sin(breathTimer * 6.5);

        // 1. Pernas Traseiras Anfíbias Articuladas
        gc.setStroke(Color.web("#1B5E20"));
        gc.setLineWidth(2.4);

        if (inHop) {
            double legKick = Math.sin(walk * 2.0) * 3.0;
            gc.strokeLine(-4.0, -4.5, -12.0 - legKick, -5.5);
            gc.strokeLine(-12.0 - legKick, -5.5, -17.0 - legKick, -3.5);
            gc.strokeLine(-4.0, 4.5, -12.0 - legKick, 5.5);
            gc.strokeLine(-12.0 - legKick, 5.5, -17.0 - legKick, 3.5);

            gc.setFill(Color.web("#2E7D32"));
            gc.fillPolygon(new double[]{-16.0 - legKick, -20.0 - legKick, -19.0 - legKick},
                           new double[]{-3.5, -6.0, -1.0}, 3);
            gc.fillPolygon(new double[]{-16.0 - legKick, -20.0 - legKick, -19.0 - legKick},
                           new double[]{3.5, 6.0, 1.0}, 3);
        } else {
            gc.strokeLine(-3.0, -4.5, -7.0, -8.5);
            gc.strokeLine(-7.0, -8.5, -4.0, -9.5);
            gc.strokeLine(-3.0, 4.5, -7.0, 8.5);
            gc.strokeLine(-7.0, 8.5, -4.0, 9.5);

            gc.setFill(Color.web("#2E7D32"));
            gc.fillOval(-5.5, -11.0, 4.0, 3.0);
            gc.fillOval(-5.5, 8.0, 4.0, 3.0);
        }

        // 2. Patas Dianteiras Pequenas
        gc.setStroke(Color.web("#2E7D32"));
        gc.setLineWidth(1.8);
        gc.strokeLine(2.5, -3.5, 4.5, -6.5);
        gc.strokeLine(2.5, 3.5, 4.5, 6.5);
        gc.setFill(Color.web("#388E3C"));
        gc.fillOval(3.5, -7.5, 2.5, 2.5);
        gc.fillOval(3.5, 5.0, 2.5, 2.5);

        // 3. Corpo Achatado Oliváceo e Texturizado
        gc.scale(1.0 + breath * 0.4, 1.0 + breath * 0.7);
        gc.setFill(Color.web("#0D3813"));
        gc.fillOval(-6.5, -5.5, 13.0, 11.0);
        gc.setFill(Color.web("#2E7D32"));
        gc.fillOval(-5.5, -4.8, 11.5, 9.6);
        gc.setFill(Color.web("#43A047"));
        gc.fillOval(-4.5, -3.8, 9.5, 7.6);

        // Pintas de camuflagem
        gc.setFill(Color.web("#1B5E20"));
        gc.fillOval(-3.5, -2.5, 1.8, 1.6);
        gc.fillOval(-1.0, -3.2, 1.6, 1.4);
        gc.fillOval(-2.0, 1.8, 1.8, 1.6);
        gc.fillOval(0.5, 2.2, 1.5, 1.3);
        gc.fillOval(-4.5, 0.5, 1.4, 1.4);
        gc.setFill(Color.web("#C8E6C9", 0.60));
        gc.fillOval(-4.0, -0.6, 8.0, 1.2);

        // 4. Saco Vocal / Garganta Pulsante
        gc.setFill(Color.web("#DCEDC8", 0.75 + throatPulse * 0.20));
        gc.fillOval(3.2, -2.2 - throatPulse * 0.5, 3.8 + throatPulse * 1.2, 4.4 + throatPulse * 1.0);

        // 5. Olhos Bulbares Grandes e Salientes no Topo da Cabeça
        gc.setFill(Color.web("#0D3813"));
        gc.fillOval(2.0, -5.2, 4.2, 4.2);
        gc.setFill(Color.web("#FBC02D"));
        gc.fillOval(2.4, -4.8, 3.4, 3.4);
        gc.setFill(Color.web("#1A1A1A"));
        gc.fillRect(2.6, -3.6, 3.0, 1.1);
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(3.2, -4.4, 0.8, 0.8);

        gc.setFill(Color.web("#0D3813"));
        gc.fillOval(2.0, 1.0, 4.2, 4.2);
        gc.setFill(Color.web("#FBC02D"));
        gc.fillOval(2.4, 1.4, 3.4, 3.4);
        gc.setFill(Color.web("#1A1A1A"));
        gc.fillRect(2.6, 2.5, 3.0, 1.1);
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(3.2, 1.8, 0.8, 0.8);
    }

    private void drawBirdTopDown(double walk, double breath, double fly, boolean moving, boolean flyingAway, double peckTimer) {
        if (flyingAway) {
            // === MODO EM VOO: Envergadura Aberta e Asas Batendo ===
            double wingFlap = Math.sin(walk * 2.2);
            double wingTipY = 13.5 * Math.abs(wingFlap) + 4.0;
            double wingLiftX = wingFlap * 2.8;

            // 1. Cauda Aberta em Leque
            gc.setFill(Color.web("#3E2723"));
            gc.fillPolygon(new double[]{-4.0, -11.0, -12.5, -11.0},
                           new double[]{0.0, -4.5, 0.0, 4.5}, 4);
            gc.setFill(Color.web("#5D4037"));
            gc.fillPolygon(new double[]{-3.5, -9.5, -10.5, -9.5},
                           new double[]{0.0, -3.2, 0.0, 3.2}, 4);
            gc.setFill(Color.web("#ECEFF1", 0.80));
            gc.fillOval(-12.0, -2.5, 2.5, 5.0);

            // 2. Asas Abertas em Envergadura com Penas Primárias
            // Asa Esquerda
            gc.setFill(Color.web("#3E2723"));
            gc.fillPolygon(new double[]{-1.0, 3.0, 1.0 - wingLiftX, -6.0 - wingLiftX, -3.0},
                           new double[]{-2.5, -3.5, -wingTipY, -wingTipY + 3.0, -2.5}, 5);
            gc.setFill(Color.web("#6D4C41"));
            gc.fillPolygon(new double[]{0.0, 2.5, 0.5 - wingLiftX, -4.5 - wingLiftX, -2.0},
                           new double[]{-2.0, -2.8, -wingTipY + 1.5, -wingTipY + 3.5, -2.0}, 5);
            gc.setStroke(Color.web("#D7CCC8", 0.75));
            gc.setLineWidth(0.8);
            gc.strokeLine(-1.0, -5.0, -3.0 - wingLiftX, -wingTipY + 3.0);
            gc.strokeLine(0.5, -5.0, -1.0 - wingLiftX, -wingTipY + 2.0);

            // Asa Direita
            gc.setFill(Color.web("#3E2723"));
            gc.fillPolygon(new double[]{-1.0, 3.0, 1.0 - wingLiftX, -6.0 - wingLiftX, -3.0},
                           new double[]{2.5, 3.5, wingTipY, wingTipY - 3.0, 2.5}, 5);
            gc.setFill(Color.web("#6D4C41"));
            gc.fillPolygon(new double[]{0.0, 2.5, 0.5 - wingLiftX, -4.5 - wingLiftX, -2.0},
                           new double[]{2.0, 2.8, wingTipY - 1.5, wingTipY - 3.5, 2.0}, 5);
            gc.setStroke(Color.web("#D7CCC8", 0.75));
            gc.setLineWidth(0.8);
            gc.strokeLine(-1.0, 5.0, -3.0 - wingLiftX, wingTipY - 3.0);
            gc.strokeLine(0.5, 5.0, -1.0 - wingLiftX, wingTipY - 2.0);

            // 3. Tronco Fusiforme
            gc.setFill(Color.web("#4E342E"));
            gc.fillOval(-5.5, -3.2, 12.0, 6.4);
            gc.setFill(Color.web("#6D4C41"));
            gc.fillOval(-4.5, -2.6, 10.5, 5.2);
            gc.setFill(Color.web("#A1887F"));
            gc.fillOval(0.5, -2.0, 5.5, 4.0);

            // 4. Cabeça Aerodinâmica e Bico
            gc.setFill(Color.web("#4E342E"));
            gc.fillOval(3.5, -2.2, 5.0, 4.4);
            gc.setFill(Color.web("#FFA000"));
            gc.fillPolygon(new double[]{7.5, 11.5, 7.5}, new double[]{-1.1, 0.0, 1.1}, 3);

            // Olhos
            gc.setFill(Color.web("#111111"));
            gc.fillOval(5.2, -2.0, 1.4, 1.4);
            gc.fillOval(5.2, 0.6, 1.4, 1.4);
            gc.setFill(Color.web("#FFFFFF"));
            gc.fillOval(5.6, -1.8, 0.5, 0.5);
            gc.fillOval(5.6, 0.8, 0.5, 0.5);
        } else {
            // === MODO EM SOLO: Asas Dobradas, Postura Alerta e Animação de Debicar ===
            double peckDip = Math.max(0, Math.sin(peckTimer)) * 2.2;
            double tailFlick = Math.sin(breath * 12.0) * 1.2;

            // 1. Patinhas Delicadas com Garrinhas
            gc.setStroke(Color.web("#8D6E63"));
            gc.setLineWidth(1.1);
            gc.strokeLine(-0.5, -2.5, 0.5, -4.5);
            gc.strokeLine(-0.5, 2.5, 0.5, 4.5);
            gc.strokeLine(0.5, -4.5, 2.0, -5.2);
            gc.strokeLine(0.5, 4.5, 2.0, 5.2);

            // 2. Cauda Fechada em Leque Afilado
            gc.setFill(Color.web("#3E2723"));
            gc.fillPolygon(new double[]{-4.0, -10.5, -10.0, -4.0},
                           new double[]{-1.5, -3.2 + tailFlick, 3.2 + tailFlick, 1.5}, 4);
            gc.setFill(Color.web("#5D4037"));
            gc.fillPolygon(new double[]{-3.5, -9.0, -8.5, -3.5},
                           new double[]{-1.0, -2.2 + tailFlick, 2.2 + tailFlick, 1.0}, 4);

            // 3. Tronco Compacto com Penas do Peito
            gc.scale(1.0 + breath * 0.35, 1.0 + breath * 0.6);
            gc.setFill(Color.web("#4E342E"));
            gc.fillOval(-5.0, -3.6, 10.5, 7.2);
            gc.setFill(Color.web("#6D4C41"));
            gc.fillOval(-4.2, -3.0, 9.2, 6.0);
            gc.setFill(Color.web("#8D6E63"));
            gc.fillOval(-1.5, -2.5, 6.5, 5.0);

            // 4. Asas Dobradas
            gc.setFill(Color.web("#3E2723"));
            gc.fillRoundRect(-3.5, -4.2, 7.5, 2.2, 2, 2);
            gc.fillRoundRect(-3.5, 2.0, 7.5, 2.2, 2, 2);
            gc.setStroke(Color.web("#D7CCC8", 0.85));
            gc.setLineWidth(0.7);
            gc.strokeLine(-2.5, -3.2, 2.5, -3.2);
            gc.strokeLine(-2.5, 3.2, 2.5, 3.2);

            // 5. Cabeça com Debicar Rápido (Pecking)
            gc.save();
            gc.translate(peckDip * 0.8, 0);

            gc.setFill(Color.web("#4E342E"));
            gc.fillOval(3.0, -2.6, 5.2, 5.2);
            gc.setFill(Color.web("#6D4C41"));
            gc.fillOval(3.5, -2.2, 4.4, 4.4);

            // Bico cônico marfim/âmbar
            gc.setFill(Color.web("#FFA000"));
            gc.fillPolygon(new double[]{7.0, 10.5, 7.0}, new double[]{-1.1, 0.0, 1.1}, 3);

            // Olhos
            gc.setFill(Color.web("#D7CCC8"));
            gc.fillOval(4.6, -2.4, 2.0, 2.0);
            gc.fillOval(4.6, 0.4, 2.0, 2.0);
            gc.setFill(Color.web("#111111"));
            gc.fillOval(4.9, -2.1, 1.4, 1.4);
            gc.fillOval(4.9, 0.7, 1.4, 1.4);
            gc.setFill(Color.web("#FFFFFF"));
            gc.fillOval(5.2, -1.9, 0.6, 0.6);
            gc.fillOval(5.2, 0.9, 0.6, 0.6);

            gc.restore();
        }
    }
}
