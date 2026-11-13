package br.edu.unex.sentinela.ai;

/**
 * Catálogo dos ataques exclusivos e diferenciados dos Chefes (Urso, Pantera e Crocodilo),
 * definindo janelas de preparação (windup), alcance, largura angular e impulso físico.
 */
public enum BossAttackType {
    BEAR_CLAW_SWIPE("Patada Brutal", 0.42, 0.30, 1.30, 82.0, 50.0, 115.0, 145.0, 265.0, false, false),
    BEAR_SEISMIC_SLAM("Patada Sísmica", 0.68, 0.36, 1.65, 94.0, 92.0, 360.0, 35.0, 370.0, true, false),
    PANTHER_DOUBLE_SLASH("Corte Duplo", 0.30, 0.38, 0.92, 72.0, 46.0, 100.0, 235.0, 230.0, false, true),
    PANTHER_SHADOW_POUNCE("Salto Predatório", 0.46, 0.34, 1.32, 185.0, 44.0, 75.0, 455.0, 300.0, false, false),
    CROC_AMBUSH_LUNGE("Bote de Mandíbula", 0.44, 0.34, 1.12, 98.0, 50.0, 72.0, 395.0, 265.0, false, false),
    CROC_DEATH_ROLL("Giro da Morte", 0.50, 0.44, 1.48, 78.0, 58.0, 360.0, 180.0, 325.0, true, false),
    POLAR_CLAW_SWIPE("Patada Glacial", 0.40, 0.30, 1.24, 86.0, 52.0, 118.0, 165.0, 275.0, false, false),
    POLAR_FROST_CRUSH("Impacto Glacial", 0.64, 0.36, 1.55, 98.0, 96.0, 360.0, 48.0, 380.0, true, false);

    private final String displayName;
    private final double windupDuration;
    private final double activeDuration;
    private final double cooldownDuration;
    private final double triggerRange;
    private final double hitRange;
    private final double hitHalfArcRad;
    private final double lungeImpulse;
    private final double playerKnockback;
    private final boolean areaAttack;
    private final boolean doubleHit;

    BossAttackType(String displayName, double windupDuration, double activeDuration, double cooldownDuration,
                   double triggerRange, double hitRange, double hitArcDeg, double lungeImpulse,
                   double playerKnockback, boolean areaAttack, boolean doubleHit) {
        this.displayName = displayName;
        this.windupDuration = windupDuration;
        this.activeDuration = activeDuration;
        this.cooldownDuration = cooldownDuration;
        this.triggerRange = triggerRange;
        this.hitRange = hitRange;
        this.hitHalfArcRad = Math.toRadians(hitArcDeg * 0.5);
        this.lungeImpulse = lungeImpulse;
        this.playerKnockback = playerKnockback;
        this.areaAttack = areaAttack;
        this.doubleHit = doubleHit;
    }

    public String getDisplayName() { return displayName; }
    public double getWindupDuration() { return windupDuration; }
    public double getActiveDuration() { return activeDuration; }
    public double getCooldownDuration() { return cooldownDuration; }
    public double getTriggerRange() { return triggerRange; }
    public double getHitRange() { return hitRange; }
    public double getHitHalfArcRad() { return hitHalfArcRad; }
    public double getLungeImpulse() { return lungeImpulse; }
    public double getPlayerKnockback() { return playerKnockback; }
    public boolean isAreaAttack() { return areaAttack; }
    public boolean isDoubleHit() { return doubleHit; }
}
