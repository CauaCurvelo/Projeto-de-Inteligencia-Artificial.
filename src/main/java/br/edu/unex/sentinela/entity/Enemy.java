package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.ai.AIState;
import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.ai.Node;
import br.edu.unex.sentinela.ai.Pathfinder;
import br.edu.unex.sentinela.world.*;

import java.util.List;
import java.util.Random;

public class Enemy {
    private final EnemyType type;
    private final EnemySkin skin;
    private double x;
    private double y;
    private double baseSpeed;
    private double speed;

    // Zelda HP & Combat
    private int hp;
    private final int maxHp;
    private double damageFlashTimer = 0.0;
    private double attackCooldown = 0.0;
    private double attackWindupTimer = 0.0;
    private double attackLungeTimer = 0.0;
    private double targetAimX = 0.0;
    private double targetAimY = 0.0;
    private double knockbackVx = 0.0;
    private double knockbackVy = 0.0;

    private AIState state = AIState.PATROL;
    private List<Node> currentPath;
    private int currentPathIndex = 0;
    private int targetCol = -1;
    private int targetRow = -1;

    // Senses configuration
    private double baseVisionRange;
    private double baseHearingSensitivity;
    private double currentVisionRange;
    private double currentHearingSensitivity;
    private double fovAngle = Math.toRadians(110.0);
    private double faceAngle = 0.0;

    // Shooting mechanics
    private double shootCooldown = 0.0;

    // Progressive suspicion system
    private double suspicionMeter = 0.0;
    private double alertTimer = 0.0;
    private double repathCooldown = 0.0;
    private static final Random rand = new Random();

    public Enemy(EnemyType type, EnemySkin skin, double startX, double startY, double speedMult, double detectionMult) {
        this.type = type;
        this.skin = skin;
        this.x = startX;
        this.y = startY;
        this.maxHp = type.getMaxHp();
        this.hp = maxHp;

        switch (type) {
            case HUNTER:
                this.baseSpeed = 115.0 * speedMult;
                this.baseVisionRange = 300.0 * detectionMult;
                this.baseHearingSensitivity = 0.5 * detectionMult;
                break;
            case HOUND:
                this.baseSpeed = 185.0 * speedMult;
                this.baseVisionRange = 160.0 * detectionMult;
                this.baseHearingSensitivity = 1.9 * detectionMult;
                break;
            case RIVAL:
                this.baseSpeed = 110.0 * speedMult;
                this.baseVisionRange = 210.0 * detectionMult;
                this.baseHearingSensitivity = 1.0 * detectionMult;
                this.state = AIState.SEARCHING_FOOD;
                break;
            case BEAR_BOSS:
                this.baseSpeed = 130.0 * speedMult;
                this.baseVisionRange = 340.0 * detectionMult;
                this.baseHearingSensitivity = 1.4 * detectionMult;
                this.state = AIState.PATROL;
                break;
        }
        this.speed = baseSpeed;
        this.currentVisionRange = baseVisionRange;
        this.currentHearingSensitivity = baseHearingSensitivity;
    }

