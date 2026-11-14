package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.ai.AIState;
import br.edu.unex.sentinela.ai.AnimalVariant;
import br.edu.unex.sentinela.ai.BossArchetype;
import br.edu.unex.sentinela.ai.BossAttackType;
import br.edu.unex.sentinela.ai.BossBehaviorController;
import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.ai.MobSteeringHelper;
import br.edu.unex.sentinela.ai.Node;
import br.edu.unex.sentinela.ai.Pathfinder;
import br.edu.unex.sentinela.ai.SpeciesBehaviorProfile;
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
    private final AnimalVariant variant;
    private final SpeciesBehaviorProfile behaviorProfile;
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
    private final Pathfinder pathfinder = new Pathfinder();

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
    private double forageCooldownTimer = 0.0;
    private double trapTimer = 0.0;
    private double trapDeployCooldown = 18.0 + rand.nextDouble() * 14.0;
    private double patrolHeadingAngle;
    private double stuckTimer = 0.0;
    private double lastCheckX = 0.0;
    private double lastCheckY = 0.0;
    private static final Random rand = new Random();

    // Novas rotinas biológicas e ecologia por espécie
    private double houndBarkCooldown = 0.0;
    private double thirstTimer = 18.0 + rand.nextDouble() * 26.0;
    private double drinkingTimer = 0.0;
    private double drinkingWaterX = 0.0;
    private double drinkingWaterY = 0.0;
    private double satiationTimer = 0.0;
    private double chargeCooldown = 0.0;
    private double chargeDuration = 0.0;
    private double chargeDirX = 0.0;
    private double chargeDirY = 0.0;
    private double kitingCooldown = 0.0;
    private double waterRippleTimer = 0.0;

    // Animation state (Breathing, Walk Cycle, Footprints)
    private double walkPhase = rand.nextDouble() * 10.0;
    private double breathTimer = rand.nextDouble() * 6.0;
    private boolean moving = false;
    private double footprintTimer = 0.0;
    private boolean nextFootLeft = true;
    private BossBehaviorController bossController;

    public Enemy(EnemyType type, EnemySkin skin, double startX, double startY, double speedMult, double detectionMult) {
        this(type, skin, AnimalVariant.STANDARD, startX, startY, speedMult, detectionMult);
    }

    public Enemy(EnemyType type, EnemySkin skin, AnimalVariant variant, double startX, double startY,
                 double speedMult, double detectionMult) {
        this.type = type;
        this.skin = skin;
        this.variant = (variant != null && type != EnemyType.BEAR_BOSS) ? variant : AnimalVariant.STANDARD;
        this.behaviorProfile = SpeciesBehaviorProfile.of(type, skin);
        this.x = startX;
        this.y = startY;
        this.lastCheckX = startX;
        this.lastCheckY = startY;
        this.maxHp = type.getMaxHp() + this.variant.getBonusHpFor(type, skin);
        this.hp = maxHp;
        this.faceAngle = rand.nextDouble() * Math.PI * 2.0 - Math.PI;
        this.patrolHeadingAngle = this.faceAngle;

        double variantSpeed = this.variant.getSpeedMultiplier();
        double variantVision = this.variant.getVisionMultiplier();

        switch (type) {
            case HUNTER:
                this.baseSpeed = 118.0 * speedMult * variantSpeed;
                this.baseVisionRange = 300.0 * detectionMult * variantVision;
                this.baseHearingSensitivity = 0.55 * detectionMult;
                break;
            case HOUND:
                this.baseSpeed = 182.0 * speedMult * variantSpeed;
                this.baseVisionRange = 175.0 * detectionMult * variantVision;
                this.baseHearingSensitivity = 1.9 * detectionMult;
                break;
            case RIVAL:
                this.baseSpeed = 116.0 * speedMult * variantSpeed;
                this.baseVisionRange = 210.0 * detectionMult * variantVision;
                this.baseHearingSensitivity = 1.0 * detectionMult;
                this.state = AIState.PATROL;
                break;
            case BEAR_BOSS:
                BossArchetype archetype = BossArchetype.fromSkin(skin);
                this.baseSpeed = archetype.getPatrolSpeed() * speedMult;
                this.baseVisionRange = 340.0 * detectionMult;
                this.baseHearingSensitivity = 1.4 * detectionMult;
                this.state = AIState.PATROL;
                this.bossController = new BossBehaviorController(archetype, startX, startY, speedMult);
                break;
        }
        this.speed = baseSpeed;
        this.currentVisionRange = baseVisionRange;
        this.currentHearingSensitivity = baseHearingSensitivity;
    }

    public void update(double deltaTime, Player player, List<FoodItem> foodList,
                       List<Footprint> scentTrail, List<Projectile> projectiles, List<Enemy> allEnemies,
                       TileMap tileMap, double timeProgress, WeatherType weather) {
        update(deltaTime, player, foodList, null, null, null, scentTrail, projectiles, allEnemies, tileMap, timeProgress, weather);
    }

    public void update(double deltaTime, Player player, List<FoodItem> foodList, List<MeatItem> meatList,
                       List<PassiveAnimal> preyList, List<BearTrap> trapList,
                       List<Footprint> scentTrail, List<Projectile> projectiles, List<Enemy> allEnemies,
                       TileMap tileMap, double timeProgress, WeatherType weather) {
        update(deltaTime, player, foodList, meatList, preyList, trapList, scentTrail, projectiles, allEnemies, tileMap, null, timeProgress, weather);
    }

    public void update(double deltaTime, Player player, List<FoodItem> foodList, List<MeatItem> meatList,
                       List<PassiveAnimal> preyList, List<BearTrap> trapList,
                       List<Footprint> scentTrail, List<Projectile> projectiles, List<Enemy> allEnemies,
                       TileMap tileMap, List<WaterRipple> waterRipples, double timeProgress, WeatherType weather) {
        if (trapTimer > 0) {
            trapTimer -= deltaTime;
            state = AIState.TRAPPED;
            vx = 0;
            vy = 0;
            moving = false;
            return;
        }

        breathTimer += deltaTime;
        repathCooldown -= deltaTime;
        if (alertTimer > 0) alertTimer -= deltaTime;
        if (shootCooldown > 0) shootCooldown -= deltaTime;
        if (damageFlashTimer > 0) damageFlashTimer -= deltaTime;
        if (attackCooldown > 0) attackCooldown -= deltaTime;
        if (patrolPauseTimer > 0) patrolPauseTimer -= deltaTime;
        if (forageCooldownTimer > 0) forageCooldownTimer -= deltaTime;
        if (houndBarkCooldown > 0) houndBarkCooldown -= deltaTime;
        if (thirstTimer > 0) thirstTimer -= deltaTime;
        if (satiationTimer > 0) satiationTimer -= deltaTime;
        if (chargeCooldown > 0) chargeCooldown -= deltaTime;
        if (kitingCooldown > 0) kitingCooldown -= deltaTime;

        if (attackWindupTimer > 0) {
            attackWindupTimer -= deltaTime;
            if (attackWindupTimer <= 0) {
                executeLungeStrike();
            }
        }
        if (attackLungeTimer > 0) attackLungeTimer -= deltaTime;
        if (bossController != null) {
            bossController.tickTimers(deltaTime, hp, maxHp, attackWindupTimer > 0, attackWindupTimer, attackLungeTimer > 0);
        }

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

        // Ciclo circadiano de visão e sentidos ecológicos
        double nightFactor = (timeProgress % 1.0);
        boolean isNight = nightFactor > 0.45 && nightFactor < 0.90;
        double visionScale = isNight ? behaviorProfile.getNightVisionMult() : 1.0;
        this.currentVisionRange = baseVisionRange * weather.getVisionMultiplier() * visionScale;
        this.currentHearingSensitivity = baseHearingSensitivity * weather.getSoundMultiplier();

        double duskBoost = 1.0 + (timeProgress * 0.15);
        TileType currentTile = tileMap.getTileAt(curCol, curRow);

        // Apex predator behavior (velocidade, território e leash delegados ao BossBehaviorController)
        if (type == EnemyType.BEAR_BOSS && bossController != null) {
            updateApexBoss(player, allEnemies, tileMap, curCol, curRow, deltaTime, duskBoost, scentTrail, currentTile);
            return;
        }

        double currentActualSpeed = speed * duskBoost;
        if (skin == EnemySkin.CROCODILE) {
            if (currentTile == TileType.WATER) {
                currentActualSpeed = baseSpeed * 1.45;
            } else if (currentTile == TileType.MUD) {
                currentActualSpeed = baseSpeed * 1.05;
            } else {
                currentActualSpeed = baseSpeed * 0.65;
            }
        } else if (currentTile == TileType.MUD) {
            currentActualSpeed *= variant.isMudAdapted() ? 0.92 : 0.55;
        } else if (currentTile == TileType.WATER) {
            currentActualSpeed *= 0.45;
        } else if (currentTile == TileType.DIRT_PATH) {
            currentActualSpeed *= 1.12;
        }

        if (state == AIState.STALKING) {
            currentActualSpeed *= 0.65;
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
                updateHunter(canSeePlayer, canHearPlayer, player, allEnemies, tileMap, projectiles, trapList, waterRipples, deltaTime, curCol, curRow, distToPlayer);
                break;
            case HOUND:
                updateHound(canSeePlayer, canHearPlayer, player, allEnemies, scentTrail, tileMap, waterRipples, deltaTime, curCol, curRow);
                break;
            case RIVAL:
                updateRival(canSeePlayer, canHearPlayer, player, foodList, meatList, preyList, allEnemies, tileMap, waterRipples, deltaTime, curCol, curRow, distToPlayer);
                break;
            case BEAR_BOSS:
                break;
        }

        // Executa o movimento fluido: Perseguição Direta se corredor livre ou Seguimento de Rota Suavizada
        executeFluidMovement(deltaTime, currentActualSpeed, player, allEnemies, tileMap, scentTrail, currentTile);
    }

    private void updateApexBoss(Player player, List<Enemy> allEnemies, TileMap tileMap, int curCol, int curRow,
                                double deltaTime, double duskBoost, List<Footprint> scentTrail, TileType currentTile) {
        BossBehaviorController.BossTickDecision decision = bossController.evaluateBehavior(
            this, player, allEnemies, tileMap, curCol, curRow, deltaTime
        );
        this.state = decision.state;

        if (decision.stopMovement) {
            patrolPauseTimer = 0;
            if (decision.lockFaceAngle != null) {
                this.faceAngle = lerpAngle(this.faceAngle, decision.lockFaceAngle, Math.min(1.0, deltaTime * 14.0));
            }
            this.moving = false;
            this.vx *= 0.4;
            this.vy *= 0.4;
            return;
        }

        if (decision.state == AIState.CHASE || decision.state == AIState.RETREATING) {
            patrolPauseTimer = 0;
            requestPath(tileMap, curCol, curRow, decision.targetCol, decision.targetRow, false);
        } else if (decision.state == AIState.FIGHTING_ENEMY && decision.rivalTarget != null) {
            patrolPauseTimer = 0;
            requestPath(tileMap, curCol, curRow, decision.targetCol, decision.targetRow, false);
            double distRival = Math.hypot(decision.rivalTarget.getX() - x, decision.rivalTarget.getY() - y);
            if (distRival < 34.0 && attackCooldown <= 0) {
                decision.rivalTarget.takeDamage(2, x, y);
                attackCooldown = 0.9;
            }
        } else {
            if (isPathFinished()) {
                handleOrganicPatrolArrival(allEnemies, tileMap, curCol, curRow);
            }
        }

        double currentActualSpeed = bossController.computeCurrentSpeed(state, currentTile, duskBoost);
        executeFluidMovement(deltaTime, currentActualSpeed, player, allEnemies, tileMap, scentTrail, currentTile);
    }

    public void receiveHoundAlert(double targetX, double targetY, TileMap tileMap) {
        if (state == AIState.PATROL || state == AIState.SUSPICIOUS || alertTimer <= 0) {
            this.alertTimer = 6.0;
            this.targetAimX = targetX;
            this.targetAimY = targetY;
            this.state = AIState.ALERT;
            this.patrolPauseTimer = 0;
            int cCol = (int) Math.floor(x / TileMap.TILE_SIZE);
            int cRow = (int) Math.floor(y / TileMap.TILE_SIZE);
            int tCol = (int) Math.floor(targetX / TileMap.TILE_SIZE);
            int tRow = (int) Math.floor(targetY / TileMap.TILE_SIZE);
            requestPath(tileMap, cCol, cRow, tCol, tRow, false);
        }
    }

    private void updateHunter(boolean canSee, boolean canHear, Player player, List<Enemy> allEnemies, TileMap tileMap,
                              List<Projectile> projectiles, List<BearTrap> trapList, List<WaterRipple> ripples, double dt,
                              int curCol, int curRow, double distToPlayer) {
        trapDeployCooldown -= dt;
        if (state == AIState.PATROL && trapList != null && trapDeployCooldown <= 0 && trapList.size() < 14) {
            TileType curTile = tileMap.getTileAt(curCol, curRow);
            if (curTile.isWalkable() && curTile != TileType.WATER) {
                trapList.add(new BearTrap(x, y));
                trapDeployCooldown = 26.0 + rand.nextDouble() * 18.0;
            }
        }

        // 1. Autodefesa e Proteção do Sabujo contra Feras Selvagens Hostis (Lobos, Ursos, Panteras, Javalis)
        Enemy threateningBeast = null;
        double minBeastDist = 200.0;
        for (Enemy other : allEnemies) {
            if (other == this || other.getHp() <= 0) continue;
            if (other.getType() == EnemyType.HUNTER || other.getType() == EnemyType.HOUND) continue;
            double d = Math.hypot(other.getX() - x, other.getY() - y);
            if (d < minBeastDist && tileMap.hasLineOfSight(x, y, other.getX(), other.getY())) {
                minBeastDist = d;
                threateningBeast = other;
            }
        }

        if (threateningBeast != null && (minBeastDist < 160.0 || threateningBeast.getState() == AIState.FIGHTING_ENEMY || threateningBeast.getState() == AIState.CHASE)) {
            patrolPauseTimer = 0;
            targetAimX = threateningBeast.getX();
            targetAimY = threateningBeast.getY();

            if (minBeastDist < 65.0) {
                // Recuo tático com arco (Kiting)
                state = AIState.KITING;
                faceAngle = lerpAngle(faceAngle, Math.atan2(targetAimY - y, targetAimX - x), 0.25);
            } else {
                state = AIState.AIMING;
                faceAngle = lerpAngle(faceAngle, Math.atan2(targetAimY - y, targetAimX - x), 0.25);
                if (shootCooldown <= 0) {
                    projectiles.add(new Projectile(x, y, threateningBeast.getX(), threateningBeast.getY()));
                    shootCooldown = 1.65;
                }
            }
            return;
        }

        // 2. Caça e Engajamento com a Raposa (Player)
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

                if (distToPlayer < 65.0) {
                    // Recuo tático (Kiting) da raposa
                    state = AIState.KITING;
                    faceAngle = lerpAngle(faceAngle, Math.atan2(player.getY() - y, player.getX() - x), 0.25);
                } else if (distToPlayer < 245.0) {
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
                handleOrganicPatrolArrival(allEnemies, tileMap, curCol, curRow);
            }
        } else if (canHear) {
            state = AIState.SUSPICIOUS;
            alertTimer = 3.5;
            patrolPauseTimer = 0;
            requestPath(tileMap, curCol, curRow, (int) Math.floor(player.getX() / TileMap.TILE_SIZE), (int) Math.floor(player.getY() / TileMap.TILE_SIZE), false);
        } else {
            // Rotina de beber água na margem se com sede
            if (thirstTimer <= 0 && state == AIState.PATROL) {
                if (checkWaterDrinkingRoutine(tileMap, ripples, dt, curCol, curRow)) {
                    return;
                }
            }
            state = AIState.PATROL;
            if (isPathFinished()) {
                handleOrganicPatrolArrival(allEnemies, tileMap, curCol, curRow);
            }
        }
    }

    private void updateHound(boolean canSee, boolean canHear, Player player, List<Enemy> allEnemies,
                             List<Footprint> scentTrail, TileMap tileMap, List<WaterRipple> ripples,
                             double dt, int curCol, int curRow) {
        if (canHear || canSee) {
            state = AIState.CHASE;
            alertTimer = 4.5;
            patrolPauseTimer = 0;
            targetAimX = player.getX();
            targetAimY = player.getY();
            requestPath(tileMap, curCol, curRow, (int) Math.floor(player.getX() / TileMap.TILE_SIZE), (int) Math.floor(player.getY() / TileMap.TILE_SIZE), false);

            // Latido sonoro periódico de alarme e convocação para os caçadores humanos
            if (houndBarkCooldown <= 0) {
                houndBarkCooldown = 2.4 + rand.nextDouble() * 0.8;
                br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.DOG_BARK, 0.85, 0, 1.1 + rand.nextDouble() * 0.2);
                notifyHuntersInRadius(allEnemies, player.getX(), player.getY(), 440.0, tileMap);
            }
            return;
        }

        // Rivalidade territorial com Lobos selvagens (defesa mútua)
        if (alertTimer <= 0) {
            for (Enemy other : allEnemies) {
                if (other == this || other.getHp() <= 0) continue;
                if (other.getSkin() == EnemySkin.WOLF) {
                    double distToWolf = Math.hypot(other.getX() - x, other.getY() - y);
                    if (distToWolf < 170.0) {
                        state = AIState.FIGHTING_ENEMY;
                        patrolPauseTimer = 0;
                        targetAimX = other.getX();
                        targetAimY = other.getY();
                        int wCol = (int) Math.floor(other.getX() / TileMap.TILE_SIZE);
                        int wRow = (int) Math.floor(other.getY() / TileMap.TILE_SIZE);
                        requestPath(tileMap, curCol, curRow, wCol, wRow, false);
                        if (distToWolf <= 38.0 && attackCooldown <= 0) {
                            attackCooldown = 1.15;
                            other.takeDamage(1, x, y);
                            br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.DOG_BARK, 0.80, 0, 1.25);
                            notifyHuntersInRadius(allEnemies, x, y, 440.0, tileMap);
                        }
                        return;
                    }
                }
            }
        }

        if (alertTimer > 0) {
            state = AIState.ALERT;
            if (isPathFinished()) {
                handleOrganicPatrolArrival(allEnemies, tileMap, curCol, curRow);
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

            if (houndBarkCooldown <= 0 && rand.nextDouble() < 0.30) {
                houndBarkCooldown = 3.8;
                br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.DOG_BARK, 0.65, 0, 0.95);
            }
            return;
        }

        // Rotina de beber água na margem do rio
        if (thirstTimer <= 0 && state == AIState.PATROL) {
            if (checkWaterDrinkingRoutine(tileMap, ripples, dt, curCol, curRow)) {
                return;
            }
        }

        state = AIState.PATROL;
        if (isPathFinished()) {
            handleOrganicPatrolArrival(allEnemies, tileMap, curCol, curRow);
        }
    }

    private void notifyHuntersInRadius(List<Enemy> allEnemies, double targetX, double targetY, double radius, TileMap tileMap) {
        for (Enemy other : allEnemies) {
            if (other == this || other.getHp() <= 0) continue;
            if (other.getType() == EnemyType.HUNTER) {
                double dist = Math.hypot(other.getX() - x, other.getY() - y);
                if (dist <= radius) {
                    other.receiveHoundAlert(targetX, targetY, tileMap);
                }
            }
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

    private void updateRival(boolean canSee, boolean canHear, Player player, List<FoodItem> foodList, List<MeatItem> meatList,
                             List<PassiveAnimal> preyList, List<Enemy> allEnemies,
                             TileMap tileMap, List<WaterRipple> ripples, double dt, int curCol, int curRow, double distToPlayer) {
        // Se estiver ativamente bebendo água na margem
        if (state == AIState.DRINKING) {
            drinkingTimer -= dt;
            waterRippleTimer -= dt;
            if (waterRippleTimer <= 0 && ripples != null) {
                waterRippleTimer = 0.50;
                ripples.add(new WaterRipple(drinkingWaterX, drinkingWaterY, 13.0, 0.70));
            }
            if (drinkingTimer <= 0) {
                thirstTimer = 34.0 + rand.nextDouble() * 28.0;
                state = AIState.PATROL;
                patrolPauseTimer = 1.4;
            }
            return;
        }

        // Se o javali estiver no meio da investida retilínea (Charging)
        if (state == AIState.CHARGING) {
            chargeDuration -= dt;
            if (chargeDuration <= 0) {
                chargeCooldown = 4.0 + rand.nextDouble() * 2.0;
                state = AIState.PATROL;
                patrolPauseTimer = 1.2;
            }
            return;
        }

        // 1. Interação com a Raposa (Player)
        if (skin == EnemySkin.BOAR) {
            // Javali é neutro-territorial: avisa e só investe se invadirem seu espaço imediato (< 85px) ou o atacarem
            if (canSee && distToPlayer < 110.0) {
                alertTimer = 4.0;
                if (distToPlayer < 85.0 && chargeCooldown <= 0) {
                    state = AIState.CHARGING;
                    chargeDuration = 0.85;
                    double cdx = player.getX() - x;
                    double cdy = player.getY() - y;
                    double cd = Math.hypot(cdx, cdy);
                    chargeDirX = cd > 0.001 ? cdx / cd : Math.cos(faceAngle);
                    chargeDirY = cd > 0.001 ? cdy / cd : Math.sin(faceAngle);
                    faceAngle = Math.atan2(chargeDirY, chargeDirX);
                    br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.BOAR_GRUNT, 0.90, 0, 0.95);
                    return;
                } else {
                    state = AIState.ALERT;
                    faceAngle = lerpAngle(faceAngle, Math.atan2(player.getY() - y, player.getX() - x), 0.20);
                    return;
                }
            }
        } else if (skin == EnemySkin.PANTHER) {
            // Pantera: Espreita Furtiva (Stalking) à média distância e Bote Rápido (Pounce) quando próxima
            if (canSee) {
                targetAimX = player.getX();
                targetAimY = player.getY();
                alertTimer = 5.0;
                patrolPauseTimer = 0;
                int pCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
                int pRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);

                if (distToPlayer > 75.0 && distToPlayer < 200.0) {
                    state = AIState.STALKING;
                    requestPath(tileMap, curCol, curRow, pCol, pRow, false);
                    return;
                } else if (distToPlayer <= 75.0) {
                    state = AIState.CHASE;
                    requestPath(tileMap, curCol, curRow, pCol, pRow, false);
                    return;
                }
            }
        } else {
            // Lobos, Ursos, Crocodilos e Texugos
            if (canSee || (canHear && distToPlayer < 220.0)) {
                state = AIState.CHASE;
                alertTimer = 5.0;
                patrolPauseTimer = 0;
                targetAimX = player.getX();
                targetAimY = player.getY();
                int pCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
                int pRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);
                requestPath(tileMap, curCol, curRow, pCol, pRow, false);
                return;
            }
        }

        if (alertTimer > 0) {
            state = AIState.ALERT;
            if (isPathFinished()) {
                handleOrganicPatrolArrival(allEnemies, tileMap, curCol, curRow);
            }
            return;
        }

        // 2. Disputa Territorial de Feras contra Caçadores e Cães Sabujos
        for (Enemy other : allEnemies) {
            if (other == this || other.getHp() <= 0) continue;
            boolean isEnemyFaction = (other.getType() == EnemyType.HUNTER || other.getType() == EnemyType.HOUND);
            if (isEnemyFaction && (skin == EnemySkin.WOLF || skin == EnemySkin.BEAR || skin == EnemySkin.PANTHER)) {
                double distFaction = Math.hypot(other.getX() - x, other.getY() - y);
                if (distFaction < 165.0) {
                    state = AIState.FIGHTING_ENEMY;
                    patrolPauseTimer = 0;
                    targetAimX = other.getX();
                    targetAimY = other.getY();
                    int fCol = (int) Math.floor(other.getX() / TileMap.TILE_SIZE);
                    int fRow = (int) Math.floor(other.getY() / TileMap.TILE_SIZE);
                    requestPath(tileMap, curCol, curRow, fCol, fRow, false);

                    if (distFaction <= 38.0 && attackCooldown <= 0) {
                        attackCooldown = 1.20;
                        other.takeDamage(1, x, y);
                        br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.WOLF_SNARL, 0.80, 0, 1.0);
                    }
                    return;
                }
            }
        }

        // 3. Predadores caçam presas da fauna passiva (se não estiverem saciados)
        boolean isPredator = (skin == EnemySkin.WOLF || skin == EnemySkin.PANTHER || skin == EnemySkin.BEAR ||
                              skin == EnemySkin.CROCODILE);
        if (isPredator && preyList != null && satiationTimer <= 0) {
            PassiveAnimal nearestPrey = null;
            double minPreyDist = 220.0;
            for (PassiveAnimal prey : preyList) {
                if (prey.isDespawned() || !prey.isCatchable()) continue;
                double dp = Math.hypot(prey.getX() - x, prey.getY() - y);
                if (dp < minPreyDist) {
                    minPreyDist = dp;
                    nearestPrey = prey;
                }
            }

            if (nearestPrey != null) {
                state = (skin == EnemySkin.PANTHER && minPreyDist > 75.0) ? AIState.STALKING : AIState.HUNTING_PREY;
                patrolPauseTimer = 0;
                targetAimX = nearestPrey.getX();
                targetAimY = nearestPrey.getY();
                int preyCol = (int) Math.floor(nearestPrey.getX() / TileMap.TILE_SIZE);
                int preyRow = (int) Math.floor(nearestPrey.getY() / TileMap.TILE_SIZE);
                requestPath(tileMap, curCol, curRow, preyCol, preyRow, false);

                if (minPreyDist <= 38.0 && nearestPrey.isCatchable()) {
                    boolean dead = nearestPrey.takeDamage(1);
                    br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.BITE_WHOOSH, 0.75, 0, 1.1);
                    if (dead) {
                        patrolPauseTimer = 2.0;
                        state = AIState.EATING;
                        satiationTimer = 28.0 + rand.nextDouble() * 14.0;
                        if (hp < maxHp) hp++;
                        currentPath = null;
                    }
                }
                return;
            }
        }

        // 4. Carnívoros e onívoros disputam carcaças e carnes caídas (respeitando hierarquia de intimidação)
        if (meatList != null && (skin == EnemySkin.WOLF || skin == EnemySkin.BEAR || skin == EnemySkin.PANTHER || skin == EnemySkin.BOAR)) {
            MeatItem nearestMeat = null;
            double minMeatDist = 190.0;
            for (MeatItem meat : meatList) {
                if (meat.isEaten()) continue;
                double dm = Math.hypot(meat.getX() - x, meat.getY() - y);
                if (dm < minMeatDist) {
                    // Verifica se há outro animal de porte superior na mesma carne
                    boolean shouldYield = false;
                    for (Enemy other : allEnemies) {
                        if (other == this || other.getHp() <= 0) continue;
                        double dOtherMeat = Math.hypot(meat.getX() - other.getX(), meat.getY() - other.getY());
                        if (dOtherMeat < 80.0 && MobSteeringHelper.shouldYieldMeat(this, other)) {
                            shouldYield = true;
                            break;
                        }
                    }
                    if (!shouldYield) {
                        minMeatDist = dm;
                        nearestMeat = meat;
                    }
                }
            }

            if (nearestMeat != null) {
                state = AIState.SEARCHING_FOOD;
                targetAimX = nearestMeat.getX();
                targetAimY = nearestMeat.getY();
                int mCol = (int) Math.floor(nearestMeat.getX() / TileMap.TILE_SIZE);
                int mRow = (int) Math.floor(nearestMeat.getY() / TileMap.TILE_SIZE);
                requestPath(tileMap, curCol, curRow, mCol, mRow, false);

                if (minMeatDist < 22.0) {
                    nearestMeat.setEaten(true);
                    state = AIState.EATING;
                    patrolPauseTimer = 1.6;
                    satiationTimer = 26.0 + rand.nextDouble() * 15.0;
                    currentPath = null;
                    if (hp < maxHp) hp++;
                }
                return;
            }
        }

        // 5. Onívoros forrageiam arbustos desocupados (Javali, Urso, Texugo)
        FoodItem nearestFood = (behaviorProfile.canForageFood() && forageCooldownTimer <= 0)
                ? MobSteeringHelper.findUncrowdedFoodForForager(this, foodList, allEnemies, behaviorProfile.getFoodSeekRadiusPx())
                : null;

        if (nearestFood != null) {
            double distFoodToPlayer = Math.hypot(player.getX() - nearestFood.getX(), player.getY() - nearestFood.getY());
            if (distFoodToPlayer < 140.0 && distToPlayer < 200.0 && canSee) {
                state = AIState.CHASE;
                alertTimer = 5.0;
                patrolPauseTimer = 0;
                requestPath(tileMap, curCol, curRow, (int) Math.floor(player.getX() / TileMap.TILE_SIZE), (int) Math.floor(player.getY() / TileMap.TILE_SIZE), false);
                return;
            }

            state = AIState.SEARCHING_FOOD;
            double distToFood = Math.hypot(nearestFood.getX() - x, nearestFood.getY() - y);
            if (distToFood < 22.0) {
                nearestFood.setEaten(true);
                state = AIState.EATING;
                patrolPauseTimer = 1.25;
                forageCooldownTimer = 22.0;
                currentPath = null;
            } else {
                requestPath(tileMap, curCol, curRow, nearestFood.getCol(), nearestFood.getRow(), false);
            }
            return;
        }

        // 6. Rotina Biológica de Beber Água na Margem do Rio
        if (thirstTimer <= 0 && state == AIState.PATROL) {
            if (checkWaterDrinkingRoutine(tileMap, ripples, dt, curCol, curRow)) {
                return;
            }
        }

        state = (satiationTimer > 0) ? AIState.SATIATED : AIState.PATROL;
        if (isPathFinished()) {
            handleOrganicPatrolArrival(allEnemies, tileMap, curCol, curRow);
        }
    }

    private boolean checkWaterDrinkingRoutine(TileMap tileMap, List<WaterRipple> ripples, double dt, int curCol, int curRow) {
        int[] shoreData = MobSteeringHelper.findNearestWaterShoreTile(tileMap, curCol, curRow, 9);
        if (shoreData != null) {
            int sCol = shoreData[0];
            int sRow = shoreData[1];
            int wCol = shoreData[2];
            int wRow = shoreData[3];
            double sX = sCol * TileMap.TILE_SIZE + 16.0;
            double sY = sRow * TileMap.TILE_SIZE + 16.0;
            double distToShore = Math.hypot(sX - x, sY - y);

            if (distToShore <= 20.0) {
                state = AIState.DRINKING;
                drinkingTimer = 2.8 + rand.nextDouble() * 1.2;
                drinkingWaterX = wCol * TileMap.TILE_SIZE + 16.0;
                drinkingWaterY = wRow * TileMap.TILE_SIZE + 16.0;
                faceAngle = Math.atan2(drinkingWaterY - y, drinkingWaterX - x);
                if (ripples != null) {
                    ripples.add(new WaterRipple(drinkingWaterX, drinkingWaterY, 14.0, 0.75));
                }
                return true;
            } else {
                requestPath(tileMap, curCol, curRow, sCol, sRow, false);
                return true;
            }
        }
        return false;
    }

    /**
     * Ao concluir uma rota de patrulha, pausa em vigília de acordo com o perfil da espécie
     * antes de escolher um novo destino distante de concentrações de outros mobs.
     */
    private void handleOrganicPatrolArrival(List<Enemy> allEnemies, TileMap tileMap, int curCol, int curRow) {
        if (patrolPauseTimer <= 0) {
            if (moving) {
                moving = false;
                vx = 0;
                vy = 0;
                double minP = behaviorProfile.getPauseMinSec();
                double maxP = behaviorProfile.getPauseMaxSec();
                patrolPauseTimer = minP + rand.nextDouble() * Math.max(0.1, maxP - minP);
            } else {
                pickOrganicPatrolPoint(allEnemies, tileMap, curCol, curRow);
            }
        } else {
            faceAngle += Math.sin(breathTimer * 2.0) * 0.008;
        }
    }

    private void pickOrganicPatrolPoint(List<Enemy> allEnemies, TileMap tileMap, int curCol, int curRow) {
        if (bossController != null) {
            int[] territoryTile = bossController.pickTerritoryPatrolTile(tileMap, curCol, curRow);
            if (territoryTile != null) {
                patrolHeadingAngle = Math.atan2(territoryTile[1] - curRow, territoryTile[0] - curCol);
                requestPath(tileMap, curCol, curRow, territoryTile[0], territoryTile[1], true);
                return;
            }
        }

        int[] bestTile = MobSteeringHelper.pickDispersedPatrolTile(
            this, allEnemies, tileMap, curCol, curRow, patrolHeadingAngle, behaviorProfile, rand
        );
        if (bestTile != null) {
            patrolHeadingAngle = Math.atan2(bestTile[1] - curRow, bestTile[0] - curCol);
            requestPath(tileMap, curCol, curRow, bestTile[0], bestTile[1], true);
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
        boolean isCrocodile = (skin == EnemySkin.CROCODILE);
        List<Node> newPath = pathfinder.findPath(tileMap, startCol, startRow, destCol, destRow, isCrocodile);

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

        this.repathCooldown = (state == AIState.CHASE || state == AIState.TERRITORIAL_ATTACK || state == AIState.HUNTING_PREY || state == AIState.FIGHTING_ENEMY || state == AIState.RETREATING) ? 0.22 : 0.55;
    }

    /**
     * Motor de Movimento Fluido (Steering Behavior + Direct Pursuit + Anti-Stuck):
     * - Em perseguição com linha de visão desimpedida, move-se diretamente em direção ao jogador sem zigue-zague de grade.
     * - Em seguimento de rota, faz transição contínua entre waypoints com raio de lookahead e inércia de velocidade.
     * - Aplica força suave de separação entre inimigos próximos para evitar sobreposição.
     */
    private void executeFluidMovement(double deltaTime, double maxSpeed, Player player, List<Enemy> allEnemies,
                                      TileMap tileMap, List<Footprint> scentTrail, TileType currentTile) {
        if (state == AIState.AIMING || state == AIState.ROARING || state == AIState.DRINKING || patrolPauseTimer > 0) {
            this.moving = false;
            this.vx *= 0.5;
            this.vy *= 0.5;
            if (state != AIState.ROARING && state != AIState.DRINKING) {
                double[] drift = MobSteeringHelper.computePausedDispersionVelocity(this, allEnemies);
                if (Math.hypot(drift[0], drift[1]) > 2.0) {
                    moveWithCollisionSlide(drift[0] * deltaTime, drift[1] * deltaTime, tileMap);
                }
            }
            return;
        }

        // Investida retilínea do javali (Charging)
        if (state == AIState.CHARGING) {
            this.moving = true;
            double chargeSpeed = maxSpeed * 1.55;
            this.vx = chargeDirX * chargeSpeed;
            this.vy = chargeDirY * chargeSpeed;
            this.walkPhase += chargeSpeed * deltaTime * 0.12;
            this.faceAngle = Math.atan2(chargeDirY, chargeDirX);
            moveWithCollisionSlide(this.vx * deltaTime, this.vy * deltaTime, tileMap);
            return;
        }

        // Recuo tático do caçador com arco (Kiting)
        if (state == AIState.KITING) {
            this.moving = true;
            double kiteSpeed = maxSpeed * 0.95;
            double awayX = x - targetAimX;
            double awayY = y - targetAimY;
            double awayLen = Math.hypot(awayX, awayY);
            if (awayLen > 0.001) {
                this.vx = (awayX / awayLen) * kiteSpeed;
                this.vy = (awayY / awayLen) * kiteSpeed;
            }
            this.walkPhase += kiteSpeed * deltaTime * 0.09;
            this.faceAngle = Math.atan2(targetAimY - y, targetAimX - x);
            moveWithCollisionSlide(this.vx * deltaTime, this.vy * deltaTime, tileMap);
            return;
        }

        // Durante a preparação do bote (wind-up), mantém avanço direto e firme em direção ao alvo
        if (attackWindupTimer > 0) {
            double distTarget = Math.hypot(targetAimX - x, targetAimY - y);
            if (distTarget > 6.0) {
                double windupSpeed = maxSpeed * 0.65;
                this.vx = ((targetAimX - x) / distTarget) * windupSpeed;
                this.vy = ((targetAimY - y) / distTarget) * windupSpeed;
                moveWithCollisionSlide(this.vx * deltaTime, this.vy * deltaTime, tileMap);
                this.faceAngle = Math.atan2(targetAimY - y, targetAimX - x);
                this.moving = true;
            }
            return;
        }

        double desiredDirX = 0.0;
        double desiredDirY = 0.0;
        boolean hasTarget = false;

        boolean isChasingPlayer = (state == AIState.CHASE || state == AIState.TERRITORIAL_ATTACK) && !player.isBurrowed();
        boolean isHuntingOrFighting = (state == AIState.HUNTING_PREY || state == AIState.FIGHTING_ENEMY);
        boolean isCroc = (skin == EnemySkin.CROCODILE);

        // 1. Perseguição Vetorial Direta (Direct Pursuit) ou Retorno Direto ao Domínio
        if (state == AIState.RETREATING && bossController != null) {
            double hx = bossController.getHomeX();
            double hy = bossController.getHomeY();
            double distHome = Math.hypot(hx - x, hy - y);
            if (distHome > 16.0 && Pathfinder.hasClearWorldCorridor(tileMap, x, y, hx, hy, isCroc)) {
                desiredDirX = (hx - x) / distHome;
                desiredDirY = (hy - y) / distHome;
                hasTarget = true;
            }
        } else if (isChasingPlayer) {
            double distToP = Math.hypot(player.getX() - x, player.getY() - y);
            if (distToP > 10.0 && distToP < 320.0 &&
                Pathfinder.hasClearWorldCorridor(tileMap, x, y, player.getX(), player.getY(), isCroc)) {
                if (skin == EnemySkin.WOLF) {
                    double[] flankDir = MobSteeringHelper.computePackFlankingDirection(this, allEnemies, player.getX(), player.getY());
                    desiredDirX = flankDir[0];
                    desiredDirY = flankDir[1];
                } else {
                    desiredDirX = (player.getX() - x) / distToP;
                    desiredDirY = (player.getY() - y) / distToP;
                }
                hasTarget = true;
            }
        } else if (isHuntingOrFighting) {
            double distT = Math.hypot(targetAimX - x, targetAimY - y);
            if (distT > 10.0 && distT < 280.0 &&
                Pathfinder.hasClearWorldCorridor(tileMap, x, y, targetAimX, targetAimY, isCroc)) {
                desiredDirX = (targetAimX - x) / distT;
                desiredDirY = (targetAimY - y) / distT;
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
            double[] drift = MobSteeringHelper.computePausedDispersionVelocity(this, allEnemies);
            if (Math.hypot(drift[0], drift[1]) > 2.0) {
                moveWithCollisionSlide(drift[0] * deltaTime, drift[1] * deltaTime, tileMap);
            }
            return;
        }

        // 3. Força Modular de Separação e Espaço Pessoal entre Inimigos
        double[] sep = MobSteeringHelper.computeSeparationForce(this, allEnemies, behaviorProfile);
        double combinedX = desiredDirX + sep[0];
        double combinedY = desiredDirY + sep[1];
        double combinedLen = Math.hypot(combinedX, combinedY);
        if (combinedLen > 0.001) {
            combinedX /= combinedLen;
            combinedY /= combinedLen;
        }

        // 4. Interpolação Suave de Velocidade (Steering Inertia) e Ângulo Visual
        double steerRate = 12.0;
        if (currentTile == TileType.ICE) {
            boolean hasFrostGrip = (skin == EnemySkin.POLAR_BEAR || variant == AnimalVariant.ARCTIC);
            steerRate = hasFrostGrip ? 9.5 : 2.6;
        }
        double steerFactor = Math.min(1.0, deltaTime * steerRate);
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
                    pickOrganicPatrolPoint(allEnemies, tileMap, c, r);
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

        if (bossController != null) {
            bossController.onDamaged(hp, maxHp, fromX, fromY, x, y);
        }

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

    public void startBossAttackWindup(double targetX, double targetY, double windupDuration) {
        if (attackCooldown > 0 || attackWindupTimer > 0) return;
        this.targetAimX = targetX;
        this.targetAimY = targetY;
        this.faceAngle = Math.atan2(targetY - y, targetX - x);
        this.attackWindupTimer = windupDuration;
    }

    public void cancelActiveAttackForRetreat() {
        this.attackWindupTimer = 0.0;
        this.attackLungeTimer = 0.0;
    }

    public void startAttackWindup(double targetX, double targetY) {
        if (attackCooldown > 0 || attackWindupTimer > 0) return;

        this.targetAimX = targetX;
        this.targetAimY = targetY;
        this.faceAngle = Math.atan2(targetY - y, targetX - x);

        switch (type) {
            case BEAR_BOSS:
                this.attackWindupTimer = (skin == EnemySkin.PANTHER) ? 0.20 : (skin == EnemySkin.CROCODILE ? 0.24 : 0.28);
                break;
            case HOUND:
                this.attackWindupTimer = 0.18;
                break;
            case RIVAL:
                this.attackWindupTimer = (skin == EnemySkin.BOAR) ? 0.26 : 0.20;
                break;
            case HUNTER:
            default:
                this.attackWindupTimer = 0.24;
                break;
        }
    }

    public void triggerAttackCooldownAfterHit() {
        this.attackLungeTimer = 0.0;
        this.attackWindupTimer = 0.0;
        this.attackCooldown = Math.max(this.attackCooldown, 0.75);
    }

    private void executeLungeStrike() {
        double dist = Math.hypot(targetAimX - x, targetAimY - y);
        double dirX = dist > 0.001 ? (targetAimX - x) / dist : Math.cos(faceAngle);
        double dirY = dist > 0.001 ? (targetAimY - y) / dist : Math.sin(faceAngle);

        switch (type) {
            case BEAR_BOSS:
                if (bossController != null) {
                    BossAttackType atk = bossController.getActiveAttack();
                    double rateScale = bossController.isEnraged() ? 0.85 : 1.0;
                    this.attackCooldown = atk.getCooldownDuration() * rateScale;
                    this.attackLungeTimer = atk.getActiveDuration();
                    this.knockbackVx = dirX * atk.getLungeImpulse();
                    this.knockbackVy = dirY * atk.getLungeImpulse();
                    bossController.beginActiveStrike();
                } else {
                    this.attackCooldown = 1.15;
                    this.attackLungeTimer = 0.26;
                    this.knockbackVx = dirX * 220.0;
                    this.knockbackVy = dirY * 220.0;
                }
                break;

            case HOUND:
                this.attackCooldown = 0.85;
                this.attackLungeTimer = 0.24;
                this.knockbackVx = dirX * 240.0;
                this.knockbackVy = dirY * 240.0;
                break;

            case RIVAL:
                if (skin == EnemySkin.BOAR) {
                    this.attackCooldown = 1.15;
                    this.attackLungeTimer = 0.32;
                    this.knockbackVx = dirX * 310.0;
                    this.knockbackVy = dirY * 310.0;
                } else {
                    this.attackCooldown = 0.90;
                    this.attackLungeTimer = 0.24;
                    this.knockbackVx = dirX * 220.0;
                    this.knockbackVy = dirY * 220.0;
                }
                break;

            case HUNTER:
            default:
                this.attackCooldown = 1.20;
                this.attackLungeTimer = 0.22;
                this.knockbackVx = dirX * 180.0;
                this.knockbackVy = dirY * 180.0;
                break;
        }
    }

    public boolean isWindup() { return attackWindupTimer > 0; }
    public boolean isLunging() { return attackLungeTimer > 0; }
    public double getTargetAimX() { return targetAimX; }
    public double getTargetAimY() { return targetAimY; }
    public double getAttackCooldown() { return attackCooldown; }

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
    public AnimalVariant getVariant() { return variant; }
    public String getDisplayName() {
        if (type == EnemyType.BEAR_BOSS) {
            return skin != null ? skin.getDisplayName() : type.name();
        }
        return variant.resolveDisplayName(skin);
    }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public boolean isFlashing() { return damageFlashTimer > 0; }
    public boolean isTrapped() { return trapTimer > 0; }
    public void trapImmobilize(double duration) {
        this.trapTimer = Math.max(this.trapTimer, duration);
        this.state = AIState.TRAPPED;
        this.vx = 0;
        this.vy = 0;
        this.moving = false;
    }
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
    public boolean isBoss() { return type == EnemyType.BEAR_BOSS; }
    public BossBehaviorController getBossController() { return bossController; }
    public SpeciesBehaviorProfile getBehaviorProfile() { return behaviorProfile; }
    public double getSatiationTimer() { return satiationTimer; }
    public boolean isCharging() { return state == AIState.CHARGING; }
    public boolean isStalking() { return state == AIState.STALKING; }
    public boolean isDrinking() { return state == AIState.DRINKING; }
    public boolean isKiting() { return state == AIState.KITING; }
}
