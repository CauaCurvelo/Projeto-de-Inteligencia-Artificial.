package br.edu.unex.sentinela.world;

public class FoodItem {
    private final double x;
    private final double y;
    private final int col;
    private final int row;
    private boolean eaten = false;

    public FoodItem(int col, int row) {
        this.col = col;
        this.row = row;
        this.x = (col * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0);
        this.y = (row * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getCol() { return col; }
    public int getRow() { return row; }
    public boolean isEaten() { return eaten; }
    public void setEaten(boolean eaten) { this.eaten = eaten; }
}

