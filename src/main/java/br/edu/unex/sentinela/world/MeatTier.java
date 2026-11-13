package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.ai.EnemyType;

/**
 * Categorias nutricionais e visuais das carnes deixadas por inimigos derrotados.
 * Centraliza os ganhos de fome, cura, pontuação bônus e tempo de permanência no solo.
 */
public enum MeatTier {
    GAME_MORSEL(8.0, 0, 75, 28.0, 0.65),
    SMALL_CUT(16.0, 1, 150, 38.0, 0.85),
    HUNTER_RATION(20.0, 1, 180, 42.0, 1.00),
    BEAST_HAUNCH(26.0, 1, 250, 42.0, 1.15),
    APEX_PRIME_CUT(45.0, 1, 500, 55.0, 1.45);

    private final double nutritionGain;
    private final int healHearts;
    private final int eatScoreBonus;
    private final double lifeTimeSeconds;
    private final double visualScale;

    MeatTier(double nutritionGain, int healHearts, int eatScoreBonus, double lifeTimeSeconds, double visualScale) {
        this.nutritionGain = nutritionGain;
        this.healHearts = healHearts;
        this.eatScoreBonus = eatScoreBonus;
        this.lifeTimeSeconds = lifeTimeSeconds;
        this.visualScale = visualScale;
    }

    public static MeatTier fromEnemy(EnemyType type) {
        if (type == null) {
            return SMALL_CUT;
        }
        switch (type) {
            case HUNTER:
                return HUNTER_RATION;
            case RIVAL:
                return BEAST_HAUNCH;
            case BEAR_BOSS:
                return APEX_PRIME_CUT;
            case HOUND:
            default:
                return SMALL_CUT;
        }
    }

    public double getNutritionGain() {
        return nutritionGain;
    }

    public int getHealHearts() {
        return healHearts;
    }

    public int getEatScoreBonus() {
        return eatScoreBonus;
    }

    public double getLifeTimeSeconds() {
        return lifeTimeSeconds;
    }

    public double getVisualScale() {
        return visualScale;
    }
}
