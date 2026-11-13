package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.ai.AnimalVariant;
import br.edu.unex.sentinela.ai.BossArchetype;
import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;

import java.util.List;
import java.util.Random;

/**
 * Gerenciador Ecológico de Distribuição e Spawn (Ecosystem Spawner):
 * - Distribuição aleatória real rotacionada por partida (sem ângulos ou raios engessados)
 * - Validação de densidade local e distância mínima para impedir amontoamento
 * - Chefes nativos distribuídos em cada bioma regional (incluindo Tundra Nevada)
 */
public class EcosystemSpawner {
    public EcosystemSpawner() {}

    private static final double MIN_ENEMY_SEPARATION = 190.0;
    private static final double CROWD_CHECK_RADIUS = 280.0;
    private static final int MAX_CROWD_NEIGHBORS = 1;
    private static final double MIN_BOSS_TERRITORY_SPACING = 620.0;
    private static final int MAX_ACTIVE_BOSSES = 4;

    private final Random rand = new Random();

    public void populateInitialEcosystem(TileMap tileMap, List<Enemy> enemies, List<FoodItem> foodItems, List<Burrow> burrows) {
        rand.setSeed(tileMap.getWorldSeed() ^ System.nanoTime());

        // 1. Tocas da Raposa distribuídas em direções embaralhadas ao redor do mapa inicial
        double[] burrowAngles = EcosystemPlacementHelper.generateShuffledAngles(6, rand);
        for (double angle : burrowAngles) {
            spawnBurrowInSector(tileMap, burrows, 0, 0, angle, 11, 34);
        }

        // 2. Arbustos de frutas espalhados organicamente sem formar agrupamentos
        for (int i = 0; i < 20; i++) {
            spawnFoodNearPlayer(tileMap, foodItems, 0, 0, 8.0, 56.0, 145.0);
        }

        // 3. Patrulhas iniciais com rotação 360° aleatória e ampla dispersão radial
        double[] patrolAngles = EcosystemPlacementHelper.generateShuffledAngles(4, rand);
        spawnHunterAndHoundPair(tileMap, enemies, 0, 0, patrolAngles[0], 17, 36, 1.0);
        spawnHunterAndHoundPair(tileMap, enemies, 0, 0, patrolAngles[1], 22, 46, 1.0);
        spawnByHabitat(tileMap, enemies, EnemyType.HUNTER, EnemySkin.HUNTER_SNIPER, 0, 0, patrolAngles[2], 20, 48, 1.0);
        spawnByHabitat(tileMap, enemies, EnemyType.HOUND, EnemySkin.HOUND_TRACKER, 0, 0, patrolAngles[3], 18, 42, 1.0);

        // 4. Fauna silvestre variada conforme o bioma de cada setor sorteado
        double[] wildAngles = EcosystemPlacementHelper.generateShuffledAngles(7, rand);
        for (int i = 0; i < wildAngles.length; i++) {
            double ang = wildAngles[i];
            int minR = 16 + (i % 3) * 8;
            int maxR = 36 + (i % 3) * 14;
            double sampleDist = (minR + maxR) * 0.5;
            int sampleCol = (int) Math.round(Math.cos(ang) * sampleDist);
            int sampleRow = (int) Math.round(Math.sin(ang) * sampleDist);
            BiomeType sectorBiome = tileMap.getBiomeAt(sampleCol, sampleRow);
            EnemySkin skin = EcosystemPlacementHelper.pickWeightedRivalSkin(sectorBiome, rand);
            spawnByHabitat(tileMap, enemies, EnemyType.RIVAL, skin, 0, 0, ang, minR, maxR, 1.0);
        }

        // 5. Quatro chefes nativos ancorados em seus respectivos biomas
        spawnBossInNativeBiome(tileMap, enemies, EnemySkin.CROCODILE, 0, 0, 28, 86, 1.0);
        spawnBossInNativeBiome(tileMap, enemies, EnemySkin.PANTHER, 0, 0, 28, 86, 1.0);
        spawnBossInNativeBiome(tileMap, enemies, EnemySkin.BEAR, 0, 0, 28, 86, 1.0);
        spawnBossInNativeBiome(tileMap, enemies, EnemySkin.POLAR_BEAR, 0, 0, 28, 86, 1.0);
    }

