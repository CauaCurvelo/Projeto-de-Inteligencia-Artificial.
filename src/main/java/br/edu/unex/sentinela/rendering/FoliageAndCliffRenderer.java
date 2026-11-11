package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Responsável pela renderização em 2.5D de estruturas verticais (Árvores e Paredões de Granito),
 * incluindo Sombras Projetadas Direcionais (Cast Shadows) baseadas no sol e Oclusão Profunda.
 */
public class FoliageAndCliffRenderer {
    private final GraphicsContext gc;

    public FoliageAndCliffRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    /**
     * Passo 1: Desenha as sombras longas projetadas no solo antes das estruturas verticais e entidades.
     */
    public void renderCastShadow(TileType type, double sx, double sy, int ts, int col, int row, double sunOffsetX, double sunOffsetY) {
        if (type == TileType.TREE) {
            double jitterX = (TerrainRenderer.coordHash(col, row, 101L) - 0.5) * 6.0;
            double jitterY = (TerrainRenderer.coordHash(col, row, 202L) - 0.5) * 6.0;
            double cx = sx + ts * 0.5 + jitterX;
            double cy = sy + ts * 0.5 + jitterY;

            // Sombra da copa projetada na direção oposta à luz solar
            gc.setFill(Color.color(0.02, 0.06, 0.02, 0.38));
            gc.fillOval(cx - 18 + sunOffsetX, cy - 10 + sunOffsetY, 40, 32);

            // Oclusão densa diretamente embaixo do tronco
            gc.setFill(Color.color(0.01, 0.04, 0.01, 0.55));
            gc.fillOval(cx - 12, cy + 2, 24, 14);

        } else if (type == TileType.WALL) {
            // Sombra diagonal projetada pelo paredão de rocha
            gc.setFill(Color.color(0.0, 0.0, 0.0, 0.36));
            gc.fillRoundRect(sx + sunOffsetX * 0.65, sy + sunOffsetY * 0.65, ts + 2, ts + 2, 4, 4);
        }
    }

    /**
     * Passo 2: Renderiza Paredões de Granito 2.5D com chanfro superior iluminado, face frontal sombreada,
     * fissuras geológicas e musgo nas frestas.
     */
    public void renderGraniteWall25D(double sx, double sy, int ts, int col, int row, TileMap tileMap) {
        boolean wallNorth = tileMap.getTileAt(col, row - 1) == TileType.WALL;
        boolean wallSouth = tileMap.getTileAt(col, row + 1) == TileType.WALL;

        double variation = (TerrainRenderer.coordHash(col, row, 777L) - 0.5) * 0.08;

        // Corpo principal da rocha
        Color rockBody = Color.hsb(215, 0.14, clamp(0.36 + variation, 0.28, 0.44));
        gc.setFill(rockBody);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        // Topo iluminado 2.5D (Platô superior recebendo luz solar direta se não houver rocha ao norte)
        int topEdgeH = wallNorth ? 4 : 9;
        Color rockTop = Color.hsb(210, 0.10, clamp(0.54 + variation, 0.45, 0.65));
        gc.setFill(rockTop);
        gc.fillRect(sx, sy, ts + 0.6, topEdgeH);

        // Quina viva iluminada (Rim light no topo da falésia)
        if (!wallNorth) {
            gc.setFill(Color.web("#90A4AE", 0.70));
            gc.fillRect(sx, sy, ts + 0.6, 2.2);
        }

        // Face frontal íngreme (Escarpamento inferior mais escuro se não houver parede ao sul)
        int cliffFaceH = wallSouth ? 5 : 11;
        Color cliffShadow = Color.hsb(220, 0.18, clamp(0.20 + variation * 0.5, 0.14, 0.26));
        gc.setFill(cliffShadow);
        gc.fillRect(sx, sy + ts - cliffFaceH, ts + 0.6, cliffFaceH);

        // Oclusão profunda no pé do paredão
        if (!wallSouth) {
            gc.setFill(Color.color(0.05, 0.06, 0.08, 0.85));
            gc.fillRect(sx, sy + ts - 3.5, ts + 0.6, 3.5);
        }

        // Blocos facetados e rachaduras geológicas com highlight de borda
        double crackY = sy + 11 + TerrainRenderer.coordHash(col, row, 881L) * 7;
        gc.setStroke(Color.web("#1C2024", 0.80));
        gc.setLineWidth(1.5);
        gc.strokeLine(sx + 3, crackY, sx + 16, crackY + 3);
        gc.strokeLine(sx + 16, crackY + 3, sx + 29, crackY - 2);

        // Borda inferior da rachadura iluminada (profundidade de fenda)
        gc.setStroke(Color.web("#78909C", 0.45));
        gc.setLineWidth(1.0);
        gc.strokeLine(sx + 3, crackY + 1.5, sx + 16, crackY + 4.5);

        // Musgo orgânico crescendo nas fendas úmidas
        if (TerrainRenderer.coordHash(col, row, 991L) > 0.45) {
            gc.setFill(Color.web("#2E5928", 0.70));
            gc.fillOval(sx + 5, crackY - 1, 9, 5);
            gc.setFill(Color.web("#4CAF50", 0.45));
            gc.fillOval(sx + 6, crackY - 1, 5, 2.5);
        }
    }

