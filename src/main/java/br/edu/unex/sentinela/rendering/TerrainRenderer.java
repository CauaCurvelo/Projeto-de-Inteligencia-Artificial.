package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.settings.GraphicsQuality;
import br.edu.unex.sentinela.world.BiomeType;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/**
 * Responsável pela renderização procedural de terrenos (grama, terra, lama, água, grama alta)
 * separada em base estática (para cache de chunks) e overlays animados leves de água/mata alta.
 */
public class TerrainRenderer {
    private static final Color WALL_BASE = Color.web("#202428");
    private static final Color FLOWER_SHADOW = Color.color(0.08, 0.18, 0.05, 0.45);
    private static final Color FLOWER_YELLOW = Color.web("#FFF59D");
    private static final Color FLOWER_LAVENDER = Color.web("#E1BEE7");
    private static final Color TALL_GRASS_AO = Color.color(0.04, 0.10, 0.03, 0.50);
    private static final Color TALL_GRASS_ROOT = Color.color(0.02, 0.07, 0.02, 0.55);
    private static final Color TALL_GRASS_STEM = Color.web("#1E4618");
    private static final Color TALL_GRASS_TIP_A = Color.web("#4E8C3A");
    private static final Color TALL_GRASS_TIP_B = Color.web("#66A64F");
    private static final Color PEBBLE_SHADOW = Color.color(0.12, 0.08, 0.04, 0.55);
    private static final Color PEBBLE_BODY = Color.web("#8D7762");
    private static final Color PEBBLE_TOP = Color.web("#BCAAA4", 0.80);
    private static final Color MUD_GLOSS_A = Color.web("#8D6E63", 0.45);
    private static final Color MUD_GLOSS_B = Color.web("#CFD8DC", 0.30);
    private static final Color WATER_DEEP = Color.web("#124675");
    private static final Color WATER_SHALLOW = Color.web("#1B639E");
    private static final Color WATER_ABYSS = Color.web("#0D3357", 0.55);
    private static final Color WATER_SAND_TINT = Color.web("#2980B9", 0.45);
    private static final Color WATER_WAVE = Color.web("#64B5F6", 0.45);
    private static final Color WATER_SPARKLE = Color.web("#E1F5FE", 0.50);
    private static final Color GRASS_EDGE = Color.web("#3D732E", 0.85);
    private static final Color BIOME_AO = Color.color(0.05, 0.08, 0.03, 0.32);

    private final GraphicsContext gc;

    public TerrainRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    public void renderGroundTile(TileType type, double sx, double sy, int ts, int col, int row, TileMap tileMap, double animTime) {
        renderStaticTerrainTile(type, sx, sy, ts, col, row, tileMap, GraphicsQuality.HIGH);
        renderAnimatedTileOverlay(type, sx, sy, ts, col, row, animTime, GraphicsQuality.HIGH);
    }

    /**
     * Renderiza apenas os detalhes estáticos do tile (destinado a gravação única em cache de chunk).
     */
    public void renderStaticTerrainTile(TileType type, double sx, double sy, int ts, int col, int row,
                                        TileMap tileMap, GraphicsQuality quality) {
        BiomeType biome = tileMap.getBiomeAt(col, row);
        BiomeTerrainPalette pal = BiomeTerrainPalette.of(biome);

        if (quality == GraphicsQuality.LOW) {
            renderFastLowDetailStaticTile(type, sx, sy, ts, col, row, tileMap, pal);
            renderBiomeTransitionsAndAO(type, sx, sy, ts, col, row, tileMap, biome, pal);
            return;
        }

        double macroNoise = smoothCoordNoise(col * 0.14, row * 0.14, 71L);
        double microNoise = coordHash(col, row, 193L);

        switch (type) {
            case GRASS:
            case TREE: // O chão embaixo da árvore é grama sombreada
                renderDetailedGrass(sx, sy, ts, col, row, macroNoise, microNoise, type == TileType.TREE, biome, pal);
                break;

            case TALL_GRASS:
                renderDetailedTallGrassStatic(sx, sy, ts, col, row, macroNoise, biome, pal);
                break;

            case DIRT_PATH:
                renderDetailedDirtPath(sx, sy, ts, col, row, macroNoise, microNoise, pal);
                break;

            case MUD:
                renderDetailedMud(sx, sy, ts, col, row, macroNoise, pal);
                break;

            case WATER:
                renderDetailedWaterStatic(sx, sy, ts, col, row, tileMap, biome, pal);
                break;

            case WALL:
                gc.setFill(WALL_BASE);
                gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);
                break;
        }

