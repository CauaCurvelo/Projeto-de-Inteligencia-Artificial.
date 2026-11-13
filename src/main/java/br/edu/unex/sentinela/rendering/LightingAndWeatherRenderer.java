package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.settings.GameSettings;
import br.edu.unex.sentinela.settings.GraphicsQuality;
import br.edu.unex.sentinela.world.BiomeType;
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
    private static final Color RAIN_COLOR = Color.web("#B3E5FC", 0.58);
    private static final Color RAIN_RIPPLE_COLOR = Color.web("#E1F5FE", 0.35);
    private static final Color FOG_LOW_COLOR = Color.color(0.82, 0.88, 0.92, 0.34);

    private static final RadialGradient FOG_GRAD = new RadialGradient(
        0, 0, 0.5, 0.5, 0.72,
        true, CycleMethod.NO_CYCLE,
        new Stop(0.0, Color.color(0.88, 0.92, 0.95, 0.16)),
        new Stop(0.6, Color.color(0.82, 0.88, 0.92, 0.36)),
        new Stop(1.0, Color.color(0.75, 0.82, 0.88, 0.52))
    );

    private static final LinearGradient SUN_BEAM_GRAD = new LinearGradient(
        0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0.0, Color.color(1.0, 0.95, 0.72, 0.14)),
        new Stop(0.45, Color.color(1.0, 0.90, 0.60, 0.04)),
        new Stop(1.0, Color.color(0.02, 0.08, 0.04, 0.12))
    );

    private static final RadialGradient VIGNETTE_GRAD = new RadialGradient(
        0, 0, 0.5, 0.5, 0.78,
        true, CycleMethod.NO_CYCLE,
        new Stop(0.0, Color.TRANSPARENT),
        new Stop(0.68, Color.color(0.0, 0.0, 0.0, 0.05)),
        new Stop(1.0, Color.color(0.0, 0.02, 0.0, 0.38))
    );

    private final GraphicsContext gc;

    public LightingAndWeatherRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    public void renderWeatherVisuals(World world, double visibleW, double visibleH, double animTime) {
        WeatherType weather = world.getCurrentWeather();
        GraphicsQuality quality = GameSettings.get().getGraphicsQuality();
        BiomeType playerBiome = BiomeType.MEADOW;
        if (world.getPlayer() != null && world.getTileMap() != null) {
            playerBiome = world.getTileMap().getBiomeAtWorld(world.getPlayer().getX(), world.getPlayer().getY());
        }
        boolean inSnow = playerBiome == BiomeType.SNOW;

        if (inSnow) {
            renderSnowfall(visibleW, visibleH, animTime, quality, weather == WeatherType.RAIN);
        }

        if (weather == WeatherType.RAIN && !inSnow) {
            var drops = world.getRainDrops();
            int step = (quality == GraphicsQuality.LOW) ? 2 : 1;
            gc.setStroke(RAIN_COLOR);
            gc.setLineWidth(1.3);
            for (int i = 0; i < drops.size(); i += step) {
                WeatherDrop drop = drops.get(i);
                double dx = drop.getX();
                double dy = drop.getY();
                gc.strokeLine(dx, dy, dx - 4.5, dy + drop.getLength());

                // Pequenos anéis de respingo (water splash ripples) no chão quando a gota está no fim do curso
                if (quality != GraphicsQuality.LOW && ((int) (dy + dx) % 19) == 0) {
                    gc.setStroke(RAIN_RIPPLE_COLOR);
                    gc.strokeOval(dx - 6, dy + drop.getLength() - 1.5, 5, 2.5);
                    gc.setStroke(RAIN_COLOR);
                }
            }

            // Atmosfera nublada azul-acinzentada fria
            gc.setFill(Color.color(0.06, 0.12, 0.24, 0.18));
            gc.fillRect(0, 0, visibleW, visibleH);

        } else if (weather == WeatherType.FOG) {
            gc.setFill(quality == GraphicsQuality.LOW ? FOG_LOW_COLOR : FOG_GRAD);
            gc.fillRect(0, 0, visibleW, visibleH);
        }
    }

    private void renderSnowfall(double visibleW, double visibleH, double animTime,
                                GraphicsQuality quality, boolean blizzard) {
        int count = quality == GraphicsQuality.LOW ? 28 : (blizzard ? 95 : 54);
        double speedY = blizzard ? 92.0 : 46.0;
        double windX = blizzard ? -34.0 : -14.0;

        gc.setFill(Color.color(0.75, 0.90, 0.98, blizzard ? 0.14 : 0.06));
        gc.fillRect(0, 0, visibleW, visibleH);

        for (int i = 0; i < count; i++) {
            double h1 = TerrainRenderer.coordHash(i * 17, 3, 9101L);
            double h2 = TerrainRenderer.coordHash(i * 31, 7, 9203L);
            double fx = ((h1 * visibleW + animTime * (windX + (i % 5) * 4.0) + Math.sin(animTime * 1.4 + i) * 14.0) % visibleW + visibleW) % visibleW;
            double fy = ((h2 * visibleH + animTime * (speedY + (i % 7) * 6.0)) % visibleH + visibleH) % visibleH;
            double size = 2.0 + (i % 3) * 1.1;
            gc.setFill(i % 4 == 0 ? Color.web("#BAE6FD", 0.72) : Color.web("#FFFFFF", 0.78));
            gc.fillOval(fx, fy, size, size);
        }
    }

    public void renderDynamicLightingAndVignette(World world, double playerScreenX, double playerScreenY, double visibleW, double visibleH) {
        double cycleProgress = world.getCycleProgressTimer() / world.getCycleDuration();
        double nightFactor = Math.sin(cycleProgress * Math.PI); // 0 = Meio-dia, 1 = Meia-noite
        GraphicsQuality quality = GameSettings.get().getGraphicsQuality();

        // 1. Luz Solar Direcional (Diurna / Golden Hour) vinda de Noroeste (Topo-Esquerda)
        if (nightFactor < 0.45 && quality != GraphicsQuality.LOW) {
            double sunStrength = (0.45 - nightFactor) / 0.45;
            gc.save();
            gc.setGlobalAlpha(sunStrength);
            gc.setFill(SUN_BEAM_GRAD);
            gc.fillRect(0, 0, visibleW, visibleH);
            gc.restore();
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
            if (quality == GraphicsQuality.LOW) {
                gc.setFill(Color.color(0.03, 0.05, 0.14, maxDarkness * 0.72));
                gc.fillRect(0, 0, visibleW, visibleH);
            } else {
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
        }

        // 4. Vinheta Atmosférica Permanente (Oclusão suave nas bordas da tela para dar profundidade)
        if (quality != GraphicsQuality.LOW) {
            gc.setFill(VIGNETTE_GRAD);
            gc.fillRect(0, 0, visibleW, visibleH);
        }
    }
}
