package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.entity.Enemy;

/**
 * Armadilha mecânica de mandíbulas de aço colocada por caçadores no solo.
 * Camuflada entre a relva, dispara ao contato imobilizando e ferindo qualquer criatura que a pisar.
 */
public class BearTrap {
    private final double x;
    private final double y;
    private final int col;
    private final int row;
    private final Enemy owner;
    private boolean armed = true;
    private double sprungTimer = 0.0;

    public BearTrap(double x, double y) {
        this(x, y, null);
    }

    public BearTrap(double x, double y, Enemy owner) {
        this.x = x;
        this.y = y;
        this.col = (int) Math.floor(x / TileMap.TILE_SIZE);
        this.row = (int) Math.floor(y / TileMap.TILE_SIZE);
        this.owner = owner;
    }

    public void update(double dt) {
        if (!armed) {
            sprungTimer += dt;
        }
    }

    public void snap() {
        this.armed = false;
        this.sprungTimer = 0.0;
    }

    public boolean isArmed() {
        return armed;
    }

    public boolean isExpired() {
        return !armed && sprungTimer >= 18.0;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getCol() { return col; }
    public int getRow() { return row; }
    public Enemy getOwner() { return owner; }
    public double getSprungTimer() { return sprungTimer; }
}