        // Aplica Oclusão de Ambiente (Ambient Occlusion) e transições suaves de borda entre biomas
        renderBiomeTransitionsAndAO(type, sx, sy, ts, col, row, tileMap, biome, pal);
    }

    public void renderAnimatedTileOverlay(TileType type, double sx, double sy, int ts, int col, int row,
                                          double animTime, GraphicsQuality quality) {
        renderAnimatedTileOverlay(type, sx, sy, ts, col, row, animTime, quality, BiomeType.MEADOW);
    }

    /**
     * Overlay animado leve aplicado por frame somente em tiles de Água e Mata Alta visíveis.
     */
    public void renderAnimatedTileOverlay(TileType type, double sx, double sy, int ts, int col, int row,
                                          double animTime, GraphicsQuality quality, BiomeType biome) {
        if (quality == GraphicsQuality.LOW) return;
        BiomeTerrainPalette pal = BiomeTerrainPalette.of(biome);

        if (type == TileType.WATER) {
            double waveShift = Math.sin(animTime * 1.8 + col * 0.9 + row * 0.6) * 3.0;
            gc.setStroke(pal.waterRimLight);
            gc.setLineWidth(1.4);
            gc.strokeArc(sx + 4 + waveShift, sy + 6, 12, 5, 15, 150, ArcType.OPEN);
            gc.strokeArc(sx + 14 - waveShift, sy + 18, 11, 4, 195, 150, ArcType.OPEN);

            if (quality == GraphicsQuality.HIGH) {
                double sparkle = Math.sin(animTime * 3.5 + col * 1.7 - row * 1.3);
                if (sparkle > 0.35) {
                    gc.setFill(pal.waterFoam);
                    gc.fillOval(sx + 9 + waveShift * 0.5, sy + 10, 4.5, 2.0);
                }
            }
        } else if (type == TileType.TALL_GRASS && quality == GraphicsQuality.HIGH) {
            double sway = Math.sin(animTime * 2.2 + col * 0.7 + row * 0.5) * 2.2;
            gc.setLineWidth(1.5);
            for (int i = 0; i < 4; i++) {
                double hX = coordHash(col * 3 + i, row * 7 + i, 2001L);
                double hY = coordHash(col * 5 + i, row * 11 + i, 2002L);
                double bx = sx + 4 + hX * (ts - 8);
                double by = sy + 9 + hY * (ts - 11);
                double height = 9.5 + (i % 2) * 3.0;
                gc.setStroke(i % 2 == 0 ? pal.tallGrassTipA : pal.tallGrassTipB);
                gc.strokeLine(bx + sway * 0.35, by - height * 0.4, bx + sway + (hX - 0.5) * 3.5, by - height);
            }
        }
    }

    private void renderFastLowDetailStaticTile(TileType type, double sx, double sy, int ts, int col, int row,
                                               TileMap tileMap, BiomeTerrainPalette pal) {
        switch (type) {
            case GRASS:
            case TREE:
                gc.setFill(type == TileType.TREE ? pal.tallGrassStem : Color.hsb(pal.grassHueBase, pal.grassSatBase, pal.grassBriOpen));
                gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);
                break;
            case TALL_GRASS:
                gc.setFill(pal.tallGrassStem);
                gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);
                break;
            case DIRT_PATH:
                gc.setFill(Color.hsb(pal.dirtHue, pal.dirtSat, pal.dirtBri));
                gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);
                break;
            case MUD:
                gc.setFill(Color.hsb(pal.mudHue, pal.mudSat, pal.mudBri));
                gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);
                break;
            case WATER:
                boolean isDeep = tileMap.getTileAt(col, row - 1) == TileType.WATER
                        && tileMap.getTileAt(col, row + 1) == TileType.WATER
                        && tileMap.getTileAt(col - 1, row) == TileType.WATER
                        && tileMap.getTileAt(col + 1, row) == TileType.WATER;
                gc.setFill(isDeep ? pal.waterDeep : pal.waterShallow);
                gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);
                break;
            case WALL:
                gc.setFill(WALL_BASE);
                gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);
                break;
        }
    }

    private void renderDetailedGrass(double sx, double sy, int ts, int col, int row, double macro, double micro,
                                     boolean underTree, BiomeType biome, BiomeTerrainPalette pal) {
        double hue = pal.grassHueBase + (macro - 0.5) * 14.0;
        double sat = pal.grassSatBase + (micro - 0.5) * 0.10;
        double bri = (underTree ? pal.grassBriShade : pal.grassBriOpen) + (macro - 0.5) * 0.08 + (micro - 0.5) * 0.04;
        double minSat = (biome == BiomeType.SNOW) ? 0.05 : 0.25;
        double maxBri = (biome == BiomeType.SNOW) ? 0.90 : 0.62;
        Color baseColor = Color.hsb(hue, clamp(sat, minSat, 0.72), clamp(bri, 0.20, maxBri));

        gc.setFill(baseColor);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        double patchOffset = (coordHash(col, row, 31L) - 0.5) * 8.0;
        gc.setFill(baseColor.deriveColor((macro - 0.5) * 8, 1.05, biome == BiomeType.SNOW ? 0.96 : 0.92, 0.45));
        gc.fillOval(sx + 2 + patchOffset, sy + 2 - patchOffset * 0.5, ts - 4, ts - 4);

        if (biome == BiomeType.SNOW) {
            renderSnowGroundDetails(sx, sy, ts, col, row, underTree);
            return;
        }

        int tuftCount = (biome == BiomeType.HIGHLAND ? 2 : 3) + (int) (coordHash(col, row, 89L) * 3);
        Color shadowColor = baseColor.deriveColor(0, 1.15, 0.62, 0.65);
        Color bladeMid = baseColor.deriveColor(-3, 1.1, 0.85, 0.90);
        Color bladeTip = baseColor.deriveColor(5, 0.92, 1.28, 0.85);

        for (int i = 0; i < tuftCount; i++) {
            double h1 = coordHash(col * 7 + i, row * 13 + i, 501L);
            double h2 = coordHash(col * 11 + i, row * 5 + i, 907L);
            double tx = sx + 4 + h1 * (ts - 8);
            double ty = sy + 6 + h2 * (ts - 10);

            gc.setFill(shadowColor);
            gc.fillOval(tx - 2.5, ty + 1.0, 6.0, 2.8);

            gc.setStroke(bladeMid);
            gc.setLineWidth(1.3);
            gc.strokeLine(tx, ty + 2, tx - 1.8, ty - 2.5);
            gc.strokeLine(tx + 1.5, ty + 2, tx + 2.8, ty - 2.0);

            gc.setStroke(bladeTip);
            gc.setLineWidth(1.0);
            gc.strokeLine(tx - 0.8, ty, tx - 2.2, ty - 3.5);
            gc.strokeLine(tx + 0.5, ty + 1, tx + 1.2, ty - 4.0);
        }

        // Lascas de granito ocasionais no solo da Serra
        if (biome == BiomeType.HIGHLAND && !underTree && coordHash(col, row, 711L) > 0.72) {
            double rx = sx + 6 + coordHash(col, row, 733L) * (ts - 14);
            double ry = sy + 8 + coordHash(col, row, 751L) * (ts - 14);
            gc.setFill(Color.web("#78909C", 0.72));
            gc.fillOval(rx, ry, 5.5, 3.6);
            gc.setFill(Color.web("#CFD8DC", 0.55));
            gc.fillOval(rx + 0.6, ry + 0.4, 2.8, 1.6);
        }

        // Samambaias na Mata Fechada e musgos no Pântano
        if (biome == BiomeType.DENSE_FOREST && coordHash(col, row, 821L) > 0.78) {
            double cx = sx + 7 + coordHash(col, row, 823L) * (ts - 14);
            double cy = sy + 10 + coordHash(col, row, 827L) * (ts - 14);
            gc.setStroke(pal.tallGrassTipB);
            gc.setLineWidth(1.4);
            gc.strokeLine(cx, cy, cx - 4.5, cy - 3.0);
            gc.strokeLine(cx, cy, cx, cy - 5.2);
            gc.strokeLine(cx, cy, cx + 4.5, cy - 3.0);
        }

        // Flores silvestres / orquídeas / lírios regionais
        if (!underTree && coordHash(col, row, 1237L) > pal.flowerDensityThreshold) {
            double fx = sx + 8 + coordHash(col, row, 1400L) * (ts - 16);
            double fy = sy + 8 + coordHash(col, row, 1500L) * (ts - 16);
            gc.setFill(FLOWER_SHADOW);
            gc.fillOval(fx - 2, fy, 5, 3);
            gc.setFill(coordHash(col, row, 1600L) > 0.5 ? pal.flowerPrimary : pal.flowerSecondary);
            gc.fillOval(fx - 1.8, fy - 1.8, 3.8, 3.8);
            gc.setFill(pal.flowerCenter);
            gc.fillOval(fx - 0.6, fy - 0.6, 1.4, 1.4);
        }
    }

    private void renderSnowGroundDetails(double sx, double sy, int ts, int col, int row, boolean underTree) {
        if (!underTree && coordHash(col, row, 611L) > 0.42) {
            double dx = sx + 5 + coordHash(col, row, 613L) * (ts - 16);
            double dy = sy + 7 + coordHash(col, row, 617L) * (ts - 14);
            gc.setFill(Color.web("#FFFFFF", 0.58));
            gc.fillOval(dx, dy, 9.0, 3.6);
        }
        if (coordHash(col, row, 631L) > 0.68) {
            double gx = sx + 6 + coordHash(col, row, 641L) * (ts - 12);
            double gy = sy + 6 + coordHash(col, row, 643L) * (ts - 12);
            gc.setFill(Color.web("#7DD3FC", 0.72));
            gc.fillRect(gx, gy, 2.0, 2.0);
        }
        if (!underTree && coordHash(col, row, 653L) > 0.76) {
            double rx = sx + 7 + coordHash(col, row, 659L) * (ts - 15);
            double ry = sy + 9 + coordHash(col, row, 661L) * (ts - 15);
            gc.setFill(Color.web("#64748B", 0.65));
            gc.fillOval(rx, ry, 5.2, 3.4);
            gc.setFill(Color.web("#F1F5F9", 0.85));
            gc.fillOval(rx + 0.5, ry - 0.3, 3.8, 1.8);
        }
    }

    private void renderDetailedTallGrassStatic(double sx, double sy, int ts, int col, int row,
                                               double macro, BiomeType biome, BiomeTerrainPalette pal) {
        double bri = 0.28 + (macro - 0.5) * 0.06;
        Color deepGround = Color.hsb(pal.tallGrassHue, pal.tallGrassSat, clamp(bri, 0.20, 0.36));
        gc.setFill(deepGround);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        gc.setFill(TALL_GRASS_AO);
        gc.fillOval(sx + 1, sy + 4, ts - 2, ts - 5);

        for (int i = 0; i < 7; i++) {
            double hX = coordHash(col * 3 + i, row * 7 + i, 2001L);
            double hY = coordHash(col * 5 + i, row * 11 + i, 2002L);
            double bx = sx + 3 + hX * (ts - 6);
            double by = sy + 8 + hY * (ts - 10);
            double height = 10.0 + (i % 3) * 3.5;

            gc.setFill(TALL_GRASS_ROOT);
            gc.fillOval(bx - 2, by - 1, 5, 2.5);

            gc.setStroke(pal.tallGrassStem);
            gc.setLineWidth(2.2);
            gc.strokeLine(bx, by, bx, by - height * 0.65);

            gc.setStroke(i % 2 == 0 ? pal.tallGrassTipA : pal.tallGrassTipB);
            gc.setLineWidth(1.3);
            gc.strokeLine(bx, by - height * 0.45, bx + (hX - 0.5) * 3.0, by - height * 0.85);

            // Taboas (cattails) nos juncos altos do Pântano
            if (biome == BiomeType.SWAMP && i % 3 == 0) {
                double tipX = bx + (hX - 0.5) * 2.2;
                double tipY = by - height * 0.78;
                gc.setFill(Color.web("#5D4037"));
                gc.fillRoundRect(tipX - 1.3, tipY - 3.2, 2.6, 4.6, 2, 2);
            }
        }
    }

    private void renderDetailedDirtPath(double sx, double sy, int ts, int col, int row,
                                        double macro, double micro, BiomeTerrainPalette pal) {
        double hue = pal.dirtHue + (macro - 0.5) * 6.0;
        double sat = pal.dirtSat + (micro - 0.5) * 0.08;
        double bri = pal.dirtBri + (macro - 0.5) * 0.07;
        Color dirtBase = Color.hsb(hue, clamp(sat, 0.20, 0.60), clamp(bri, 0.28, 0.58));

        gc.setFill(dirtBase);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        gc.setFill(dirtBase.deriveColor(-2, 1.08, 0.84, 0.65));
        gc.fillOval(sx + 2, sy + 2, ts - 4, ts - 4);

        gc.setFill(dirtBase.deriveColor(2, 0.90, 1.16, 0.35));
        gc.fillOval(sx + 4, sy + 3, ts - 10, ts - 12);

        int pebbleCount = 2 + (int) (coordHash(col, row, 3001L) * 4);
        for (int i = 0; i < pebbleCount; i++) {
            double px = sx + 4 + coordHash(col + i * 13, row - i * 7, 3100L) * (ts - 8);
            double py = sy + 4 + coordHash(col - i * 9, row + i * 17, 3200L) * (ts - 8);
            double pw = 2.5 + coordHash(col * i, row + i, 3300L) * 3.5;
            double ph = pw * 0.75;

            gc.setFill(PEBBLE_SHADOW);
            gc.fillOval(px + 0.8, py + 1.0, pw, ph);

            gc.setFill(PEBBLE_BODY);
            gc.fillOval(px, py, pw, ph);

            gc.setFill(PEBBLE_TOP);
            gc.fillOval(px + 0.4, py + 0.3, pw * 0.55, ph * 0.55);
        }
    }

    private void renderDetailedMud(double sx, double sy, int ts, int col, int row, double macro, BiomeTerrainPalette pal) {
        double bri = pal.mudBri + (macro - 0.5) * 0.06;
        Color mudBase = Color.hsb(pal.mudHue, pal.mudSat, clamp(bri, 0.16, 0.38));
        gc.setFill(mudBase);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        double ox = (coordHash(col, row, 4050L) - 0.5) * 10.0;
        double oy = (coordHash(col, row, 4070L) - 0.5) * 8.0;
        gc.setFill(mudBase.deriveColor(0, 1.08, 0.82, 0.42));
        gc.fillOval(sx + 2 + ox, sy + 3 + oy, ts - 4, ts - 6);

        if (coordHash(col, row, 4090L) > 0.35) {
            double rx = sx + 6 + coordHash(col, row, 4001L) * 11;
            double ry = sy + 6 + coordHash(col, row, 4002L) * 11;
            gc.setFill(MUD_GLOSS_A.deriveColor(0, 1.0, 1.0, 0.65));
            gc.fillOval(rx, ry, 8, 3.6);
            gc.setFill(MUD_GLOSS_B);
            gc.fillOval(rx + 1.5, ry + 0.8, 3.5, 1.5);
        }
    }

    private void renderDetailedWaterStatic(double sx, double sy, int ts, int col, int row,
                                           TileMap tileMap, BiomeType biome, BiomeTerrainPalette pal) {
        boolean nWater = tileMap.getTileAt(col, row - 1) == TileType.WATER;
        boolean sWater = tileMap.getTileAt(col, row + 1) == TileType.WATER;
        boolean wWater = tileMap.getTileAt(col - 1, row) == TileType.WATER;
        boolean eWater = tileMap.getTileAt(col + 1, row) == TileType.WATER;
        boolean isDeep = nWater && sWater && wWater && eWater;

        gc.setFill(isDeep ? pal.waterDeep : pal.waterShallow);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        if (isDeep) {
            gc.setFill(pal.waterAbyss);
            gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);
        } else {
            gc.setFill(pal.waterSandTint);
            gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);
        }

        // Vitórias-régias ocasionais nas águas calmas do Pântano Sombrio
        if (biome == BiomeType.SWAMP && coordHash(col, row, 5113L) > 0.85) {
            double lx = sx + 6 + coordHash(col, row, 5119L) * (ts - 18);
            double ly = sy + 6 + coordHash(col, row, 5147L) * (ts - 18);
            gc.setFill(Color.color(0.03, 0.12, 0.08, 0.45));
            gc.fillOval(lx + 1, ly + 1.5, 11, 10);
            gc.setFill(Color.web("#388E3C"));
            gc.fillArc(lx, ly, 11, 10, 20, 320, ArcType.ROUND);
            gc.setFill(Color.web("#66BB6A", 0.70));
            gc.fillArc(lx + 1.5, ly + 1.5, 6.5, 6.0, 20, 300, ArcType.ROUND);
            if (coordHash(col, row, 5167L) > 0.62) {
                gc.setFill(Color.web("#F8BBD0"));
                gc.fillOval(lx + 3.5, ly + 3.0, 4.0, 4.0);
                gc.setFill(Color.web("#FFF176"));
                gc.fillOval(lx + 4.8, ly + 4.2, 1.5, 1.5);
            }
        } else if (biome == BiomeType.SNOW && coordHash(col, row, 5209L) > 0.82) {
            // Placas de gelo flutuantes nos lagos/rios glaciais
            double ix = sx + 5 + coordHash(col, row, 5227L) * (ts - 16);
            double iy = sy + 5 + coordHash(col, row, 5233L) * (ts - 15);
            gc.setFill(Color.web("#BAE6FD", 0.78));
            gc.fillRoundRect(ix, iy, 10, 7, 3, 3);
            gc.setFill(Color.web("#F8FAFC", 0.85));
            gc.fillRoundRect(ix + 1, iy + 0.8, 7.5, 4.2, 2, 2);
        }
    }

    /**
     * Renderiza Oclusão de Ambiente (Ambient Occlusion), barrancos 3D na margem da água
     * e chanfros orgânicos entre biomas/terrenos adjacentes para eliminar linhas retas de grade.
     */
    private void renderBiomeTransitionsAndAO(TileType type, double sx, double sy, int ts, int col, int row,
                                             TileMap tileMap, BiomeType biome, BiomeTerrainPalette pal) {
        TileType north = tileMap.getTileAt(col, row - 1);
        TileType south = tileMap.getTileAt(col, row + 1);
        TileType west = tileMap.getTileAt(col - 1, row);
        TileType east = tileMap.getTileAt(col + 1, row);

        // 1. Margem de Rio com barranco 3D (Terra faz sombra dentro da água ao Norte/Oeste)
        if (type == TileType.WATER) {
            if (north != TileType.WATER) {
                gc.setFill(Color.web("#3E2723", 0.75));
                gc.fillRect(sx, sy, ts + 0.6, 3.5);
                gc.setFill(Color.color(0.02, 0.08, 0.14, 0.48));
                gc.fillRect(sx, sy + 3.5, ts + 0.6, 5.5);
                gc.setFill(pal.waterFoam);
                gc.fillRoundRect(sx + 2, sy + 3.5, ts - 4, 2.0, 2, 2);
            }
            if (south != TileType.WATER) {
                gc.setFill(pal.waterRimLight);
                gc.fillRect(sx, sy + ts - 3, ts + 0.6, 3);
            }
            if (west != TileType.WATER) {
                gc.setFill(Color.color(0.02, 0.08, 0.14, 0.40));
                gc.fillRect(sx, sy, 5.0, ts + 0.6);
            }
            if (east != TileType.WATER) {
                gc.setFill(pal.waterRimLight.deriveColor(0, 1.0, 1.0, 0.78));
                gc.fillRect(sx + ts - 3, sy, 3.0, ts + 0.6);
            }
            return;
        }

        // 2. Transição orgânica de Grama invadindo Trilha de Terra ou Lama
        if (type == TileType.DIRT_PATH || type == TileType.MUD) {
            Color grassEdge = pal.grassEdge;
            Color aoShadow = BIOME_AO;

            if (north == TileType.GRASS || north == TileType.TALL_GRASS || north == TileType.TREE) {
                gc.setFill(aoShadow);
                gc.fillRect(sx, sy, ts + 0.6, 5);
                gc.setFill(grassEdge);
                gc.fillOval(sx - 2, sy - 4, 16, 8);
                gc.fillOval(sx + 12, sy - 5, 22, 9);
            }
            if (south == TileType.GRASS || south == TileType.TALL_GRASS || south == TileType.TREE) {
                gc.setFill(grassEdge);
                gc.fillOval(sx + 2, sy + ts - 4, 18, 8);
                gc.fillOval(sx + 16, sy + ts - 3, 16, 7);
            }
            if (west == TileType.GRASS || west == TileType.TALL_GRASS || west == TileType.TREE) {
                gc.setFill(aoShadow);
                gc.fillRect(sx, sy, 4, ts + 0.6);
                gc.setFill(grassEdge);
                gc.fillOval(sx - 4, sy + 2, 8, 16);
                gc.fillOval(sx - 3, sy + 16, 7, 16);
            }
            if (east == TileType.GRASS || east == TileType.TALL_GRASS || east == TileType.TREE) {
                gc.setFill(grassEdge);
                gc.fillOval(sx + ts - 4, sy + 3, 8, 15);
                gc.fillOval(sx + ts - 3, sy + 16, 7, 15);
            }
        }

        // 2B. Mescla suave em fronteiras entre dois biomas diferentes em grama aberta
        if (type == TileType.GRASS) {
            BiomeType bN = tileMap.getBiomeAt(col, row - 1);
            if (bN != biome) {
                gc.setFill(BiomeTerrainPalette.of(bN).grassEdge.deriveColor(0, 1.0, 1.0, 0.35));
                gc.fillOval(sx + 2, sy - 3, 14, 7);
                gc.fillOval(sx + 17, sy - 2, 13, 6);
            }
            BiomeType bS = tileMap.getBiomeAt(col, row + 1);
            if (bS != biome) {
                gc.setFill(BiomeTerrainPalette.of(bS).grassEdge.deriveColor(0, 1.0, 1.0, 0.30));
                gc.fillOval(sx + 4, sy + ts - 4, 13, 7);
                gc.fillOval(sx + 19, sy + ts - 4, 11, 6);
            }
            BiomeType bW = tileMap.getBiomeAt(col - 1, row);
            if (bW != biome) {
                gc.setFill(BiomeTerrainPalette.of(bW).grassEdge.deriveColor(0, 1.0, 1.0, 0.35));
                gc.fillOval(sx - 3, sy + 3, 7, 13);
                gc.fillOval(sx - 2, sy + 17, 6, 12);
            }
            BiomeType bE = tileMap.getBiomeAt(col + 1, row);
            if (bE != biome) {
                gc.setFill(BiomeTerrainPalette.of(bE).grassEdge.deriveColor(0, 1.0, 1.0, 0.30));
                gc.fillOval(sx + ts - 4, sy + 4, 7, 13);
                gc.fillOval(sx + ts - 4, sy + 18, 6, 11);
            }
        }

        // 3. Oclusão de Ambiente (Contact Shadow) ao redor de Paredões de Rocha (WALL)
        if (type != TileType.WALL) {
            if (north == TileType.WALL) {
                gc.setFill(Color.color(0.0, 0.0, 0.0, 0.45));
                gc.fillRect(sx, sy, ts + 0.6, 7);
                gc.setFill(Color.color(0.0, 0.0, 0.0, 0.22));
                gc.fillRect(sx, sy + 7, ts + 0.6, 5);
            }
            if (south == TileType.WALL) {
                gc.setFill(Color.color(0.0, 0.0, 0.0, 0.28));
                gc.fillRect(sx, sy + ts - 5, ts + 0.6, 5);
            }
            if (west == TileType.WALL) {
                gc.setFill(Color.color(0.0, 0.0, 0.0, 0.38));
                gc.fillRect(sx, sy, 6, ts + 0.6);
            }
            if (east == TileType.WALL) {
                gc.setFill(Color.color(0.0, 0.0, 0.0, 0.22));
                gc.fillRect(sx + ts - 4, sy, 4, ts + 0.6);
            }
        }
    }

    private double smoothCoordNoise(double x, double y, long seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double sx = x - x0;
        double sy = y - y0;
        double u = sx * sx * (3.0 - 2.0 * sx);
        double v = sy * sy * (3.0 - 2.0 * sy);

        double n00 = coordHash(x0, y0, seed);
        double n10 = coordHash(x0 + 1, y0, seed);
        double n01 = coordHash(x0, y0 + 1, seed);
        double n11 = coordHash(x0 + 1, y0 + 1, seed);

        return (n00 * (1 - u) + n10 * u) * (1 - v) + (n01 * (1 - u) + n11 * u) * v;
    }

    public static double coordHash(int x, int y, long seed) {
        long n = x * 374761393L + y * 668265263L + seed * 1442695040888963407L;
        n = (n ^ (n >> 13)) * 1274126177L;
        return ((n ^ (n >> 16)) & 0x7FFFFFFFL) / (double) 0x7FFFFFFFL;
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
