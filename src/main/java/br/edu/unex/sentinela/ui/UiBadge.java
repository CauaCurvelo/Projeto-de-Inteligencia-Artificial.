package br.edu.unex.sentinela.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Gerador de ícones e emblemas vetoriais desenhados proceduralmente em Canvas
 * para garantir 100% de consistência visual sem caracteres unicode/emojis quebrados.
 */
public final class UiBadge {
    private UiBadge() {}

    public static Canvas createBadge(String type, double size, Color color) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        renderIcon(gc, type, size, color);
        return canvas;
    }

    private static void renderIcon(GraphicsContext gc, String type, double s, Color c) {
        gc.clearRect(0, 0, s, s);
        gc.setFill(c);
        gc.setStroke(c);
        gc.setLineWidth(Math.max(1.5, s * 0.08));

        double pad = s * 0.15;
        double w = s - pad * 2;
        double h = s - pad * 2;

        switch (type.toUpperCase()) {
            case "PLAY":
                gc.fillPolygon(
                    new double[]{pad + w * 0.2, pad + w * 0.9, pad + w * 0.2},
                    new double[]{pad, pad + h * 0.5, pad + h},
                    3
                );
                break;

            case "TROPHY":
                // Taça e alças
                gc.fillRoundRect(pad + w * 0.2, pad, w * 0.6, h * 0.5, 4, 4);
                gc.strokeArc(pad + w * 0.05, pad + h * 0.05, w * 0.4, h * 0.35, 90, 180, javafx.scene.shape.ArcType.OPEN);
                gc.strokeArc(pad + w * 0.55, pad + h * 0.05, w * 0.4, h * 0.35, 270, 180, javafx.scene.shape.ArcType.OPEN);
                // Haste e base
                gc.fillRect(pad + w * 0.42, pad + h * 0.5, w * 0.16, h * 0.25);
                gc.fillRoundRect(pad + w * 0.25, pad + h * 0.75, w * 0.5, h * 0.2, 4, 4);
                break;

            case "GEAR":
                double cx = s / 2.0, cy = s / 2.0, r = w * 0.45;
                for (int i = 0; i < 6; i++) {
                    double ang = i * Math.PI / 3.0;
                    gc.fillOval(cx + Math.cos(ang) * r - 2.5, cy + Math.sin(ang) * r - 2.5, 5, 5);
                }
                gc.strokeOval(cx - r * 0.7, cy - r * 0.7, r * 1.4, r * 1.4);
                gc.fillOval(cx - r * 0.3, cy - r * 0.3, r * 0.6, r * 0.6);
                break;

            case "BOOK":
                gc.strokeRoundRect(pad, pad + h * 0.1, w * 0.46, h * 0.8, 4, 4);
                gc.strokeRoundRect(pad + w * 0.54, pad + h * 0.1, w * 0.46, h * 0.8, 4, 4);
                gc.strokeLine(pad + w * 0.12, pad + h * 0.35, pad + w * 0.38, pad + h * 0.35);
                gc.strokeLine(pad + w * 0.12, pad + h * 0.55, pad + w * 0.38, pad + h * 0.55);
                gc.strokeLine(pad + w * 0.62, pad + h * 0.35, pad + w * 0.88, pad + h * 0.35);
                gc.strokeLine(pad + w * 0.62, pad + h * 0.55, pad + w * 0.88, pad + h * 0.55);
                break;

            case "CHART":
                gc.fillRect(pad + w * 0.1, pad + h * 0.6, w * 0.2, h * 0.4);
                gc.fillRect(pad + w * 0.4, pad + h * 0.3, w * 0.2, h * 0.7);
                gc.fillRect(pad + w * 0.7, pad, w * 0.2, h);
                break;

            case "LOCK":
                // Arco do cadeado
                gc.strokeArc(pad + w * 0.2, pad, w * 0.6, h * 0.6, 0, 180, javafx.scene.shape.ArcType.OPEN);
                // Corpo do cadeado
                gc.fillRoundRect(pad + w * 0.15, pad + h * 0.35, w * 0.7, h * 0.6, 4, 4);
                // Buraco da chave
                gc.setFill(Color.rgb(18, 24, 20));
                gc.fillOval(pad + w * 0.42, pad + h * 0.52, w * 0.16, w * 0.16);
                gc.fillRect(pad + w * 0.45, pad + h * 0.6, w * 0.1, h * 0.16);
                break;

            case "SWORD":
                gc.save();
                gc.translate(s / 2.0, s / 2.0);
                gc.rotate(45);
                gc.fillRect(-1.5, -h * 0.45, 3, h * 0.7);
                gc.fillRect(-w * 0.25, h * 0.2, w * 0.5, 3);
                gc.fillOval(-3, h * 0.38, 6, 6);
                gc.restore();
                break;

            case "DOOR":
                gc.strokeRoundRect(pad + w * 0.15, pad, w * 0.7, h, 3, 3);
                gc.fillOval(pad + w * 0.65, pad + h * 0.52, 4, 4);
                break;

            case "STAR":
                drawStar(gc, s / 2.0, s / 2.0, 5, w * 0.48, w * 0.22);
                break;

            case "CLOCK":
                gc.strokeOval(pad, pad, w, h);
                gc.strokeLine(s / 2.0, s / 2.0, s / 2.0, pad + h * 0.25);
                gc.strokeLine(s / 2.0, s / 2.0, pad + w * 0.75, s / 2.0);
                break;

            case "APPLE":
                gc.fillOval(pad + w * 0.1, pad + h * 0.2, w * 0.8, h * 0.75);
                gc.strokeArc(pad + w * 0.4, pad, w * 0.3, h * 0.35, 45, 90, javafx.scene.shape.ArcType.OPEN);
                break;

            default:
                gc.fillOval(pad, pad, w, h);
                break;
        }
    }

    private static void drawStar(GraphicsContext gc, double cx, double cy, int spikes, double outerR, double innerR) {
        double rot = Math.PI / 2.0 * 3;
        double step = Math.PI / spikes;
        double[] xPts = new double[spikes * 2];
        double[] yPts = new double[spikes * 2];

        for (int i = 0; i < spikes * 2; i++) {
            double r = (i % 2 == 0) ? outerR : innerR;
            xPts[i] = cx + Math.cos(rot) * r;
            yPts[i] = cy + Math.sin(rot) * r;
            rot += step;
        }
        gc.fillPolygon(xPts, yPts, spikes * 2);
    }
}
