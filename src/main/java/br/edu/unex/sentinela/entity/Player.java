package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.world.*;

import java.util.List;
import java.util.Random;

public class Player {
    private double x;
    private double y;
    private double baseSpeed = 190.0;
    private double currentSpeed = 190.0;

    private double faceDx = 0;
    private double faceDy = 1;
    private double visualAngle = Math.PI * 0.5;

    private double width = 22.0;
    private double height = 22.0;

    // Animation state (Breathing, Walk Cycle, Footprints)
    private double walkPhase = 0.0;
    private double breathTimer = 0.0;
    private boolean moving = false;
    private boolean nextFootLeft = true;

    // Survival stats & Zelda Hearts
    private int lives;
    private int maxLives;
    private double hunger = 100.0;
    private double maxHunger = 100.0;

    // Sprint / Stamina mechanics
    public static final double STAMINA_DRAIN_PER_SEC = 32.0;
    public static final double STAMINA_REGEN_IDLE_PER_SEC = 24.0;
    public static final double STAMINA_REGEN_WALK_PER_SEC = 15.0;
    public static final double STAMINA_EXHAUSTION_RECOVERY_THRESHOLD = 30.0;
    public static final double ATTACK_STAMINA_COST_PCT = 0.15;
    public static final double LOW_HUNGER_SPRINT_THRESHOLD = 25.0;
    public static final double LOW_LIFE_SPEED_MULTIPLIER = 0.78;

    private double stamina = 100.0;
    private double maxStamina = 100.0;
    private boolean isSprintExhausted = false;
    private double attackStaminaAlertTimer = 0.0;

    // Status flags
    private boolean isSprinting = false;
    private boolean isHiddenInGrass = false;
    private boolean isSwimming = false;
    private boolean isBurrowed = false;
    private double burrowTimeLeft = 0.0;
    private double burrowCooldown = 0.0;

    // Zelda-Style Melee Combat (Bite / Claw Swipe)
    private boolean isAttacking = false;
    private double attackTimer = 0.0;
    private double attackCooldown = 0.0;
    private boolean attackHitProcessed = false;

    // Damage & Adrenaline
    private double invulnerabilityTimer = 0.0;
    private double adrenalineTimer = 0.0;
    private double knockbackVx = 0.0;
    private double knockbackVy = 0.0;

    private double noiseRadius = 0.0;

    // Timers
    private double footprintTimer = 0.0;
    private double particleTimer = 0.0;
    private static final Random rand = new Random();

    public Player(double startX, double startY, int startingLives) {
        this.x = startX;
        this.y = startY;
        this.lives = startingLives;
        this.maxLives = startingLives;
    }

