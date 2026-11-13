package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.ui.AchievementVisualCatalog.EmblemKind;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Desenhador vetorial de medalhões circulares temáticos, crânio de Game Over e anel de progresso.
 */
public final class UiTrophyEmblem {
    private UiTrophyEmblem() {}

    public static Canvas createMedallion(EmblemKind kind, double size, Color accent, boolean unlocked) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawMedallionFrame(gc, size, accent, unlocked);
        drawEmblemGlyph(gc, kind, size, unlocked ? accent : Color.web("#64748B"), unlocked);
        if (!unlocked) {
            drawMiniCornerLock(gc, size);
        }
        return canvas;
    }

    public static Canvas createSkullMedallion(double size, Color accent) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawMedallionFrame(gc, size, accent, true);
        double c = size * 0.5;
        double r = size * 0.22;

        gc.setFill(accent);
        gc.fillOval(c - r, c - r * 1.15, r * 2.0, r * 1.65);
        gc.fillRoundRect(c - r * 0.58, c + r * 0.15, r * 1.16, r * 0.72, 4, 4);

        gc.setFill(Color.web("#0D1311"));
        gc.fillOval(c - r * 0.55, c - r * 0.42, r * 0.42, r * 0.42);
        gc.fillOval(c + r * 0.13, c - r * 0.42, r * 0.42, r * 0.42);
        gc.fillPolygon(new double[]{c, c - r * 0.14, c + r * 0.14},
                new double[]{c + r * 0.02, c + r * 0.26, c + r * 0.26}, 3);
        return canvas;
    }

    public static Canvas createProgressRing(int unlocked, int total, double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double ratio = total <= 0 ? 0.0 : Math.min(1.0, (double) unlocked / total);
        double cx = size * 0.5;
        double cy = size * 0.5;
        double pad = 5.0;
        double d = size - pad * 2.0;

        gc.setFill(Color.web("#0B110F", 0.85));
        gc.fillOval(pad, pad, d, d);

        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineWidth(4.2);
        gc.setStroke(Color.web("#FFFFFF", 0.10));
        gc.strokeOval(pad, pad, d, d);

        if (ratio > 0.0) {
            gc.setStroke(ratio >= 1.0 ? Color.web(UiTheme.ACCENT_AMBER) : Color.web(UiTheme.ACCENT_EMERALD));
            gc.strokeArc(pad, pad, d, d, 90, -360.0 * ratio, ArcType.OPEN);
        }

        int pct = (int) Math.round(ratio * 100.0);
        gc.setFill(Color.web(UiTheme.TEXT_PRIMARY));
        gc.setFont(UiTypography.font(UiTypography.TypoRole.MICRO, 0.96));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText(pct + "%", cx, cy);
        return canvas;
    }

    private static void drawMedallionFrame(GraphicsContext gc, double size, Color accent, boolean unlocked) {
        double pad = 2.0;
        double d = size - pad * 2.0;
        if (unlocked) {
            gc.setFill(new RadialGradient(
                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, accent.deriveColor(0, 1.0, 1.0, 0.28)),
                new Stop(0.72, Color.web("#0E1513", 0.96)),
                new Stop(1.0, Color.web("#090D0C", 0.98))
            ));
            gc.fillOval(pad, pad, d, d);
            gc.setStroke(accent.deriveColor(0, 1.0, 1.0, 0.72));
            gc.setLineWidth(1.6);
            gc.strokeOval(pad, pad, d, d);
            gc.setStroke(Color.web("#FFFFFF", 0.12));
            gc.setLineWidth(0.9);
            gc.strokeOval(pad + 3.0, pad + 3.0, d - 6.0, d - 6.0);
        } else {
            gc.setFill(Color.web("#111715", 0.85));
            gc.fillOval(pad, pad, d, d);
            gc.setStroke(Color.web("#FFFFFF", 0.10));
            gc.setLineWidth(1.2);
            gc.strokeOval(pad, pad, d, d);
        }
    }

    private static void drawEmblemGlyph(GraphicsContext gc, EmblemKind kind, double size, Color c, boolean unlocked) {
        gc.save();
        gc.setGlobalAlpha(unlocked ? 1.0 : 0.42);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        double cx = size * 0.5;
        double cy = size * 0.5;
        double u = size * 0.23; // half-extent of glyph (~20% margin inside circle)
        gc.setFill(c);
        gc.setStroke(c);
        gc.setLineWidth(Math.max(1.7, size * 0.048));

        switch (kind) {
            case SWORD:
                gc.save();
                gc.translate(cx, cy);
                gc.rotate(-42);
                gc.fillPolygon(
                    new double[]{0, -u * 0.20, -u * 0.16, u * 0.16, u * 0.20},
                    new double[]{-u * 1.15, -u * 0.72, u * 0.46, u * 0.46, -u * 0.72}, 5);
                gc.fillRoundRect(-u * 0.56, u * 0.36, u * 1.12, u * 0.20, 3, 3);
                gc.fillRect(-u * 0.12, u * 0.54, u * 0.24, u * 0.45);
                gc.fillOval(-u * 0.18, u * 0.90, u * 0.36, u * 0.32);
                gc.restore();
                break;

            case BOW:
                gc.strokeArc(cx - u * 0.95, cy - u * 0.95, u * 1.55, u * 1.90, -72, 144, ArcType.OPEN);
                gc.setLineWidth(Math.max(1.2, size * 0.032));
                gc.strokeLine(cx - u * 0.24, cy - u * 0.88, cx - u * 0.24, cy + u * 0.88);
                gc.setLineWidth(Math.max(1.7, size * 0.048));
                gc.strokeLine(cx - u * 0.78, cy, cx + u * 0.92, cy);
                gc.fillPolygon(
                    new double[]{cx + u * 1.08, cx + u * 0.65, cx + u * 0.65},
                    new double[]{cy, cy - u * 0.24, cy + u * 0.24}, 3);
                break;

            case CROWN:
                gc.fillPolygon(
                    new double[]{cx - u * 0.95, cx - u * 0.85, cx - u * 0.36, cx, cx + u * 0.36, cx + u * 0.85, cx + u * 0.95},
                    new double[]{cy + u * 0.65, cy - u * 0.45, cy + u * 0.12, cy - u * 0.75, cy + u * 0.12, cy - u * 0.45, cy + u * 0.65},
                    7);
                gc.fillRoundRect(cx - u * 0.95, cy + u * 0.55, u * 1.90, u * 0.25, 3, 3);
                gc.setFill(unlocked ? Color.web("#FFFBEB") : c);
                gc.fillOval(cx - u * 0.15, cy + u * 0.05, u * 0.30, u * 0.30);
                break;

            case MOON:
                gc.beginPath();
                gc.arc(cx + u * 0.08, cy, u * 0.85, u * 0.85, 62, 236);
                gc.arc(cx + u * 0.45, cy - u * 0.10, u * 0.68, u * 0.68, 285, -210);
                gc.closePath();
                gc.fill();
                drawStarShape(gc, cx + u * 0.55, cy - u * 0.55, 4, u * 0.28, u * 0.11);
                break;

            case APPLE:
                gc.fillOval(cx - u * 0.80, cy - u * 0.45, u * 1.60, u * 1.38);
                gc.strokeArc(cx - u * 0.10, cy - u * 1.05, u * 0.65, u * 0.75, 115, 70, ArcType.OPEN);
                gc.setFill(unlocked ? Color.web("#A7F3D0") : c);
                gc.fillOval(cx + u * 0.10, cy - u * 0.95, u * 0.56, u * 0.32);
                break;

            case MEAT:
                gc.save();
                gc.translate(cx, cy);
                gc.rotate(-28);
                gc.fillOval(-u * 0.90, -u * 0.56, u * 1.48, u * 1.12);
                gc.setFill(unlocked ? Color.web("#FEF3C7") : Color.web("#94A3B8"));
                gc.fillOval(-u * 0.52, -u * 0.22, u * 0.42, u * 0.32);
                gc.fillRoundRect(u * 0.38, -u * 0.16, u * 0.66, u * 0.32, 4, 4);
                gc.restore();
                break;

            case FOX_MASK:
                gc.fillPolygon(
                    new double[]{cx - u * 0.86, cx - u * 0.72, cx - u * 0.28, cx + u * 0.28, cx + u * 0.72, cx + u * 0.86, cx},
                    new double[]{cy - u * 0.02, cy - u * 0.95, cy - u * 0.38, cy - u * 0.38, cy - u * 0.95, cy - u * 0.02, cy + u * 0.88},
                    7);
                gc.setFill(Color.web("#0D1311"));
                gc.fillOval(cx - u * 0.46, cy - u * 0.06, u * 0.24, u * 0.20);
                gc.fillOval(cx + u * 0.22, cy - u * 0.06, u * 0.24, u * 0.20);
                break;

            case DROPLET:
                gc.fillPolygon(
                    new double[]{cx, cx - u * 0.62, cx, cx + u * 0.62},
                    new double[]{cy - u * 0.96, cy + u * 0.18, cy + u * 0.88, cy + u * 0.18}, 4);
                gc.fillOval(cx - u * 0.64, cy - u * 0.08, u * 1.28, u * 0.96);
                gc.setStroke(c.deriveColor(0, 1, 1, 0.55));
                gc.strokeArc(cx - u * 0.88, cy + u * 0.44, u * 1.76, u * 0.56, 200, 140, ArcType.OPEN);
                break;

            case STORM:
                gc.fillOval(cx - u * 0.85, cy - u * 0.55, u * 0.85, u * 0.65);
                gc.fillOval(cx - u * 0.32, cy - u * 0.82, u * 0.95, u * 0.78);
                gc.fillOval(cx + u * 0.12, cy - u * 0.50, u * 0.74, u * 0.60);
                gc.setFill(unlocked ? Color.web("#FACC15") : c);
                gc.fillPolygon(
                    new double[]{cx + u * 0.10, cx - u * 0.25, cx + u * 0.05, cx - u * 0.20, cx + u * 0.32, cx - u * 0.02},
                    new double[]{cy + u * 0.02, cy + u * 0.48, cy + u * 0.48, cy + u * 1.00, cy + u * 0.36, cy + u * 0.36},
                    6);
                break;

            case CLOCK:
                gc.strokeOval(cx - u * 0.84, cy - u * 0.84, u * 1.68, u * 1.68);
                gc.strokeLine(cx, cy, cx, cy - u * 0.48);
                gc.strokeLine(cx, cy, cx + u * 0.46, cy + u * 0.14);
                break;

            case STAR:
                drawStarShape(gc, cx, cy, 5, u * 0.95, u * 0.42);
                break;

            case HEART:
                gc.fillOval(cx - u * 0.86, cy - u * 0.62, u * 0.90, u * 0.86);
                gc.fillOval(cx - u * 0.04, cy - u * 0.62, u * 0.90, u * 0.86);
                gc.fillPolygon(
                    new double[]{cx - u * 0.84, cx + u * 0.84, cx},
                    new double[]{cy - u * 0.04, cy - u * 0.04, cy + u * 0.92}, 3);
                break;

            case EYE:
                gc.strokeArc(cx - u * 0.96, cy - u * 0.68, u * 1.92, u * 1.36, 20, 140, ArcType.OPEN);
                gc.strokeArc(cx - u * 0.96, cy - u * 0.68, u * 1.92, u * 1.36, 200, 140, ArcType.OPEN);
                gc.fillOval(cx - u * 0.36, cy - u * 0.36, u * 0.72, u * 0.72);
                gc.setFill(Color.web("#0D1311"));
                gc.fillOval(cx - u * 0.14, cy - u * 0.14, u * 0.28, u * 0.28);
                break;

            case TROPHY:
            default:
                gc.fillRoundRect(cx - u * 0.56, cy - u * 0.80, u * 1.12, u * 0.82, 5, 5);
                gc.strokeArc(cx - u * 0.92, cy - u * 0.70, u * 0.52, u * 0.56, 90, 180, ArcType.OPEN);
                gc.strokeArc(cx + u * 0.40, cy - u * 0.70, u * 0.52, u * 0.56, 270, 180, ArcType.OPEN);
                gc.fillRect(cx - u * 0.14, cy + u * 0.02, u * 0.28, u * 0.46);
                gc.fillRoundRect(cx - u * 0.50, cy + u * 0.46, u * 1.00, u * 0.30, 4, 4);
                break;
        }
        gc.restore();
    }

    private static void drawMiniCornerLock(GraphicsContext gc, double size) {
        double r = Math.max(12.0, size * 0.32);
        double x = size - r - 1.0;
        double y = size - r - 1.0;
        gc.setFill(Color.web("#0E1412"));
        gc.fillOval(x, y, r, r);
        gc.setStroke(Color.web("#475569"));
        gc.setLineWidth(1.1);
        gc.strokeOval(x, y, r, r);

        double lx = x + r * 0.28;
        double ly = y + r * 0.44;
        double lw = r * 0.44;
        double lh = r * 0.34;
        gc.setStroke(Color.web("#94A3B8"));
        gc.strokeArc(x + r * 0.33, y + r * 0.19, r * 0.34, r * 0.34, 0, 180, ArcType.OPEN);
        gc.setFill(Color.web("#94A3B8"));
        gc.fillRoundRect(lx, ly, lw, lh, 2, 2);
    }

    private static void drawStarShape(GraphicsContext gc, double cx, double cy, int spikes, double outerR, double innerR) {
        double rot = -Math.PI * 0.5;
        double step = Math.PI / spikes;
        double[] xs = new double[spikes * 2];
        double[] ys = new double[spikes * 2];
        for (int i = 0; i < spikes * 2; i++) {
            double rad = (i % 2 == 0) ? outerR : innerR;
            xs[i] = cx + Math.cos(rot) * rad;
            ys[i] = cy + Math.sin(rot) * rad;
            rot += step;
        }
        gc.fillPolygon(xs, ys, spikes * 2);
    }
}
