package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.settings.GraphicsQuality;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache LRU de chunks estáticos do terreno (16x16 tiles = 512x512 px em WritableImage).
 * Elimina dezenas de milhares de primitivas vetoriais por frame em solo estático,
 * preservando transições e oclusão de ambiente nas bordas entre chunks.
 */
public class TerrainChunkCache {
    public static final int CHUNK_TILES = 16;
    private static final int CHUNK_PX = CHUNK_TILES * TileMap.TILE_SIZE;
    private static final int MAX_CACHED_CHUNKS = 48;
    private static final Color BACKFILL = Color.web("#101A0D");

    private final Map<Long, WritableImage> chunkImages = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, WritableImage> eldest) {
            return size() > MAX_CACHED_CHUNKS;
        }
    };

    private final Canvas bakeCanvas = new Canvas(CHUNK_PX, CHUNK_PX);
    private final TerrainRenderer bakeTerrainRenderer = new TerrainRenderer(bakeCanvas.getGraphicsContext2D());
    private final SnapshotParameters snapshotParams = new SnapshotParameters();

    private TileMap cachedTileMap;
    private long cachedWorldSeed = Long.MIN_VALUE;
    private GraphicsQuality cachedQuality;

    public void renderCachedGround(
            GraphicsContext gc,
            TileMap tileMap,
            double camX, double camY,
            int startCol, int endCol,
            int startRow, int endRow,
            GraphicsQuality quality
    ) {
        if (tileMap != cachedTileMap || tileMap.getWorldSeed() != cachedWorldSeed || quality != cachedQuality) {
            chunkImages.clear();
            cachedTileMap = tileMap;
            cachedWorldSeed = tileMap.getWorldSeed();
            cachedQuality = quality;
        }

        int ts = TileMap.TILE_SIZE;
        int startChunkX = Math.floorDiv(startCol, CHUNK_TILES);
        int endChunkX = Math.floorDiv(endCol, CHUNK_TILES);
        int startChunkY = Math.floorDiv(startRow, CHUNK_TILES);
        int endChunkY = Math.floorDiv(endRow, CHUNK_TILES);

        for (int cy = startChunkY; cy <= endChunkY; cy++) {
            for (int cx = startChunkX; cx <= endChunkX; cx++) {
                WritableImage img = getOrBakeChunk(cx, cy, tileMap, quality);
                double drawX = (cx * CHUNK_TILES) * ts - camX;
                double drawY = (cy * CHUNK_TILES) * ts - camY;
                // +0.5 fecha eventuais microfrestas subpixel em fatores de zoom fracionários
                gc.drawImage(img, drawX, drawY, CHUNK_PX + 0.5, CHUNK_PX + 0.5);
            }
        }
    }

    public void invalidate() {
        chunkImages.clear();
    }

    private WritableImage getOrBakeChunk(int chunkX, int chunkY, TileMap tileMap, GraphicsQuality quality) {
        long key = (((long) chunkX) << 32) | (chunkY & 0xFFFFFFFFL);
        WritableImage cached = chunkImages.get(key);
        if (cached != null) {
            return cached;
        }

        GraphicsContext bgc = bakeCanvas.getGraphicsContext2D();
        bgc.setFill(BACKFILL);
        bgc.fillRect(0, 0, CHUNK_PX, CHUNK_PX);

        int ts = TileMap.TILE_SIZE;
        int originCol = chunkX * CHUNK_TILES;
        int originRow = chunkY * CHUNK_TILES;

        // Varre margem de +-1 tile para capturar transições orgânicas de borda sem costura entre chunks
        for (int lr = -1; lr <= CHUNK_TILES; lr++) {
            for (int lc = -1; lc <= CHUNK_TILES; lc++) {
                int worldCol = originCol + lc;
                int worldRow = originRow + lr;
                TileType type = tileMap.getTileAt(worldCol, worldRow);
                bakeTerrainRenderer.renderStaticTerrainTile(
                        type, lc * ts, lr * ts, ts, worldCol, worldRow, tileMap, quality
                );
            }
        }

        WritableImage snapshot = new WritableImage(CHUNK_PX, CHUNK_PX);
        bakeCanvas.snapshot(snapshotParams, snapshot);
        chunkImages.put(key, snapshot);
        return snapshot;
    }
}
