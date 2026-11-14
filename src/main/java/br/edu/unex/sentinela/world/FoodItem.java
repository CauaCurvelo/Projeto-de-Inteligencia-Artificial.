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
    private double spawnProgress = 1.0;

    public FoodItem(int col, int row) {
        this(col, row, FoodVariant.WILD_APPLE, false);
    }

    public FoodItem(int col, int row, FoodVariant variant) {
        this(col, row, variant, false);
    }

    public FoodItem(int col, int row, FoodVariant variant, boolean animateSpawn) {
        this.col = col;
        this.row = row;
        this.variant = variant != null ? variant : FoodVariant.WILD_APPLE;
        this.x = (col * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0);
        this.y = (row * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0);
        this.spawnProgress = animateSpawn ? 0.0 : 1.0;
    }

    public void update(double dt) {
        if (spawnProgress < 1.0) {
            spawnProgress = Math.min(1.0, spawnProgress + dt * 2.2);
        }
    }

    public double getSpawnProgress() { return spawnProgress; }
    public boolean isPopping() { return spawnProgress < 1.0; }

    public double getPopScale() {
        if (spawnProgress >= 1.0) return 1.0;
        double t = Math.max(0.0, Math.min(1.0, spawnProgress));
        if (t < 0.65) {
            return (t / 0.65) * 1.20;
        } else {
            double settle = (t - 0.65) / 0.35;
            return 1.20 - (0.20 * Math.sin(settle * Math.PI * 0.5));
        }
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
