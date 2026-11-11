package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Responsável pela renderização procedural de terrenos (grama, terra, lama, água, grama alta)
 * com variação cromática contínua, micro-relevo iluminado, sombras de contato e oclusão de ambiente (AO).
 */
public class TerrainRenderer {
    private final GraphicsContext gc;

    public TerrainRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    public void renderGroundTile(TileType type, double sx, double sy, int ts, int col, int row, TileMap tileMap, double animTime) {
        double macroNoise = smoothCoordNoise(col * 0.14, row * 0.14, 71L);
        double microNoise = coordHash(col, row, 193L);

        switch (type) {
            case GRASS:
            case TREE: // O chão embaixo da árvore é grama sombreada
                renderDetailedGrass(sx, sy, ts, col, row, macroNoise, microNoise, type == TileType.TREE);
                break;

            case TALL_GRASS:
                renderDetailedTallGrass(sx, sy, ts, col, row, macroNoise, microNoise, animTime);
                break;

            case DIRT_PATH:
                renderDetailedDirtPath(sx, sy, ts, col, row, macroNoise, microNoise, tileMap);
                break;

            case MUD:
                renderDetailedMud(sx, sy, ts, col, row, macroNoise, microNoise);
                break;

            case WATER:
                renderDetailedWater(sx, sy, ts, col, row, tileMap, animTime);
                break;

            case WALL:
                // Base escura sob a rocha caso haja frestas
                gc.setFill(Color.web("#202428"));
                gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);
                break;
        }

