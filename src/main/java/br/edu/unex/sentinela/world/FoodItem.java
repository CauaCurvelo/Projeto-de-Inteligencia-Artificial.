package br.edu.unex.sentinela.world;

public class FoodItem {
    public static final double NUTRITION_GAIN = 8.0;
    public static final int SCORE_BONUS = 100;

    private final double x;
    private final double y;
    private final int col;
    private final int row;
    private final FoodVariant variant;
    private boolean eaten = false;

    public FoodItem(int col, int row) {
        this(col, row, FoodVariant.WILD_APPLE);
    }

    public FoodItem(int col, int row, FoodVariant variant) {
        this.col = col;
        this.row = row;
        this.variant = variant != null ? variant : FoodVariant.WILD_APPLE;
        this.x = (col * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0);
        this.y = (row * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getCol() { return col; }
    public int getRow() { return row; }
    public FoodVariant getVariant() { return variant; }
    public double getNutritionGain() { return variant.getNutritionGain(); }
    public double getStaminaGain() { return variant.getStaminaGain(); }
    public int getScoreBonus() { return variant.getScoreBonus(); }
    public boolean isEaten() { return eaten; }
    public void setEaten(boolean eaten) { this.eaten = eaten; }
}
