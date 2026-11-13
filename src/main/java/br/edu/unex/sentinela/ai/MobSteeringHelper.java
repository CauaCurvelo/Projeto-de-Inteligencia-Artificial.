package br.edu.unex.sentinela.ai;

import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.world.FoodItem;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;

import java.util.List;
import java.util.Random;

/**
 * Auxiliar modular de navegação e dispersão de fauna:
 * - Mantém espaço pessoal fluido entre mobs em patrulha, busca e até mesmo em pausas de vigília.
 * - Penaliza destinos de patrulha com aglomeração de outros mobs para distribuir a fauna organicamente.
 * - Filtra arbustos de frutas já ocupados por outro animal para evitar congestionamento.
 */
public final class MobSteeringHelper {
    private MobSteeringHelper() {}

    public static double[] computeSeparationForce(Enemy self, List<Enemy> allEnemies, SpeciesBehaviorProfile profile) {
        AIState st = self.getState();
        boolean inCombat = (st == AIState.CHASE || st == AIState.TERRITORIAL_ATTACK || st == AIState.FIGHTING_ENEMY);
        double radius = inCombat ? profile.getCombatPersonalSpacePx() : profile.getPatrolPersonalSpacePx();

        double sepX = 0.0;
        double sepY = 0.0;
        for (Enemy other : allEnemies) {
            if (other == self || other.getHp() <= 0) continue;

            // Duplas Caçador + Cão toleram distância menor entre si do que desconhecidos
            boolean isHunterHoundPair = !inCombat
                && ((self.getType() == EnemyType.HOUND && other.getType() == EnemyType.HUNTER)
                    || (self.getType() == EnemyType.HUNTER && other.getType() == EnemyType.HOUND));
            double effectiveRadius = isHunterHoundPair ? Math.min(radius, 64.0) : radius;

            double dx = self.getX() - other.getX();
            double dy = self.getY() - other.getY();
            double d = Math.hypot(dx, dy);
            if (d < effectiveRadius) {
                if (d < 0.001) {
                    double fallbackAngle = (System.identityHashCode(self) & 255) * (Math.PI * 2.0 / 256.0);
                    sepX += Math.cos(fallbackAngle) * 1.4;
                    sepY += Math.sin(fallbackAngle) * 1.4;
                } else {
                    double ratio = (effectiveRadius - d) / effectiveRadius;
                    double weight = (d < 40.0) ? (0.85 + ratio * 1.10) : (ratio * 0.78);
                    double nx = dx / d;
                    double ny = dy / d;
                    double side = ((System.identityHashCode(self) ^ System.identityHashCode(other)) & 1) == 0 ? 0.22 : -0.22;
                    sepX += (nx - ny * side) * weight;
                    sepY += (ny + nx * side) * weight;
                }
            }
        }
        return new double[]{sepX, sepY};
    }

    /** Desfaz empilhamentos residuais mesmo quando dois mobs param perto um do outro em vigília. */
    public static double[] computePausedDispersionVelocity(Enemy self, List<Enemy> allEnemies) {
        double pushX = 0.0;
        double pushY = 0.0;
        double relaxRadius = 62.0;

        for (Enemy other : allEnemies) {
            if (other == self || other.getHp() <= 0) continue;
            double dx = self.getX() - other.getX();
            double dy = self.getY() - other.getY();
            double d = Math.hypot(dx, dy);
            if (d < relaxRadius) {
                if (d < 0.001) {
                    double fallbackAngle = (System.identityHashCode(self) & 255) * (Math.PI * 2.0 / 256.0);
                    pushX += Math.cos(fallbackAngle) * 32.0;
                    pushY += Math.sin(fallbackAngle) * 32.0;
                } else {
                    double strength = ((relaxRadius - d) / relaxRadius) * 42.0;
                    pushX += (dx / d) * strength;
                    pushY += (dy / d) * strength;
                }
            }
        }
        return new double[]{pushX, pushY};
    }

