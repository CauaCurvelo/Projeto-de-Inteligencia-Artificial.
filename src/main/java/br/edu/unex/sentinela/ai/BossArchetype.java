package br.edu.unex.sentinela.ai;

import br.edu.unex.sentinela.world.BiomeType;
import br.edu.unex.sentinela.world.TileType;

/**
 * Arquétipos comportamentais e estatísticos exclusivos de cada espécie de Chefe,
 * garantindo velocidades, áreas territoriais, limites de perseguição, domínios de bioma e arsenais distintos.
 */
public enum BossArchetype {
    GRIZZLY_BEAR(
        86.0, 128.0, 146.0,
        260.0, 460.0, 285.0, 0.55, 4.5, 0.0,
        BossAttackType.BEAR_CLAW_SWIPE, BossAttackType.BEAR_SEISMIC_SLAM,
        BiomeType.HIGHLAND,
        "#D97706", "#FF6D00", "#E67E22"
    ),
    SHADOW_PANTHER(
        122.0, 176.0, 198.0,
        310.0, 560.0, 315.0, 0.42, 4.0, 82.0,
        BossAttackType.PANTHER_DOUBLE_SLASH, BossAttackType.PANTHER_SHADOW_POUNCE,
        BiomeType.DENSE_FOREST,
        "#7C4DFF", "#FFD600", "#FFD700"
    ),
    RIVER_CROCODILE(
        76.0, 102.0, 118.0,
        250.0, 410.0, 275.0, 0.48, 4.8, 0.0,
        BossAttackType.CROC_AMBUSH_LUNGE, BossAttackType.CROC_DEATH_ROLL,
        BiomeType.SWAMP,
        "#00BFA5", "#00E676", "#00E676"
    ),
    POLAR_BEAR(
        92.0, 134.0, 152.0,
        270.0, 480.0, 295.0, 0.52, 4.2, 0.0,
        BossAttackType.POLAR_CLAW_SWIPE, BossAttackType.POLAR_FROST_CRUSH,
        BiomeType.SNOW,
        "#38BDF8", "#E0F7FA", "#7DD3FC"
    );

    private final double patrolSpeed;
    private final double chaseSpeed;
    private final double enragedSpeed;
    private final double territoryRadius;
    private final double leashRadius;
    private final double aggroRadius;
    private final double roarDuration;
    private final double specialCooldown;
    private final double specialMinRange;
    private final BossAttackType primaryAttack;
    private final BossAttackType specialAttack;
    private final BiomeType nativeBiome;
    private final String territoryColorHex;
    private final String accentColorHex;
    private final String particleColorHex;

    BossArchetype(double patrolSpeed, double chaseSpeed, double enragedSpeed,
                  double territoryRadius, double leashRadius, double aggroRadius,
                  double roarDuration, double specialCooldown, double specialMinRange,
                  BossAttackType primaryAttack, BossAttackType specialAttack,
                  BiomeType nativeBiome,
                  String territoryColorHex, String accentColorHex, String particleColorHex) {
        this.patrolSpeed = patrolSpeed;
        this.chaseSpeed = chaseSpeed;
        this.enragedSpeed = enragedSpeed;
        this.territoryRadius = territoryRadius;
        this.leashRadius = leashRadius;
        this.aggroRadius = aggroRadius;
        this.roarDuration = roarDuration;
        this.specialCooldown = specialCooldown;
        this.specialMinRange = specialMinRange;
        this.primaryAttack = primaryAttack;
        this.specialAttack = specialAttack;
        this.nativeBiome = nativeBiome;
        this.territoryColorHex = territoryColorHex;
        this.accentColorHex = accentColorHex;
        this.particleColorHex = particleColorHex;
    }

    public static BossArchetype fromSkin(EnemySkin skin) {
        if (skin == EnemySkin.PANTHER) return SHADOW_PANTHER;
        if (skin == EnemySkin.CROCODILE) return RIVER_CROCODILE;
        if (skin == EnemySkin.POLAR_BEAR) return POLAR_BEAR;
        return GRIZZLY_BEAR;
    }

    public static BossArchetype forBiome(BiomeType biome) {
        if (biome == BiomeType.SWAMP) return RIVER_CROCODILE;
        if (biome == BiomeType.DENSE_FOREST) return SHADOW_PANTHER;
        if (biome == BiomeType.SNOW) return POLAR_BEAR;
        return GRIZZLY_BEAR;
    }

    public EnemySkin toSkin() {
        if (this == SHADOW_PANTHER) return EnemySkin.PANTHER;
        if (this == RIVER_CROCODILE) return EnemySkin.CROCODILE;
        if (this == POLAR_BEAR) return EnemySkin.POLAR_BEAR;
        return EnemySkin.BEAR;
    }

    /** Calcula o modificador de velocidade no terreno próprio de cada espécie de chefe. */
    public double computeTerrainSpeedMultiplier(TileType tile) {
        if (tile == null) return 1.0;
        switch (this) {
            case RIVER_CROCODILE:
                if (tile == TileType.WATER) return 1.65;
                if (tile == TileType.MUD) return 1.25;
                if (tile == TileType.TALL_GRASS) return 0.92;
                return 1.0;
            case SHADOW_PANTHER:
                if (tile == TileType.TALL_GRASS) return 1.12;
                if (tile == TileType.DIRT_PATH) return 1.08;
                if (tile == TileType.MUD) return 0.72;
                if (tile == TileType.WATER) return 0.52;
                return 1.0;
            case POLAR_BEAR:
                if (tile == TileType.MUD) return 0.98; // adaptado à neve fofa e gelo fino
                if (tile == TileType.WATER) return 0.92; // excelente nadador glacial
                if (tile == TileType.DIRT_PATH) return 1.08;
                return 1.0;
            case GRIZZLY_BEAR:
            default:
                if (tile == TileType.DIRT_PATH) return 1.08;
                if (tile == TileType.MUD) return 0.80;
                if (tile == TileType.WATER) return 0.78;
                if (tile == TileType.TALL_GRASS) return 0.96;
                return 1.0;
        }
    }

    public double getPatrolSpeed() { return patrolSpeed; }
    public double getChaseSpeed() { return chaseSpeed; }
    public double getEnragedSpeed() { return enragedSpeed; }
    public double getTerritoryRadius() { return territoryRadius; }
    public double getLeashRadius() { return leashRadius; }
    public double getAggroRadius() { return aggroRadius; }
    public double getRoarDuration() { return roarDuration; }
    public double getSpecialCooldown() { return specialCooldown; }
    public double getSpecialMinRange() { return specialMinRange; }
    public BossAttackType getPrimaryAttack() { return primaryAttack; }
    public BossAttackType getSpecialAttack() { return specialAttack; }
    public BiomeType getNativeBiome() { return nativeBiome; }
    public String getTerritoryColorHex() { return territoryColorHex; }
    public String getAccentColorHex() { return accentColorHex; }
    public String getParticleColorHex() { return particleColorHex; }
}
