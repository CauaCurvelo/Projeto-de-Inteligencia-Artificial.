package br.edu.unex.sentinela.ai;

import br.edu.unex.sentinela.world.BiomeType;

import java.util.Random;

/**
 * Subespécies e adaptações regionais para animais e caçadores existentes,
 * diferenciando pelagem, nome exibido e traços sutis de sobrevivência por bioma.
 */
public enum AnimalVariant {
    STANDARD("Clássico", 1.00, 1.00, 0, false),
    MARSH("do Brejo", 0.99, 1.03, 0, true),
    JUNGLE("da Mata", 1.05, 1.06, 0, false),
    ALPINE("Serrano", 1.03, 1.00, 1, false),
    ARCTIC("Ártico", 1.05, 1.04, 1, true);

    private final String regionTag;
    private final double speedMultiplier;
    private final double visionMultiplier;
    private final int toughBeastBonusHp;
    private final boolean mudAdapted;

    AnimalVariant(String regionTag, double speedMultiplier, double visionMultiplier,
                  int toughBeastBonusHp, boolean mudAdapted) {
        this.regionTag = regionTag;
        this.speedMultiplier = speedMultiplier;
        this.visionMultiplier = visionMultiplier;
        this.toughBeastBonusHp = toughBeastBonusHp;
        this.mudAdapted = mudAdapted;
    }

    public static AnimalVariant forBiome(BiomeType biome, Random rand) {
        if (biome == null) return STANDARD;
        double roll = rand != null ? rand.nextDouble() : 0.0;
        switch (biome) {
            case SWAMP:
                return roll < 0.84 ? MARSH : STANDARD;
            case DENSE_FOREST:
                return roll < 0.84 ? JUNGLE : STANDARD;
            case HIGHLAND:
                return roll < 0.84 ? ALPINE : STANDARD;
            case SNOW:
                return roll < 0.88 ? ARCTIC : STANDARD;
            case MEADOW:
            default:
                return roll < 0.88 ? STANDARD : (roll < 0.94 ? JUNGLE : ALPINE);
        }
    }

    /** Retorna o nome adaptado da criatura para exibição na nameplate RPG. */
    public String resolveDisplayName(EnemySkin skin) {
        if (skin == null) return "Criatura";
        if (this == STANDARD) {
            return skin.getDisplayName();
        }
        switch (skin) {
            case HUNTER_SNIPER:
                if (this == MARSH) return "Caçador do Brejo";
                if (this == JUNGLE) return "Caçador Mateiro";
                if (this == ARCTIC) return "Caçador Ártico";
                return "Caçador Alpino";
            case HOUND_TRACKER:
                if (this == MARSH) return "Cão Pantaneiro";
                if (this == JUNGLE) return "Cão da Selva";
                if (this == ARCTIC) return "Husky Ártico";
                return "Cão Serrano";
            case BOAR:
                if (this == MARSH) return "Javali do Lodo";
                if (this == JUNGLE) return "Javali Pintado";
                if (this == ARCTIC) return "Javali das Neves";
                return "Javali Serrano";
            case WOLF:
                if (this == MARSH) return "Lobo do Mangue";
                if (this == JUNGLE) return "Lobo da Selva";
                if (this == ARCTIC) return "Lobo Branco";
                return "Lobo Serrano";
            case BADGER:
                if (this == MARSH) return "Texugo do Brejo";
                if (this == JUNGLE) return "Texugo da Sombra";
                if (this == ARCTIC) return "Texugo Polar";
                return "Texugo de Granito";
            default:
                return skin.getDisplayName();
        }
    }

    public int getBonusHpFor(EnemyType type, EnemySkin skin) {
        if (toughBeastBonusHp <= 0 || type != EnemyType.RIVAL) {
            return 0;
        }
        return (skin == EnemySkin.BOAR || skin == EnemySkin.BADGER) ? toughBeastBonusHp : 0;
    }

    public String getRegionTag() { return regionTag; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public double getVisionMultiplier() { return visionMultiplier; }
    public boolean isMudAdapted() { return mudAdapted; }
}