    /**
     * Passo 3: Renderiza o tronco de madeira com textura de casca e raízes antes da copa superior.
     */
    public void renderTreeTrunk(double sx, double sy, int ts, int col, int row) {
        double jitterX = (TerrainRenderer.coordHash(col, row, 101L) - 0.5) * 6.0;
        double jitterY = (TerrainRenderer.coordHash(col, row, 202L) - 0.5) * 6.0;
        double tx = sx + ts * 0.5 + jitterX;
        double ty = sy + ts * 0.5 + jitterY;

        // Raízes escuras ancoradas no solo com oclusão
        gc.setFill(Color.web("#261407"));
        gc.fillOval(tx - 7, ty + 8, 14, 7);

        // Tronco principal cilíndrico (Sombra na direita, luz na esquerda)
        gc.setFill(Color.web("#3B210E"));
        gc.fillRoundRect(tx - 5, ty - 2, 10, 15, 3, 3);

        // Lado iluminado da casca da árvore (Luz solar noroeste)
        gc.setFill(Color.web("#5C381B"));
        gc.fillRoundRect(tx - 4.5, ty - 2, 4.5, 14, 2, 2);

        // Sombra projetada pela própria copa sobre o topo do tronco (Self-Occlusion)
        gc.setFill(Color.color(0.04, 0.08, 0.03, 0.65));
        gc.fillRect(tx - 5, ty - 2, 10, 5);
    }

    /**
     * Passo 4: Renderiza a copa volumétrica 2.5D das árvores (Multi-cluster com 4 níveis de iluminação e vento).
     */
    public void renderTreeCanopy(double sx, double sy, int ts, int col, int row, double animTime) {
        double h1 = TerrainRenderer.coordHash(col, row, 101L);
        double h2 = TerrainRenderer.coordHash(col, row, 202L);
        double h3 = TerrainRenderer.coordHash(col, row, 303L);

        double jitterX = (h1 - 0.5) * 6.0;
        double jitterY = (h2 - 0.5) * 6.0;
        double sway = Math.sin(animTime * 1.6 + col * 0.8 + row * 0.6) * 1.4;

        double cx = sx + ts * 0.5 + jitterX + sway;
        double cy = sy + ts * 0.5 + jitterY - 4.0; // Deslocado para cima (efeito 2.5D de altura)
        double scale = 0.92 + h3 * 0.22;

        // Variação sutil de tom entre árvores (verde-pinho, verde-esmeralda, verde-oliva)
        double hueShift = (h1 - 0.5) * 14.0;
        Color deepOcclusion = Color.hsb(118 + hueShift, 0.68, 0.18);
        Color darkFoliage   = Color.hsb(115 + hueShift, 0.64, 0.28);
        Color midFoliage    = Color.hsb(110 + hueShift, 0.60, 0.42);
        Color litCanopy     = Color.hsb(104 + hueShift, 0.56, 0.56);
        Color sunRim        = Color.hsb(96 + hueShift,  0.52, 0.68, 0.85);

        double rBase = 18.0 * scale;

        // Camada 1: Oclusão interna profunda (Base inferior direita da copa)
        gc.setFill(deepOcclusion);
        gc.fillOval(cx - rBase + 2, cy - rBase + 4, rBase * 2, rBase * 1.9);
        gc.fillOval(cx - rBase * 0.9 - 5, cy - rBase * 0.6 + 3, rBase * 1.3, rBase * 1.3);
        gc.fillOval(cx - rBase * 0.3 + 5, cy - rBase * 0.6 + 3, rBase * 1.3, rBase * 1.3);

        // Camada 2: Volume de folhagem de sombra média (Clusters orgânicos assimétricos)
        gc.setFill(darkFoliage);
        gc.fillOval(cx - rBase, cy - rBase + 1, rBase * 1.95, rBase * 1.85);
        gc.fillOval(cx - rBase * 1.05, cy - rBase * 0.7, rBase * 1.25, rBase * 1.25);
        gc.fillOval(cx - rBase * 0.15, cy - rBase * 0.75, rBase * 1.25, rBase * 1.25);
        gc.fillOval(cx - rBase * 0.6, cy - rBase * 1.05, rBase * 1.3, rBase * 1.2);

        // Camada 3: Folhagem iluminada intermediária (Deslocada para Noroeste - direção do Sol)
        gc.setFill(midFoliage);
        gc.fillOval(cx - rBase * 0.88 - 1.5, cy - rBase * 0.85 - 2.0, rBase * 1.35, rBase * 1.3);
        gc.fillOval(cx - rBase * 0.25 - 1.0, cy - rBase * 0.65 - 1.5, rBase * 1.15, rBase * 1.1);
        gc.fillOval(cx - rBase * 0.60 - 1.5, cy - rBase * 0.35 - 1.0, rBase * 1.2, rBase * 1.1);

        // Camada 4: Topo da copa recebendo luz solar direta
        gc.setFill(litCanopy);
        gc.fillOval(cx - rBase * 0.72 - 2.5, cy - rBase * 0.75 - 3.0, rBase * 0.95, rBase * 0.88);
        gc.fillOval(cx - rBase * 0.20 - 1.5, cy - rBase * 0.55 - 2.0, rBase * 0.75, rBase * 0.72);

        // Camada 5: Brilho especular de folhas no topo esquerdo (Sun Rim Highlight)
        gc.setFill(sunRim);
        gc.fillOval(cx - rBase * 0.55 - 3.0, cy - rBase * 0.62 - 3.5, rBase * 0.48, rBase * 0.42);
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