        // Aplica Oclusão de Ambiente (Ambient Occlusion) e transições suaves de borda entre biomas
        renderBiomeTransitionsAndAO(type, sx, sy, ts, col, row, tileMap);
    }

    private void renderDetailedGrass(double sx, double sy, int ts, int col, int row, double macro, double micro, boolean underTree) {
        // Variação orgânica de matiz, saturação e brilho (fim do efeito papel de parede)
        double hue = 104.0 + (macro - 0.5) * 16.0;
        double sat = 0.48 + (micro - 0.5) * 0.10;
        double bri = (underTree ? 0.36 : 0.46) + (macro - 0.5) * 0.09 + (micro - 0.5) * 0.04;
        Color baseColor = Color.hsb(hue, clamp(sat, 0.3, 0.7), clamp(bri, 0.25, 0.62));

        // +0.6 evita frestas de subpixel quando há zoom
        gc.setFill(baseColor);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        // Mancha suave de sub-superfície orgânica (musgo/solo úmido)
        double patchOffset = (coordHash(col, row, 31L) - 0.5) * 8.0;
        gc.setFill(baseColor.deriveColor((macro - 0.5) * 8, 1.05, 0.93, 0.45));
        gc.fillOval(sx + 2 + patchOffset, sy + 2 - patchOffset * 0.5, ts - 4, ts - 4);

        // Desenha múltiplos tufos de grama com sombra de base (oclusão) e ponta iluminada pelo sol
        int tuftCount = 3 + (int) (coordHash(col, row, 89L) * 3);
        Color shadowColor = baseColor.deriveColor(0, 1.15, 0.62, 0.65);
        Color bladeMid = baseColor.deriveColor(-3, 1.1, 0.85, 0.90);
        Color bladeTip = baseColor.deriveColor(5, 0.92, 1.28, 0.85);

        for (int i = 0; i < tuftCount; i++) {
            double h1 = coordHash(col * 7 + i, row * 13 + i, 501L);
            double h2 = coordHash(col * 11 + i, row * 5 + i, 907L);
            double tx = sx + 4 + h1 * (ts - 8);
            double ty = sy + 6 + h2 * (ts - 10);

            // Sombra de oclusão na raiz do tufo
            gc.setFill(shadowColor);
            gc.fillOval(tx - 2.5, ty + 1.0, 6.0, 2.8);

            // Folhas da grama (base escura -> ponta iluminada)
            gc.setStroke(bladeMid);
            gc.setLineWidth(1.3);
            gc.strokeLine(tx, ty + 2, tx - 1.8, ty - 2.5);
            gc.strokeLine(tx + 1.5, ty + 2, tx + 2.8, ty - 2.0);

            gc.setStroke(bladeTip);
            gc.setLineWidth(1.0);
            gc.strokeLine(tx - 0.8, ty, tx - 2.2, ty - 3.5);
            gc.strokeLine(tx + 0.5, ty + 1, tx + 1.2, ty - 4.0);
        }

        // Pequenas flores silvestres ou trevos ocasionais em clareiras
        if (!underTree && coordHash(col, row, 1237L) > 0.87) {
            double fx = sx + 8 + coordHash(col, row, 1400L) * (ts - 16);
            double fy = sy + 8 + coordHash(col, row, 1500L) * (ts - 16);
            gc.setFill(Color.color(0.08, 0.18, 0.05, 0.45));
            gc.fillOval(fx - 2, fy, 5, 3);
            gc.setFill(coordHash(col, row, 1600L) > 0.5 ? Color.web("#FFF59D") : Color.web("#E1BEE7"));
            gc.fillOval(fx - 1.5, fy - 1.5, 3.5, 3.5);
        }
    }

    private void renderDetailedTallGrass(double sx, double sy, int ts, int col, int row, double macro, double micro, double animTime) {
        // Solo escuro e denso por baixo da mata alta (oclusão profunda)
        double bri = 0.30 + (macro - 0.5) * 0.06;
        Color deepGround = Color.hsb(108, 0.55, clamp(bri, 0.22, 0.38));
        gc.setFill(deepGround);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        // Sombra de oclusão ambiente acumulada entre os caules
        gc.setFill(Color.color(0.04, 0.10, 0.03, 0.50));
        gc.fillOval(sx + 1, sy + 4, ts - 2, ts - 5);

        double sway = Math.sin(animTime * 2.2 + col * 0.7 + row * 0.5) * 2.0;

        // Camadas de capim alto com profundidade (trás escuro -> frente iluminada)
        for (int i = 0; i < 7; i++) {
            double hX = coordHash(col * 3 + i, row * 7 + i, 2001L);
            double hY = coordHash(col * 5 + i, row * 11 + i, 2002L);
            double bx = sx + 3 + hX * (ts - 6);
            double by = sy + 8 + hY * (ts - 10);
            double height = 10.0 + (i % 3) * 3.5;

            // Sombra na raiz
            gc.setFill(Color.color(0.02, 0.07, 0.02, 0.55));
            gc.fillOval(bx - 2, by - 1, 5, 2.5);

            // Caule sombreado
            gc.setStroke(Color.web("#1E4618"));
            gc.setLineWidth(2.2);
            gc.strokeLine(bx, by, bx + sway * 0.6, by - height * 0.65);

            // Ponta iluminada pela luz solar superior
            gc.setStroke(i % 2 == 0 ? Color.web("#4E8C3A") : Color.web("#66A64F"));
            gc.setLineWidth(1.5);
            gc.strokeLine(bx + sway * 0.4, by - height * 0.45, bx + sway + (hX - 0.5) * 4, by - height);
        }
    }

    private void renderDetailedDirtPath(double sx, double sy, int ts, int col, int row, double macro, double micro, TileMap tileMap) {
        double hue = 28.0 + (macro - 0.5) * 6.0;
        double sat = 0.46 + (micro - 0.5) * 0.08;
        double bri = 0.48 + (macro - 0.5) * 0.07;
        Color dirtBase = Color.hsb(hue, clamp(sat, 0.35, 0.60), clamp(bri, 0.38, 0.58));

        gc.setFill(dirtBase);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        // Sulco central compactado (mais escuro por oclusão/umidade)
        gc.setFill(dirtBase.deriveColor(-2, 1.08, 0.84, 0.65));
        gc.fillOval(sx + 2, sy + 2, ts - 4, ts - 4);

        // Luz especular suave no relevo da trilha (topo-esquerda)
        gc.setFill(dirtBase.deriveColor(2, 0.90, 1.16, 0.35));
        gc.fillOval(sx + 4, sy + 3, ts - 10, ts - 12);

        // Pedregulhos e seixos 3D (Sombra projetada + Corpo + Brilho superior)
        int pebbleCount = 2 + (int) (coordHash(col, row, 3001L) * 4);
        for (int i = 0; i < pebbleCount; i++) {
            double px = sx + 4 + coordHash(col + i * 13, row - i * 7, 3100L) * (ts - 8);
            double py = sy + 4 + coordHash(col - i * 9, row + i * 17, 3200L) * (ts - 8);
            double pw = 2.5 + coordHash(col * i, row + i, 3300L) * 3.5;
            double ph = pw * 0.75;

            // Sombra proyectada do seixo (sudeste)
            gc.setFill(Color.color(0.12, 0.08, 0.04, 0.55));
            gc.fillOval(px + 0.8, py + 1.0, pw, ph);

            // Corpo da pedra
            gc.setFill(Color.web("#8D7762"));
            gc.fillOval(px, py, pw, ph);

            // Topo iluminado (noroeste)
            gc.setFill(Color.web("#BCAAA4", 0.80));
            gc.fillOval(px + 0.4, py + 0.3, pw * 0.55, ph * 0.55);
        }
    }

    private void renderDetailedMud(double sx, double sy, int ts, int col, int row, double macro, double micro) {
        double bri = 0.28 + (macro - 0.5) * 0.06;
        Color mudBase = Color.hsb(25, 0.58, clamp(bri, 0.20, 0.36));
        gc.setFill(mudBase);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        // Poças escuras viscosas com oclusão
        gc.setFill(mudBase.deriveColor(0, 1.1, 0.75, 0.75));
        gc.fillOval(sx + 3, sy + 4, ts - 6, ts - 8);

        // Brilho úmido especular (efeito de lama molhada refletindo o céu)
        double rx = sx + 7 + coordHash(col, row, 4001L) * 8;
        double ry = sy + 7 + coordHash(col, row, 4002L) * 8;
        gc.setFill(Color.web("#8D6E63", 0.45));
        gc.fillOval(rx, ry, 9, 4);
        gc.setFill(Color.web("#CFD8DC", 0.30));
        gc.fillOval(rx + 1.5, ry + 0.8, 4, 1.8);
    }

    private void renderDetailedWater(double sx, double sy, int ts, int col, int row, TileMap tileMap, double animTime) {
        boolean nWater = tileMap.getTileAt(col, row - 1) == TileType.WATER;
        boolean sWater = tileMap.getTileAt(col, row + 1) == TileType.WATER;
        boolean wWater = tileMap.getTileAt(col - 1, row) == TileType.WATER;
        boolean eWater = tileMap.getTileAt(col + 1, row) == TileType.WATER;
        boolean isDeep = nWater && sWater && wWater && eWater;

        // Profundidade abissal no meio do rio vs água rasa cristalina nas margens
        Color waterBase = isDeep ? Color.web("#124675") : Color.web("#1B639E");
        gc.setFill(waterBase);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        if (isDeep) {
            gc.setFill(Color.web("#0D3357", 0.55));
            gc.fillOval(sx - 2, sy - 2, ts + 4, ts + 4);
        } else {
            // Sub-superfície esverdeada/turquesa de banco de areia raso
            gc.setFill(Color.web("#2980B9", 0.45));
            gc.fillRect(sx, sy, ts, ts);
        }

        // Ondas dinâmicas suaves e cáusticas de luz solar na água
        double waveShift = Math.sin(animTime * 1.8 + col * 0.9 + row * 0.6) * 3.0;
        gc.setStroke(Color.web("#64B5F6", 0.45));
        gc.setLineWidth(1.4);
        gc.strokeArc(sx + 4 + waveShift, sy + 6, 12, 5, 15, 150, javafx.scene.shape.ArcType.OPEN);
        gc.strokeArc(sx + 14 - waveShift, sy + 18, 11, 4, 195, 150, javafx.scene.shape.ArcType.OPEN);

        // Reflexo especular solar cintilante
        double sparkle = Math.sin(animTime * 3.5 + col * 1.7 - row * 1.3);
        if (sparkle > 0.35) {
            gc.setFill(Color.web("#E1F5FE", (sparkle - 0.35) * 0.75));
            gc.fillOval(sx + 9 + waveShift * 0.5, sy + 10, 4.5, 2.0);
        }
    }

    /**
     * Renderiza Oclusão de Ambiente (Ambient Occlusion), barrancos 3D na margem da água
     * e chanfros orgânicos entre biomas adjacentes para eliminar linhas retas de grade.
     */
    private void renderBiomeTransitionsAndAO(TileType type, double sx, double sy, int ts, int col, int row, TileMap tileMap) {
        TileType north = tileMap.getTileAt(col, row - 1);
        TileType south = tileMap.getTileAt(col, row + 1);
        TileType west = tileMap.getTileAt(col - 1, row);
        TileType east = tileMap.getTileAt(col + 1, row);

        // 1. Margem de Rio com barranco 3D (Terra faz sombra dentro da água ao Norte/Oeste)
        if (type == TileType.WATER) {
            if (north != TileType.WATER) {
                // Barranco submerso + Sombra projetada da margem sobre a água
                gc.setFill(Color.web("#3E2723", 0.75));
                gc.fillRect(sx, sy, ts + 0.6, 3.5);
                gc.setFill(Color.color(0.02, 0.08, 0.18, 0.48));
                gc.fillRect(sx, sy + 3.5, ts + 0.6, 5.5);
                // Espuma da margem
                gc.setFill(Color.web("#E1F5FE", 0.55));
                gc.fillRoundRect(sx + 2, sy + 3.5, ts - 4, 2.0, 2, 2);
            }
            if (south != TileType.WATER) {
                gc.setFill(Color.web("#81D4FA", 0.45));
                gc.fillRect(sx, sy + ts - 3, ts + 0.6, 3);
            }
            if (west != TileType.WATER) {
                gc.setFill(Color.color(0.02, 0.08, 0.18, 0.40));
                gc.fillRect(sx, sy, 5.0, ts + 0.6);
            }
            if (east != TileType.WATER) {
                gc.setFill(Color.web("#81D4FA", 0.35));
                gc.fillRect(sx + ts - 3, sy, 3.0, ts + 0.6);
            }
            return;
        }

        // 2. Transição orgânica de Grama invadindo Trilha de Terra ou Lama
        if (type == TileType.DIRT_PATH || type == TileType.MUD) {
            Color grassEdge = Color.web("#3D732E", 0.85);
            Color aoShadow = Color.color(0.05, 0.08, 0.03, 0.32);

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

        // 3. Oclusão de Ambiente (Contact Shadow) ao redor de Paredões de Rocha (WALL) e Mata Alta
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
