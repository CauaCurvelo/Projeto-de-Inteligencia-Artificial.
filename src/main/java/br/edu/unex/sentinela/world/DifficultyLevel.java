package br.edu.unex.sentinela.world;

public enum DifficultyLevel {
    EASY("Fácil", 180.0, 4, 0.85, 0.85),       // 3 minutes, 4 lives, slower enemies
    NORMAL("Normal", 150.0, 3, 1.0, 1.0),      // 2.5 minutes, 3 lives, standard
    HARD("Difícil", 120.0, 2, 1.25, 1.20);     // 2 minutes, 2 lives, faster & sharper enemies

    private final String displayName;
    private final double totalTime;
    private final int maxLives;
    private final double enemySpeedMultiplier;
    private final double detectionMultiplier;

    DifficultyLevel(String displayName, double totalTime, int maxLives, double enemySpeedMultiplier, double detectionMultiplier) {
        this.displayName = displayName;
        this.totalTime = totalTime;
        this.maxLives = maxLives;
        this.enemySpeedMultiplier = enemySpeedMultiplier;
        this.detectionMultiplier = detectionMultiplier;
    }

    public String getDisplayName() { return displayName; }
    public double getTotalTime() { return totalTime; }
    public int getMaxLives() { return maxLives; }
    public double getEnemySpeedMultiplier() { return enemySpeedMultiplier; }
    public double getDetectionMultiplier() { return detectionMultiplier; }
}

