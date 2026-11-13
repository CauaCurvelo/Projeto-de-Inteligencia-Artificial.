package br.edu.unex.sentinela.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Amostrador climático contínuo (Temperatura × Umidade × Altitude) para geração orgânica de biomas.
 * Substitui divisões angulares rígidas por bacias climáticas gaussianas em espaço distorcido (domain warp)
 * integradas suavemente a continentes infinitos em FBM multi-oitava.
 */
public final class BiomeSampler {
    private static final int SAFE_SPAWN_TILES = 11;
    private static final double MACRO_SCALE = 0.0086;
    private static final double CLIMATE_OFFSET_X = 13.7;
    private static final double CLIMATE_OFFSET_Y = 29.3;
    private static final double WARP_MACRO_SCALE = 0.011;
    private static final double WARP_DETAIL_SCALE = 0.032;
    private static final double WARP_MACRO_AMP = 12.0;
    private static final double WARP_DETAIL_AMP = 2.6;

    private static final class ClimateBasin {
        final double cx;
        final double cy;
        final double sigmaSq2;
        final double targetTemp;
        final double targetMoisture;
        final double targetAltitude;

        ClimateBasin(double cx, double cy, double sigma,
                     double targetTemp, double targetMoisture, double targetAltitude) {
            this.cx = cx;
            this.cy = cy;
            this.sigmaSq2 = 2.0 * sigma * sigma;
            this.targetTemp = targetTemp;
            this.targetMoisture = targetMoisture;
            this.targetAltitude = targetAltitude;
        }
    }

    private final long worldSeed;
    private final ClimateBasin[] starterBasins;

    public BiomeSampler(long worldSeed) {
        this.worldSeed = worldSeed;
        this.starterBasins = buildStarterBasins(worldSeed);
    }

    private static ClimateBasin[] buildStarterBasins(long seed) {
        Random rng = new Random(seed ^ 0x5EEDB10EL);
        List<BiomeType> ringBiomes = new ArrayList<>(List.of(
            BiomeType.SNOW, BiomeType.SWAMP, BiomeType.DENSE_FOREST, BiomeType.HIGHLAND
        ));
        Collections.shuffle(ringBiomes, rng);

        double baseAngle = rng.nextDouble() * Math.PI * 2.0;
        ClimateBasin[] basins = new ClimateBasin[ringBiomes.size()];
        for (int i = 0; i < ringBiomes.size(); i++) {
            double slotAngle = baseAngle + i * (Math.PI * 0.5) + (rng.nextDouble() - 0.5) * 0.58;
            double dist = 48.0 + rng.nextDouble() * 34.0;
            double bx = Math.cos(slotAngle) * dist;
            double by = Math.sin(slotAngle) * dist;
            double sigma = 31.0 + rng.nextDouble() * 9.0;

            BiomeType b = ringBiomes.get(i);
            switch (b) {
                case SNOW:
                    basins[i] = new ClimateBasin(bx, by, sigma, 0.14, 0.48, 0.52);
                    break;
                case SWAMP:
                    basins[i] = new ClimateBasin(bx, by, sigma, 0.72, 0.88, 0.24);
                    break;
                case DENSE_FOREST:
                    basins[i] = new ClimateBasin(bx, by, sigma, 0.64, 0.72, 0.48);
                    break;
                case HIGHLAND:
                default:
                    basins[i] = new ClimateBasin(bx, by, sigma, 0.58, 0.30, 0.86);
                    break;
            }
        }
        return basins;
    }

