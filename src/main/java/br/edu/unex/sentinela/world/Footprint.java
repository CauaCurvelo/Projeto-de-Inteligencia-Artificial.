package br.edu.unex.sentinela.world;

public class Footprint {
    private final double x;
    private final double y;
    private double lifeTime; // in seconds
    private final double maxLifeTime;

    public Footprint(double x, double y, double duration) {
        this.x = x;
        this.y = y;
        this.lifeTime = duration;
        this.maxLifeTime = duration;
    }

    public void update(double deltaTime) {
        this.lifeTime -= deltaTime;
    }

    public boolean isExpired() {
        return lifeTime <= 0;
    }

    public double getAlpha() {
        return Math.max(0.0, lifeTime / maxLifeTime);
    }

    public double getX() { return x; }
    public double getY() { return y; }
}

