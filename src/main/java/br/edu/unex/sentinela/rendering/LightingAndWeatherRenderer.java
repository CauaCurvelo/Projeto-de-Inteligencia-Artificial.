package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.world.WeatherDrop;
import br.edu.unex.sentinela.world.WeatherType;
import br.edu.unex.sentinela.world.World;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Responsável pelo pós-processamento atmosférico:
 * - Iluminação direcional solar (Luz quente dourada no canto superior esquerdo)
 * - Iluminação noturna com Halo Radial suave focado na raposa (Visão Noturna)
 * - Vinheta de borda de lente (Ambient Vignette) para profundidade cinematográfica
 * - Efeitos climáticos volumétricos (Chuva com ripples de impacto e Neblina densa em camadas)
 */
public class LightingAndWeatherRenderer {
    private final GraphicsContext gc;

    public LightingAndWeatherRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    public void renderWeatherVisuals(World world, double visibleW, double visibleH, double animTime) {
        WeatherType weather = world.getCurrentWeather();

        if (weather == WeatherType.RAIN) {
            gc.setStroke(Color.web("#B3E5FC", 0.58));
            gc.setLineWidth(1.3);
            for (WeatherDrop drop : world.getRainDrops()) {
                double dx = drop.getX();
                double dy = drop.getY();
                gc.strokeLine(dx, dy, dx - 4.5, dy + drop.getLength());

                // Pequenos anéis de respingo (water splash ripples) no chão quando a gota está no fim do curso
                if (((int) (dy + dx) % 19) == 0) {
                    gc.setStroke(Color.web("#E1F5FE", 0.35));
                    gc.strokeOval(dx - 6, dy + drop.getLength() - 1.5, 5, 2.5);
                }
            }

            // Atmosfera nublada azul-acinzentada fria
            gc.setFill(Color.color(0.06, 0.12, 0.24, 0.18));
            gc.fillRect(0, 0, visibleW, visibleH);

        } else if (weather == WeatherType.FOG) {
            // Neblina volumétrica usando gradiente radial suave
            RadialGradient fogGrad = new RadialGradient(
                0, 0, visibleW * 0.5, visibleH * 0.5, Math.max(visibleW, visibleH) * 0.65,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0.88, 0.92, 0.95, 0.16)),
                new Stop(0.6, Color.color(0.82, 0.88, 0.92, 0.36)),
                new Stop(1.0, Color.color(0.75, 0.82, 0.88, 0.52))
            );
            gc.setFill(fogGrad);
            gc.fillRect(0, 0, visibleW, visibleH);
        }
    }

    public void renderDynamicLightingAndVignette(World world, double playerScreenX, double playerScreenY, double visibleW, double visibleH) {
        double cycleProgress = world.getCycleProgressTimer() / world.getCycleDuration();
        double nightFactor = Math.sin(cycleProgress * Math.PI); // 0 = Meio-dia, 1 = Meia-noite

        // 1. Luz Solar Direcional (Diurna / Golden Hour) vinda de Noroeste (Topo-Esquerda)
        if (nightFactor < 0.45) {
            double sunStrength = (0.45 - nightFactor) / 0.45;
            LinearGradient sunBeam = new LinearGradient(
                0, 0, visibleW, visibleH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(1.0, 0.95, 0.72, 0.14 * sunStrength)),
                new Stop(0.45, Color.color(1.0, 0.90, 0.60, 0.04 * sunStrength)),
                new Stop(1.0, Color.color(0.02, 0.08, 0.04, 0.12 * sunStrength))
            );
            gc.setFill(sunBeam);
            gc.fillRect(0, 0, visibleW, visibleH);
        }

        // 2. Transição Crepúsculo (Golden Hour / Pôr do Sol alaranjado)
        if (nightFactor >= 0.20 && nightFactor <= 0.60) {
            double duskIntensity = 1.0 - Math.abs(nightFactor - 0.40) / 0.20;
            gc.setFill(Color.color(0.85, 0.38, 0.12, 0.16 * duskIntensity));
            gc.fillRect(0, 0, visibleW, visibleH);
        }

        // 3. Iluminação Noturna Profunda com Halo Focal ao redor da Raposa
        if (nightFactor > 0.15) {
            double maxDarkness = Math.min(0.78, (nightFactor - 0.15) * 0.88);
            double lightRadius = Math.max(visibleW, visibleH) * 0.36;

            RadialGradient nightLight = new RadialGradient(
                0, 0, playerScreenX, playerScreenY, lightRadius,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.0,  Color.color(0.05, 0.08, 0.20, maxDarkness * 0.22)),
                new Stop(0.45, Color.color(0.04, 0.06, 0.18, maxDarkness * 0.58)),
                new Stop(1.0,  Color.color(0.02, 0.03, 0.10, maxDarkness))
            );
            gc.setFill(nightLight);
            gc.fillRect(0, 0, visibleW, visibleH);
        }

        // 4. Vinheta Atmosférica Permanente (Oclusão suave nas bordas da tela para dar profundidade)
        double vignetteRadius = Math.hypot(visibleW, visibleH) * 0.55;
        RadialGradient vignette = new RadialGradient(
            0, 0, visibleW * 0.5, visibleH * 0.5, vignetteRadius,
            false, CycleMethod.NO_CYCLE,
            new Stop(0.0,  Color.TRANSPARENT),
            new Stop(0.68, Color.color(0.0, 0.0, 0.0, 0.05)),
            new Stop(1.0,  Color.color(0.0, 0.02, 0.0, 0.38))
        );
        gc.setFill(vignette);
        gc.fillRect(0, 0, visibleW, visibleH);
    }
}
