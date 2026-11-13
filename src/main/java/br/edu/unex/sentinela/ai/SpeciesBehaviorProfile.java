package br.edu.unex.sentinela.ai;

/**
 * Perfil ecológico e comportamental específico de cada espécie.
 * Diferencia dieta (carnívoros não buscam frutas), raio de ronda, ritmo de vigília
 * e distância de espaço pessoal para evitar comportamento homogêneo e aglomeração.
 */
public enum SpeciesBehaviorProfile {
    HUNTER(false, 0.0, 10.0, 19.0, 0.9, 2.1, 120.0, 42.0),
    HOUND(false, 0.0, 9.0, 17.0, 0.5, 1.3, 92.0, 36.0),
    WOLF(false, 0.0, 11.0, 23.0, 0.7, 1.7, 118.0, 38.0),
    BOAR(true, 155.0, 7.0, 15.0, 1.2, 2.8, 102.0, 38.0),
    BADGER(true, 115.0, 5.5, 11.5, 1.6, 3.2, 112.0, 38.0),
    BOSS(false, 0.0, 6.0, 12.0, 0.8, 1.8, 84.0, 40.0);

    private final boolean foragesFood;
    private final double foodSeekRadiusPx;
    private final double patrolMinTiles;
    private final double patrolMaxTiles;
    private final double pauseMinSec;
    private final double pauseMaxSec;
    private final double patrolPersonalSpacePx;
    private final double combatPersonalSpacePx;

    SpeciesBehaviorProfile(boolean foragesFood, double foodSeekRadiusPx,
                           double patrolMinTiles, double patrolMaxTiles,
                           double pauseMinSec, double pauseMaxSec,
                           double patrolPersonalSpacePx, double combatPersonalSpacePx) {
        this.foragesFood = foragesFood;
        this.foodSeekRadiusPx = foodSeekRadiusPx;
        this.patrolMinTiles = patrolMinTiles;
        this.patrolMaxTiles = patrolMaxTiles;
        this.pauseMinSec = pauseMinSec;
        this.pauseMaxSec = pauseMaxSec;
        this.patrolPersonalSpacePx = patrolPersonalSpacePx;
        this.combatPersonalSpacePx = combatPersonalSpacePx;
    }

    public static SpeciesBehaviorProfile of(EnemyType type, EnemySkin skin) {
        if (type == EnemyType.BEAR_BOSS) return BOSS;
        if (type == EnemyType.HUNTER) return HUNTER;
        if (type == EnemyType.HOUND) return HOUND;
        if (skin == EnemySkin.BOAR) return BOAR;
        if (skin == EnemySkin.BADGER) return BADGER;
        return WOLF;
    }

    public boolean canForageFood() { return foragesFood; }
    public double getFoodSeekRadiusPx() { return foodSeekRadiusPx; }
    public double getPatrolMinTiles() { return patrolMinTiles; }
    public double getPatrolMaxTiles() { return patrolMaxTiles; }
    public double getPauseMinSec() { return pauseMinSec; }
    public double getPauseMaxSec() { return pauseMaxSec; }
    public double getPatrolPersonalSpacePx() { return patrolPersonalSpacePx; }
    public double getCombatPersonalSpacePx() { return combatPersonalSpacePx; }
}
