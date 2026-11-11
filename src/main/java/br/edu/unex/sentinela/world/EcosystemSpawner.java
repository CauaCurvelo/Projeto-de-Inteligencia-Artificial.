package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;

import java.util.List;
import java.util.Random;

/**
 * Gerenciador Ecológico de Distribuição e Spawn (Ecosystem Spawner):
 * - Distribuição em anel radial 360° (eliminando o viés de spawn apenas nas 4 diagonais)
 * - Validação estrita de colisão e espaçamento (Poisson-disk mínimo para nunca nascer dentro de árvores/paredes ou empilhados)
 * - Afinidade de Habitat por Espécie:
 *   * Caçador & Cão: Trilhas de terra e clareiras abertas (frequentemente em dupla coordenada)
 *   * Lobo &Pantera: Bordas de floresta densa
 *   * Javali & Texugo: Mata alta, lamaçais e proximidades de arbustos de frutas
 *   * Crocodilo: Exclusivo de rios e margens de lama
 *   * Urso Pardo: Clareiras próximas a encostas rochosas ou mata fechada
 */
public class EcosystemSpawner {
    private static final Random rand = new Random();
    private static final double MIN_ENEMY_SEPARATION = 96.0; // Mínimo de 3 tiles entre inimigos no spawn

    public void populateInitialEcosystem(TileMap tileMap, List<Enemy> enemies, List<FoodItem> foodItems, List<Burrow> burrows) {
        // 1. Tocas da Raposa distribuídas em setores angulares ao redor do mapa inicial
        for (int sector = 0; sector < 6; sector++) {
            double baseAngle = sector * (Math.PI / 3.0);
            spawnBurrowInSector(tileMap, burrows, 0, 0, baseAngle, 10, 26);
        }

        // 2. Arbustos de Frutas iniciais próximos a bosques e campinas
        for (int i = 0; i < 14; i++) {
            spawnFoodNearPlayer(tileMap, foodItems, 0, 0);
        }

        // 3. Distribuição Inicial Balanceada em 360° por Habitat (Sem coordenadas fixas cegas!)
        // Duplas de Caçador + Cão Rastreador patrulhando trilhas/clareiras em setores opostos
        spawnHunterAndHoundPair(tileMap, enemies, 0, 0, Math.toRadians(35), 18, 26, 1.0);
        spawnHunterAndHoundPair(tileMap, enemies, 0, 0, Math.toRadians(215), 20, 28, 1.0);

        // Caçador solitário e Cão farejador em outros quadrantes
        spawnByHabitat(tileMap, enemies, EnemyType.HUNTER, EnemySkin.HUNTER_SNIPER, 0, 0, Math.toRadians(130), 20, 28, 1.0);
        spawnByHabitat(tileMap, enemies, EnemyType.HOUND, EnemySkin.HOUND_TRACKER, 0, 0, Math.toRadians(310), 18, 26, 1.0);

        // Fauna Rival distribuída em todos os ângulos (Javalis, Lobos e Texugos)
        spawnByHabitat(tileMap, enemies, EnemyType.RIVAL, EnemySkin.WOLF,   0, 0, Math.toRadians(15),  14, 22, 1.0);
        spawnByHabitat(tileMap, enemies, EnemyType.RIVAL, EnemySkin.BOAR,   0, 0, Math.toRadians(85),  13, 21, 1.0);
        spawnByHabitat(tileMap, enemies, EnemyType.RIVAL, EnemySkin.BADGER, 0, 0, Math.toRadians(155), 14, 22, 1.0);
        spawnByHabitat(tileMap, enemies, EnemyType.RIVAL, EnemySkin.WOLF,   0, 0, Math.toRadians(200), 16, 24, 1.0);
        spawnByHabitat(tileMap, enemies, EnemyType.RIVAL, EnemySkin.BOAR,   0, 0, Math.toRadians(260), 14, 22, 1.0);
        spawnByHabitat(tileMap, enemies, EnemyType.RIVAL, EnemySkin.BADGER, 0, 0, Math.toRadians(335), 15, 23, 1.0);

        // Chefe Territorial Inicial (Distante da clareira central)
        spawnByHabitat(tileMap, enemies, EnemyType.BEAR_BOSS, EnemySkin.BEAR, 0, 0, Math.toRadians(70), 25, 33, 1.0);
    }

    public void spawnReinforcementNearPlayer(TileMap tileMap, List<Enemy> enemies, Player player, double survivalTime) {
        int pCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
        int pRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);
        double scale = 1.0 + (survivalTime / 240.0);

        // Escolhe um ângulo 360° real aleatório para evitar concentração nos cantos
        double angle = rand.nextDouble() * Math.PI * 2.0;
        int roll = rand.nextInt(100);

