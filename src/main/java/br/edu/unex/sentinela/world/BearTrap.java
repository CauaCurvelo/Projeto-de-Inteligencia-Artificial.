package br.edu.unex.sentinela.world;

/**
 * Armadilha mecânica de mandíbulas de aço colocada por caçadores no solo.
 * Camuflada entre a relva, dispara ao contato imobilizando e ferindo qualquer criatura que a pisar.
 */
public class BearTrap {
    private final double x;
    private final double y;
    private final int col;
    private final int row;
    private boolean armed = true;
    private double sprungTimer = 0.0;

    public BearTrap(double x, double y) {
        this.x = x;
        this.y = y;
        this.col = (int) Math.floor(x / TileMap.TILE_SIZE);
        this.row = (int) Math.floor(y / TileMap.TILE_SIZE);
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
    public double getSprungTimer() { return sprungTimer; }
}
