package br.edu.unex.sentinela.world;

import java.util.HashMap;
import java.util.Map;

public class TileMap {
    public static final int TILE_SIZE = 32;
    public static final int CHUNK_SIZE = 32; // 32x32 tiles per chunk = 1024x1024 px
    
    // Chunk caching for truly infinite procedural generation
    private final Map<Long, TileType[][]> chunkCache = new HashMap<>();
    private final long worldSeed;

    public TileMap() {
        this.worldSeed = 133742L;
    }

    public TileType getTileAt(int col, int row) {
        int chunkX = Math.floorDiv(col, CHUNK_SIZE);
        int chunkY = Math.floorDiv(row, CHUNK_SIZE);
        long chunkKey = (((long) chunkX) << 32) | (chunkY & 0xFFFFFFFFL);

        TileType[][] chunk = chunkCache.get(chunkKey);
        if (chunk == null) {
            chunk = generateChunk(chunkX, chunkY);
            // Limit memory usage if thousands of chunks are explored
            if (chunkCache.size() > 250) {
                chunkCache.clear();
            }
            chunkCache.put(chunkKey, chunk);
        }

        int localCol = col - (chunkX * CHUNK_SIZE);
        int localRow = row - (chunkY * CHUNK_SIZE);
        return chunk[localRow][localCol];
    }

    private TileType[][] generateChunk(int chunkX, int chunkY) {
        TileType[][] chunk = new TileType[CHUNK_SIZE][CHUNK_SIZE];

        for (int r = 0; r < CHUNK_SIZE; r++) {
            for (int c = 0; c < CHUNK_SIZE; c++) {
                int worldCol = chunkX * CHUNK_SIZE + c;
                int worldRow = chunkY * CHUNK_SIZE + r;

                // 1. Initial Spawn clearing guarantee (around 0,0) - clear, beautiful natural meadow
                if (Math.abs(worldCol) <= 12 && Math.abs(worldRow) <= 12) {
                    chunk[r][c] = TileType.GRASS;
                    continue;
                }

                // 2. Multi-octave Fractal Perlin-like layered noise (fBm)
                double elevation = fbm(worldCol * 0.022, worldRow * 0.022, 101);
                double moisture = fbm(worldCol * 0.030, worldRow * 0.030, 303);
                double forestClumping = fbm(worldCol * 0.055, worldRow * 0.055, 707);

                // 3. Elegant Sinusoidal River with natural gentle meander (placed far enough from spawn)
                double riverCenterY = Math.sin(worldCol * 0.018) * 16.0 + (worldCol * 0.10) + 48.0;
                double riverDist = Math.abs(worldRow - riverCenterY);

                // Natural bridges across the river every ~50 tiles
                boolean isBridge = Math.abs(Math.floorMod(worldCol + 5000, 50) - 25) <= 1 && riverDist <= 4.0;

                if (isBridge) {
                    chunk[r][c] = TileType.DIRT_PATH;
                    continue;
                }

                if (riverDist < 1.7) {
                    chunk[r][c] = TileType.WATER;
                    continue;
                } else if (riverDist < 2.9) {
                    // Riverbanks: Mud near water, Tall Grass on higher bank
                    chunk[r][c] = (moisture > 0.48) ? TileType.MUD : TileType.TALL_GRASS;
                    continue;
                }

                // 4. Natural Meandering Wilderness Footpaths (organic connectivity)
                double trail1 = Math.abs(worldRow - (Math.sin(worldCol * 0.03) * 10.0 + Math.cos(worldCol * 0.015) * 8.0));
                double trail2 = Math.abs(worldCol - (Math.sin(worldRow * 0.025) * 12.0 + 95.0));
                if ((trail1 < 0.75 && Math.abs(worldCol) > 16) || (trail2 < 0.75 && Math.abs(worldRow) > 16)) {
                    chunk[r][c] = TileType.DIRT_PATH;
                    continue;
                }

                // 5. Natural Clustered Lakes / Ponds (low elevation + high moisture)
                if (elevation < 0.20 && moisture > 0.58) {
                    if (elevation < 0.16) {
                        chunk[r][c] = TileType.WATER;
                    } else {
                        chunk[r][c] = TileType.MUD;
                    }
                    continue;
                }

                // 6. Mountain Cliffs / Granite Formations (high elevation peaks)
                if (elevation > 0.83) {
                    // Clustered naturally on dry mountain ridges
                    if (moisture < 0.48 && forestClumping > 0.48) {
                        chunk[r][c] = TileType.WALL;
                        continue;
                    }
                }

                // 7. Forests & Woods: Formed in organic clustered groves, preserving open glades
                if (forestClumping > 0.65) {
                    if (moisture > 0.46) {
                        chunk[r][c] = TileType.TREE;
                    } else {
                        chunk[r][c] = TileType.TALL_GRASS;
                    }
                    continue;
                }

                // 8. Savanna / Tall Grass Meadows
                if (forestClumping > 0.52 && moisture > 0.54) {
                    chunk[r][c] = TileType.TALL_GRASS;
                    continue;
                }

                // 9. Standard Lush Grass
                chunk[r][c] = TileType.GRASS;
            }
        }

        return chunk;
    }

