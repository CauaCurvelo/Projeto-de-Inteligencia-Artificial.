package br.edu.unex.sentinela.world;

/**
 * Ondulação concêntrica na superfície da água gerada pela movimentação
 * de criaturas, mergulhos de sapos, pegadas e gotas de chuva.
 */
public class WaterRipple {
    private final double x;
    private final double y;
    private final double maxRadius;
    private final double maxLifeTime;
    private double currentRadius;
    private double lifeTime;
    private double alpha;

    public WaterRipple(double x, double y) {
        this(x, y, 16.0, 0.75);
    }

    public WaterRipple(double x, double y, double maxRadius, double duration) {
        this.x = x;
        this.y = y;
        this.maxRadius = maxRadius;
        this.maxLifeTime = duration;
        this.currentRadius = 3.0;
        this.lifeTime = 0.0;
        this.alpha = 0.65;
    }

    public void update(double dt) {
        lifeTime += dt;
        double progress = Math.min(1.0, lifeTime / maxLifeTime);
        currentRadius = 3.0 + progress * (maxRadius - 3.0);
        alpha = Math.max(0.0, (1.0 - progress) * 0.60);
    }

    public boolean isExpired() {
        return lifeTime >= maxLifeTime;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return currentRadius; }
    public double getAlpha() { return alpha; }
}