    public void update(double deltaTime, Player player, List<FoodItem> foodList, 
                       List<Footprint> scentTrail, List<Projectile> projectiles, List<Enemy> allEnemies,
                       TileMap tileMap, double timeProgress, WeatherType weather) {
        repathCooldown -= deltaTime;
        if (alertTimer > 0) alertTimer -= deltaTime;
        if (shootCooldown > 0) shootCooldown -= deltaTime;
        if (damageFlashTimer > 0) damageFlashTimer -= deltaTime;
        if (attackCooldown > 0) attackCooldown -= deltaTime;
        if (attackWindupTimer > 0) {
            attackWindupTimer -= deltaTime;
            if (attackWindupTimer <= 0) {
                // Wind-up completed -> unleash violent lunge strike!
                executeLungeStrike();
            }
        }
        if (attackLungeTimer > 0) attackLungeTimer -= deltaTime;

        // Apply physical knockback decay
        if (Math.abs(knockbackVx) > 0.1 || Math.abs(knockbackVy) > 0.1) {
            double nextKx = x + knockbackVx * deltaTime;
            double nextKy = y + knockbackVy * deltaTime;
            if (tileMap.isWalkable(nextKx, y)) x = nextKx;
            if (tileMap.isWalkable(x, nextKy)) y = nextKy;
            knockbackVx *= Math.pow(0.05, deltaTime);
            knockbackVy *= Math.pow(0.05, deltaTime);
        } else {
            knockbackVx = 0;
            knockbackVy = 0;
        }

        this.currentVisionRange = baseVisionRange * weather.getVisionMultiplier();
        this.currentHearingSensitivity = baseHearingSensitivity * weather.getSoundMultiplier();

        double duskBoost = 1.0 + (timeProgress * 0.15);
        double currentActualSpeed = speed * duskBoost;

        int curCol = (int) Math.floor(x / TileMap.TILE_SIZE);
        int curRow = (int) Math.floor(y / TileMap.TILE_SIZE);
        TileType currentTile = tileMap.getTileAt(curCol, curRow);
        
        // Crocodile boss has specialized reversed aquatic speeds!
        if (skin == EnemySkin.CROCODILE) {
            if (currentTile == TileType.WATER) {
                currentActualSpeed = baseSpeed * 1.50; // Fast apex predator in water!
            } else if (currentTile == TileType.MUD) {
                currentActualSpeed = baseSpeed * 0.85;
            } else {
                currentActualSpeed = baseSpeed * 0.55; // Sluggish on dry land!
            }
        } else if (currentTile == TileType.MUD) {
            currentActualSpeed *= 0.50;
        } else if (currentTile == TileType.WATER) {
            currentActualSpeed *= 0.40;
        }

        // Apex predator behavior
        if (type == EnemyType.BEAR_BOSS) {
            updateApexBoss(player, allEnemies, tileMap, curCol, curRow, deltaTime);
            followPath(deltaTime, currentActualSpeed);
            return;
        }

        double distToPlayer = Math.hypot(player.getX() - x, player.getY() - y);
        boolean canSeePlayer = !player.isBurrowed() && canSensePlayerBySight(player, tileMap, distToPlayer);
        boolean canHearPlayer = !player.isBurrowed() && canSensePlayerBySound(player, distToPlayer);

        if (canSeePlayer) {
            suspicionMeter = Math.min(1.0, suspicionMeter + deltaTime * 3.5); // Fast alert!
        } else {
            suspicionMeter = Math.max(0.0, suspicionMeter - deltaTime * 0.8);
        }

        switch (type) {
            case HUNTER:
                updateHunter(canSeePlayer, canHearPlayer, player, tileMap, projectiles, curCol, curRow, distToPlayer);
                break;
            case HOUND:
                updateHound(canSeePlayer, canHearPlayer, player, scentTrail, tileMap, curCol, curRow);
                break;
            case RIVAL:
                updateRival(canSeePlayer, player, foodList, tileMap, curCol, curRow, distToPlayer);
                break;
        }

        // When in close-quarters chase, smoothly face player even if between path nodes
        if (state == AIState.CHASE || state == AIState.TERRITORIAL_ATTACK) {
            if (distToPlayer < 90.0) {
                faceAngle = Math.atan2(player.getY() - y, player.getX() - x);
            }
        }

        followPath(deltaTime, currentActualSpeed);
    }

    private void updateApexBoss(Player player, List<Enemy> allEnemies, TileMap tileMap, int curCol, int curRow, double deltaTime) {
        Enemy nearestTargetEnemy = null;
        double minEnemyDist = Double.MAX_VALUE;

        for (Enemy other : allEnemies) {
            if (other != this && other.getHp() > 0) {
                double d = Math.hypot(other.getX() - x, other.getY() - y);
                if (d < 220.0 && d < minEnemyDist) {
                    minEnemyDist = d;
                    nearestTargetEnemy = other;
                }
            }
        }

        double distToPlayer = Math.hypot(player.getX() - x, player.getY() - y);
        boolean targetIsPlayer = (distToPlayer < minEnemyDist && !player.isBurrowed() && distToPlayer < 260.0);

        if (targetIsPlayer) {
            state = AIState.CHASE;
            faceAngle = Math.atan2(player.getY() - y, player.getX() - x);
            requestPath(tileMap, curCol, curRow, (int)(player.getX() / TileMap.TILE_SIZE), (int)(player.getY() / TileMap.TILE_SIZE), false);
        } else if (nearestTargetEnemy != null) {
            state = AIState.FIGHTING_ENEMY;
            faceAngle = Math.atan2(nearestTargetEnemy.getY() - y, nearestTargetEnemy.getX() - x);
            requestPath(tileMap, curCol, curRow, (int)(nearestTargetEnemy.getX() / TileMap.TILE_SIZE), (int)(nearestTargetEnemy.getY() / TileMap.TILE_SIZE), false);

            if (minEnemyDist < 28.0 && attackCooldown <= 0) {
                nearestTargetEnemy.takeDamage(2);
                attackCooldown = 0.9;
            }
        } else {
            state = AIState.PATROL;
            if (currentPath == null || currentPathIndex >= currentPath.size()) {
                pickRandomPatrolPoint(tileMap, curCol, curRow);
            }
        }
    }

