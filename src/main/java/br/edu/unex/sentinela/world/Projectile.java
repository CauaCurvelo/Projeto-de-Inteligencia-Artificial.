package br.edu.unex.sentinela.world;

public class Projectile {
    private double x;
    private double y;
    private final double vx;
    private final double vy;
    private final double speed = 320.0;
    private double distanceTraveled = 0;
    private final double maxDistance = 300.0;
    private boolean active = true;

    public Projectile(double startX, double startY, double targetX, double targetY) {
        this.x = startX;
        this.y = startY;
        double dx = targetX - startX;
        double dy = targetY - startY;
        double dist = Math.hypot(dx, dy);
        if (dist > 0.0001) {
            this.vx = (dx / dist) * speed;
            this.vy = (dy / dist) * speed;
        } else {
            this.vx = speed;
            this.vy = 0;
        }
    }

    public void update(double deltaTime, TileMap tileMap) {
        update(deltaTime, tileMap, 0.0, 0.0);
    }

    public void update(double deltaTime, TileMap tileMap, double windVx, double windVy) {
        if (!active) return;

        double stepX = (vx + windVx * 38.0) * deltaTime;
        double stepY = (vy + windVy * 38.0) * deltaTime;
        x += stepX;
        y += stepY;
        distanceTraveled += Math.hypot(stepX, stepY);

        if (distanceTraveled >= maxDistance) {
            active = false;
            return;
        }

        // Collision with trees or stone walls
        int c = (int) Math.floor(x / TileMap.TILE_SIZE);
        int r = (int) Math.floor(y / TileMap.TILE_SIZE);
        TileType tile = tileMap.getTileAt(c, r);
        if (tile == TileType.WALL || tile == TileType.TREE) {
            active = false;
        }
    }

    public boolean isActive() { return active; }
    public void deactivate() { this.active = false; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public double getAngle() { return Math.atan2(vy, vx); }
    public double getDistanceTraveled() { return distanceTraveled; }
    public double getMaxDistance() { return maxDistance; }
}

