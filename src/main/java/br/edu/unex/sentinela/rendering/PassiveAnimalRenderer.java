package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.entity.PassiveAnimal;
import br.edu.unex.sentinela.entity.PassiveAnimalType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Renderizador visual especializado da fauna passiva (Coelho, Cervo, Sapo e Pássaro).
 * Desenha silhuetas anatômicas 2.5D com animações de salto, corrida arisca,
 * debicar e bater de asas em tempo real.
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
        double faceDx = animal.getFaceDx();
        double anim = animal.getAnimPhase();
        PassiveAnimalType type = animal.getType();

        // 1. Sombra projetada no solo (permanece no chão mesmo se o animal salta ou alça voo)
        double shadowScale = Math.max(0.2, 1.0 - (hop + fly) / 60.0);
        double shadowAlpha = Math.max(0.1, (0.42 - (fly / 120.0)) * shadowScale);
        gc.setFill(Color.color(0.02, 0.05, 0.02, shadowAlpha));

        double sw = (type == PassiveAnimalType.DEER ? 26.0 : (type == PassiveAnimalType.RABBIT ? 15.0 : 11.0)) * shadowScale;
        double sh = sw * 0.48;
        gc.fillOval(ax - sw / 2, ay - sh / 2 + 2, sw, sh);

        // 2. Altura real do sprite no espaço 2.5D
        double sy = ay - hop - fly;

        gc.save();
        // Espelhamento horizontal conforme a direção para onde o animal olha
        if (faceDx < 0) {
            gc.translate(ax, sy);
            gc.scale(-1, 1);
            gc.translate(-ax, -sy);
        }

        switch (type) {
            case RABBIT:
                renderRabbit(ax, sy, anim, animal.getState() == PassiveAnimal.State.FLEEING);
                break;
            case DEER:
                renderDeer(ax, sy, anim, animal.getState() == PassiveAnimal.State.FLEEING);
                break;
            case FROG:
                renderFrog(ax, sy, anim, hop > 0.5);
                break;
            case BIRD:
                renderBird(ax, sy, anim, animal.getState() == PassiveAnimal.State.FLYING_AWAY);
                break;
        }

        gc.restore();
    }

    private void renderRabbit(double rx, double ry, double anim, boolean fleeing) {
        // Corpo felpudo marrom-acinzentado
        gc.setFill(Color.web("#5D4037"));
        gc.fillOval(rx - 7.5, ry - 4.5, 14.5, 9.5);
        gc.setFill(Color.web("#8D6E63"));
        gc.fillOval(rx - 7.0, ry - 5.0, 13.5, 9.0);

        // Patas traseiras e dianteiras
        double legStretch = fleeing ? Math.sin(anim * 1.5) * 4.0 : 0.0;
        gc.setFill(Color.web("#6D4C41"));
        gc.fillOval(rx - 8.0 - legStretch, ry + 1.0, 5.0, 4.0);
        gc.fillOval(rx + 3.0 + legStretch, ry + 1.5, 4.5, 3.5);

        // Cabeça
        gc.setFill(Color.web("#A1887F"));
        gc.fillOval(rx + 2.5, ry - 7.0, 7.5, 7.0);

        // Orelhas longas empinadas
        double earWiggle = Math.sin(anim * 2.5) * 1.5;
        gc.setFill(Color.web("#8D6E63"));
        gc.fillOval(rx + 3.5 + earWiggle, ry - 14.5, 3.2, 9.0);
        gc.fillOval(rx + 6.0 - earWiggle, ry - 14.0, 3.0, 8.5);

        // Interior rosado da orelha
        gc.setFill(Color.web("#F8BBD0", 0.85));
        gc.fillOval(rx + 4.2 + earWiggle, ry - 13.5, 1.8, 6.5);
        gc.fillOval(rx + 6.6 - earWiggle, ry - 13.0, 1.6, 6.0);

        // Focinho e olho
        gc.setFill(Color.web("#212121"));
        gc.fillOval(rx + 7.5, ry - 5.0, 2.0, 2.0); // olho escuro brilhante
        gc.setFill(Color.web("#FFCDD2"));
        gc.fillOval(rx + 9.5, ry - 4.0, 1.8, 1.4); // focinho rosa

        // Rabinho pompom branco
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(rx - 8.5, ry - 2.5, 4.0, 4.0);
    }

    private void renderDeer(double dx, double dy, double anim, boolean fleeing) {
        // Pernas esbeltas com cascos
        double legCycle = Math.sin(anim * (fleeing ? 2.2 : 1.2));
        gc.setStroke(Color.web("#5D4037"));
        gc.setLineWidth(2.2);
        gc.strokeLine(dx - 9, dy + 2, dx - 11 - legCycle * 5, dy + 15);
        gc.strokeLine(dx - 6, dy + 2, dx - 7 + legCycle * 5, dy + 15);
        gc.strokeLine(dx + 7, dy + 2, dx + 6 + legCycle * 6, dy + 15);
        gc.strokeLine(dx + 10, dy + 2, dx + 12 - legCycle * 6, dy + 15);

        // Cascos
        gc.setFill(Color.web("#212121"));
        gc.fillRect(dx - 12 - legCycle * 5, dy + 14, 2.5, 2.0);
        gc.fillRect(dx + 11 - legCycle * 6, dy + 14, 2.5, 2.0);

        // Tronco tawny do cervo
        gc.setFill(Color.web("#8D5B28"));
        gc.fillOval(dx - 12, dy - 6, 23, 13);
        gc.setFill(Color.web("#B87333"));
        gc.fillOval(dx - 11, dy - 7, 21, 11);

        // Mancha branca suave no ventre/peito
        gc.setFill(Color.web("#FFF8E1", 0.65));
        gc.fillOval(dx - 3, dy - 2, 9, 6);

        // Pescoço e Cabeça altiva
        gc.setFill(Color.web("#8D5B28"));
        gc.fillPolygon(new double[]{dx + 7, dx + 14, dx + 11, dx + 4},
                       new double[]{dy - 4, dy - 14, dy - 16, dy - 5}, 4);
        gc.fillOval(dx + 10, dy - 18, 9, 7);

        // Chifres ramificados
        gc.setStroke(Color.web("#D7CCC8"));
        gc.setLineWidth(1.6);
        gc.strokeLine(dx + 13, dy - 17, dx + 15, dy - 27);
        gc.strokeLine(dx + 14, dy - 23, dx + 19, dy - 26);
        gc.strokeLine(dx + 14, dy - 20, dx + 11, dy - 24);

        // Olho escuro vigilante e orelhas
        gc.setFill(Color.web("#1A1A1A"));
        gc.fillOval(dx + 14.5, dy - 16.5, 2.2, 2.2);
        gc.setFill(Color.web("#8D5B28"));
        gc.fillOval(dx + 9, dy - 20, 3.5, 5.0);

        // Rabinho com ponta branca
        gc.setFill(Color.web("#8D5B28"));
        gc.fillOval(dx - 13.5, dy - 4, 4, 5);
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(dx - 14.0, dy - 2.5, 2.8, 3.5);
    }

    private void renderFrog(double fx, double fy, double anim, boolean inHop) {
        // Pernas traseiras dobradas ou esticadas no pulo
        gc.setStroke(Color.web("#2E7D32"));
        gc.setLineWidth(2.4);
        if (inHop) {
            gc.strokeLine(fx - 4, fy + 1, fx - 10, fy + 8);
            gc.strokeLine(fx + 2, fy + 1, fx + 8, fy + 8);
        } else {
            gc.strokeLine(fx - 4, fy + 1, fx - 6, fy + 3);
            gc.strokeLine(fx + 3, fy + 1, fx + 5, fy + 3);
        }

        // Corpo oliváceo arredondado
        gc.setFill(Color.web("#1B5E20"));
        gc.fillOval(fx - 5.5, fy - 3.5, 11, 8.5);
        gc.setFill(Color.web("#43A047"));
        gc.fillOval(fx - 4.5, fy - 4.0, 9.5, 7.5);

        // Garganta pulsante
        double throatPulse = 0.5 + 0.5 * Math.sin(anim * 4.0);
        gc.setFill(Color.web("#C8E6C9", 0.70));
        gc.fillOval(fx + 0.5, fy - 1.5, 4.0 + throatPulse, 3.5);

        // Olhos salientes no topo da cabeça
        gc.setFill(Color.web("#FDD835"));
        gc.fillOval(fx - 1.0, fy - 6.5, 3.2, 3.2);
        gc.fillOval(fx + 2.5, fy - 6.5, 3.2, 3.2);

        // Pupilas pretas
        gc.setFill(Color.web("#1A1A1A"));
        gc.fillOval(fx - 0.2, fy - 5.8, 1.6, 1.8);
        gc.fillOval(fx + 3.3, fy - 5.8, 1.6, 1.8);
    }

    private void renderBird(double bx, double by, double anim, boolean flying) {
        if (flying) {
            // Asas batendo rapidamente no voo
            double flap = Math.sin(anim * 22.0);
            gc.setFill(Color.web("#4E342E"));
            // Asa esquerda
            gc.fillPolygon(new double[]{bx - 3, bx - 11, bx - 7},
                           new double[]{by - 1, by - 8 * flap, by - 1}, 3);
            // Asa direita
            gc.fillPolygon(new double[]{bx + 1, bx + 9, bx + 5},
                           new double[]{by - 1, by - 8 * flap, by - 1}, 3);

            // Corpo no voo
            gc.setFill(Color.web("#6D4C41"));
            gc.fillOval(bx - 4.5, by - 3.5, 9.5, 6.5);
            gc.setFill(Color.web("#E65100")); // peito ferrugíneo
            gc.fillOval(bx + 1.0, by - 2.5, 4.5, 4.5);

            // Bico
            gc.setFill(Color.web("#FFB300"));
            gc.fillPolygon(new double[]{bx + 5.5, bx + 9.0, bx + 5.5},
                           new double[]{by - 2.0, by - 1.0, by + 0.5}, 3);
        } else {
            // Pássaro pousado debicando
            double peck = Math.max(0.0, Math.sin(anim * 3.5)) * 2.0;

            // Patinhas finas
            gc.setStroke(Color.web("#3E2723"));
            gc.setLineWidth(1.2);
            gc.strokeLine(bx - 2, by + 2, bx - 2, by + 6);
            gc.strokeLine(bx + 2, by + 2, bx + 2, by + 6);

            // Corpo pousado
            gc.setFill(Color.web("#5D4037"));
            gc.fillOval(bx - 4.5, by - 3.0, 9.0, 6.0);
            gc.setFill(Color.web("#E65100"));
            gc.fillOval(bx + 0.5, by - 2.0, 4.0, 4.0);

            // Cauda inclinada
            gc.setFill(Color.web("#3E2723"));
            gc.fillPolygon(new double[]{bx - 4.5, bx - 9.0, bx - 5.5},
                           new double[]{by - 1.0, by - 4.0, by + 1.0}, 3);

            // Cabeça debicando
            gc.setFill(Color.web("#6D4C41"));
            gc.fillOval(bx + 2.5, by - 4.5 + peck, 5.0, 4.5);

            // Bico e olho
            gc.setFill(Color.web("#212121"));
            gc.fillOval(bx + 4.5, ryToEye(by, peck), 1.4, 1.4);
            gc.setFill(Color.web("#FFB300"));
            gc.fillPolygon(new double[]{bx + 6.5, bx + 9.5, bx + 6.5},
                           new double[]{by - 3.5 + peck, by - 2.5 + peck, by - 1.5 + peck}, 3);
        }
    }

    private double ryToEye(double by, double peck) {
        return by - 3.8 + peck;
    }
}