    private void updateHunter(boolean canSee, boolean canHear, Player player, TileMap tileMap, 
                             List<Projectile> projectiles, int curCol, int curRow, double distToPlayer) {
        if (canSee) {
            faceAngle = Math.atan2(player.getY() - y, player.getX() - x);

            if (suspicionMeter < 0.35) {
                state = AIState.SUSPICIOUS;
            } else {
                alertTimer = 5.0;
                targetAimX = player.getX();
                targetAimY = player.getY();
                if (distToPlayer < 280.0) {
                    state = AIState.AIMING;
                    if (shootCooldown <= 0) {
                        projectiles.add(new Projectile(x, y, player.getX(), player.getY()));
                        shootCooldown = 1.8;
                    }
                } else {
                    state = AIState.CHASE;
                }
                requestPath(tileMap, curCol, curRow, (int)(player.getX() / TileMap.TILE_SIZE), (int)(player.getY() / TileMap.TILE_SIZE), false);
            }
        } else if (alertTimer > 0) {
            state = AIState.ALERT;
        } else if (canHear) {
            state = AIState.SUSPICIOUS;
            alertTimer = 3.5;
            requestPath(tileMap, curCol, curRow, (int)(player.getX() / TileMap.TILE_SIZE), (int)(player.getY() / TileMap.TILE_SIZE), true);
        } else {
            state = AIState.PATROL;
            if (currentPath == null || currentPathIndex >= currentPath.size()) {
                pickRandomPatrolPoint(tileMap, curCol, curRow);
            }
        }
    }

    private void updateHound(boolean canSee, boolean canHear, Player player, 
                            List<Footprint> scentTrail, TileMap tileMap, int curCol, int curRow) {
        if (canHear || canSee) {
            state = AIState.CHASE;
            alertTimer = 4.0;
            requestPath(tileMap, curCol, curRow, (int)(player.getX() / TileMap.TILE_SIZE), (int)(player.getY() / TileMap.TILE_SIZE), false);
            return;
        } 
        
        if (alertTimer > 0) {
            state = AIState.ALERT;
            return;
        }

        Footprint nearestScent = findNearestScent(scentTrail, 260.0);
        if (nearestScent != null) {
            state = AIState.SNIFFING_TRACK;
            int scentCol = (int) Math.floor(nearestScent.getX() / TileMap.TILE_SIZE);
            int scentRow = (int) Math.floor(nearestScent.getY() / TileMap.TILE_SIZE);
            requestPath(tileMap, curCol, curRow, scentCol, scentRow, false);
            return;
        }

        state = AIState.PATROL;
        if (currentPath == null || currentPathIndex >= currentPath.size()) {
            pickRandomPatrolPoint(tileMap, curCol, curRow);
        }
    }

    private Footprint findNearestScent(List<Footprint> scentTrail, double maxDist) {
        Footprint closest = null;
        double minDist = maxDist;
        for (Footprint fp : scentTrail) {
            if (!fp.isExpired()) {
                double d = Math.hypot(fp.getX() - x, fp.getY() - y);
                if (d < minDist) {
                    minDist = d;
                    closest = fp;
                }
            }
        }
        return closest;
    }

