package br.edu.unex.sentinela.world;

public class MeatItem {
    private final double x;
    private final double y;
    private boolean eaten = false;
    private double lifeTime = 40.0; // Stays on ground for 40s

    public MeatItem(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void update(double deltaTime) {
        lifeTime -= deltaTime;
        if (lifeTime <= 0) {
            eaten = true;
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isEaten() { return eaten; }
    public void setEaten(boolean eaten) { this.eaten = eaten; }
}