    public void update(double dx, double dy, boolean sprint, boolean attackPressed, double deltaTime, TileMap tileMap,
                       List<Footprint> scentTrail, List<Particle> particles, List<Burrow> burrows) {
        breathTimer += deltaTime;
        if (invulnerabilityTimer > 0) invulnerabilityTimer -= deltaTime;
        if (adrenalineTimer > 0) adrenalineTimer -= deltaTime;
        if (burrowCooldown > 0) burrowCooldown -= deltaTime;
        if (attackCooldown > 0) attackCooldown -= deltaTime;
        if (attackStaminaAlertTimer > 0) {
            attackStaminaAlertTimer = Math.max(0.0, attackStaminaAlertTimer - deltaTime);
        }

        // Apply physical knockback when struck
        if (Math.abs(knockbackVx) > 0.1 || Math.abs(knockbackVy) > 0.1) {
            double nextKx = x + knockbackVx * deltaTime;
            double nextKy = y + knockbackVy * deltaTime;
            if (!isColliding(nextKx, y, tileMap)) x = nextKx;
            if (!isColliding(x, nextKy, tileMap)) y = nextKy;
            knockbackVx *= Math.pow(0.04, deltaTime);
            knockbackVy *= Math.pow(0.04, deltaTime);
        } else {
            knockbackVx = 0;
            knockbackVy = 0;
        }

        // Combat attack animation timer
        if (isAttacking) {
            attackTimer -= deltaTime;
            if (attackTimer <= 0) {
                isAttacking = false;
            }
        }

        tryTriggerPrimaryAttack(attackPressed);

        checkBurrowInteraction(dx, dy, deltaTime, burrows);

        this.moving = (dx != 0 || dy != 0) && !isBurrowed;
        updateSprintAndStamina(sprint, this.moving, deltaTime);

        if (dx != 0 || dy != 0) {
            faceDx = dx;
            faceDy = dy;
            double targetAngle = Math.atan2(dy, dx);
            visualAngle = lerpAngle(visualAngle, targetAngle, Math.min(1.0, deltaTime * 16.0));
        }

        int currentCol = (int) Math.floor(x / TileMap.TILE_SIZE);
        int currentRow = (int) Math.floor(y / TileMap.TILE_SIZE);
        TileType currentTile = tileMap.getTileAt(currentCol, currentRow);

        this.isHiddenInGrass = (currentTile == TileType.TALL_GRASS);
        this.isSwimming = (currentTile == TileType.WATER);

        double speedMultiplier = 1.0;
        if (currentTile == TileType.MUD) {
            speedMultiplier = 0.40;
        } else if (currentTile == TileType.WATER) {
            speedMultiplier = 0.45;
        } else if (currentTile == TileType.DIRT_PATH) {
            speedMultiplier = 1.15;
        }

        if (isLowLife()) {
            speedMultiplier *= LOW_LIFE_SPEED_MULTIPLIER;
        }

        if (adrenalineTimer > 0) {
            speedMultiplier *= 1.25;
        }

        if (isSprinting && moving) {
            currentSpeed = baseSpeed * 1.55 * speedMultiplier;
            noiseRadius = 240.0;
            hunger -= 3.8 * deltaTime;
        } else if (moving) {
            currentSpeed = baseSpeed * speedMultiplier;
            noiseRadius = 100.0;
            hunger -= 1.4 * deltaTime;
        } else if (!isAttacking) {
            currentSpeed = 0;
            noiseRadius = 0.0;
            hunger -= 0.5 * deltaTime;
        }

        if (hunger < 0) hunger = 0;

        if (moving) {
            walkPhase += currentSpeed * deltaTime * 0.095;

            double nextX = x + dx * currentSpeed * deltaTime;
            double nextY = y + dy * currentSpeed * deltaTime;

            if (!isColliding(nextX, y, tileMap)) {
                x = nextX;
            }
            if (!isColliding(x, nextY, tileMap)) {
                y = nextY;
            }

            // Pegadas direcionadas alternando esquerda/direita (Paw Prints)
            footprintTimer += deltaTime * (isSprinting ? 1.5 : 1.0);
            if (footprintTimer >= 0.18) {
                footprintTimer = 0.0;
                if (!isSwimming && !isBurrowed) {
                    double perpAngle = visualAngle + (nextFootLeft ? -Math.PI * 0.5 : Math.PI * 0.5);
                    double fpX = x - Math.cos(visualAngle) * 4 + Math.cos(perpAngle) * 3.5;
                    double fpY = y - Math.sin(visualAngle) * 4 + Math.sin(perpAngle) * 3.5;
                    scentTrail.add(new Footprint(fpX, fpY, visualAngle, nextFootLeft, Footprint.PrintStyle.PAW, true, 6.5));
                    nextFootLeft = !nextFootLeft;
                }
            }

            // Partículas orgânicas de movimento por bioma
            particleTimer += deltaTime;
            double emitInterval = isSprinting ? 0.045 : 0.095;
            if (particleTimer >= emitInterval) {
                particleTimer = 0.0;
                spawnMovementParticles(currentTile, dx, dy, particles);
            }
        }
    }

