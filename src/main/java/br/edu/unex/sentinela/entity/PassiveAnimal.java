package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.world.MeatItem;
import br.edu.unex.sentinela.world.MeatTier;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import br.edu.unex.sentinela.world.WaterRipple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Entidade de fauna silvestre passiva e ágil.
 * Possui inteligência de alerta, fuga com desvios em zigue-zague e interação ambiental.
 */
public class PassiveAnimal {
    private static final Random RAND = new Random();

    public enum State {
        FORAGING,
        ALERT,
        FLEEING,
        FLYING_AWAY
    }

    private final PassiveAnimalType type;
    private double x;
    private double y;
    private double vx;
    private double vy;
    private double faceDx = 1.0;
    private double faceDy = 0.0;
    private int hp;

    private State state = State.FORAGING;
    private double stateTimer = 0.0;
    private double animPhase = 0.0;
    private double hopTimer = 0.0;
    private double hopHeight = 0.0;
    private double flightHeight = 0.0;
    private double zigZagTimer = 0.0;
    private double zigZagAngle = 0.0;
    private double rippleCooldown = 0.0;
    private boolean despawned = false;

    public PassiveAnimal(PassiveAnimalType type, double x, double y) {
        this.type = type != null ? type : PassiveAnimalType.RABBIT;
        this.x = x;
        this.y = y;
        this.hp = this.type.getMaxHp();
        this.stateTimer = 1.0 + RAND.nextDouble() * 2.0;
        this.faceDx = RAND.nextBoolean() ? 1.0 : -1.0;
        this.faceDy = (RAND.nextDouble() - 0.5) * 0.4;
    }

    public void update(double dt, Player player, List<Enemy> predators, TileMap tileMap, List<WaterRipple> ripples) {
        if (despawned) return;

        stateTimer -= dt;
        animPhase += dt * 6.0;
        rippleCooldown -= dt;

        // 1. Identificar a ameaça mais próxima (Jogador ou Predador)
        double nearestThreatDist = 99999.0;
        double threatX = 0;
        double threatY = 0;

        if (player != null && !player.isBurrowed()) {
            double dp = Math.hypot(player.getX() - x, player.getY() - y);
            if (dp < nearestThreatDist) {
                nearestThreatDist = dp;
                threatX = player.getX();
                threatY = player.getY();
            }
        }

        if (predators != null) {
            for (Enemy enemy : predators) {
                if (enemy.getHp() <= 0) continue;
                double de = Math.hypot(enemy.getX() - x, enemy.getY() - y);
                if (de < nearestThreatDist) {
                    nearestThreatDist = de;
                    threatX = enemy.getX();
                    threatY = enemy.getY();
                }
            }
        }

        // 2. Máquina de Estados da Presa
        boolean sensedThreat = nearestThreatDist <= type.getPerceptionRadius();

        if (state == State.FLYING_AWAY) {
            flightHeight += dt * 45.0;
            double flySpeed = type.getBaseSpeed() * 1.35;
            x += faceDx * flySpeed * dt;
            y += faceDy * flySpeed * dt;
            if (flightHeight >= 90.0) {
                despawned = true;
            }
            return;
        }

        if (sensedThreat) {
            if (type == PassiveAnimalType.BIRD) {
                // Pássaro bate asas e voa para longe
                state = State.FLYING_AWAY;
                double awayAngle = Math.atan2(y - threatY, x - threatX);
                faceDx = Math.cos(awayAngle);
                faceDy = Math.sin(awayAngle);
                return;
            } else {
                state = State.FLEEING;
            }
        } else if (state == State.FLEEING && stateTimer <= 0) {
            state = State.ALERT;
            stateTimer = 1.2 + RAND.nextDouble() * 1.5;
        } else if (state == State.ALERT && stateTimer <= 0) {
            state = State.FORAGING;
            stateTimer = 2.0 + RAND.nextDouble() * 3.0;
        }

        // 3. Comportamento Específico por Estado
        switch (state) {
            case FORAGING:
                updateForaging(dt, tileMap);
                break;
            case ALERT:
                vx *= 0.85;
                vy *= 0.85;
                if (sensedThreat) {
                    faceDx = Math.signum(threatX - x);
                }
                break;
            case FLEEING:
                updateFleeing(dt, threatX, threatY, tileMap);
                break;
            default:
                break;
        }

        // 4. Mecânica de Saltos (Coelhos e Sapos)
        if (type == PassiveAnimalType.RABBIT || type == PassiveAnimalType.FROG) {
            hopTimer += dt * (state == State.FLEEING ? 9.5 : 4.5);
            hopHeight = Math.max(0.0, Math.sin(hopTimer) * (type == PassiveAnimalType.RABBIT ? 7.0 : 4.5));
        }

        // 5. Aplicação de Movimento e Colisão
        moveWithCollision(dt, tileMap);

        // 6. Geração de Ondulações na Água
        if (ripples != null && rippleCooldown <= 0) {
            int col = (int) Math.floor(x / TileMap.TILE_SIZE);
            int row = (int) Math.floor(y / TileMap.TILE_SIZE);
            TileType curTile = tileMap.getTileAt(col, row);
            if (curTile == TileType.WATER) {
                double speedLen = Math.hypot(vx, vy);
                if (speedLen > 15.0 || hopHeight > 2.0) {
                    ripples.add(new WaterRipple(x, y, type == PassiveAnimalType.DEER ? 18.0 : 12.0, 0.70));
                    rippleCooldown = type == PassiveAnimalType.FROG ? 0.35 : 0.22;
                }
            }
        }
    }

