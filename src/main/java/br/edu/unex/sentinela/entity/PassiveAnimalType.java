package br.edu.unex.sentinela.entity;

/**
 * Tipos de fauna passiva silvestre:
 * - RABBIT: rápido e arisco em zigue-zague
 * - DEER: vigilante com arrancada veloz de longa distância
 * - FROG: saltitante anfíbio em brejos e lagoas
 * - BIRD: debica em terra e alça voo ao sinal de perigo
 */
public enum PassiveAnimalType {
    RABBIT("Coelho Silvestre", 1, 115.0, 140.0, 1),
    DEER("Cervo da Floresta", 2, 160.0, 230.0, 2),
    FROG("Sapo do Brejo", 1, 55.0, 95.0, 1),
    BIRD("Pássaro Campestre", 1, 135.0, 115.0, 1);

    private final String displayName;
    private final int maxHp;
    private final double baseSpeed;
    private final double perceptionRadius;
    private final int morselDropCount;

    PassiveAnimalType(String displayName, int maxHp, double baseSpeed, double perceptionRadius, int morselDropCount) {
        this.displayName = displayName;
        this.maxHp = maxHp;
        this.baseSpeed = baseSpeed;
        this.perceptionRadius = perceptionRadius;
        this.morselDropCount = morselDropCount;
    }

    public String getDisplayName() { return displayName; }
    public int getMaxHp() { return maxHp; }
    public double getBaseSpeed() { return baseSpeed; }
    public double getPerceptionRadius() { return perceptionRadius; }
    public int getMorselDropCount() { return morselDropCount; }
}