    private void spawnMovementParticles(TileType currentTile, double dx, double dy, List<Particle> particles) {
        if (isSwimming) {
            // Espuma e gotículas na água
            particles.add(new Particle(
                x - dx * 6 + (rand.nextDouble() * 10 - 5),
                y - dy * 6 + (rand.nextDouble() * 10 - 5),
                -dx * 25 + (rand.nextDouble() * 14 - 7),
                -dy * 25 + (rand.nextDouble() * 14 - 7),
                0.45,
                rand.nextBoolean() ? "#81D4FA" : "#E1F5FE",
                rand.nextDouble() * 3.5 + 2.0
            ));
        } else if (currentTile == TileType.TALL_GRASS || (currentTile == TileType.GRASS && isSprinting)) {
            // Folhinhas verdes voando quando corre na mata
            particles.add(new Particle(
                x + (rand.nextDouble() * 10 - 5),
                y + (rand.nextDouble() * 10 - 5),
                -dx * 22 + (rand.nextDouble() * 24 - 12),
                -dy * 22 - 12 + (rand.nextDouble() * 16 - 8),
                0.40,
                rand.nextBoolean() ? "#66BB6A" : "#43A047",
                rand.nextDouble() * 3.0 + 1.8
            ));
        } else if (currentTile == TileType.MUD) {
            // Respingos de lama escura
            particles.add(new Particle(
                x - dx * 5 + (rand.nextDouble() * 8 - 4),
                y - dy * 5 + (rand.nextDouble() * 8 - 4),
                -dx * 28 + (rand.nextDouble() * 18 - 9),
                -dy * 28 + (rand.nextDouble() * 18 - 9),
                0.38,
                "#5D4037",
                rand.nextDouble() * 3.5 + 2.0
            ));
        } else if (isSprinting || currentTile == TileType.DIRT_PATH) {
            // Pufes de poeira de corrida
            particles.add(new Particle(
                x - dx * 8 + (rand.nextDouble() * 6 - 3),
                y - dy * 8 + (rand.nextDouble() * 6 - 3),
                -dx * 32 + (rand.nextDouble() * 16 - 8),
                -dy * 32 + (rand.nextDouble() * 16 - 8),
                0.36,
                "#BCAAA4",
                rand.nextDouble() * 3.5 + 2.2
            ));
        }
    }

    private double lerpAngle(double current, double target, double factor) {
        double diff = target - current;
        while (diff < -Math.PI) diff += Math.PI * 2;
        while (diff > Math.PI) diff -= Math.PI * 2;
        return current + diff * factor;
    }

    private void checkBurrowInteraction(double dx, double dy, double deltaTime, List<Burrow> burrows) {
        if (isBurrowed) {
            burrowTimeLeft -= deltaTime;
            if (burrowTimeLeft <= 0 || dx != 0 || dy != 0) {
                isBurrowed = false;
                burrowCooldown = 6.0;
            }
            return;
        }

        if (dx == 0 && dy == 0 && burrowCooldown <= 0) {
            for (Burrow b : burrows) {
                double dist = Math.hypot(b.getX() - x, b.getY() - y);
                if (dist < 14.0) {
                    isBurrowed = true;
                    burrowTimeLeft = 4.5;
                    break;
                }
            }
        }
    }

    public boolean takeDamage(double fromX, double fromY) {
        return takeDamage(fromX, fromY, 280.0);
    }

    public boolean takeDamage(double fromX, double fromY, double knockbackForce) {
        if (invulnerabilityTimer > 0 || isBurrowed) {
            return false;
        }

        lives--;
        invulnerabilityTimer = 0.95;
        adrenalineTimer = 1.3;

        double dx = x - fromX;
        double dy = y - fromY;
        double dist = Math.hypot(dx, dy);
        if (dist > 0.001) {
            this.knockbackVx = (dx / dist) * knockbackForce;
            this.knockbackVy = (dy / dist) * knockbackForce;
        }
        return true;
    }

    public boolean takeDamage() {
        return takeDamage(x, y);
    }

    public void healLife(int amount) {
        this.lives = Math.min(maxLives, this.lives + amount);
    }

    private boolean isColliding(double nextX, double nextY, TileMap tileMap) {
        double left = nextX - width / 2;
        double right = nextX + width / 2;
        double top = nextY - height / 2;
        double bottom = nextY + height / 2;

        return !tileMap.isWalkable(left, top) ||
               !tileMap.isWalkable(right, top) ||
               !tileMap.isWalkable(left, bottom) ||
               !tileMap.isWalkable(right, bottom);
    }