    /** Escolhe um destino de patrulha distante de concentrações de outros inimigos e fiel ao habitat. */
    public static int[] pickDispersedPatrolTile(Enemy self, List<Enemy> allEnemies, TileMap tileMap,
                                                int curCol, int curRow, double patrolHeadingAngle,
                                                SpeciesBehaviorProfile profile, Random rand) {
        boolean isCroc = (self.getSkin() == EnemySkin.CROCODILE);
        boolean coldHardy = self.getVariant() == AnimalVariant.ARCTIC || self.getSkin() == EnemySkin.POLAR_BEAR;
        int bestC = Integer.MIN_VALUE;
        int bestR = Integer.MIN_VALUE;
        double bestScore = -1e9;

        for (int tries = 0; tries < 24; tries++) {
            double angleDelta = (tries < 14) ? (rand.nextDouble() - 0.5) * (Math.PI * 0.90)
                                             : (rand.nextDouble() * Math.PI * 2.0);
            double candidateAngle = patrolHeadingAngle + angleDelta;
            double minT = profile.getPatrolMinTiles();
            double maxT = profile.getPatrolMaxTiles();
            double distTiles = minT + rand.nextDouble() * (maxT - minT);

            int targetC = curCol + (int) Math.round(Math.cos(candidateAngle) * distTiles);
            int targetR = curRow + (int) Math.round(Math.sin(candidateAngle) * distTiles);

            TileType t = tileMap.getTileAt(targetC, targetR);
            if (!t.isWalkable()) continue;

            double score = scoreTerrainAffinity(self, t, isCroc, coldHardy);

            // Penalização forte para evitar que vários animais escolham o mesmo quadrante
            double wx = targetC * TileMap.TILE_SIZE + 16.0;
            double wy = targetR * TileMap.TILE_SIZE + 16.0;
            for (Enemy other : allEnemies) {
                if (other == self || other.getHp() <= 0) continue;
                double d = Math.hypot(other.getX() - wx, other.getY() - wy);
                if (d < 150.0) {
                    score -= 4.5;
                } else if (d < 250.0) {
                    score -= 2.0;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestC = targetC;
                bestR = targetR;
            }
        }
        return bestC != Integer.MIN_VALUE ? new int[]{bestC, bestR} : null;
    }

    private static double scoreTerrainAffinity(Enemy self, TileType t, boolean isCroc, boolean coldHardy) {
        if (isCroc) {
            if (t == TileType.WATER) return 5.0;
            if (t == TileType.MUD) return 3.0;
            return -3.5;
        }
        if (t == TileType.WATER) return coldHardy ? -1.5 : -5.5;
        if (self.getType() == EnemyType.HUNTER && t == TileType.DIRT_PATH) return 4.0;
        if (self.getSkin() == EnemySkin.BOAR && (t == TileType.MUD || t == TileType.TALL_GRASS)) return 3.2;
        if (self.getSkin() == EnemySkin.WOLF && (t == TileType.GRASS || t == TileType.DIRT_PATH)) return 2.8;
        return (t == TileType.GRASS || t == TileType.DIRT_PATH || t == TileType.TALL_GRASS) ? 2.2 : 0.6;
    }

    /** Retorna o arbusto comestível mais próximo dentro do raio se ainda não houver outro animal nele. */
    public static FoodItem findUncrowdedFoodForForager(Enemy self, List<FoodItem> foodList,
                                                       List<Enemy> allEnemies, double maxRadiusPx) {
        FoodItem best = null;
        double minDist = maxRadiusPx;

        for (FoodItem item : foodList) {
            if (item.isEaten()) continue;
            double dist = Math.hypot(item.getX() - self.getX(), item.getY() - self.getY());
            if (dist >= minDist) continue;

            boolean occupiedByOther = false;
            for (Enemy other : allEnemies) {
                if (other == self || other.getHp() <= 0) continue;
                double otherDist = Math.hypot(item.getX() - other.getX(), item.getY() - other.getY());
                if (otherDist < Math.max(96.0, dist * 0.90)) {
                    occupiedByOther = true;
                    break;
                }
            }
            if (!occupiedByOther) {
                minDist = dist;
                best = item;
            }
        }
        return best;
    }
}
