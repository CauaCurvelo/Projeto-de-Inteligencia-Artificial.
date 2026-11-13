package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.settings.GraphicsQuality;
import br.edu.unex.sentinela.world.BiomeType;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Responsável pela renderização em 2.5D de estruturas verticais (Árvores e Paredões de Granito),
 * utilizando banco pré-renderizado de copas multi-camada (zero alocações por frame) e paletas em cache.
 */
public class FoliageAndCliffRenderer {
    private static final Color CAST_SHADOW_CANOPY = Color.color(0.02, 0.06, 0.02, 0.38);
    private static final Color CAST_SHADOW_TRUNK = Color.color(0.01, 0.04, 0.01, 0.55);
    private static final Color CAST_SHADOW_WALL = Color.color(0.0, 0.0, 0.0, 0.36);

    private static final Color WALL_RIM_LIGHT = Color.web("#90A4AE", 0.70);
    private static final Color WALL_FOOT_AO = Color.color(0.05, 0.06, 0.08, 0.85);
    private static final Color WALL_CRACK_DARK = Color.web("#1C2024", 0.80);
    private static final Color WALL_CRACK_LIGHT = Color.web("#78909C", 0.45);

    private static final Color TRUNK_ROOT = Color.web("#261407");
    private static final Color TRUNK_BODY = Color.web("#3B210E");
    private static final Color TRUNK_LIT = Color.web("#5C381B");
    private static final Color TRUNK_AO = Color.color(0.04, 0.08, 0.03, 0.65);

    private static final Color[] ROCK_BODIES = new Color[4];
    private static final Color[] ROCK_TOPS = new Color[4];
    private static final Color[] ROCK_CLIFFS = new Color[4];

    static {
        for (int i = 0; i < 4; i++) {
            double variation = (i / 3.0 - 0.5) * 0.08;
            ROCK_BODIES[i] = Color.hsb(215, 0.14, clamp(0.36 + variation, 0.28, 0.44));
            ROCK_TOPS[i] = Color.hsb(210, 0.10, clamp(0.54 + variation, 0.45, 0.65));
            ROCK_CLIFFS[i] = Color.hsb(220, 0.18, clamp(0.20 + variation * 0.5, 0.14, 0.26));
        }
    }

    private final GraphicsContext gc;
    private WritableImage[][] canopySprites;

    public FoliageAndCliffRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    private void ensureCanopySprites() {
        if (canopySprites != null) return;
        BiomeType[] biomes = BiomeType.values();
        canopySprites = new WritableImage[biomes.length][8];
        Canvas off = new Canvas(64, 64);
        GraphicsContext ogc = off.getGraphicsContext2D();
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);

