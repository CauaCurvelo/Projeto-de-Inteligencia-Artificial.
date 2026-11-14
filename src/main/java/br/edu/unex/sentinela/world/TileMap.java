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
    private final double tribCos;
    private final double tribSin;
    private final double tribOffset;
    private final double tribPhaseA;
    private final double tribPhaseB;
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

        // Riacho afluente secundário convergindo no relevo
        double tribAngle = riverAngle + (rawHash(47, 89, 993L) >= 0.5 ? 1.0 : -1.0) * (0.65 + rawHash(53, 19, 997L) * 0.5);
        this.tribCos = Math.cos(tribAngle);
        this.tribSin = Math.sin(tribAngle);
        double tribSide = rawHash(79, 31, 1009L) >= 0.5 ? 1.0 : -1.0;
        this.tribOffset = tribSide * (26.0 + rawHash(83, 43, 1013L) * 26.0);
        this.tribPhaseA = rawHash(89, 67, 1019L) * Math.PI * 2.0;
        this.tribPhaseB = rawHash(97, 73, 1021L) * Math.PI * 2.0;

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

                // 1. Clareira inicial do jogador (ao redor de 0,0) - Prado limpo circular sem quinas artificiais
                double spawnDist = Math.hypot(worldCol, worldRow);
                if (spawnDist <= 11.5) {
                    chunk[r][c] = TileType.GRASS;
                    continue;
                }

                BiomeType biome = getBiomeAt(worldCol, worldRow);

                // 2. Multi-octave Fractal Perlin-like layered noise (fBm)
                double elevation = fbm(worldCol * 0.022, worldRow * 0.022, 101);
                double moisture = fbm(worldCol * 0.030, worldRow * 0.030, 303);
                double forestClumping = fbm(worldCol * 0.055, worldRow * 0.055, 707);

                // 3. Sistema Hidrográfico Orgânico: Rio Principal, Bacias Lacustres e Riachos
                double ru = worldCol * riverCos + worldRow * riverSin;
                double rv = -worldCol * riverSin + worldRow * riverCos;

                double riverCenterV = computeRiverCenterV(ru);
                double riverDist = Math.abs(rv - riverCenterV);

                double radialDist = Math.hypot(worldCol, worldRow);
                double tu1 = worldCol * Math.cos(trailAngleA) + worldRow * Math.sin(trailAngleA);
                double tv1 = -worldCol * Math.sin(trailAngleA) + worldRow * Math.cos(trailAngleA);
                double trail1 = Math.abs(tv1 - (trailOffsetA + Math.sin(tu1 * 0.028 + riverPhaseA) * 11.0 + Math.cos(tu1 * 0.014) * 7.0));

                double tu2 = worldCol * Math.cos(trailAngleB) + worldRow * Math.sin(trailAngleB);
                double tv2 = -worldCol * Math.sin(trailAngleB) + worldRow * Math.cos(trailAngleB);
                double trail2 = Math.abs(tv2 - (trailOffsetB + Math.sin(tu2 * 0.024 + riverPhaseB) * 13.0));

                double baseRiverWidth = (biome == BiomeType.SWAMP) ? 3.5 : (biome == BiomeType.SNOW ? 2.1 : 2.6);

                // 3a. Grandes Lagos Fluviais e Bacias de Expansão Fluvial
                // A cada ~84 tiles de curso do rio, o rio deságua e expande-se em um grande lago navegável
                int lakeCell = (int) Math.floor(ru / 84.0);
                double lakeCellSeed = rawHash(lakeCell, 19, 2027L);
                double lakeU = (lakeCell + 0.35 + lakeCellSeed * 0.30) * 84.0;
                double lakeV = computeRiverCenterV(lakeU) + (rawHash(lakeCell, 23, 2039L) - 0.5) * 3.5;

                double dLu = ru - lakeU;
                double dLv = rv - lakeV;
                double distToLake = Math.hypot(dLu, dLv);

                double baseLakeRadius = (biome == BiomeType.SWAMP ? 11.5 : (biome == BiomeType.SNOW ? 8.5 : 9.8))
                                        + rawHash(lakeCell, 31, 2053L) * 5.2; // Raio de 8.5 a 16.7 tiles
                double lakeAngle = Math.atan2(dLv, dLu);
                double lakeWarp = (fbm(worldCol * 0.07, worldRow * 0.07, 2069L) - 0.5) * 4.4
                                  + Math.sin(lakeAngle * 3.0 + lakeCellSeed * 6.28) * 2.2;
                double effectiveLakeRadius = baseLakeRadius + lakeWarp;

                // Pontes sobre o rio: extremamente raras (espaçadas a cada ~190 tiles e com chance controlada)
                // NUNCA geradas atravessando lagos
                int bridgeCell = (int) Math.floor(ru / 190.0);
                double bridgeRoll = rawHash(bridgeCell, 7, 1777L);
                boolean hasBridge = bridgeRoll > 0.48; // Apenas ~52% dos segmentos de 190 tiles possuem ponte
                double bridgePos = (bridgeCell + 0.30 + bridgeRoll * 0.40) * 190.0;
                boolean isRusticBridge = hasBridge
                        && Math.abs(ru - bridgePos) <= 1.0
                        && riverDist <= (baseRiverWidth + 2.2)
                        && distToLake > (effectiveLakeRadius + 4.0);

                if (isRusticBridge) {
                    chunk[r][c] = TileType.DIRT_PATH;
                    continue;
                }

                // Processa o Grande Lago Fluvial se o ponto estiver dentro do raio da bacia lacustre
                if (distToLake < effectiveLakeRadius) {
                    if (biome == BiomeType.SNOW) {
                        // Grandes pistas de gelo e calotas congeladas nos lagos do bioma glacial
                        double freezeNoise = fbm(worldCol * 0.08, worldRow * 0.08, 4013L);
                        if (distToLake > effectiveLakeRadius * 0.32 || freezeNoise > 0.30) {
                            chunk[r][c] = TileType.ICE;
                            continue;
                        }
                    } else if (biome == BiomeType.SWAMP) {
                        // No PANTANO: a água se fragmenta em bancos de lodo, restingas e juncais (bayou/wetland)
                        double swampNoise = fbm(worldCol * 0.11, worldRow * 0.11, 3109L);
                        double edgeFactor = distToLake / effectiveLakeRadius;
                        if (swampNoise > 0.62 || (swampNoise > 0.48 && edgeFactor > 0.52)) {
                            chunk[r][c] = TileType.MUD; // Bancos de lodo/lama emergente
                            continue;
                        } else if (swampNoise > 0.44 && rawHash(worldCol, worldRow, 3119L) > 0.35) {
                            chunk[r][c] = TileType.TALL_GRASS; // Juncos, taboas e vegetação pantaneira
                            continue;
                        }
                    }
                    chunk[r][c] = TileType.WATER;
                    continue;
                } else if (distToLake < effectiveLakeRadius + 1.8) {
                    // Praia / orla de lama e seixos ao redor do lago
                    chunk[r][c] = (biome == BiomeType.SNOW || biome == BiomeType.SWAMP || moisture > 0.38)
                            ? TileType.MUD : TileType.TALL_GRASS;
                    continue;
                } else if (distToLake < effectiveLakeRadius + 3.4) {
                    // Várzea e juncos ao redor do lago
                    chunk[r][c] = (biome == BiomeType.SWAMP || moisture > 0.40) ? TileType.TALL_GRASS : TileType.GRASS;
                    continue;
                }

                // 3b. Lagos de Meandro em Ferradura (Oxbow Lakes)
                int oxbowCell = (int) Math.floor((ru + 42.0) / 84.0);
                double oxbowSeed = rawHash(oxbowCell, 37, 2099L);
                if (oxbowSeed > 0.35) { // 65% das bacias possuem lago de ferradura anexo
                    double oxbowU = (oxbowCell + 0.5) * 84.0;
                    double oxbowSide = (rawHash(oxbowCell, 39, 2101L) >= 0.5 ? 1.0 : -1.0);
                    double oxbowV = computeRiverCenterV(oxbowU) + oxbowSide * (11.0 + rawHash(oxbowCell, 41, 2111L) * 6.0);
                    double dOxU = ru - oxbowU;
                    double dOxV = (rv - oxbowV) * 1.45;
                    double distOxbow = Math.hypot(dOxU, dOxV);
                    double oxbowRadius = 4.2 + rawHash(oxbowCell, 43, 2129L) * 2.6;

                    if (distOxbow < oxbowRadius) {
                        if (biome == BiomeType.SNOW) {
                            chunk[r][c] = TileType.ICE; // Lagoas de ferradura congeladas na neve
                        } else if (biome == BiomeType.SWAMP) {
                            double oxbowSwamp = fbm(worldCol * 0.12, worldRow * 0.12, 3137L);
                            if (oxbowSwamp > 0.58) {
                                chunk[r][c] = TileType.MUD; // Bancos de lodo nos meandros do pântano
                                continue;
                            } else if (oxbowSwamp > 0.46) {
                                chunk[r][c] = TileType.TALL_GRASS; // Taboas em charco estagnado
                                continue;
                            } else {
                                chunk[r][c] = TileType.WATER;
                            }
                        } else {
                            chunk[r][c] = TileType.WATER;
                        }
                        continue;
                    } else if (distOxbow < oxbowRadius + 1.5) {
                        chunk[r][c] = TileType.MUD;
                        continue;
                    } else if (distOxbow < oxbowRadius + 2.8) {
                        chunk[r][c] = TileType.TALL_GRASS;
                        continue;
                    }
                }

                // 3c. Curso Normal do Rio (fora das bacias lacustres)
                double widthNoise = fbm(ru * 0.032, 7.2, 853L); // 0.0 .. 1.0
                double riverHalfWidth = baseRiverWidth + (widthNoise - 0.45) * 1.8; // Varia entre 1.7 e 4.4 tiles

                // Margens e várzeas estratificadas
                double bankNoise = (fbm(worldCol * 0.08, worldRow * 0.08, 941L) - 0.5) * 0.8;
                double mudHalfWidth = riverHalfWidth + (biome == BiomeType.SWAMP ? 1.9 : 1.1) + bankNoise;
                double marshHalfWidth = mudHalfWidth + (biome == BiomeType.SWAMP ? 2.2 : 1.4);

                if (riverDist < riverHalfWidth) {
                    if (biome == BiomeType.SNOW && (riverDist > riverHalfWidth * 0.55 || fbm(ru * 0.06, 4.2, 4027L) > 0.60)) {
                        chunk[r][c] = TileType.ICE; // Calota de gelo nas margens e remansos fluviais congelados
                        continue;
                    } else if (biome == BiomeType.SWAMP) {
                        // No PANTANO: rios formam bancos de lodo e canais rasos entrelaçados (braided river)
                        double riverSwampNoise = fbm(worldCol * 0.13, worldRow * 0.13, 3121L);
                        if (riverDist > riverHalfWidth * 0.28) {
                            if (riverSwampNoise > 0.62) {
                                chunk[r][c] = TileType.MUD; // Banco de lodo emergente
                                continue;
                            } else if (riverSwampNoise > 0.50) {
                                chunk[r][c] = TileType.TALL_GRASS; // Juncais e taboas ribeirinhas
                                continue;
                            }
                        }
                    }
                    chunk[r][c] = TileType.WATER;
                    continue;
                } else if (riverDist < mudHalfWidth) {
                    chunk[r][c] = (biome == BiomeType.SNOW || biome == BiomeType.SWAMP || moisture > 0.42)
                            ? TileType.MUD : TileType.TALL_GRASS;
                    continue;
                } else if (riverDist < marshHalfWidth) {
                    chunk[r][c] = (biome == BiomeType.SWAMP || moisture > 0.40) ? TileType.TALL_GRASS : TileType.GRASS;
                    continue;
                }

                // 3d. Riacho Afluente Secundário (Tributary Stream)
                double tru = worldCol * tribCos + worldRow * tribSin;
                double trv = -worldCol * tribSin + worldRow * tribCos;
                double tribCenterV = tribOffset + Math.sin(tru * 0.021 + tribPhaseA) * 14.0
                                     + Math.cos(tru * 0.042 + tribPhaseB) * 6.5
                                     + (fbm(tru * 0.08, 2.1, 967L) - 0.5) * 5.0;
                double tribDist = Math.abs(trv - tribCenterV);
                double tribWidth = (biome == BiomeType.SWAMP ? 1.6 : 1.25) + (fbm(tru * 0.04, 1.2, 971L) - 0.5) * 0.4;

                int tribBridgeCell = (int) Math.floor(tru / 180.0);
                double tribBridgeRoll = rawHash(tribBridgeCell, 11, 1888L);
                boolean hasTribBridge = (tribBridgeRoll > 0.52);
                double tribBridgePos = (tribBridgeCell + 0.35 + tribBridgeRoll * 0.30) * 180.0;
                boolean isTribBridge = hasTribBridge && Math.abs(tru - tribBridgePos) <= 1.0 && tribDist <= (tribWidth + 1.4);

                if (isTribBridge) {
                    chunk[r][c] = TileType.DIRT_PATH;
                    continue;
                }

                if (tribDist < tribWidth) {
                    if (biome == BiomeType.SNOW && (tribDist > tribWidth * 0.50 || rawHash(worldCol, worldRow, 4049L) > 0.42)) {
                        chunk[r][c] = TileType.ICE;
                        continue;
                    } else if (biome == BiomeType.SWAMP) {
                        double tribSwampNoise = fbm(worldCol * 0.16, worldRow * 0.16, 3181L);
                        if (tribSwampNoise > 0.66) {
                            chunk[r][c] = TileType.MUD; // Vales lamacentos e restingas
                            continue;
                        }
                    }
                    chunk[r][c] = TileType.WATER;
                    continue;
                } else if (tribDist < tribWidth + 1.2) {
                    chunk[r][c] = (biome == BiomeType.SWAMP || moisture > 0.48) ? TileType.MUD : TileType.TALL_GRASS;
                    continue;
                }

                // 4. Trilhas naturais orgânicas conectando os biomas sem repetir sempre o mesmo caminho
                if (radialDist > 15.0 && (trail1 < 0.72 || trail2 < 0.72)) {
                    chunk[r][c] = TileType.DIRT_PATH;
                    continue;
                }

                // 5. Lagos Regionais Autônomos, Lagoas Glaciais e Brejos Profundos
                double adjElevation = elevation - biome.getWaterBias();
                double adjMoisture = moisture + biome.getMudBias();
                if (adjElevation < 0.24 && adjMoisture > 0.52) {
                    if (biome == BiomeType.SWAMP) {
                        double bogPattern = fbm(worldCol * 0.10, worldRow * 0.10, 3163L);
                        if (bogPattern > 0.54) {
                            chunk[r][c] = TileType.MUD; // Lodaçais e restingas de turfa
                        } else if (bogPattern > 0.38) {
                            chunk[r][c] = TileType.TALL_GRASS; // Juncais densos de brejo
                        } else {
                            chunk[r][c] = TileType.WATER; // Poças e canais de água barrenta
                        }
                    } else if (biome == BiomeType.SNOW) {
                        if (adjElevation < 0.12) {
                            chunk[r][c] = TileType.WATER;
                        } else if (adjElevation < 0.21) {
                            chunk[r][c] = TileType.ICE; // Lagoas e platôs de gelo cristalino
                        } else {
                            chunk[r][c] = (rawHash(worldCol, worldRow, 529L) > 0.40) ? TileType.MUD : TileType.TALL_GRASS;
                        }
                    } else if (biome == BiomeType.HIGHLAND) {
                        if (adjElevation < 0.14) {
                            chunk[r][c] = TileType.WATER;
                        } else if (adjElevation < 0.19 && moisture > 0.58) {
                            chunk[r][c] = TileType.ICE; // Geleiras de altitude
                        } else {
                            chunk[r][c] = TileType.MUD;
                        }
                    } else {
                        if (adjElevation < 0.17) {
                            chunk[r][c] = TileType.WATER;
                        } else {
                            chunk[r][c] = (moisture > 0.56) ? TileType.MUD : TileType.TALL_GRASS;
                        }
                    }
                    continue;
                }

                // Bolsões de lama no Pântano e poças congeladas/slush na Tundra
                if (biome == BiomeType.SWAMP && forestClumping > 0.36 && forestClumping < 0.44 && moisture > 0.50) {
                    chunk[r][c] = TileType.MUD;
                    continue;
                }
                if (biome == BiomeType.SNOW && forestClumping > 0.34 && forestClumping < 0.39 && moisture > 0.56) {
                    chunk[r][c] = rawHash(worldCol, worldRow, 533L) > 0.40 ? TileType.ICE : TileType.MUD;
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

    private double computeRiverCenterV(double u) {
        double meanderMacro = Math.sin(u * 0.012 + riverPhaseA) * 20.0;
        double meanderMed = Math.cos(u * 0.026 + riverPhaseB) * 11.0;
        double meanderDetail = (fbm(u * 0.065, 3.8, 821L) - 0.5) * 7.5;
        return riverOffset + meanderMacro + meanderMed + meanderDetail;
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