    private void updateRival(boolean canSee, Player player, List<FoodItem> foodList, TileMap tileMap, int curCol, int curRow, double distToPlayer) {
        // Rivals aggressively engage player if close!
        if (distToPlayer < 150.0 && canSee) {
            state = AIState.TERRITORIAL_ATTACK;
            faceAngle = Math.atan2(player.getY() - y, player.getX() - x);
            requestPath(tileMap, curCol, curRow, (int)(player.getX() / TileMap.TILE_SIZE), (int)(player.getY() / TileMap.TILE_SIZE), false);
            return;
        }

        FoodItem nearestFood = findNearestUnconsumedFood(foodList);

        if (nearestFood != null) {
            double distFoodToPlayer = Math.hypot(player.getX() - nearestFood.getX(), player.getY() - nearestFood.getY());
            
            if (distFoodToPlayer < 160.0 && distToPlayer < 200.0 && canSee) {
                state = AIState.TERRITORIAL_ATTACK;
                requestPath(tileMap, curCol, curRow, (int)(player.getX() / TileMap.TILE_SIZE), (int)(player.getY() / TileMap.TILE_SIZE), false);
                return;
            }

            state = AIState.SEARCHING_FOOD;
            double distToFood = Math.hypot(nearestFood.getX() - x, nearestFood.getY() - y);
            if (distToFood < 18.0) {
                nearestFood.setEaten(true);
                state = AIState.EATING;
            } else {
                requestPath(tileMap, curCol, curRow, nearestFood.getCol(), nearestFood.getRow(), true);
            }
        } else {
            state = AIState.PATROL;
            if (currentPath == null || currentPathIndex >= currentPath.size()) {
                pickRandomPatrolPoint(tileMap, curCol, curRow);
            }
        }
    }

    public boolean takeDamage(int amount, double fromX, double fromY) {
        this.hp -= amount;
        this.damageFlashTimer = 0.22;
        
        // Pushback away from strike point
        double dx = x - fromX;
        double dy = y - fromY;
        double dist = Math.hypot(dx, dy);
        if (dist > 0.001) {
            double knockForce = (type == EnemyType.BEAR_BOSS) ? 140.0 : 260.0;
            this.knockbackVx = (dx / dist) * knockForce;
            this.knockbackVy = (dy / dist) * knockForce;
        }
        return hp <= 0;
    }

    public boolean takeDamage(int amount) {
        return takeDamage(amount, x, y);
    }

    public void startAttackWindup(double targetX, double targetY) {
        if (attackCooldown > 0 || attackWindupTimer > 0) return;

        this.targetAimX = targetX;
        this.targetAimY = targetY;
        this.faceAngle = Math.atan2(targetY - y, targetX - x);

        // Wind-up duration before releasing the strike
        switch (type) {
            case BEAR_BOSS:
                this.attackWindupTimer = (skin == EnemySkin.PANTHER) ? 0.35 : (skin == EnemySkin.CROCODILE ? 0.40 : 0.55);
                break;
            case HOUND:
                this.attackWindupTimer = 0.28;
                break;
            case RIVAL:
                this.attackWindupTimer = (skin == EnemySkin.BOAR) ? 0.45 : 0.35; // Boar charges up its tusks!
                break;
            case HUNTER:
            default:
                this.attackWindupTimer = 0.40;
                break;
        }
    }

    private void executeLungeStrike() {
        double dist = Math.hypot(targetAimX - x, targetAimY - y);
        double dirX = dist > 0.001 ? (targetAimX - x) / dist : Math.cos(faceAngle);
        double dirY = dist > 0.001 ? (targetAimY - y) / dist : Math.sin(faceAngle);

        switch (type) {
            case BEAR_BOSS:
                if (skin == EnemySkin.PANTHER) {
                    this.attackCooldown = 0.95;
                    this.attackLungeTimer = 0.25;
                    this.knockbackVx = dirX * 220.0;
                    this.knockbackVy = dirY * 220.0;
                } else if (skin == EnemySkin.CROCODILE) {
                    this.attackCooldown = 1.15;
                    this.attackLungeTimer = 0.32;
                    this.knockbackVx = dirX * 180.0;
                    this.knockbackVy = dirY * 180.0;
                } else {
                    this.attackCooldown = 1.45;
                    this.attackLungeTimer = 0.35;
                    this.knockbackVx = dirX * 90.0;
                    this.knockbackVy = dirY * 90.0;
                }
                break;

            case HOUND:
                this.attackCooldown = 0.85;
                this.attackLungeTimer = 0.22;
                this.knockbackVx = dirX * 190.0;
                this.knockbackVy = dirY * 190.0;
                break;

            case RIVAL:
                if (skin == EnemySkin.BOAR) {
                    // Boar violent forward charge
                    this.attackCooldown = 1.20;
                    this.attackLungeTimer = 0.38;
                    this.knockbackVx = dirX * 240.0;
                    this.knockbackVy = dirY * 240.0;
                } else {
                    this.attackCooldown = 0.95;
                    this.attackLungeTimer = 0.25;
                    this.knockbackVx = dirX * 150.0;
                    this.knockbackVy = dirY * 150.0;
                }
                break;

            case HUNTER:
            default:
                this.attackCooldown = 1.30;
                this.attackLungeTimer = 0.25;
                break;
        }
    }