        if (roll < 22) {
            // Patrulha de Caçador (às vezes acompanhado de Cão)
            if (rand.nextBoolean() && enemies.size() + 2 <= 24) {
                spawnHunterAndHoundPair(tileMap, enemies, pCol, pRow, angle, 20, 30, scale);
            } else {
                spawnByHabitat(tileMap, enemies, EnemyType.HUNTER, EnemySkin.HUNTER_SNIPER, pCol, pRow, angle, 20, 30, scale);
            }
        } else if (roll < 45) {
            spawnByHabitat(tileMap, enemies, EnemyType.HOUND, EnemySkin.HOUND_TRACKER, pCol, pRow, angle, 19, 28, scale);
        } else {
            int rivalPick = rand.nextInt(3);
            EnemySkin skin = (rivalPick == 0) ? EnemySkin.WOLF : (rivalPick == 1 ? EnemySkin.BOAR : EnemySkin.BADGER);
            spawnByHabitat(tileMap, enemies, EnemyType.RIVAL, skin, pCol, pRow, angle, 17, 26, scale);
        }
    }

    public void spawnApexBossNearPlayer(TileMap tileMap, List<Enemy> enemies, Player player, double survivalTime) {
        int pCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
        int pRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);
        double scale = 1.0 + (survivalTime / 240.0);

        int bossChoice = rand.nextInt(3);
        EnemySkin bossSkin = (bossChoice == 0) ? EnemySkin.BEAR :
                             (bossChoice == 1 ? EnemySkin.CROCODILE : EnemySkin.PANTHER);

        double angle = rand.nextDouble() * Math.PI * 2.0;
        spawnByHabitat(tileMap, enemies, EnemyType.BEAR_BOSS, bossSkin, pCol, pRow, angle, 22, 32, scale);
    }

    private void spawnHunterAndHoundPair(TileMap tileMap, List<Enemy> enemies, int centerCol, int centerRow,
                                         double baseAngle, int minRadiusTiles, int maxRadiusTiles, double scale) {
        Enemy hunter = spawnByHabitat(tileMap, enemies, EnemyType.HUNTER, EnemySkin.HUNTER_SNIPER,
                                      centerCol, centerRow, baseAngle, minRadiusTiles, maxRadiusTiles, scale);
        if (hunter != null) {
            int hCol = (int) Math.floor(hunter.getX() / TileMap.TILE_SIZE);
            int hRow = (int) Math.floor(hunter.getY() / TileMap.TILE_SIZE);
            // Spawna o Cão Rastreador a 2-3 blocos ao lado do seu mestre Caçador
            for (int tries = 0; tries < 16; tries++) {
                int c = hCol + (rand.nextInt(5) - 2);
                int r = hRow + (rand.nextInt(5) - 2);
                if (isTileClearForSpawn(tileMap, enemies, c, r, 36.0)) {
                    Enemy hound = new Enemy(EnemyType.HOUND, EnemySkin.HOUND_TRACKER,
                                            c * TileMap.TILE_SIZE + 16, r * TileMap.TILE_SIZE + 16, scale, scale);
                    enemies.add(hound);
                    break;
                }
            }
        }
    }

    /**
     * Procura no setor angular indicado um tile que combine com o habitat natural da espécie
     * e que esteja livre de árvores, rochas e outros inimigos.
     */
    public Enemy spawnByHabitat(TileMap tileMap, List<Enemy> enemies, EnemyType type, EnemySkin skin,
                                int centerCol, int centerRow, double preferredAngle,
                                int minRadiusTiles, int maxRadiusTiles, double scale) {
        int bestCol = Integer.MIN_VALUE;
        int bestRow = Integer.MIN_VALUE;
        int bestScore = -1;

        for (int attempt = 0; attempt < 45; attempt++) {
            // Espalha gradualmente o leque angular se não achar de primeira no ângulo exato
            double angleSpread = (attempt < 15) ? 0.65 : Math.PI;
            double angle = preferredAngle + (rand.nextDouble() * 2.0 - 1.0) * angleSpread;
            double distTiles = minRadiusTiles + rand.nextDouble() * (maxRadiusTiles - minRadiusTiles);

            int c = centerCol + (int) Math.round(Math.cos(angle) * distTiles);
            int r = centerRow + (int) Math.round(Math.sin(angle) * distTiles);

            if (!isTileClearForSpawn(tileMap, enemies, c, r, MIN_ENEMY_SEPARATION)) {
                continue;
            }

            int affinityScore = evaluateBiomeAffinity(tileMap, type, skin, c, r);
            if (affinityScore > bestScore) {
                bestScore = affinityScore;
                bestCol = c;
                bestRow = r;
                if (bestScore >= 3) {
                    break; // Habitat ideal encontrado!
                }
            }
        }

        if (bestCol != Integer.MIN_VALUE) {
            double wx = bestCol * TileMap.TILE_SIZE + 16.0;
            double wy = bestRow * TileMap.TILE_SIZE + 16.0;
            Enemy spawned = new Enemy(type, skin, wx, wy, scale, scale);
            enemies.add(spawned);
            return spawned;
        }
        return null;
    }

    /**
     * Calcula a pontuação de afinidade ecológica (0 a 4) entre a espécie e o terreno candidato.
     */
    private int evaluateBiomeAffinity(TileMap tileMap, EnemyType type, EnemySkin skin, int col, int row) {
        TileType tile = tileMap.getTileAt(col, row);

        // Crocodilo: Deve nascer na água ou lama
        if (skin == EnemySkin.CROCODILE) {
            if (tile == TileType.WATER) return 4;
            if (tile == TileType.MUD) return 3;
            return hasNeighborOfType(tileMap, col, row, TileType.WATER) ? 2 : 0;
        }

        // Animais terrestres não devem nascer submersos no meio do rio
        if (tile == TileType.WATER) {
            return -1;
        }

        if (type == EnemyType.HUNTER || type == EnemyType.HOUND) {
            if (tile == TileType.DIRT_PATH) return 4;
            if (tile == TileType.GRASS && hasNeighborOfType(tileMap, col, row, TileType.DIRT_PATH)) return 3;
            if (tile == TileType.GRASS) return 2;
            return 1;
        }

        if (skin == EnemySkin.BOAR) {
            if (tile == TileType.MUD || tile == TileType.TALL_GRASS) return 4;
            if (hasNeighborOfType(tileMap, col, row, TileType.MUD)) return 3;
            return 2;
        }

        if (skin == EnemySkin.WOLF || skin == EnemySkin.PANTHER || skin == EnemySkin.BADGER) {
            if (tile == TileType.TALL_GRASS) return 4;
            if (tile == TileType.GRASS && hasNeighborOfType(tileMap, col, row, TileType.TREE)) return 3;
            return 2;
        }

        if (skin == EnemySkin.BEAR) {
            if (tile == TileType.GRASS && hasNeighborOfType(tileMap, col, row, TileType.WALL)) return 4;
            if (tile == TileType.GRASS && hasNeighborOfType(tileMap, col, row, TileType.TREE)) return 3;
            return 2;
        }

        return 2;
    }

    /**
     * Garante que o tile e suas bordas imediatas não sejam obstáculos e que não haja outro inimigo muito próximo.
     */
    private boolean isTileClearForSpawn(TileMap tileMap, List<Enemy> enemies, int col, int row, double minSeparationPx) {
        TileType center = tileMap.getTileAt(col, row);
        if (!center.isWalkable()) {
            return false;
        }

        // Garante que o inimigo não nasça totalmente cercado por 4 árvores/paredes (beco sem saída de 1 tile)
        int walkableNeighbors = 0;
        if (tileMap.getTileAt(col + 1, row).isWalkable()) walkableNeighbors++;
        if (tileMap.getTileAt(col - 1, row).isWalkable()) walkableNeighbors++;
        if (tileMap.getTileAt(col, row + 1).isWalkable()) walkableNeighbors++;
        if (tileMap.getTileAt(col, row - 1).isWalkable()) walkableNeighbors++;
        if (walkableNeighbors < 2) {
            return false;
        }

        double wx = col * TileMap.TILE_SIZE + 16.0;
        double wy = row * TileMap.TILE_SIZE + 16.0;

        for (Enemy existing : enemies) {
            if (Math.hypot(existing.getX() - wx, existing.getY() - wy) < minSeparationPx) {
                return false;
            }
        }
        return true;
    }

    public void spawnFoodNearPlayer(TileMap tileMap, List<FoodItem> foodItems, int playerCol, int playerRow) {
        for (int tries = 0; tries < 45; tries++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double dist = 7 + rand.nextDouble() * 22;
            int c = playerCol + (int) Math.round(Math.cos(angle) * dist);
            int r = playerRow + (int) Math.round(Math.sin(angle) * dist);
            TileType t = tileMap.getTileAt(c, r);

            if (t == TileType.GRASS || t == TileType.TALL_GRASS) {
                boolean nearVegetation = (t == TileType.TALL_GRASS) || hasNeighborOfType(tileMap, c, r, TileType.TREE);
                if (!nearVegetation) continue;

                double wx = c * TileMap.TILE_SIZE + 16;
                double wy = r * TileMap.TILE_SIZE + 16;
                boolean tooClose = false;
                for (FoodItem f : foodItems) {
                    if (!f.isEaten() && Math.hypot(f.getX() - wx, f.getY() - wy) < 96.0) {
                        tooClose = true;
                        break;
                    }
                }
                if (!tooClose) {
                    foodItems.add(new FoodItem(c, r));
                    break;
                }
            }
        }
    }

    public void spawnBurrowInSector(TileMap tileMap, List<Burrow> burrows, int centerCol, int centerRow,
                                    double sectorAngle, int minDist, int maxDist) {
        for (int tries = 0; tries < 25; tries++) {
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
                    if (Math.hypot(b.getX() - wx, b.getY() - wy) < 160.0) {
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

    private boolean hasNeighborOfType(TileMap tileMap, int c, int r, TileType targetType) {
        return tileMap.getTileAt(c + 1, r) == targetType ||
               tileMap.getTileAt(c - 1, r) == targetType ||
               tileMap.getTileAt(c, r + 1) == targetType ||
               tileMap.getTileAt(c, r - 1) == targetType;
    }
}