    public BiomeType sampleAt(int col, int row) {
        double dist = Math.hypot(col, row);
        if (dist <= SAFE_SPAWN_TILES) {
            return BiomeType.MEADOW;
        }

        // Domain warping duplo orgânico para linhas costeiras e transições naturais
        double wx = col
            + (fbm3(col * WARP_MACRO_SCALE, row * WARP_MACRO_SCALE, 1011L) - 0.5) * 2.0 * WARP_MACRO_AMP
            + (smoothNoise(col * WARP_DETAIL_SCALE, row * WARP_DETAIL_SCALE, 1307L) - 0.5) * 2.0 * WARP_DETAIL_AMP;
        double wy = row
            + (fbm3(col * WARP_MACRO_SCALE, row * WARP_MACRO_SCALE, 2027L) - 0.5) * 2.0 * WARP_MACRO_AMP
            + (smoothNoise(col * WARP_DETAIL_SCALE, row * WARP_DETAIL_SCALE, 2503L) - 0.5) * 2.0 * WARP_DETAIL_AMP;

        double cx = wx * MACRO_SCALE + CLIMATE_OFFSET_X;
        double cy = wy * MACRO_SCALE + CLIMATE_OFFSET_Y;
        double temperature = stretchClimate(fbm3(cx, cy, 1907L));
        double moisture = stretchClimate(fbm3(cx, cy, 3109L));
        double altitude = stretchClimate(fbm3(cx, cy, 4517L));

        double warpedDist = Math.hypot(wx, wy);

        // Bacias climáticas gaussianas suaves perto da região inicial (sem divisões em fatias retas)
        if (warpedDist < 145.0) {
            double totalWeight = 0.0;
            double accTemp = 0.0;
            double accMoist = 0.0;
            double accAlt = 0.0;

            for (ClimateBasin basin : starterBasins) {
                double dx = wx - basin.cx;
                double dy = wy - basin.cy;
                double w = Math.exp(-(dx * dx + dy * dy) / basin.sigmaSq2);
                if (w > 0.002) {
                    totalWeight += w;
                    accTemp += basin.targetTemp * w;
                    accMoist += basin.targetMoisture * w;
                    accAlt += basin.targetAltitude * w;
                }
            }

            double spawnGuard = smoothstep(SAFE_SPAWN_TILES, 26.0, warpedDist);
            double outerFade = 1.0 - smoothstep(95.0, 145.0, warpedDist);
            double influence = Math.min(0.92, totalWeight) * spawnGuard * outerFade;
            if (totalWeight > 0.001 && influence > 0.001) {
                temperature = lerp(temperature, accTemp / totalWeight, influence);
                moisture = lerp(moisture, accMoist / totalWeight, influence);
                altitude = lerp(altitude, accAlt / totalWeight, influence);
            }
        }

        // Mistura orgânica na saída da clareira inicial (borda deformada pelo domain warp)
        if (warpedDist < 26.0) {
            double edgeHold = 1.0 - smoothstep(SAFE_SPAWN_TILES, 26.0, warpedDist);
            temperature = lerp(temperature, 0.58, edgeHold * 0.72);
            moisture = lerp(moisture, 0.35, edgeHold * 0.72);
            altitude = lerp(altitude, 0.36, edgeHold * 0.72);
        }

        return classify(temperature, moisture, altitude);
    }

    private static BiomeType classify(double temperature, double moisture, double altitude) {
        if (temperature < 0.28 || (altitude >= 0.75 && temperature < 0.38)) {
            return BiomeType.SNOW;
        }
        if (altitude >= 0.69) {
            return BiomeType.HIGHLAND;
        }
        if (moisture >= 0.58 && altitude < 0.60) {
            return BiomeType.SWAMP;
        }
        if (moisture >= 0.42) {
            return BiomeType.DENSE_FOREST;
        }
        return BiomeType.MEADOW;
    }

    private double fbm3(double x, double y, long seedOffset) {
        return smoothNoise(x, y, seedOffset) * 0.70
             + smoothNoise(x * 2.02, y * 2.02, seedOffset + 7919L) * 0.22
             + smoothNoise(x * 3.95, y * 3.95, seedOffset + 15485L) * 0.08;
    }

    private double smoothNoise(double x, double y, long seedOffset) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double sx = x - x0;
        double sy = y - y0;
        double u = sx * sx * (3.0 - 2.0 * sx);
        double v = sy * sy * (3.0 - 2.0 * sy);

        double n00 = hash01(x0, y0, worldSeed + seedOffset);
        double n10 = hash01(x0 + 1, y0, worldSeed + seedOffset);
        double n01 = hash01(x0, y0 + 1, worldSeed + seedOffset);
        double n11 = hash01(x0 + 1, y0 + 1, worldSeed + seedOffset);

        double nx0 = n00 * (1.0 - u) + n10 * u;
        double nx1 = n01 * (1.0 - u) + n11 * u;
        return nx0 * (1.0 - v) + nx1 * v;
    }

    private static double hash01(int x, int y, long seed) {
        long n = x * 374761393L + y * 668265263L + seed * 1442695040888963407L;
        n = (n ^ (n >> 13)) * 1274126177L;
        return ((n ^ (n >> 16)) & 0x7FFFFFFFL) / (double) 0x7FFFFFFFL;
    }

    private static double stretchClimate(double value) {
        double scaled = (value - 0.5) * 1.62 + 0.5;
        return Math.max(0.0, Math.min(1.0, scaled));
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0.0, Math.min(1.0, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