    public boolean isWindup() {
        return attackWindupTimer > 0;
    }

    public boolean isLunging() {
        return attackLungeTimer > 0;
    }

    public double getTargetAimX() { return targetAimX; }
    public double getTargetAimY() { return targetAimY; }
    public double getAttackCooldown() {
        return attackCooldown;
    }

    private FoodItem findNearestUnconsumedFood(List<FoodItem> foodList) {
        FoodItem nearest = null;
        double minDist = Double.MAX_VALUE;
        for (FoodItem item : foodList) {
            if (!item.isEaten()) {
                double d = Math.hypot(item.getX() - x, item.getY() - y);
                if (d < minDist) {
                    minDist = d;
                    nearest = item;
                }
            }
        }
        return nearest;
    }

    private boolean canSensePlayerBySight(Player player, TileMap tileMap, double dist) {
        if (dist > currentVisionRange) return false;
        if (player.isHiddenInGrass() && type != EnemyType.HOUND) {
            if (dist > currentVisionRange * 0.25) return false;
        }

        double angleToPlayer = Math.atan2(player.getY() - y, player.getX() - x);
        double diff = Math.abs(normalizeAngle(angleToPlayer - faceAngle));
        if (diff > (fovAngle / 2.0)) return false;

        return tileMap.hasLineOfSight(x, y, player.getX(), player.getY());
    }

    private boolean canSensePlayerBySound(Player player, double dist) {
        double heardDist = player.getNoiseRadius() * currentHearingSensitivity;
        return dist <= heardDist && player.getNoiseRadius() > 0;
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private void pickRandomPatrolPoint(TileMap tileMap, int curCol, int curRow) {
        boolean isCrocodile = (skin == EnemySkin.CROCODILE);

        for (int tries = 0; tries < 15; tries++) {
            int dc = rand.nextInt(17) - 8;
            int dr = rand.nextInt(17) - 8;
            int targetC = curCol + dc;
            int targetR = curRow + dr;

            TileType t = tileMap.getTileAt(targetC, targetR);
            if (t.isWalkable()) {
                // Crocodile prioritizes patrolling in water or mud
                if (isCrocodile && tries < 10 && t != TileType.WATER && t != TileType.MUD) {
                    continue;
                }
                requestPath(tileMap, curCol, curRow, targetC, targetR, true);
                break;
            }
        }
    }

    private void requestPath(TileMap tileMap, int startCol, int startRow, int destCol, int destRow, boolean force) {
        if (!force && repathCooldown > 0 && targetCol == destCol && targetRow == destRow) {
            return;
        }
        this.targetCol = destCol;
        this.targetRow = destRow;
        Pathfinder pf = new Pathfinder();
        boolean isCrocodile = (skin == EnemySkin.CROCODILE);
        this.currentPath = pf.findPath(tileMap, startCol, startRow, destCol, destRow, isCrocodile);
        this.currentPathIndex = 0;
        this.repathCooldown = 0.4;
    }

    private void followPath(double deltaTime, double currentSpeed) {
        if (currentPath == null || currentPathIndex >= currentPath.size()) return;

        Node next = currentPath.get(currentPathIndex);
        double targetX = (next.col * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0);
        double targetY = (next.row * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0);

        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);

        if (distance > 0.001) {
            faceAngle = Math.atan2(dy, dx);
        }

        if (distance < 3.0) {
            x = targetX;
            y = targetY;
            currentPathIndex++;
        } else {
            double step = currentSpeed * deltaTime;
            if (step > distance) step = distance;
            x += (dx / distance) * step;
            y += (dy / distance) * step;
        }
    }

    public EnemyType getType() { return type; }
    public EnemySkin getSkin() { return skin; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public boolean isFlashing() { return damageFlashTimer > 0; }
    public AIState getState() { return state; }
    public double getFaceAngle() { return faceAngle; }
    public double getVisionRange() { return currentVisionRange; }
    public double getFovAngle() { return fovAngle; }
    public double getSuspicionMeter() { return suspicionMeter; }
    public List<Node> getCurrentPath() { return currentPath; }
}
