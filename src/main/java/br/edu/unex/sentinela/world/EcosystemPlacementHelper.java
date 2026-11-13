package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.entity.Enemy;

import java.util.List;
import java.util.Random;

/**
 * Auxiliar modular de posicionamento ecológico:
 * garante rotação 360° aleatória real, seleção ponderada por bioma
 * e validação de densidade local para evitar spawns amontoados.
 */
public final class EcosystemPlacementHelper {
    private EcosystemPlacementHelper() {}

    public static double[] generateShuffledAngles(int count, Random rand) {
        double[] angles = new double[count];
        double base = rand.nextDouble() * Math.PI * 2.0;
        double step = (Math.PI * 2.0) / Math.max(1, count);
        for (int i = 0; i < count; i++) {
            double jitter = (rand.nextDouble() - 0.5) * step * 0.72;
            angles[i] = base + i * step + jitter;
        }
        for (int i = count - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            double tmp = angles[i];
            angles[i] = angles[j];
            angles[j] = tmp;
        }
        return angles;
    }

    public static EnemySkin pickWeightedRivalSkin(BiomeType biome, Random rand) {
        double r = rand.nextDouble();
        if (biome == null) biome = BiomeType.MEADOW;
        switch (biome) {
            case SNOW:
                if (r < 0.50) return EnemySkin.WOLF;
                return r < 0.76 ? EnemySkin.BOAR : EnemySkin.BADGER;
            case SWAMP:
                if (r < 0.52) return EnemySkin.BOAR;
                return r < 0.80 ? EnemySkin.BADGER : EnemySkin.WOLF;
            case DENSE_FOREST:
                if (r < 0.44) return EnemySkin.WOLF;
                return r < 0.78 ? EnemySkin.BADGER : EnemySkin.BOAR;
            case HIGHLAND:
                if (r < 0.44) return EnemySkin.BADGER;
                return r < 0.84 ? EnemySkin.WOLF : EnemySkin.BOAR;
            case MEADOW:
            default:
                if (r < 0.34) return EnemySkin.WOLF;
                return r < 0.67 ? EnemySkin.BOAR : EnemySkin.BADGER;
        }
    }

    public static int countEnemiesWithin(List<Enemy> enemies, double wx, double wy, double radiusPx) {
        int count = 0;
        for (Enemy existing : enemies) {
            if (existing.getHp() > 0 && Math.hypot(existing.getX() - wx, existing.getY() - wy) < radiusPx) {
                count++;
            }
        }
        return count;
    }

    public static boolean isTileClearAndUncrowded(TileMap tileMap, List<Enemy> enemies, int col, int row,
                                                  double minSeparationPx, double crowdRadiusPx, int maxCrowdNeighbors) {
        return isTileClearAndUncrowded(tileMap, enemies, col, row, minSeparationPx, crowdRadiusPx, maxCrowdNeighbors, null, minSeparationPx);
    }

    public static boolean isTileClearAndUncrowded(TileMap tileMap, List<Enemy> enemies, int col, int row,
                                                  double minSeparationPx, double crowdRadiusPx, int maxCrowdNeighbors,
                                                  Enemy pairedAlly, double pairedMinSeparationPx) {
        TileType center = tileMap.getTileAt(col, row);
        if (!center.isWalkable()) {
            return false;
        }

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
            double reqSep = (existing == pairedAlly) ? pairedMinSeparationPx : minSeparationPx;
            if (Math.hypot(existing.getX() - wx, existing.getY() - wy) < reqSep) {
                return false;
            }
        }
        return countEnemiesWithin(enemies, wx, wy, crowdRadiusPx) <= maxCrowdNeighbors;
    }

    public static int evaluateHabitatAffinity(TileMap tileMap, EnemyType type, EnemySkin skin, int col, int row) {
        TileType tile = tileMap.getTileAt(col, row);
        if (skin == EnemySkin.CROCODILE) {
            if (tile == TileType.WATER) return 4;
            if (tile == TileType.MUD) return 3;
            return hasNeighborOfType(tileMap, col, row, TileType.WATER) ? 2 : 0;
        }
        if (tile == TileType.WATER) {
            return -1;
        }
        if (type == EnemyType.HUNTER || type == EnemyType.HOUND) {
            if (tile == TileType.DIRT_PATH) return 4;
            if (tile == TileType.GRASS && hasNeighborOfType(tileMap, col, row, TileType.DIRT_PATH)) return 3;
            return tile == TileType.GRASS ? 2 : 1;
        }
        if (skin == EnemySkin.BOAR) {
            if (tile == TileType.MUD || tile == TileType.TALL_GRASS) return 4;
            return hasNeighborOfType(tileMap, col, row, TileType.MUD) ? 3 : 2;
        }
        if (skin == EnemySkin.WOLF || skin == EnemySkin.PANTHER || skin == EnemySkin.BADGER) {
            if (tile == TileType.TALL_GRASS) return 4;
            return (tile == TileType.GRASS && hasNeighborOfType(tileMap, col, row, TileType.TREE)) ? 3 : 2;
        }
        if (skin == EnemySkin.BEAR || skin == EnemySkin.POLAR_BEAR) {
            BiomeType target = (skin == EnemySkin.POLAR_BEAR) ? BiomeType.SNOW : BiomeType.HIGHLAND;
            if (tileMap.getBiomeAt(col, row) == target && hasNeighborOfType(tileMap, col, row, TileType.WALL)) return 5;
            if (tile == TileType.GRASS && hasNeighborOfType(tileMap, col, row, TileType.WALL)) return 4;
            return (tile == TileType.GRASS && hasNeighborOfType(tileMap, col, row, TileType.TREE)) ? 3 : 2;
        }
        return 2;
    }

    public static boolean hasNeighborOfType(TileMap tileMap, int c, int r, TileType targetType) {
        return tileMap.getTileAt(c + 1, r) == targetType
            || tileMap.getTileAt(c - 1, r) == targetType
            || tileMap.getTileAt(c, r + 1) == targetType
            || tileMap.getTileAt(c, r - 1) == targetType;
    }
}
