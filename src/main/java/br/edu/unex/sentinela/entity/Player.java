package br.edu.unex.sentinela.entity;

public class Player {
    private double x;
    private double y;
    private double speed = 200.0; // pixels per second
    
    // Direction the player is facing (for rendering)
    private double faceDx = 0;
    private double faceDy = 1;

    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }

    public void move(double dx, double dy, double deltaTime) {
        if (dx != 0 || dy != 0) {
            faceDx = dx;
            faceDy = dy;
        }

        x += dx * speed * deltaTime;
        y += dy * speed * deltaTime;

        // Simple screen bounds collision (Assuming 800x600 and player size 32)
        if (x < 16) x = 16;
        if (x > 800 - 16) x = 800 - 16;
        if (y < 16) y = 16;
        if (y > 600 - 16) y = 600 - 16;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getFaceDx() { return faceDx; }
    public double getFaceDy() { return faceDy; }
}
