package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.world.TileMap;

public class Player {
    private double x;
    private double y;
    private double speed = 200.0; // pixels per second
    
    // Direction the player is facing (for rendering)
    private double faceDx = 0;
    private double faceDy = 1;

    // Player bounding box size (slightly smaller than visual size to be forgiving)
    private double width = 24.0;
    private double height = 24.0;

    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }

    public void move(double dx, double dy, double deltaTime, TileMap tileMap) {
        if (dx != 0 || dy != 0) {
            faceDx = dx;
            faceDy = dy;
        }

        double nextX = x + dx * speed * deltaTime;
        double nextY = y + dy * speed * deltaTime;

        // Try moving in X
        if (!isColliding(nextX, y, tileMap)) {
            x = nextX;
        }
        
        // Try moving in Y
        if (!isColliding(x, nextY, tileMap)) {
            y = nextY;
        }
    }

    // AABB collision check against the TileMap
    private boolean isColliding(double nextX, double nextY, TileMap tileMap) {
        // Player's top-left corner is (x - width/2, y - height/2) because x, y is the center
        double left = nextX - width / 2;
        double right = nextX + width / 2;
        double top = nextY - height / 2;
        double bottom = nextY + height / 2;

        // Check the 4 corners of the bounding box
        return !tileMap.isWalkable(left, top) ||
               !tileMap.isWalkable(right, top) ||
               !tileMap.isWalkable(left, bottom) ||
               !tileMap.isWalkable(right, bottom);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getFaceDx() { return faceDx; }
    public double getFaceDy() { return faceDy; }
}