    public void feed(double amount) {
        this.hunger = Math.min(maxHunger, this.hunger + amount);
    }

    public void restoreStamina(double amount) {
        if (amount <= 0) return;
        this.stamina = Math.min(maxStamina, this.stamina + amount);
        if (isSprintExhausted && stamina >= STAMINA_EXHAUSTION_RECOVERY_THRESHOLD) {
            isSprintExhausted = false;
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getFaceDx() { return faceDx; }
    public double getFaceDy() { return faceDy; }
    public double getVisualAngle() { return visualAngle; }
    public void setVisualAngle(double visualAngle) { this.visualAngle = visualAngle; }
    public double getWalkPhase() { return walkPhase; }
    public void setWalkPhase(double walkPhase) { this.walkPhase = walkPhase; }
    public double getBreathTimer() { return breathTimer; }
    public void setBreathTimer(double breathTimer) { this.breathTimer = breathTimer; }
    public boolean isMoving() { return moving; }
    public void setMoving(boolean moving) { this.moving = moving; }
    private void tryTriggerPrimaryAttack(boolean attackPressed) {
        if (!attackPressed || attackCooldown > 0 || isBurrowed || isSwimming) {
            return;
        }

        double staminaCost = getAttackStaminaCost();
        if (isSprintExhausted || stamina < staminaCost) {
            attackStaminaAlertTimer = 0.30;
            return;
        }

        stamina = Math.max(0.0, stamina - staminaCost);
        if (stamina <= 0.0) {
            stamina = 0.0;
            isSprintExhausted = true;
        }

        isAttacking = true;
        attackHitProcessed = false;
        attackTimer = 0.22;
        attackCooldown = 0.40;
        noiseRadius = 180.0;
        hunger = Math.max(0.0, hunger - 0.8);
    }

    private void updateSprintAndStamina(boolean wantSprint, boolean isActivelyMoving, double deltaTime) {
        boolean canSprint = !isBurrowed && !isSprintExhausted && !isSprintBlockedByHunger() && stamina > 0.0;
        this.isSprinting = wantSprint && isActivelyMoving && canSprint;

        if (this.isSprinting) {
            stamina -= STAMINA_DRAIN_PER_SEC * deltaTime;
            if (stamina <= 0.0) {
                stamina = 0.0;
                isSprintExhausted = true;
                this.isSprinting = false;
            }
        } else if (!this.isAttacking) {
            double regenRate = isActivelyMoving ? STAMINA_REGEN_WALK_PER_SEC : STAMINA_REGEN_IDLE_PER_SEC;
            stamina = Math.min(maxStamina, stamina + regenRate * deltaTime);
            if (isSprintExhausted && stamina >= STAMINA_EXHAUSTION_RECOVERY_THRESHOLD) {
                isSprintExhausted = false;
            }
        }
    }

    public boolean isSprinting() { return isSprinting; }
    public double getStamina() { return stamina; }
    public double getMaxStamina() { return maxStamina; }
    public double getAttackStaminaCost() { return maxStamina * ATTACK_STAMINA_COST_PCT; }
    public boolean isAttackBlockedByStaminaAlert() { return attackStaminaAlertTimer > 0; }
    public boolean isSprintExhausted() { return isSprintExhausted; }
    public boolean isSprintBlockedByHunger() { return hunger <= LOW_HUNGER_SPRINT_THRESHOLD; }
    public boolean isLowLife() { return lives > 0 && lives <= 1; }
    public int getLives() { return lives; }
    public int getMaxLives() { return maxLives; }
    public double getHunger() { return hunger; }
    public double getMaxHunger() { return maxHunger; }
    public boolean isHiddenInGrass() { return isHiddenInGrass; }
    public boolean isSwimming() { return isSwimming; }
    public boolean isBurrowed() { return isBurrowed; }
    public boolean isInvulnerable() { return invulnerabilityTimer > 0; }
    public boolean isAttacking() { return isAttacking; }
    public boolean hasProcessedAttackHit() { return attackHitProcessed; }
    public void markAttackHitProcessed() { this.attackHitProcessed = true; }
    public double getNoiseRadius() { return isBurrowed ? 0.0 : noiseRadius; }
}
