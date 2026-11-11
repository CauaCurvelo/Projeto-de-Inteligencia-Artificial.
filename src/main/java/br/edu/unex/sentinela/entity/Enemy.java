package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.ai.AIState;
import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.ai.Node;
import br.edu.unex.sentinela.ai.Pathfinder;
import br.edu.unex.sentinela.world.*;

import java.util.List;
import java.util.Random;

/**
 * Representa os predadores, caçadores e chefes da floresta com:
 * - Steering Behavior fluido (Inércia de velocidade, transições de waypoints sem parada e separação de bando)
 * - Perseguição Vetorial Direta (Direct Line-of-Sight Pursuit) quando não há obstáculos entre o inimigo e o alvo
 * - Patrulha Orgânica por Habitat com pausas naturais de observação e farejamento
 * - Sistema anti-stuck e deslizamento suave em bordas de árvores/rochas
 */
public class Enemy {
    private final EnemyType type;
    private final EnemySkin skin;
    private double x;
    private double y;
    private double vx = 0.0;
    private double vy = 0.0;
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
    private double fovAngle = Math.toRadians(115.0);
    private double faceAngle;

    // Shooting mechanics
    private double shootCooldown = 0.0;

    // Progressive suspicion & Natural patrol timers
    private double suspicionMeter = 0.0;
    private double alertTimer = 0.0;
    private double repathCooldown = 0.0;
    private double patrolPauseTimer = 0.0;
    private double patrolHeadingAngle;
    private double stuckTimer = 0.0;
    private double lastCheckX = 0.0;
    private double lastCheckY = 0.0;
    private static final Random rand = new Random();

    // Animation state (Breathing, Walk Cycle, Footprints)
    private double walkPhase = rand.nextDouble() * 10.0;
    private double breathTimer = rand.nextDouble() * 6.0;
    private boolean moving = false;
    private double footprintTimer = 0.0;
    private boolean nextFootLeft = true;