        for (BiomeType biome : biomes) {
            for (int i = 0; i < 8; i++) {
                ogc.clearRect(0, 0, 64, 64);
                double h1 = (i + 0.5) / 8.0;
                double hueShift = (h1 - 0.5) * 14.0;
                drawCanopyForBiome(ogc, 32.0, 32.0, 18.0, hueShift, biome);
                WritableImage sprite = new WritableImage(64, 64);
                off.snapshot(sp, sprite);
                canopySprites[biome.ordinal()][i] = sprite;
            }
        }
    }

    private static void drawCanopyForBiome(GraphicsContext target, double cx, double cy, double rBase, double hueShift, BiomeType biome) {
        switch (biome) {
            case SWAMP:
                drawSwampWillowCanopy(target, cx, cy, rBase, hueShift);
                break;
            case HIGHLAND:
                drawHighlandPineCanopy(target, cx, cy, rBase, hueShift);
                break;
            case SNOW:
                drawSnowPineCanopy(target, cx, cy, rBase, hueShift);
                break;
            case DENSE_FOREST:
                drawCanopyLayers(target, cx, cy, rBase * 1.04, 138.0 + hueShift, 0.72, 0.0);
                break;
            case MEADOW:
            default:
                drawCanopyLayers(target, cx, cy, rBase, 114.0 + hueShift, 0.64, 0.04);
                break;
        }
    }

    private static void drawSwampWillowCanopy(GraphicsContext target, double cx, double cy, double rBase, double hueShift) {
        Color deepMoss = Color.hsb(92 + hueShift, 0.45, 0.17);
        Color willowBody = Color.hsb(86 + hueShift, 0.42, 0.28);
        Color willowMid = Color.hsb(82 + hueShift, 0.38, 0.40);
        Color mossDrapes = Color.hsb(98 + hueShift, 0.30, 0.52, 0.85);

        // Folhagem pendente e alongada típica de salgueiro/cipreste de brejo
        target.setFill(deepMoss);
        target.fillOval(cx - rBase * 1.05, cy - rBase * 0.85 + 4, rBase * 2.1, rBase * 1.9);
        target.setFill(willowBody);
        target.fillOval(cx - rBase * 0.95, cy - rBase * 0.95, rBase * 1.9, rBase * 1.7);
        target.setFill(willowMid);
        target.fillOval(cx - rBase * 0.78, cy - rBase * 0.88 - 1.5, rBase * 1.35, rBase * 1.2);
        // Ramos pendentes nas bordas
        target.setFill(mossDrapes);
        target.fillOval(cx - rBase * 0.92, cy + 4, rBase * 0.50, rBase * 0.85);
        target.fillOval(cx + rBase * 0.35, cy + 5, rBase * 0.48, rBase * 0.80);
        target.fillOval(cx - rBase * 0.38, cy - rBase * 0.65 - 2.0, rBase * 0.72, rBase * 0.58);
    }

    private static void drawHighlandPineCanopy(GraphicsContext target, double cx, double cy, double rBase, double hueShift) {
        Color shadow = Color.hsb(148 + hueShift * 0.7, 0.48, 0.16);
        Color outerFrond = Color.hsb(145 + hueShift * 0.7, 0.46, 0.28);
        Color innerFrond = Color.hsb(138 + hueShift * 0.7, 0.42, 0.41);
        Color crownTip = Color.hsb(130 + hueShift * 0.7, 0.36, 0.56);

        drawConiferStar(target, cx + 1.5, cy + 3.0, rBase * 1.18, rBase * 0.56, 7, shadow);
        drawConiferStar(target, cx, cy, rBase * 1.12, rBase * 0.52, 7, outerFrond);
        drawConiferStar(target, cx - 1.0, cy - 1.5, rBase * 0.82, rBase * 0.38, 6, innerFrond);
        target.setFill(crownTip);
        target.fillOval(cx - 5.5, cy - 6.0, 9.5, 9.0);
    }

    private static void drawSnowPineCanopy(GraphicsContext target, double cx, double cy, double rBase, double hueShift) {
        Color shadow = Color.hsb(182 + hueShift * 0.5, 0.36, 0.17);
        Color deepSpruce = Color.hsb(176 + hueShift * 0.5, 0.38, 0.27);
        Color frostLayer = Color.web("#94A3B8", 0.85);
        Color snowCap = Color.web("#E0F2FE");
        Color snowCrown = Color.web("#F8FAFC");

        drawConiferStar(target, cx + 1.5, cy + 3.0, rBase * 1.16, rBase * 0.56, 7, shadow);
        drawConiferStar(target, cx, cy, rBase * 1.10, rBase * 0.52, 7, deepSpruce);
        drawConiferStar(target, cx - 0.8, cy - 1.2, rBase * 0.88, rBase * 0.42, 7, frostLayer);
        drawConiferStar(target, cx - 1.2, cy - 2.0, rBase * 0.72, rBase * 0.34, 6, snowCap);
        target.setFill(snowCrown);
        target.fillOval(cx - 6.2, cy - 6.6, 10.5, 10.0);
    }

    private static void drawConiferStar(GraphicsContext target, double cx, double cy,
                                        double outerR, double innerR, int points, Color fill) {
        int n = points * 2;
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double ang = (Math.PI * i / points) - Math.PI * 0.5;
            double r = (i % 2 == 0) ? outerR : innerR;
            xs[i] = cx + Math.cos(ang) * r;
            ys[i] = cy + Math.sin(ang) * r;
        }
        target.setFill(fill);
        target.fillPolygon(xs, ys, n);
    }

    private static void drawCanopyLayers(GraphicsContext target, double cx, double cy, double rBase,
                                         double baseHue, double sat, double briBoost) {
        Color deepOcclusion = Color.hsb(baseHue + 4, clamp(sat + 0.04, 0.35, 0.85), clamp(0.16 + briBoost * 0.5, 0.12, 0.30));
        Color darkFoliage   = Color.hsb(baseHue,     clamp(sat, 0.35, 0.82), clamp(0.26 + briBoost, 0.18, 0.40));
        Color midFoliage    = Color.hsb(baseHue - 5, clamp(sat - 0.04, 0.35, 0.78), clamp(0.39 + briBoost, 0.25, 0.54));
        Color litCanopy     = Color.hsb(baseHue - 10, clamp(sat - 0.08, 0.35, 0.74), clamp(0.52 + briBoost, 0.36, 0.66));
        Color sunRim        = Color.hsb(baseHue - 16, clamp(sat - 0.12, 0.30, 0.70), clamp(0.64 + briBoost, 0.44, 0.76), 0.85);

        target.setFill(deepOcclusion);
        target.fillOval(cx - rBase + 2, cy - rBase + 4, rBase * 2, rBase * 1.9);
        target.fillOval(cx - rBase * 0.9 - 5, cy - rBase * 0.6 + 3, rBase * 1.3, rBase * 1.3);
        target.fillOval(cx - rBase * 0.3 + 5, cy - rBase * 0.6 + 3, rBase * 1.3, rBase * 1.3);

        target.setFill(darkFoliage);
        target.fillOval(cx - rBase, cy - rBase + 1, rBase * 1.95, rBase * 1.85);
        target.fillOval(cx - rBase * 1.05, cy - rBase * 0.7, rBase * 1.25, rBase * 1.25);
        target.fillOval(cx - rBase * 0.15, cy - rBase * 0.75, rBase * 1.25, rBase * 1.25);
        target.fillOval(cx - rBase * 0.6, cy - rBase * 1.05, rBase * 1.3, rBase * 1.2);

        target.setFill(midFoliage);
        target.fillOval(cx - rBase * 0.88 - 1.5, cy - rBase * 0.85 - 2.0, rBase * 1.35, rBase * 1.3);
        target.fillOval(cx - rBase * 0.25 - 1.0, cy - rBase * 0.65 - 1.5, rBase * 1.15, rBase * 1.1);
        target.fillOval(cx - rBase * 0.60 - 1.5, cy - rBase * 0.35 - 1.0, rBase * 1.2, rBase * 1.1);

        target.setFill(litCanopy);
        target.fillOval(cx - rBase * 0.72 - 2.5, cy - rBase * 0.75 - 3.0, rBase * 0.95, rBase * 0.88);
        target.fillOval(cx - rBase * 0.20 - 1.5, cy - rBase * 0.55 - 2.0, rBase * 0.75, rBase * 0.72);

        target.setFill(sunRim);
        target.fillOval(cx - rBase * 0.55 - 3.0, cy - rBase * 0.62 - 3.5, rBase * 0.48, rBase * 0.42);
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

            gc.setFill(CAST_SHADOW_CANOPY);
            gc.fillOval(cx - 18 + sunOffsetX, cy - 10 + sunOffsetY, 40, 32);

            gc.setFill(CAST_SHADOW_TRUNK);
            gc.fillOval(cx - 12, cy + 2, 24, 14);

        } else if (type == TileType.WALL) {
            gc.setFill(CAST_SHADOW_WALL);
            gc.fillRoundRect(sx + sunOffsetX * 0.65, sy + sunOffsetY * 0.65, ts + 2, ts + 2, 4, 4);
        }
    }

    public void renderGraniteWall25D(double sx, double sy, int ts, int col, int row, TileMap tileMap) {
        renderGraniteWall25D(sx, sy, ts, col, row, tileMap, GraphicsQuality.HIGH);
    }

    /**
     * Passo 2: Renderiza Paredões de Granito 2.5D com chanfro superior iluminado, face frontal sombreada,
     * fissuras geológicas e musgo nas frestas.
     */
    public void renderGraniteWall25D(double sx, double sy, int ts, int col, int row, TileMap tileMap, GraphicsQuality quality) {
        boolean wallNorth = tileMap.getTileAt(col, row - 1) == TileType.WALL;
        boolean wallSouth = tileMap.getTileAt(col, row + 1) == TileType.WALL;
        int pal = (int) (TerrainRenderer.coordHash(col, row, 777L) * 4) & 3;

        gc.setFill(ROCK_BODIES[pal]);
        gc.fillRect(sx, sy, ts + 0.6, ts + 0.6);

        int topEdgeH = wallNorth ? 4 : 9;
        gc.setFill(ROCK_TOPS[pal]);
        gc.fillRect(sx, sy, ts + 0.6, topEdgeH);

        if (!wallNorth) {
            gc.setFill(WALL_RIM_LIGHT);
            gc.fillRect(sx, sy, ts + 0.6, 2.2);
        }

        int cliffFaceH = wallSouth ? 5 : 11;
        gc.setFill(ROCK_CLIFFS[pal]);
        gc.fillRect(sx, sy + ts - cliffFaceH, ts + 0.6, cliffFaceH);

        if (!wallSouth) {
            gc.setFill(WALL_FOOT_AO);
            gc.fillRect(sx, sy + ts - 3.5, ts + 0.6, 3.5);
        }

        if (quality == GraphicsQuality.LOW) return;

        double crackY = sy + 11 + TerrainRenderer.coordHash(col, row, 881L) * 7;
        gc.setStroke(WALL_CRACK_DARK);
        gc.setLineWidth(1.5);
        gc.strokeLine(sx + 3, crackY, sx + 16, crackY + 3);
        gc.strokeLine(sx + 16, crackY + 3, sx + 29, crackY - 2);

        gc.setStroke(WALL_CRACK_LIGHT);
        gc.setLineWidth(1.0);
        gc.strokeLine(sx + 3, crackY + 1.5, sx + 16, crackY + 4.5);

        if (quality == GraphicsQuality.HIGH && TerrainRenderer.coordHash(col, row, 991L) > 0.45) {
            BiomeTerrainPalette palBio = BiomeTerrainPalette.of(tileMap.getBiomeAt(col, row));
            gc.setFill(palBio.cliffMossDark);
            gc.fillOval(sx + 5, crackY - 1, 9, 5);
            gc.setFill(palBio.cliffMossLight);
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

        gc.setFill(TRUNK_ROOT);
        gc.fillOval(tx - 7, ty + 8, 14, 7);

        gc.setFill(TRUNK_BODY);
        gc.fillRoundRect(tx - 5, ty - 2, 10, 15, 3, 3);

        gc.setFill(TRUNK_LIT);
        gc.fillRoundRect(tx - 4.5, ty - 2, 4.5, 14, 2, 2);

        gc.setFill(TRUNK_AO);
        gc.fillRect(tx - 5, ty - 2, 10, 5);
    }

    public void renderTreeCanopy(double sx, double sy, int ts, int col, int row, double animTime) {
        renderTreeCanopy(sx, sy, ts, col, row, animTime, GraphicsQuality.HIGH, BiomeType.MEADOW);
    }

    public void renderTreeCanopy(double sx, double sy, int ts, int col, int row, double animTime, GraphicsQuality quality) {
        renderTreeCanopy(sx, sy, ts, col, row, animTime, quality, BiomeType.MEADOW);
    }

    /**
     * Passo 4: Renderiza a copa volumétrica 2.5D via sprite pré-renderizado específico do bioma (1 blit por árvore).
     */
    public void renderTreeCanopy(double sx, double sy, int ts, int col, int row, double animTime,
                                 GraphicsQuality quality, BiomeType biome) {
        ensureCanopySprites();
        int biomeIdx = (biome != null ? biome : BiomeType.MEADOW).ordinal();

        double h1 = TerrainRenderer.coordHash(col, row, 101L);
        double h2 = TerrainRenderer.coordHash(col, row, 202L);
        double h3 = TerrainRenderer.coordHash(col, row, 303L);

        double jitterX = (h1 - 0.5) * 6.0;
        double jitterY = (h2 - 0.5) * 6.0;
        double sway = (quality == GraphicsQuality.HIGH) ? Math.sin(animTime * 1.6 + col * 0.8 + row * 0.6) * 1.4 : 0.0;

        double cx = sx + ts * 0.5 + jitterX + sway;
        double cy = sy + ts * 0.5 + jitterY - 4.0;
        double scale = 0.84 + h3 * 0.20;
        double drawSize = 64.0 * scale;

        int variant = Math.min(7, Math.max(0, (int) (h1 * 8)));
        gc.drawImage(canopySprites[biomeIdx][variant], cx - drawSize * 0.5, cy - drawSize * 0.5, drawSize, drawSize);
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