    private void updateForaging(double dt, TileMap tileMap) {
        if (stateTimer <= 0) {
            stateTimer = 1.5 + RAND.nextDouble() * 2.5;
            if (RAND.nextDouble() < 0.45) {
                // Parada para debicar/pastar
                vx = 0;
                vy = 0;
            } else {
                // Caminhada lenta
                double ang = RAND.nextDouble() * Math.PI * 2.0;
                double speed = type.getBaseSpeed() * 0.30;
                vx = Math.cos(ang) * speed;
                vy = Math.sin(ang) * speed;
                faceDx = Math.signum(vx);
                faceDy = vy / speed;
            }
        }
    }

    private void updateFleeing(double dt, double threatX, double threatY, TileMap tileMap) {
        zigZagTimer -= dt;
        if (zigZagTimer <= 0) {
            zigZagTimer = 0.25 + RAND.nextDouble() * 0.20;
            // Zigue-zague angular arisco
            zigZagAngle = (RAND.nextDouble() - 0.5) * (type == PassiveAnimalType.RABBIT ? 1.4 : 0.8);
        }

        double baseAwayAngle = Math.atan2(y - threatY, x - threatX);
        double finalAngle = baseAwayAngle + zigZagAngle;

        double fleeSpeed = type.getBaseSpeed() * (type == PassiveAnimalType.DEER ? 1.25 : 1.10);
        vx = Math.cos(finalAngle) * fleeSpeed;
        vy = Math.sin(finalAngle) * fleeSpeed;

        if (Math.abs(vx) > 5.0) {
            faceDx = Math.signum(vx);
        }
        faceDy = vy / fleeSpeed;
    }

    private void moveWithCollision(double dt, TileMap tileMap) {
        double nextX = x + vx * dt;
        double nextY = y + vy * dt;

        boolean canWalkWater = (type == PassiveAnimalType.FROG);
        boolean isWalkableX = canWalkOnTile(tileMap, nextX, y, canWalkWater);
        boolean isWalkableY = canWalkOnTile(tileMap, x, nextY, canWalkWater);

        if (isWalkableX) {
            x = nextX;
        } else {
            vx = -vx * 0.5;
        }

        if (isWalkableY) {
            y = nextY;
        } else {
            vy = -vy * 0.5;
        }
    }

    private boolean canWalkOnTile(TileMap tileMap, double wx, double wy, boolean canWalkWater) {
        int col = (int) Math.floor(wx / TileMap.TILE_SIZE);
        int row = (int) Math.floor(wy / TileMap.TILE_SIZE);
        TileType t = tileMap.getTileAt(col, row);
        if (canWalkWater && t == TileType.WATER) return true;
        return t.isWalkable();
    }

    public boolean takeDamage(int amount) {
        hp -= amount;
        if (hp <= 0) {
            despawned = true;
            return true;
        }
        state = State.FLEEING;
        stateTimer = 4.0;
        return false;
    }

    public List<MeatItem> createMorselDrops() {
        List<MeatItem> list = new ArrayList<>();
        int count = type.getMorselDropCount();
        for (int i = 0; i < count; i++) {
            double ox = (RAND.nextDouble() - 0.5) * 12.0;
            double oy = (RAND.nextDouble() - 0.5) * 12.0;
            list.add(new MeatItem(x + ox, y + oy, MeatTier.GAME_MORSEL, null));
        }
        return list;
    }

    public PassiveAnimalType getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getFaceDx() { return faceDx; }
    public double getFaceDy() { return faceDy; }
    public double getAnimPhase() { return animPhase; }
    public double getHopHeight() { return hopHeight; }
    public double getFlightHeight() { return flightHeight; }
    public State getState() { return state; }
    public int getHp() { return hp; }
    public boolean isDespawned() { return despawned; }
}
