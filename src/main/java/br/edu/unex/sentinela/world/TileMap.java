package br.edu.unex.sentinela.world;

import java.util.HashMap;
import java.util.Map;

public class TileMap {
    public static final int TILE_SIZE = 32;
    public static final int CHUNK_SIZE = 32; // 32x32 tiles per chunk = 1024x1024 px

    // Chunk caching for truly infinite procedural generation
    private final Map<Long, TileType[][]> chunkCache = new HashMap<>();
    private final Map<Long, BiomeType[][]> biomeChunkCache = new HashMap<>();
    private final BiomeSampler biomeSampler;
    private final long worldSeed;

    // Parâmetros de rio e trilhas sorteados por seed para que cada partida tenha geografia única
    private final double riverCos;
    private final double riverSin;
    private final double riverOffset;
    private final double riverPhaseA;
    private final double riverPhaseB;
    private final double trailAngleA;
    private final double trailOffsetA;
    private final double trailAngleB;
    private final double trailOffsetB;

    public TileMap() {
        this(System.nanoTime() ^ 0x5EED42L);
    }

    public TileMap(long worldSeed) {
        this.worldSeed = worldSeed;
        this.biomeSampler = new BiomeSampler(worldSeed);

        double riverAngle = rawHash(13, 29, 911L) * Math.PI * 2.0;
        this.riverCos = Math.cos(riverAngle);
        this.riverSin = Math.sin(riverAngle);
        double side = rawHash(31, 7, 919L) >= 0.5 ? 1.0 : -1.0;
        this.riverOffset = side * (36.0 + rawHash(19, 41, 929L) * 26.0);
        this.riverPhaseA = rawHash(3, 53, 937L) * Math.PI * 2.0;
        this.riverPhaseB = rawHash(71, 17, 941L) * Math.PI * 2.0;

        this.trailAngleA = riverAngle + (0.45 + rawHash(17, 61, 953L) * 0.9);
        this.trailOffsetA = (rawHash(43, 11, 967L) - 0.5) * 18.0;
        this.trailAngleB = this.trailAngleA + Math.PI * (0.42 + rawHash(23, 83, 977L) * 0.25);
        this.trailOffsetB = (rawHash(67, 29, 983L) >= 0.5 ? 1.0 : -1.0) * (58.0 + rawHash(89, 13, 991L) * 42.0);
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    public BiomeType getBiomeAtWorld(double worldX, double worldY) {
        int col = (int) Math.floor(worldX / TILE_SIZE);
        int row = (int) Math.floor(worldY / TILE_SIZE);
        return getBiomeAt(col, row);
    }

    public BiomeType getBiomeAt(int col, int row) {
        int chunkX = Math.floorDiv(col, CHUNK_SIZE);
        int chunkY = Math.floorDiv(row, CHUNK_SIZE);
        long chunkKey = (((long) chunkX) << 32) | (chunkY & 0xFFFFFFFFL);

        BiomeType[][] chunk = biomeChunkCache.get(chunkKey);
        if (chunk == null) {
            chunk = new BiomeType[CHUNK_SIZE][CHUNK_SIZE];
            for (int r = 0; r < CHUNK_SIZE; r++) {
                int worldRow = chunkY * CHUNK_SIZE + r;
                for (int c = 0; c < CHUNK_SIZE; c++) {
                    chunk[r][c] = biomeSampler.sampleAt(chunkX * CHUNK_SIZE + c, worldRow);
                }
            }
            if (biomeChunkCache.size() > 250) {
                biomeChunkCache.clear();
            }
            biomeChunkCache.put(chunkKey, chunk);
        }

        int localCol = col - (chunkX * CHUNK_SIZE);
        int localRow = row - (chunkY * CHUNK_SIZE);
        return chunk[localRow][localCol];
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

                BiomeType biome = getBiomeAt(worldCol, worldRow);

                // 2. Multi-octave Fractal Perlin-like layered noise (fBm)
                double elevation = fbm(worldCol * 0.022, worldRow * 0.022, 101);
                double moisture = fbm(worldCol * 0.030, worldRow * 0.030, 303);
                double forestClumping = fbm(worldCol * 0.055, worldRow * 0.055, 707);

                // 3. Rio sinuoso parametrizado por seed (mantém clareira inicial seca e varia direção)
                double ru = worldCol * riverCos + worldRow * riverSin;
                double rv = -worldCol * riverSin + worldRow * riverCos;
                double riverCenterV = riverOffset
                        + Math.sin(ru * 0.017 + riverPhaseA) * 15.0
                        + Math.cos(ru * 0.008 + riverPhaseB) * 9.0;
                double riverDist = Math.abs(rv - riverCenterV);
                int bridgeCell = (int) Math.floor(ru / 48.0);
                double bridgePos = (bridgeCell + 0.25 + rawHash(bridgeCell, 7, 1777L) * 0.5) * 48.0;
                boolean isBridge = Math.abs(ru - bridgePos) <= 1.2 && riverDist <= 4.6;

                if (isBridge) {
                    chunk[r][c] = TileType.DIRT_PATH;
                    continue;
                }

                double riverHalfWidth = (biome == BiomeType.SWAMP) ? 2.3 : (biome == BiomeType.SNOW ? 1.5 : 1.7);
                double bankHalfWidth = (biome == BiomeType.SWAMP) ? 3.9 : 2.9;
                if (riverDist < riverHalfWidth) {
                    chunk[r][c] = TileType.WATER;
                    continue;
                } else if (riverDist < bankHalfWidth) {
                    chunk[r][c] = (biome == BiomeType.SWAMP || biome == BiomeType.SNOW || moisture > 0.48)
                            ? TileType.MUD : TileType.TALL_GRASS;
                    continue;
                }

                // 4. Trilhas naturais orgânicas conectando os biomas sem repetir sempre o mesmo caminho
                double radialDist = Math.hypot(worldCol, worldRow);
                double tu1 = worldCol * Math.cos(trailAngleA) + worldRow * Math.sin(trailAngleA);
                double tv1 = -worldCol * Math.sin(trailAngleA) + worldRow * Math.cos(trailAngleA);
                double trail1 = Math.abs(tv1 - (trailOffsetA + Math.sin(tu1 * 0.028 + riverPhaseA) * 11.0 + Math.cos(tu1 * 0.014) * 7.0));

                double tu2 = worldCol * Math.cos(trailAngleB) + worldRow * Math.sin(trailAngleB);
                double tv2 = -worldCol * Math.sin(trailAngleB) + worldRow * Math.cos(trailAngleB);
                double trail2 = Math.abs(tv2 - (trailOffsetB + Math.sin(tu2 * 0.024 + riverPhaseB) * 13.0));
                if (radialDist > 15.0 && (trail1 < 0.72 || trail2 < 0.72)) {
                    chunk[r][c] = TileType.DIRT_PATH;
                    continue;
                }

                // 5. Lagos / brejos / lagoas glaciais de acordo com o bioma
                double adjElevation = elevation - biome.getWaterBias();
                double adjMoisture = moisture + biome.getMudBias();
                if (adjElevation < 0.23 && adjMoisture > 0.56) {
                    if (biome == BiomeType.SWAMP) {
                        double bogIsland = fbm(worldCol * 0.14, worldRow * 0.14, 521L);
                        if (bogIsland > 0.57) {
                            chunk[r][c] = rawHash(worldCol, worldRow, 523L) > 0.55 ? TileType.TALL_GRASS : TileType.MUD;
                        } else if (adjElevation < 0.15) {
                            chunk[r][c] = TileType.WATER;
                        } else {
                            chunk[r][c] = TileType.MUD;
                        }
                    } else {
                        chunk[r][c] = (adjElevation < 0.16) ? TileType.WATER : TileType.MUD;
                    }
                    continue;
                }

                // Bolsões de lama no Pântano e neve úmida/slush ocasional na Tundra
                if (biome == BiomeType.SWAMP && forestClumping > 0.36 && forestClumping < 0.44 && moisture > 0.50) {
                    chunk[r][c] = TileType.MUD;
                    continue;
                }
                if (biome == BiomeType.SNOW && forestClumping > 0.34 && forestClumping < 0.39 && moisture > 0.56) {
                    chunk[r][c] = TileType.MUD;
                    continue;
                }

                // 6. Mountain Cliffs / Granite Formations (dense in Serra Granítica, with passes)
                double rockThreshold = 0.89 - biome.getRockBias();
                if (elevation > rockThreshold && forestClumping > (biome == BiomeType.HIGHLAND ? 0.50 : 0.53)) {
                    if (rawHash(worldCol, worldRow, 619L) > 0.16) {
                        chunk[r][c] = TileType.WALL;
                        continue;
                    }
                }

                // 7. Forests & Woods scaled by biome canopy density (preserving walkable glades)
                if (forestClumping > biome.getTreeThreshold()) {
                    if (biome == BiomeType.DENSE_FOREST) {
                        boolean keepGlade = rawHash(worldCol, worldRow, 887L) < 0.46;
                        chunk[r][c] = keepGlade ? TileType.TALL_GRASS : TileType.TREE;
                    } else if (biome == BiomeType.SNOW && moisture > 0.36) {
                        chunk[r][c] = rawHash(worldCol, worldRow, 889L) < 0.30 ? TileType.GRASS : TileType.TREE;
                    } else if (moisture > 0.44) {
                        chunk[r][c] = TileType.TREE;
                    } else {
                        chunk[r][c] = TileType.TALL_GRASS;
                    }
                    continue;
                }

                // 8. Tall Grass / Marsh Reeds / Undergrowth
                if (forestClumping > biome.getTallGrassThreshold()
                        && (biome == BiomeType.DENSE_FOREST || biome == BiomeType.SWAMP || biome == BiomeType.SNOW || moisture > 0.50)) {
                    chunk[r][c] = TileType.TALL_GRASS;
                    continue;
                }

                // 9. Biome Base Ground
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
        long n = x * 374761393L + y * 668265263L + worldSeed + seedOffset;
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