    public Enemy(EnemyType type, EnemySkin skin, double startX, double startY, double speedMult, double detectionMult) {
        this.type = type;
        this.skin = skin;
        this.x = startX;
        this.y = startY;
        this.lastCheckX = startX;
        this.lastCheckY = startY;
        this.maxHp = type.getMaxHp();
        this.hp = maxHp;
        this.faceAngle = rand.nextDouble() * Math.PI * 2.0 - Math.PI;
        this.patrolHeadingAngle = this.faceAngle;

        switch (type) {
            case HUNTER:
                this.baseSpeed = 118.0 * speedMult;
                this.baseVisionRange = 300.0 * detectionMult;
                this.baseHearingSensitivity = 0.55 * detectionMult;
                break;
            case HOUND:
                this.baseSpeed = 182.0 * speedMult;
                this.baseVisionRange = 175.0 * detectionMult;
                this.baseHearingSensitivity = 1.9 * detectionMult;
                break;
            case RIVAL:
                this.baseSpeed = 116.0 * speedMult;
                this.baseVisionRange = 210.0 * detectionMult;
                this.baseHearingSensitivity = 1.0 * detectionMult;
                this.state = AIState.SEARCHING_FOOD;
                break;
            case BEAR_BOSS:
                this.baseSpeed = 132.0 * speedMult;
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
        breathTimer += deltaTime;
        repathCooldown -= deltaTime;
        if (alertTimer > 0) alertTimer -= deltaTime;
        if (shootCooldown > 0) shootCooldown -= deltaTime;
        if (damageFlashTimer > 0) damageFlashTimer -= deltaTime;
        if (attackCooldown > 0) attackCooldown -= deltaTime;
        if (patrolPauseTimer > 0) patrolPauseTimer -= deltaTime;

        if (attackWindupTimer > 0) {
            attackWindupTimer -= deltaTime;
            if (attackWindupTimer <= 0) {
                executeLungeStrike();
            }
        }
        if (attackLungeTimer > 0) attackLungeTimer -= deltaTime;

        // Rescue fallback caso o inimigo acabe em cima de um tile bloqueado
        int curCol = (int) Math.floor(x / TileMap.TILE_SIZE);
        int curRow = (int) Math.floor(y / TileMap.TILE_SIZE);
        if (!tileMap.getTileAt(curCol, curRow).isWalkable()) {
            int[] safe = Pathfinder.findNearestWalkableTile(tileMap, curCol, curRow, 3);
            if (safe != null) {
                x = safe[0] * TileMap.TILE_SIZE + 16.0;
                y = safe[1] * TileMap.TILE_SIZE + 16.0;
                curCol = safe[0];
                curRow = safe[1];
            }
        }

        // Apply physical knockback decay with wall sliding
        if (Math.abs(knockbackVx) > 0.1 || Math.abs(knockbackVy) > 0.1) {
            moveWithCollisionSlide(knockbackVx * deltaTime, knockbackVy * deltaTime, tileMap);
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

        TileType currentTile = tileMap.getTileAt(curCol, curRow);

        if (skin == EnemySkin.CROCODILE) {
            if (currentTile == TileType.WATER) {
                currentActualSpeed = baseSpeed * 1.50;
            } else if (currentTile == TileType.MUD) {
                currentActualSpeed = baseSpeed * 0.90;
            } else {
                currentActualSpeed = baseSpeed * 0.60;
            }
        } else if (currentTile == TileType.MUD) {
            currentActualSpeed *= 0.55;
        } else if (currentTile == TileType.WATER) {
            currentActualSpeed *= 0.45;
        } else if (currentTile == TileType.DIRT_PATH) {
            currentActualSpeed *= 1.12;
        }

        // Apex predator behavior
        if (type == EnemyType.BEAR_BOSS) {
            updateApexBoss(player, allEnemies, tileMap, curCol, curRow, deltaTime, currentActualSpeed, scentTrail, currentTile);
            return;
        }

        double distToPlayer = Math.hypot(player.getX() - x, player.getY() - y);
        boolean canSeePlayer = !player.isBurrowed() && canSensePlayerBySight(player, tileMap, distToPlayer);
        boolean canHearPlayer = !player.isBurrowed() && canSensePlayerBySound(player, distToPlayer);

        if (canSeePlayer) {
            suspicionMeter = Math.min(1.0, suspicionMeter + deltaTime * 3.5);
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

        // Executa o movimento fluido: Perseguição Direta se corredor livre ou Seguimento de Rota Suavizada
        executeFluidMovement(deltaTime, currentActualSpeed, player, allEnemies, tileMap, scentTrail, currentTile);
    }

    private void updateApexBoss(Player player, List<Enemy> allEnemies, TileMap tileMap, int curCol, int curRow,
                                double deltaTime, double currentActualSpeed, List<Footprint> scentTrail, TileType currentTile) {
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
            patrolPauseTimer = 0;
            requestPath(tileMap, curCol, curRow, (int) Math.floor(player.getX() / TileMap.TILE_SIZE), (int) Math.floor(player.getY() / TileMap.TILE_SIZE), false);
        } else if (nearestTargetEnemy != null) {
            state = AIState.FIGHTING_ENEMY;
            patrolPauseTimer = 0;
            requestPath(tileMap, curCol, curRow, (int) Math.floor(nearestTargetEnemy.getX() / TileMap.TILE_SIZE), (int) Math.floor(nearestTargetEnemy.getY() / TileMap.TILE_SIZE), false);

            if (minEnemyDist < 32.0 && attackCooldown <= 0) {
                nearestTargetEnemy.takeDamage(2, x, y);
                attackCooldown = 0.9;
            }
        } else {
            state = AIState.PATROL;
            if (isPathFinished()) {
                handleOrganicPatrolArrival(tileMap, curCol, curRow);
            }
        }

        executeFluidMovement(deltaTime, currentActualSpeed, player, allEnemies, tileMap, scentTrail, currentTile);
    }

    private void updateHunter(boolean canSee, boolean canHear, Player player, TileMap tileMap,
                              List<Projectile> projectiles, int curCol, int curRow, double distToPlayer) {
        if (canSee) {
            patrolPauseTimer = 0;
            int pCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
            int pRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);

            if (suspicionMeter < 0.30) {
                state = AIState.SUSPICIOUS;
                double targetAngle = Math.atan2(player.getY() - y, player.getX() - x);
                faceAngle = lerpAngle(faceAngle, targetAngle, 0.18);
            } else {
                alertTimer = 5.0;
                targetAimX = player.getX();
                targetAimY = player.getY();
                if (distToPlayer < 245.0) {
                    state = AIState.AIMING;
                    double aimAngle = Math.atan2(player.getY() - y, player.getX() - x);
                    faceAngle = lerpAngle(faceAngle, aimAngle, 0.25);
                    if (shootCooldown <= 0) {
                        projectiles.add(new Projectile(x, y, player.getX(), player.getY()));
                        shootCooldown = 1.75;
                    }
                } else {
                    state = AIState.CHASE;
                    requestPath(tileMap, curCol, curRow, pCol, pRow, false);
                }
            }
        } else if (alertTimer > 0) {
            state = AIState.ALERT;
            if (isPathFinished()) {
                handleOrganicPatrolArrival(tileMap, curCol, curRow);
            }
        } else if (canHear) {
            state = AIState.SUSPICIOUS;
            alertTimer = 3.5;
            patrolPauseTimer = 0;
            requestPath(tileMap, curCol, curRow, (int) Math.floor(player.getX() / TileMap.TILE_SIZE), (int) Math.floor(player.getY() / TileMap.TILE_SIZE), false);
        } else {
            state = AIState.PATROL;
            if (isPathFinished()) {
                handleOrganicPatrolArrival(tileMap, curCol, curRow);
            }
        }
    }

    private void updateHound(boolean canSee, boolean canHear, Player player,
                             List<Footprint> scentTrail, TileMap tileMap, int curCol, int curRow) {
        if (canHear || canSee) {
            state = AIState.CHASE;
            alertTimer = 4.5;
            patrolPauseTimer = 0;
            requestPath(tileMap, curCol, curRow, (int) Math.floor(player.getX() / TileMap.TILE_SIZE), (int) Math.floor(player.getY() / TileMap.TILE_SIZE), false);
            return;
        }

        if (alertTimer > 0) {
            state = AIState.ALERT;
            if (isPathFinished()) {
                handleOrganicPatrolArrival(tileMap, curCol, curRow);
            }
            return;
        }

        Footprint nearestScent = findNearestScent(scentTrail, 260.0);
        if (nearestScent != null) {
            state = AIState.SNIFFING_TRACK;
            patrolPauseTimer = 0;
            int scentCol = (int) Math.floor(nearestScent.getX() / TileMap.TILE_SIZE);
            int scentRow = (int) Math.floor(nearestScent.getY() / TileMap.TILE_SIZE);
            requestPath(tileMap, curCol, curRow, scentCol, scentRow, false);
            return;
        }

        state = AIState.PATROL;
        if (isPathFinished()) {
            handleOrganicPatrolArrival(tileMap, curCol, curRow);
        }
    }

    private Footprint findNearestScent(List<Footprint> scentTrail, double maxDist) {
        Footprint closest = null;
        double minDist = maxDist;
        for (Footprint fp : scentTrail) {
            if (!fp.isExpired() && fp.isPlayerOrigin()) {
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
        if (distToPlayer < 160.0 && canSee) {
            state = AIState.TERRITORIAL_ATTACK;
            patrolPauseTimer = 0;
            requestPath(tileMap, curCol, curRow, (int) Math.floor(player.getX() / TileMap.TILE_SIZE), (int) Math.floor(player.getY() / TileMap.TILE_SIZE), false);
            return;
        }

        FoodItem nearestFood = findNearestUnconsumedFood(foodList);
        if (nearestFood != null && Math.hypot(nearestFood.getX() - x, nearestFood.getY() - y) < 380.0) {
            double distFoodToPlayer = Math.hypot(player.getX() - nearestFood.getX(), player.getY() - nearestFood.getY());

            if (distFoodToPlayer < 160.0 && distToPlayer < 210.0 && canSee) {
                state = AIState.TERRITORIAL_ATTACK;
                patrolPauseTimer = 0;
                requestPath(tileMap, curCol, curRow, (int) Math.floor(player.getX() / TileMap.TILE_SIZE), (int) Math.floor(player.getY() / TileMap.TILE_SIZE), false);
                return;
            }

            state = AIState.SEARCHING_FOOD;
            double distToFood = Math.hypot(nearestFood.getX() - x, nearestFood.getY() - y);
            if (distToFood < 20.0) {
                nearestFood.setEaten(true);
                state = AIState.EATING;
                patrolPauseTimer = 1.2; // Pausa saboreando a fruta
            } else {
                // CORREÇÃO CRÍTICA: force=false para não zerar o pathIndex em todo frame!
                requestPath(tileMap, curCol, curRow, nearestFood.getCol(), nearestFood.getRow(), false);
            }
        } else {
            state = AIState.PATROL;
            if (isPathFinished()) {
                handleOrganicPatrolArrival(tileMap, curCol, curRow);
            }
        }
    }

    /**
     * Quando o inimigo conclui uma rota de patrulha, faz uma breve pausa natural (0.6s a 1.8s)
     * olhando ao redor antes de escolher o próximo destino orgânico.
     */
    private void handleOrganicPatrolArrival(TileMap tileMap, int curCol, int curRow) {
        if (patrolPauseTimer <= 0) {
            if (moving) {
                // Acabou de chegar ao fim da rota -> inicia breve pausa natural
                moving = false;
                vx = 0;
                vy = 0;
                patrolPauseTimer = 0.5 + rand.nextDouble() * 1.1;
            } else {
                pickOrganicPatrolPoint(tileMap, curCol, curRow);
            }
        } else {
            // Durante a pausa de vigia, olha suavemente para os lados
            faceAngle += Math.sin(breathTimer * 2.0) * 0.008;
        }
    }

    /**
     * Escolhe rotas de patrulha orgânicas baseadas em continuidade direcional (Wander Steering)
     * e afinidade de bioma, evitando zigue-zagues aleatórios curtos.
     */
    private void pickOrganicPatrolPoint(TileMap tileMap, int curCol, int curRow) {
        boolean isCroc = (skin == EnemySkin.CROCODILE);
        int bestC = Integer.MIN_VALUE;
        int bestR = Integer.MIN_VALUE;
        int bestScore = -999;

        for (int tries = 0; tries < 22; tries++) {
            // Prefere seguir em um arco suave à frente da direção atual (60% das tentativas)
            double angleDelta = (tries < 14) ? (rand.nextDouble() - 0.5) * (Math.PI * 0.85)
                                             : (rand.nextDouble() * Math.PI * 2.0);
            double candidateAngle = patrolHeadingAngle + angleDelta;
            double distTiles = 6.0 + rand.nextDouble() * 8.0; // Caminhadas de 6 a 14 blocos (rotas amplas)

            int targetC = curCol + (int) Math.round(Math.cos(candidateAngle) * distTiles);
            int targetR = curRow + (int) Math.round(Math.sin(candidateAngle) * distTiles);

            TileType t = tileMap.getTileAt(targetC, targetR);
            if (!t.isWalkable()) continue;

            int score = 0;
            if (isCroc) {
                if (t == TileType.WATER) score += 5;
                else if (t == TileType.MUD) score += 3;
                else score -= 3;
            } else {
                if (t == TileType.WATER) score -= 5;
                else if (type == EnemyType.HUNTER && t == TileType.DIRT_PATH) score += 4;
                else if (skin == EnemySkin.BOAR && (t == TileType.MUD || t == TileType.TALL_GRASS)) score += 3;
                else if (t == TileType.GRASS || t == TileType.DIRT_PATH) score += 2;
            }

            if (score > bestScore) {
                bestScore = score;
                bestC = targetC;
                bestR = targetR;
                if (score >= 4) break;
            }
        }

        if (bestC != Integer.MIN_VALUE) {
            patrolHeadingAngle = Math.atan2(bestR - curRow, bestC - curCol);
            requestPath(tileMap, curCol, curRow, bestC, bestR, true);
        }
    }

    private boolean isPathFinished() {
        return currentPath == null || currentPathIndex >= currentPath.size();
    }

    private void requestPath(TileMap tileMap, int startCol, int startRow, int destCol, int destRow, boolean force) {
        if (!force) {
            if (repathCooldown > 0) return;
            if (targetCol == destCol && targetRow == destRow && !isPathFinished()) return;
        }

        this.targetCol = destCol;
        this.targetRow = destRow;
        Pathfinder pf = new Pathfinder();
        boolean isCrocodile = (skin == EnemySkin.CROCODILE);
        List<Node> newPath = pf.findPath(tileMap, startCol, startRow, destCol, destRow, isCrocodile);

        this.currentPath = newPath;
        this.currentPathIndex = 0;

        // Se o primeiro nó da nova rota estiver muito próximo ou atrás do movimento atual, avança para o segundo nó
        // Isso evita que o inimigo dê um "passo para trás" quando a rota é recalculada durante uma perseguição!
        if (currentPath != null && currentPath.size() > 1) {
            Node first = currentPath.get(0);
            double fX = first.col * TileMap.TILE_SIZE + 16.0;
            double fY = first.row * TileMap.TILE_SIZE + 16.0;
            if (Math.hypot(fX - x, fY - y) < 20.0) {
                currentPathIndex = 1;
            }
        }

        this.repathCooldown = (state == AIState.CHASE || state == AIState.TERRITORIAL_ATTACK) ? 0.28 : 0.55;
    }

    /**
     * Motor de Movimento Fluido (Steering Behavior + Direct Pursuit + Anti-Stuck):
     * - Em perseguição com linha de visão desimpedida, move-se diretamente em direção ao jogador sem zigue-zague de grade.
     * - Em seguimento de rota, faz transição contínua entre waypoints com raio de lookahead e inércia de velocidade.
     * - Aplica força suave de separação entre inimigos próximos para evitar sobreposição.
     */
    private void executeFluidMovement(double deltaTime, double maxSpeed, Player player, List<Enemy> allEnemies,
                                      TileMap tileMap, List<Footprint> scentTrail, TileType currentTile) {
        if (state == AIState.AIMING || patrolPauseTimer > 0 || attackWindupTimer > 0) {
            this.moving = false;
            this.vx *= 0.5;
            this.vy *= 0.5;
            return;
        }

        double desiredDirX = 0.0;
        double desiredDirY = 0.0;
        boolean hasTarget = false;

        boolean isChasingPlayer = (state == AIState.CHASE || state == AIState.TERRITORIAL_ATTACK) && !player.isBurrowed();
        boolean isCroc = (skin == EnemySkin.CROCODILE);

        // 1. Perseguição Vetorial Direta (Direct Pursuit) quando há corredor livre até a Raposa
        if (isChasingPlayer) {
            double distToP = Math.hypot(player.getX() - x, player.getY() - y);
            if (distToP > 14.0 && distToP < 260.0 &&
                Pathfinder.hasClearWorldCorridor(tileMap, x, y, player.getX(), player.getY(), isCroc)) {
                desiredDirX = (player.getX() - x) / distToP;
                desiredDirY = (player.getY() - y) / distToP;
                hasTarget = true;
            }
        }

        // 2. Caso contrário, segue os Waypoints suavizados do Pathfinder
        if (!hasTarget && currentPath != null && currentPathIndex < currentPath.size()) {
            // Avança waypoints intermediários antes de parar no centro (Raio de Lookahead = 12px)
            while (currentPathIndex < currentPath.size()) {
                Node wp = currentPath.get(currentPathIndex);
                double wpX = wp.col * TileMap.TILE_SIZE + 16.0;
                double wpY = wp.row * TileMap.TILE_SIZE + 16.0;
                double distToWp = Math.hypot(wpX - x, wpY - y);

                boolean isLastNode = (currentPathIndex == currentPath.size() - 1);
                double threshold = isLastNode ? 4.0 : 12.0;

                if (distToWp <= threshold) {
                    currentPathIndex++;
                } else {
                    desiredDirX = (wpX - x) / distToWp;
                    desiredDirY = (wpY - y) / distToWp;
                    hasTarget = true;
                    break;
                }
            }
        }

        if (!hasTarget) {
            this.moving = false;
            this.vx = 0;
            this.vy = 0;
            return;
        }

        // 3. Força Suave de Separação entre Inimigos (Flocking Separation para não empilharem no mesmo ponto)
        double sepX = 0.0;
        double sepY = 0.0;
        for (Enemy other : allEnemies) {
            if (other != this && other.getHp() > 0) {
                double dx = x - other.getX();
                double dy = y - other.getY();
                double d = Math.hypot(dx, dy);
                if (d > 0.001 && d < 28.0) {
                    double push = (28.0 - d) / 28.0;
                    sepX += (dx / d) * push * 0.45;
                    sepY += (dy / d) * push * 0.45;
                }
            }
        }

        double combinedX = desiredDirX + sepX;
        double combinedY = desiredDirY + sepY;
        double combinedLen = Math.hypot(combinedX, combinedY);
        if (combinedLen > 0.001) {
            combinedX /= combinedLen;
            combinedY /= combinedLen;
        }

        // 4. Interpolação Suave de Velocidade (Steering Inertia) e Ângulo Visual
        double steerFactor = Math.min(1.0, deltaTime * 12.0);
        double targetVx = combinedX * maxSpeed;
        double targetVy = combinedY * maxSpeed;
        this.vx = this.vx + (targetVx - this.vx) * steerFactor;
        this.vy = this.vy + (targetVy - this.vy) * steerFactor;

        double actualSpeed = Math.hypot(vx, vy);
        if (actualSpeed > 5.0) {
            this.moving = true;
            this.walkPhase += actualSpeed * deltaTime * 0.085;
            double moveAngle = Math.atan2(vy, vx);
            this.faceAngle = lerpAngle(this.faceAngle, moveAngle, Math.min(1.0, deltaTime * 11.0));

            moveWithCollisionSlide(vx * deltaTime, vy * deltaTime, tileMap);

            // Detector Anti-Stuck: Se tentou andar por 0.6s mas não saiu do lugar, recalcula nova rota
            stuckTimer += deltaTime;
            if (stuckTimer >= 0.65) {
                double movedDist = Math.hypot(x - lastCheckX, y - lastCheckY);
                lastCheckX = x;
                lastCheckY = y;
                stuckTimer = 0.0;
                if (movedDist < 6.0) {
                    int c = (int) Math.floor(x / TileMap.TILE_SIZE);
                    int r = (int) Math.floor(y / TileMap.TILE_SIZE);
                    patrolHeadingAngle += Math.PI * 0.65;
                    pickOrganicPatrolPoint(tileMap, c, r);
                }
            }

            // Emissão de pegadas direcionadas
            footprintTimer += deltaTime;
            if (footprintTimer >= 0.34 && currentTile != TileType.WATER) {
                footprintTimer = 0.0;
                Footprint.PrintStyle style = Footprint.PrintStyle.PAW;
                if (type == EnemyType.HUNTER) style = Footprint.PrintStyle.BOOT;
                else if (skin == EnemySkin.BOAR) style = Footprint.PrintStyle.HOOF;

                double perp = faceAngle + (nextFootLeft ? -Math.PI * 0.5 : Math.PI * 0.5);
                double fpX = x - Math.cos(faceAngle) * 4 + Math.cos(perp) * 4.0;
                double fpY = y - Math.sin(faceAngle) * 4 + Math.sin(perp) * 4.0;
                scentTrail.add(new Footprint(fpX, fpY, faceAngle, nextFootLeft, style, false, 4.2));
                nextFootLeft = !nextFootLeft;
            }
        } else {
            this.moving = false;
        }
    }

    /**
     * Move a entidade verificando colisão independente em X e Y (Wall Sliding),
     * permitindo deslizar suavemente na quina de árvores ou rochas sem travar.
     */
    private void moveWithCollisionSlide(double stepX, double stepY, TileMap tileMap) {
        double radius = 9.5;
        double nextX = x + stepX;
        double nextY = y + stepY;

        if (isBoxWalkable(nextX, y, radius, tileMap)) {
            x = nextX;
        }
        if (isBoxWalkable(x, nextY, radius, tileMap)) {
            y = nextY;
        }
    }

    private boolean isBoxWalkable(double cx, double cy, double r, TileMap tileMap) {
        return tileMap.isWalkable(cx - r, cy - r) &&
               tileMap.isWalkable(cx + r, cy - r) &&
               tileMap.isWalkable(cx - r, cy + r) &&
               tileMap.isWalkable(cx + r, cy + r);
    }

    public boolean takeDamage(int amount, double fromX, double fromY) {
        this.hp -= amount;
        this.damageFlashTimer = 0.22;

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

        switch (type) {
            case BEAR_BOSS:
                this.attackWindupTimer = (skin == EnemySkin.PANTHER) ? 0.35 : (skin == EnemySkin.CROCODILE ? 0.40 : 0.55);
                break;
            case HOUND:
                this.attackWindupTimer = 0.28;
                break;
            case RIVAL:
                this.attackWindupTimer = (skin == EnemySkin.BOAR) ? 0.45 : 0.35;
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

    public boolean isWindup() { return attackWindupTimer > 0; }
    public boolean isLunging() { return attackLungeTimer > 0; }
    public double getTargetAimX() { return targetAimX; }
    public double getTargetAimY() { return targetAimY; }
    public double getAttackCooldown() { return attackCooldown; }

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

    private double lerpAngle(double current, double target, double factor) {
        double diff = target - current;
        while (diff < -Math.PI) diff += Math.PI * 2;
        while (diff > Math.PI) diff -= Math.PI * 2;
        return current + diff * factor;
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
    public void setFaceAngle(double faceAngle) { this.faceAngle = faceAngle; }
    public double getWalkPhase() { return walkPhase; }
    public void setWalkPhase(double walkPhase) { this.walkPhase = walkPhase; }
    public double getBreathTimer() { return breathTimer; }
    public void setBreathTimer(double breathTimer) { this.breathTimer = breathTimer; }
    public boolean isMoving() { return moving; }
    public void setMoving(boolean moving) { this.moving = moving; }
    public double getVisionRange() { return currentVisionRange; }
    public double getFovAngle() { return fovAngle; }
    public double getSuspicionMeter() { return suspicionMeter; }
    public List<Node> getCurrentPath() { return currentPath; }
    public int getCurrentPathIndex() { return currentPathIndex; }
}