    public void spawnReinforcementNearPlayer(TileMap tileMap, List<Enemy> enemies, Player player, double survivalTime) {
        int pCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
        int pRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);
        double scale = 1.0 + (survivalTime / 240.0);

        double angle = rand.nextDouble() * Math.PI * 2.0;
        int sampleCol = pCol + (int) Math.round(Math.cos(angle) * 24.0);
        int sampleRow = pRow + (int) Math.round(Math.sin(angle) * 24.0);
        BiomeType sectorBiome = tileMap.getBiomeAt(sampleCol, sampleRow);
        int roll = rand.nextInt(100);

        if (roll < 22) {
            if (rand.nextBoolean() && enemies.size() + 2 <= 24) {
                spawnHunterAndHoundPair(tileMap, enemies, pCol, pRow, angle, 20, 32, scale);
            } else {
                spawnByHabitat(tileMap, enemies, EnemyType.HUNTER, EnemySkin.HUNTER_SNIPER, pCol, pRow, angle, 20, 32, scale);
            }
        } else if (roll < 42) {
            spawnByHabitat(tileMap, enemies, EnemyType.HOUND, EnemySkin.HOUND_TRACKER, pCol, pRow, angle, 19, 30, scale);
        } else {
            EnemySkin skin = EcosystemPlacementHelper.pickWeightedRivalSkin(sectorBiome, rand);
            spawnByHabitat(tileMap, enemies, EnemyType.RIVAL, skin, pCol, pRow, angle, 17, 30, scale);
        }
    }

    public void spawnApexBossNearPlayer(TileMap tileMap, List<Enemy> enemies, Player player, double survivalTime) {
        long activeBosses = enemies.stream()
            .filter(e -> e.getType() == EnemyType.BEAR_BOSS && e.getHp() > 0)
            .count();
        if (activeBosses >= MAX_ACTIVE_BOSSES) return;

        int pCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
        int pRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);
        double scale = 1.0 + (survivalTime / 240.0);

        BiomeType playerBiome = tileMap.getBiomeAt(pCol, pRow);
        BossArchetype preferred = BossArchetype.forBiome(playerBiome);
        if (spawnBossInNativeBiome(tileMap, enemies, preferred.toSkin(), pCol, pRow, 22, 46, scale) != null) {
            return;
        }

        for (EnemySkin candidateSkin : new EnemySkin[]{EnemySkin.CROCODILE, EnemySkin.PANTHER, EnemySkin.BEAR, EnemySkin.POLAR_BEAR}) {
            if (candidateSkin != preferred.toSkin()
                    && spawnBossInNativeBiome(tileMap, enemies, candidateSkin, pCol, pRow, 24, 52, scale) != null) {
                return;
            }
        }
    }

    private Enemy spawnBossInNativeBiome(TileMap tileMap, List<Enemy> enemies, EnemySkin bossSkin,
                                         int centerCol, int centerRow, int minRadiusTiles, int maxRadiusTiles, double scale) {
        BiomeType targetBiome = BossArchetype.fromSkin(bossSkin).getNativeBiome();
        int bestCol = Integer.MIN_VALUE;
        int bestRow = Integer.MIN_VALUE;
        int bestScore = Integer.MIN_VALUE;

        for (int attempt = 0; attempt < 180; attempt++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double distTiles = minRadiusTiles + rand.nextDouble() * (maxRadiusTiles - minRadiusTiles);
            int c = centerCol + (int) Math.round(Math.cos(angle) * distTiles);
            int r = centerRow + (int) Math.round(Math.sin(angle) * distTiles);

            if (!EcosystemPlacementHelper.isTileClearAndUncrowded(
                    tileMap, enemies, c, r, MIN_ENEMY_SEPARATION, CROWD_CHECK_RADIUS, MAX_CROWD_NEIGHBORS)
                    || !isBossTerritoryClear(enemies, c, r)) {
                continue;
            }
            if (tileMap.getBiomeAt(c, r) != targetBiome) {
                continue;
            }

            int score = 6 + EcosystemPlacementHelper.evaluateHabitatAffinity(tileMap, EnemyType.BEAR_BOSS, bossSkin, c, r);
            if (score > bestScore) {
                bestScore = score;
                bestCol = c;
                bestRow = r;
                if (score >= 9) break;
            }
        }

        if (bestCol != Integer.MIN_VALUE) {
            Enemy boss = new Enemy(
                EnemyType.BEAR_BOSS, bossSkin, AnimalVariant.STANDARD,
                bestCol * TileMap.TILE_SIZE + 16.0, bestRow * TileMap.TILE_SIZE + 16.0,
                scale, scale
            );
            enemies.add(boss);
            return boss;
        }
        return null;
    }

    private void spawnHunterAndHoundPair(TileMap tileMap, List<Enemy> enemies, int centerCol, int centerRow,
                                         double baseAngle, int minRadiusTiles, int maxRadiusTiles, double scale) {
        Enemy hunter = spawnByHabitat(tileMap, enemies, EnemyType.HUNTER, EnemySkin.HUNTER_SNIPER,
                                      centerCol, centerRow, baseAngle, minRadiusTiles, maxRadiusTiles, scale);
        if (hunter == null) return;

        int hCol = (int) Math.floor(hunter.getX() / TileMap.TILE_SIZE);
        int hRow = (int) Math.floor(hunter.getY() / TileMap.TILE_SIZE);
        for (int tries = 0; tries < 20; tries++) {
            double offsetAngle = rand.nextDouble() * Math.PI * 2.0;
            double offsetDist = 2.4 + rand.nextDouble() * 1.4; // 76..122 px do caçador (sem empilhar)
            int c = hCol + (int) Math.round(Math.cos(offsetAngle) * offsetDist);
            int r = hRow + (int) Math.round(Math.sin(offsetAngle) * offsetDist);
            if (EcosystemPlacementHelper.isTileClearAndUncrowded(
                    tileMap, enemies, c, r, MIN_ENEMY_SEPARATION, CROWD_CHECK_RADIUS, MAX_CROWD_NEIGHBORS, hunter, 72.0)) {
                Enemy hound = new Enemy(
                    EnemyType.HOUND, EnemySkin.HOUND_TRACKER, hunter.getVariant(),
                    c * TileMap.TILE_SIZE + 16.0, r * TileMap.TILE_SIZE + 16.0, scale, scale
                );
                enemies.add(hound);
                break;
            }
        }
    }

    public Enemy spawnByHabitat(TileMap tileMap, List<Enemy> enemies, EnemyType type, EnemySkin skin,
                                int centerCol, int centerRow, double preferredAngle,
                                int minRadiusTiles, int maxRadiusTiles, double scale) {
        int bestCol = Integer.MIN_VALUE;
        int bestRow = Integer.MIN_VALUE;
        int bestScore = -1;

        for (int attempt = 0; attempt < 55; attempt++) {
            double angleSpread = (attempt < 18) ? 0.65 : Math.PI;
            double angle = preferredAngle + (rand.nextDouble() * 2.0 - 1.0) * angleSpread;
            double distTiles = minRadiusTiles + rand.nextDouble() * (maxRadiusTiles - minRadiusTiles);

            int c = centerCol + (int) Math.round(Math.cos(angle) * distTiles);
            int r = centerRow + (int) Math.round(Math.sin(angle) * distTiles);

            if (!EcosystemPlacementHelper.isTileClearAndUncrowded(
                    tileMap, enemies, c, r, MIN_ENEMY_SEPARATION, CROWD_CHECK_RADIUS, MAX_CROWD_NEIGHBORS)) {
                continue;
            }
            if (type == EnemyType.BEAR_BOSS && !isBossTerritoryClear(enemies, c, r)) {
                continue;
            }

            int affinityScore = EcosystemPlacementHelper.evaluateHabitatAffinity(tileMap, type, skin, c, r);
            if (affinityScore > bestScore) {
                bestScore = affinityScore;
                bestCol = c;
                bestRow = r;
                if (bestScore >= 3) break;
            }
        }

        if (bestCol != Integer.MIN_VALUE) {
            double wx = bestCol * TileMap.TILE_SIZE + 16.0;
            double wy = bestRow * TileMap.TILE_SIZE + 16.0;
            BiomeType biome = tileMap.getBiomeAt(bestCol, bestRow);
            AnimalVariant variant = (type == EnemyType.BEAR_BOSS)
                    ? AnimalVariant.STANDARD
                    : AnimalVariant.forBiome(biome, rand);
            Enemy spawned = new Enemy(type, skin, variant, wx, wy, scale, scale);
            enemies.add(spawned);
            return spawned;
        }
        return null;
    }

    private boolean isBossTerritoryClear(List<Enemy> enemies, int col, int row) {
        double wx = col * TileMap.TILE_SIZE + 16.0;
        double wy = row * TileMap.TILE_SIZE + 16.0;
        for (Enemy existing : enemies) {
            if (existing.getType() == EnemyType.BEAR_BOSS && existing.getHp() > 0 && existing.getBossController() != null) {
                double distHome = Math.hypot(existing.getBossController().getHomeX() - wx,
                                             existing.getBossController().getHomeY() - wy);
                if (distHome < MIN_BOSS_TERRITORY_SPACING) {
                    return false;
                }
            }
        }
        return true;
    }

    public void spawnFoodNearPlayer(TileMap tileMap, List<FoodItem> foodItems, int playerCol, int playerRow) {
        spawnFoodNearPlayer(tileMap, foodItems, playerCol, playerRow, 7.0, 32.0, 128.0);
    }

    private void spawnFoodNearPlayer(TileMap tileMap, List<FoodItem> foodItems, int playerCol, int playerRow,
                                     double minDistTiles, double maxDistTiles, double minSpacingPx) {
        for (int tries = 0; tries < 55; tries++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double dist = minDistTiles + rand.nextDouble() * (maxDistTiles - minDistTiles);
            int c = playerCol + (int) Math.round(Math.cos(angle) * dist);
            int r = playerRow + (int) Math.round(Math.sin(angle) * dist);
            TileType t = tileMap.getTileAt(c, r);
            BiomeType biome = tileMap.getBiomeAt(c, r);

            if (t == TileType.GRASS || t == TileType.TALL_GRASS) {
                boolean nearVegetation = (t == TileType.TALL_GRASS)
                    || EcosystemPlacementHelper.hasNeighborOfType(tileMap, c, r, TileType.TREE)
                    || (biome == BiomeType.SWAMP && EcosystemPlacementHelper.hasNeighborOfType(tileMap, c, r, TileType.WATER))
                    || ((biome == BiomeType.HIGHLAND || biome == BiomeType.SNOW)
                        && EcosystemPlacementHelper.hasNeighborOfType(tileMap, c, r, TileType.WALL));
                if (!nearVegetation) continue;

                double wx = c * TileMap.TILE_SIZE + 16;
                double wy = r * TileMap.TILE_SIZE + 16;
                boolean tooClose = false;
                for (FoodItem f : foodItems) {
                    if (!f.isEaten() && Math.hypot(f.getX() - wx, f.getY() - wy) < minSpacingPx) {
                        tooClose = true;
                        break;
                    }
                }
                if (!tooClose) {
                    foodItems.add(new FoodItem(c, r, FoodVariant.pickForBiome(biome, rand)));
                    break;
                }
            }
        }
    }

    public void spawnBurrowInSector(TileMap tileMap, List<Burrow> burrows, int centerCol, int centerRow,
                                    double sectorAngle, int minDist, int maxDist) {
        for (int tries = 0; tries < 28; tries++) {
            double angle = sectorAngle + (rand.nextDouble() - 0.5) * (Math.PI / 3.0);
            double dist = minDist + rand.nextDouble() * (maxDist - minDist);
            int c = centerCol + (int) Math.round(Math.cos(angle) * dist);
            int r = centerRow + (int) Math.round(Math.sin(angle) * dist);

            TileType t = tileMap.getTileAt(c, r);
            if (t == TileType.GRASS || t == TileType.TALL_GRASS) {
                double wx = c * TileMap.TILE_SIZE + 16;
                double wy = r * TileMap.TILE_SIZE + 16;
                boolean tooClose = false;
                for (Burrow b : burrows) {
                    if (Math.hypot(b.getX() - wx, b.getY() - wy) < 170.0) {
                        tooClose = true;
                        break;
                    }
                }
                if (!tooClose) {
                    burrows.add(new Burrow(c, r));
                    break;
                }
            }
        }
    }
}
