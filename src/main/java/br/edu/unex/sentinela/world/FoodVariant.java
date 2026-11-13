package br.edu.unex.sentinela.world;

import java.util.Random;

/**
 * Variações regionais de frutas e bagas silvestres.
 * Mantém a nutrição equilibrada (8% a 12%) para preservar o valor estratégico da carne caçada,
 * enquanto diferencia cada bioma visualmente e restaura parte da stamina ao forragear.
 */
public enum FoodVariant {
    WILD_APPLE(
        "Maçã Silvestre",
        BiomeType.MEADOW,
        8.0, 6.0, 100,
        "#E53935", "#FF8A80", "#1E4620", "#43A047"
    ),
    GOLDEN_HONEYBERRY(
        "Amora Dourada",
        BiomeType.MEADOW,
        10.0, 15.0, 135,
        "#FFB300", "#FFF59D", "#2E5A27", "#66BB6A"
    ),
    SWAMP_CRANBERRY(
        "Oxicoco do Brejo",
        BiomeType.SWAMP,
        9.0, 12.0, 120,
        "#8E24AA", "#EA80FC", "#1B3B2B", "#4DB6AC"
    ),
    GLOW_MYRTLE(
        "Mirtilo Luminescente",
        BiomeType.DENSE_FOREST,
        11.0, 18.0, 140,
        "#00ACC1", "#84FFFF", "#103A20", "#26C6DA"
    ),
    HIGHLAND_ROWAN(
        "Sorveira Serrana",
        BiomeType.HIGHLAND,
        12.0, 14.0, 150,
        "#F4511E", "#FFCC80", "#2F4538", "#81A684"
    ),
    FROST_BERRY(
        "Baga Glacial",
        BiomeType.SNOW,
        9.5, 22.0, 145,
        "#38BDF8", "#E0F7FA", "#1F3F47", "#5F8D96"
    );

    private final String displayName;
    private final BiomeType primaryBiome;
    private final double nutritionGain;
    private final double staminaGain;
    private final int scoreBonus;
    private final String berryColorHex;
    private final String highlightColorHex;
    private final String bushDarkHex;
    private final String bushLightHex;

    FoodVariant(String displayName, BiomeType primaryBiome, double nutritionGain,
                double staminaGain, int scoreBonus, String berryColorHex,
                String highlightColorHex, String bushDarkHex, String bushLightHex) {
        this.displayName = displayName;
        this.primaryBiome = primaryBiome;
        this.nutritionGain = nutritionGain;
        this.staminaGain = staminaGain;
        this.scoreBonus = scoreBonus;
        this.berryColorHex = berryColorHex;
        this.highlightColorHex = highlightColorHex;
        this.bushDarkHex = bushDarkHex;
        this.bushLightHex = bushLightHex;
    }

    /** Seleciona a fruta predominante do bioma com chance menor de variedade rara. */
    public static FoodVariant pickForBiome(BiomeType biome, Random rand) {
        double roll = rand != null ? rand.nextDouble() : 0.0;
        if (biome == null) return WILD_APPLE;
        switch (biome) {
            case SWAMP:
                return roll < 0.84 ? SWAMP_CRANBERRY : WILD_APPLE;
            case DENSE_FOREST:
                return roll < 0.84 ? GLOW_MYRTLE : GOLDEN_HONEYBERRY;
            case HIGHLAND:
                return roll < 0.82 ? HIGHLAND_ROWAN : GOLDEN_HONEYBERRY;
            case SNOW:
                return roll < 0.86 ? FROST_BERRY : GOLDEN_HONEYBERRY;
            case MEADOW:
            default:
                return roll < 0.78 ? WILD_APPLE : GOLDEN_HONEYBERRY;
        }
    }

    public String getDisplayName() { return displayName; }
    public BiomeType getPrimaryBiome() { return primaryBiome; }
    public double getNutritionGain() { return nutritionGain; }
    public double getStaminaGain() { return staminaGain; }
    public int getScoreBonus() { return scoreBonus; }
    public String getBerryColorHex() { return berryColorHex; }
    public String getHighlightColorHex() { return highlightColorHex; }
    public String getBushDarkHex() { return bushDarkHex; }
    public String getBushLightHex() { return bushLightHex; }
}
