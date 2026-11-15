package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.settings.GameSettings;
import br.edu.unex.sentinela.settings.GraphicsQuality;
import br.edu.unex.sentinela.world.BiomeType;
import br.edu.unex.sentinela.world.Camera;
import br.edu.unex.sentinela.world.LightningStrike;
import br.edu.unex.sentinela.world.WeatherDrop;
import br.edu.unex.sentinela.world.WindSystem;
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
        double rainIntensity = world.getRainIntensity();
        double fogIntensity = world.getFogIntensity();
        GraphicsQuality quality = GameSettings.get().getGraphicsQuality();
        BiomeType playerBiome = BiomeType.MEADOW;
        if (world.getPlayer() != null && world.getTileMap() != null) {
            playerBiome = world.getTileMap().getBiomeAtWorld(world.getPlayer().getX(), world.getPlayer().getY());
        }
        boolean inSnow = playerBiome == BiomeType.SNOW;

        if (inSnow) {
            renderSnowfall(visibleW, visibleH, animTime, quality, rainIntensity > 0.4);
        }

        renderFireflies(world, world.getCamera() != null ? world.getCamera().getX() : 0,
                        world.getCamera() != null ? world.getCamera().getY() : 0, visibleW, visibleH);

        if (rainIntensity > 0.01 && !inSnow) {
            var drops = world.getRainDrops();
            int totalDrops = drops.size();
            int activeCount = (int) Math.round(totalDrops * rainIntensity);
            activeCount = Math.max(1, Math.min(totalDrops, activeCount));

            int step = (quality == GraphicsQuality.LOW) ? 2 : 1;
            double dropAlpha = 0.58 * Math.min(1.0, rainIntensity * 1.25);
            Color currentRainColor = Color.web("#B3E5FC", dropAlpha);
            Color currentRippleColor = Color.web("#E1F5FE", 0.35 * Math.min(1.0, rainIntensity * 1.25));

            Camera cam = world.getCamera();
            double deltaTime = world.getLastDeltaTime();
            double camDeltaX = cam.getDeltaX();
            double camDeltaY = cam.getDeltaY();
            double safeDt = Math.max(0.001, deltaTime);

            // Inclinação e alongamento da gota diretamente proporcionais ao vetor físico de vento
            double windVx = world.getWindVx();
            double windVy = world.getWindVy();
            double physicalVx = -35.0 + (windVx * 340.0);
            double physicalVy = 620.0 + (windVy * 80.0);
            double streakScale = 0.026;
            double streakDx = Math.max(-34.0, Math.min(34.0, physicalVx * streakScale));
            double streakDy = Math.max(8.0, Math.min(36.0, physicalVy * streakScale));

            double stormInt = world.getStormIntensity();
            gc.setStroke(currentRainColor);
            gc.setLineWidth(1.3 + stormInt * 0.4);
            for (int i = 0; i < activeCount; i += step) {
                WeatherDrop drop = drops.get(i);
                double dx = drop.getX();
                double dy = drop.getY();
                gc.strokeLine(dx, dy, dx + streakDx, dy + streakDy);

                // Pequenos anéis de respingo (water splash ripples) no chão quando a gota está no fim do curso
                if (quality != GraphicsQuality.LOW && rainIntensity > 0.25 && ((int) (dy + dx) % 15) == 0) {
                    gc.setStroke(currentRippleColor);
                    gc.strokeOval(dx + streakDx - 2.5, dy + streakDy - 1.2, 5, 2.5);
                    gc.setStroke(currentRainColor);
                }
            }

            // Atmosfera nublada azul-acinzentada fria com reforço gradual de tempestade
            double cloudAlpha = Math.min(0.55, 0.18 * rainIntensity + 0.25 * stormInt);
            gc.setFill(Color.color(0.04, 0.08, 0.18, cloudAlpha));
            gc.fillRect(0, 0, visibleW, visibleH);

        }

        if (fogIntensity > 0.01) {
            gc.save();
            gc.setGlobalAlpha(fogIntensity);
            gc.setFill(quality == GraphicsQuality.LOW ? FOG_LOW_COLOR : FOG_GRAD);
            gc.fillRect(0, 0, visibleW, visibleH);
            gc.restore();
        }

        Camera cam = world.getCamera();
        double camX = (cam != null) ? cam.getX() : 0.0;
        double camY = (cam != null) ? cam.getY() : 0.0;

        // Vento diegético (linhas de corrente de ar e folhas voadoras)
        renderWindVisuals(world, camX, camY, visibleW, visibleH);

        // Raios procedurais e avisos eletrostáticos no solo
        renderLightningStrikes(world, camX, camY, visibleW, visibleH);

        // Flash de tela relâmpago
        double flash = world.getLightningFlash();
        if (flash > 0.01) {
            gc.setFill(Color.color(0.92, 0.96, 1.0, Math.min(0.85, flash * 0.85)));
            gc.fillRect(0, 0, visibleW, visibleH);
        }
    }

    private void renderWindVisuals(World world, double camX, double camY, double visibleW, double visibleH) {
        WindSystem wind = world.getWindSystem();
        if (wind == null) return;

        // 1. Linhas aerodinâmicas de corrente de ar (WindStreaks)
        var streaks = wind.getStreaks();
        if (streaks != null && !streaks.isEmpty()) {
            gc.setLineWidth(1.3);
            for (WindSystem.WindStreak s : streaks) {
                double sx = s.x - camX;
                double sy = s.y - camY;
                if (sx < -80 || sx > visibleW + 80 || sy < -80 || sy > visibleH + 80) continue;
                double a = s.getAlpha();
                if (a <= 0.01) continue;

                gc.setStroke(Color.color(0.92, 0.96, 1.0, a));
                double endX = sx + Math.cos(s.angle) * s.length;
                double endY = sy + Math.sin(s.angle) * s.length;
                gc.strokeLine(sx, sy, endX, endY);
            }
        }

        // 2. Folhas e pétalas voadoras rodopiando em 3D (WindLeaves)
        var leaves = wind.getLeaves();
        if (leaves != null && !leaves.isEmpty()) {
            for (WindSystem.WindLeaf l : leaves) {
                double sx = l.x - camX;
                double sy = l.y - camY - l.z;
                if (sx < -40 || sx > visibleW + 40 || sy < -40 || sy > visibleH + 40) continue;
                double a = l.getAlpha();
                if (a <= 0.01) continue;

                gc.save();
                gc.translate(sx, sy);
                gc.rotate(l.rot);
                gc.scale(Math.cos(l.flutterPhase), 1.0);
                gc.setFill(l.color.deriveColor(0, 1.0, 1.0, a));
                gc.fillOval(-l.size * 0.5, -l.size * 0.28, l.size, l.size * 0.56);
                gc.restore();
            }
        }
    }

    private void renderLightningStrikes(World world, double camX, double camY, double visibleW, double visibleH) {
        var strikes = world.getActiveLightningStrikes();
        if (strikes == null || strikes.isEmpty()) return;

        for (LightningStrike strike : strikes) {
            double sx = strike.getX() - camX;
            double sy = strike.getY() - camY;

            if (sx < -100 || sx > visibleW + 100 || sy < -100 || sy > visibleH + 100) {
                if (!strike.isStriking()) continue;
            }

            if (strike.isWarning()) {
                double p = strike.getWarningProgress();
                double radius = 38.0 * (1.0 - p * 0.22);

                // Círculo elétrico de aviso no solo
                gc.setStroke(Color.web("#00E5FF", 0.40 + p * 0.50));
                gc.setLineWidth(1.8);
                gc.strokeOval(sx - radius, sy - radius * 0.55, radius * 2.0, radius * 1.1);

                gc.setStroke(Color.web("#FFD54F", 0.30 + p * 0.60));
                gc.setLineWidth(1.2);
                gc.strokeOval(sx - radius * 0.65, sy - radius * 0.35, radius * 1.3, radius * 0.7);

                // Faíscas convergentes de carga estática
                for (double[] spark : strike.getSparkOffsets()) {
                    double angle = spark[0];
                    double baseDist = spark[1];
                    double speed = spark[2];
                    double curDist = baseDist * (1.0 - p * 0.75);

                    double spX = sx + Math.cos(angle + p * speed) * curDist;
                    double spY = sy + Math.sin(angle + p * speed) * (curDist * 0.55);

                    Color spColor = (p > 0.65 && ((int) (p * 24) % 2 == 0)) ? Color.web("#FFF59D", 0.95) : Color.web("#80D8FF", 0.85);
                    gc.setFill(spColor);
                    gc.fillOval(spX - 1.5, spY - 1.5, 3.0, 3.0);
                }
            } else if (strike.isStriking()) {
                // Relâmpago procedural descendo do céu
                var mainPoints = strike.getMainBoltPoints();
                if (mainPoints != null && mainPoints.size() > 1) {
                    // 1. Halo elétrico ciano espesso
                    gc.setStroke(Color.web("#00E5FF", 0.65));
                    gc.setLineWidth(6.0);
                    for (int i = 0; i < mainPoints.size() - 1; i++) {
                        double[] p1 = mainPoints.get(i);
                        double[] p2 = mainPoints.get(i + 1);
                        gc.strokeLine(p1[0] - camX, p1[1] - camY, p2[0] - camX, p2[1] - camY);
                    }

                    // 2. Núcleo branco incandescente
                    gc.setStroke(Color.web("#FFFFFF", 0.98));
                    gc.setLineWidth(2.6);
                    for (int i = 0; i < mainPoints.size() - 1; i++) {
                        double[] p1 = mainPoints.get(i);
                        double[] p2 = mainPoints.get(i + 1);
                        gc.strokeLine(p1[0] - camX, p1[1] - camY, p2[0] - camX, p2[1] - camY);
                    }
                }

                // Galhos laterais bifurcados
                var branches = strike.getBranchBolts();
                if (branches != null) {
                    gc.setStroke(Color.web("#80D8FF", 0.85));
                    gc.setLineWidth(1.4);
                    for (var branch : branches) {
                        for (int i = 0; i < branch.size() - 1; i++) {
                            double[] p1 = branch.get(i);
                            double[] p2 = branch.get(i + 1);
                            gc.strokeLine(p1[0] - camX, p1[1] - camY, p2[0] - camX, p2[1] - camY);
                        }
                    }
                }

                // Explosão luminosa de impacto no solo
                gc.setFill(Color.web("#FFFFFF", 0.95));
                gc.fillOval(sx - 24, sy - 12, 48, 24);
                gc.setFill(Color.web("#00E5FF", 0.55));
                gc.fillOval(sx - 44, sy - 22, 88, 44);
            } else if (strike.isLingering()) {
                double progress = strike.getLingerProgress();
                // Solo chamuscado e cinzas
                gc.setFill(Color.color(0.08, 0.08, 0.10, progress * 0.60));
                gc.fillOval(sx - 18, sy - 9, 36, 18);

                // Brasas e micro faíscas residuais
                gc.setFill(Color.color(1.0, 0.60, 0.10, progress * 0.75));
                gc.fillOval(sx - 1.5, sy - 4 - (1.0 - progress) * 16.0, 3.0, 3.0);
            }
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

    public void renderFireflies(World world, double camX, double camY, double visibleW, double visibleH) {
        if (world == null || world.getFireflies() == null) return;
        var fireflies = world.getFireflies();
        if (fireflies.isEmpty()) return;

        GraphicsQuality quality = GameSettings.get().getGraphicsQuality();

        for (br.edu.unex.sentinela.world.Firefly f : fireflies) {
            double fx = f.getX() - camX;
            double fy = f.getY() - camY - f.getFloatZ();

            if (fx < -25 || fx > visibleW + 25 || fy < -25 || fy > visibleH + 25) continue;

            double glow = f.getGlowAlpha();
            if (glow <= 0.04) continue;

            if (quality != GraphicsQuality.LOW) {
                RadialGradient halo = new RadialGradient(
                    0, 0, fx, fy, 18.0, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.color(0.85, 0.98, 0.35, glow * 0.38)),
                    new Stop(0.5, Color.color(0.55, 0.90, 0.25, glow * 0.16)),
                    new Stop(1.0, Color.color(0.20, 0.70, 0.15, 0.0))
                );
                gc.setFill(halo);
                gc.fillOval(fx - 18, fy - 18, 36, 36);
            }

            gc.setFill(Color.color(0.98, 1.0, 0.70, Math.min(1.0, glow * 1.25)));
            gc.fillOval(fx - 1.8, fy - 1.8, 3.6, 3.6);
            gc.setFill(Color.web("#FFFFFF", glow));
            gc.fillOval(fx - 1.0, fy - 1.0, 2.0, 2.0);
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
