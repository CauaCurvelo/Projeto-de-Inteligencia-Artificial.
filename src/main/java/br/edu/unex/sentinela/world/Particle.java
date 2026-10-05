package br.edu.unex.sentinela.world;

public class Particle {
    private double x;
    private double y;
    private double vx;
    private double vy;
    private double life;
    private final double maxLife;
    private final String colorHex;
    private final double size;

    public Particle(double x, double y, double vx, double vy, double duration, String colorHex, double size) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = duration;
        this.maxLife = duration;
        this.colorHex = colorHex;
        this.size = size;
    }

    public void update(double deltaTime) {
        x += vx * deltaTime;
        y += vy * deltaTime;
        life -= deltaTime;
    }

    public boolean isExpired() {
        return life <= 0;
    }

    public double getAlpha() {
        return Math.max(0.0, life / maxLife);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getSize() { return size; }
    public String getColorHex() { return colorHex; }
}

