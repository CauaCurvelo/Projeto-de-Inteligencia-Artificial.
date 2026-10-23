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

    private double width = 22.0;
    private double height = 22.0;

    // Survival stats & Zelda Hearts
    private int lives;
    private int maxLives;
    private double hunger = 100.0;
    private double maxHunger = 100.0;
    
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
        if (invulnerabilityTimer > 0) invulnerabilityTimer -= deltaTime;
        if (adrenalineTimer > 0) adrenalineTimer -= deltaTime;
        if (burrowCooldown > 0) burrowCooldown -= deltaTime;
        if (attackCooldown > 0) attackCooldown -= deltaTime;

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

        // Trigger Attack (Spacebar or Left-Click)
        if (attackPressed && attackCooldown <= 0 && !isBurrowed && !isSwimming) {
            isAttacking = true;
            attackHitProcessed = false;
            attackTimer = 0.22;    // 0.22s slash duration
            attackCooldown = 0.40; // 0.40s cooldown between bites
            noiseRadius = 180.0;   // Attack makes sound
            hunger -= 0.8;         // Attacking exerts energy
        }

        checkBurrowInteraction(dx, dy, deltaTime, burrows);

        this.isSprinting = sprint && (hunger > 10.0) && !isBurrowed;

        if (dx != 0 || dy != 0) {
            faceDx = dx;
            faceDy = dy;
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

        if (adrenalineTimer > 0) {
            speedMultiplier *= 1.25;
        }

        boolean isMoving = (dx != 0 || dy != 0) && !isBurrowed;

        if (isSprinting && isMoving) {
            currentSpeed = baseSpeed * 1.55 * speedMultiplier;
            noiseRadius = 240.0;
            hunger -= 3.8 * deltaTime;
        } else if (isMoving) {
            currentSpeed = baseSpeed * speedMultiplier;
            noiseRadius = 100.0;
            hunger -= 1.4 * deltaTime;
        } else if (!isAttacking) {
            currentSpeed = 0;
            noiseRadius = 0.0;
            hunger -= 0.5 * deltaTime;
        }

        if (hunger < 0) hunger = 0;

        if (isMoving) {
            double nextX = x + dx * currentSpeed * deltaTime;
            double nextY = y + dy * currentSpeed * deltaTime;

            if (!isColliding(nextX, y, tileMap)) {
                x = nextX;
            }
            if (!isColliding(x, nextY, tileMap)) {
                y = nextY;
            }

            footprintTimer += deltaTime;
            if (footprintTimer >= 0.25) {
                footprintTimer = 0.0;
                if (!isSwimming && currentTile != TileType.MUD && !isBurrowed) {
                    scentTrail.add(new Footprint(x, y, 6.0));
                }
            }

            particleTimer += deltaTime;
            if (particleTimer >= 0.06) {
                particleTimer = 0.0;
                if (isSwimming) {
                    particles.add(new Particle(
                        x + (rand.nextDouble() * 12 - 6),
                        y + (rand.nextDouble() * 12 - 6),
                        -dx * 20 + (rand.nextDouble() * 10 - 5),
                        -dy * 20 + (rand.nextDouble() * 10 - 5),
                        0.4,
                        "#73B9EE",
                        rand.nextDouble() * 4 + 2
                    ));
                } else if (isSprinting) {
                    particles.add(new Particle(
                        x + (rand.nextDouble() * 8 - 4),
                        y + 8 + (rand.nextDouble() * 4),
                        -dx * 30 + (rand.nextDouble() * 16 - 8),
                        -dy * 30 + (rand.nextDouble() * 16 - 8),
                        0.35,
                        (currentTile == TileType.MUD) ? "#6E4E36" : "#A3906F",
                        rand.nextDouble() * 3 + 2
                    ));
                }
            }
        }
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
        if (invulnerabilityTimer > 0 || isBurrowed) {
            return false;
        }

        lives--;
        invulnerabilityTimer = 0.95; // Adrenaline invulnerability
        adrenalineTimer = 1.3;

        // Knockback away from attacker
        double dx = x - fromX;
        double dy = y - fromY;
        double dist = Math.hypot(dx, dy);
        if (dist > 0.001) {
            this.knockbackVx = (dx / dist) * 280.0;
            this.knockbackVy = (dy / dist) * 280.0;
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

    public double getX() { return x; }
    public double getY() { return y; }
    public double getFaceDx() { return faceDx; }
    public double getFaceDy() { return faceDy; }
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