    // Fractal Brownian Motion (fBm): 2 octaves of smooth noise for organic landmasses
    private double fbm(double x, double y, long seedOffset) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double maxAmplitude = 0.0;

        for (int i = 0; i < 2; i++) {
            total += smoothNoise(x * frequency, y * frequency, seedOffset + i * 1000L) * amplitude;
            maxAmplitude += amplitude;
            amplitude *= 0.5;
            frequency *= 2.0;
        }

        return total / maxAmplitude;
    }

    // Bilinear interpolated smooth value noise (eliminates harsh hash checkerboards)
    private double smoothNoise(double x, double y, long seedOffset) {
        int x0 = (int) Math.floor(x);
        int x1 = x0 + 1;
        int y0 = (int) Math.floor(y);
        int y1 = y0 + 1;

        double sx = x - x0;
        double sy = y - y0;

        // Smoothstep interpolation curve (3x^2 - 2x^3)
        double u = sx * sx * (3.0 - 2.0 * sx);
        double v = sy * sy * (3.0 - 2.0 * sy);

        double n00 = rawHash(x0, y0, seedOffset);
        double n10 = rawHash(x1, y0, seedOffset);
        double n01 = rawHash(x0, y1, seedOffset);
        double n11 = rawHash(x1, y1, seedOffset);

        double nx0 = n00 * (1.0 - u) + n10 * u;
        double nx1 = n01 * (1.0 - u) + n11 * u;

        return nx0 * (1.0 - v) + nx1 * v;
    }

    private double rawHash(int x, int y, long seedOffset) {
        long n = (long)(x * 374761393L + y * 668265263L + worldSeed + seedOffset);
        n = (n ^ (n >> 13)) * 1274126177L;
        return ((n ^ (n >> 16)) & 0x7FFFFFFFL) / (double) 0x7FFFFFFFL;
    }

    public boolean isWalkable(double worldX, double worldY) {
        int col = (int) Math.floor(worldX / TILE_SIZE);
        int row = (int) Math.floor(worldY / TILE_SIZE);
        return getTileAt(col, row).isWalkable();
    }

    public boolean hasLineOfSight(double x1, double y1, double x2, double y2) {
        double dist = Math.hypot(x2 - x1, y2 - y1);
        int steps = (int) Math.ceil(dist / (TILE_SIZE / 2.0));
        if (steps <= 0) return true;

        for (int i = 1; i < steps; i++) {
            double checkX = x1 + (x2 - x1) * ((double) i / steps);
            double checkY = y1 + (y2 - y1) * ((double) i / steps);
            int c = (int) Math.floor(checkX / TILE_SIZE);
            int r = (int) Math.floor(checkY / TILE_SIZE);

            TileType type = getTileAt(c, r);
            if (type == TileType.WALL || type == TileType.TREE) {
                return false;
            }
        }
        return true;
    }
}
