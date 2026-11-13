package br.edu.unex.sentinela.world;

/**
 * Catálogo dos biomas procedurais do mundo de Sentinela.
 * Centraliza parâmetros ecológicos e identidade visual sem acoplar renderizadores ou IA.
 */
public enum BiomeType {
    MEADOW("Clareira Verde", "CLAREIRA VERDE", "#69F0AE", 0.0, 0.0, 0.56, 0.66, 0.08),
    SWAMP("Pântano Sombrio", "PÂNTANO SOMBRIO", "#26A69A", 0.14, 0.18, 0.52, 0.70, 0.03),
    DENSE_FOREST("Mata Fechada", "MATA FECHADA", "#00E676", -0.02, 0.04, 0.48, 0.57, 0.06),
    HIGHLAND("Serra Granítica", "SERRA GRANÍTICA", "#90CAF9", -0.06, -0.05, 0.63, 0.69, 0.24),
    SNOW("Tundra Nevada", "TUNDRA NEVADA", "#7DD3FC", 0.04, 0.05, 0.72, 0.66, 0.09);

    private final String displayName;
    private final String hudBadgeLabel;
    private final String accentColorHex;
    private final double waterBias;
    private final double mudBias;
    private final double tallGrassThreshold;
    private final double treeThreshold;
    private final double rockBias;

    BiomeType(String displayName, String hudBadgeLabel, String accentColorHex,
              double waterBias, double mudBias, double tallGrassThreshold,
              double treeThreshold, double rockBias) {
        this.displayName = displayName;
        this.hudBadgeLabel = hudBadgeLabel;
        this.accentColorHex = accentColorHex;
        this.waterBias = waterBias;
        this.mudBias = mudBias;
        this.tallGrassThreshold = tallGrassThreshold;
        this.treeThreshold = treeThreshold;
        this.rockBias = rockBias;
    }

    public String getDisplayName() { return displayName; }
    public String getHudBadgeLabel() { return hudBadgeLabel; }
    public String getAccentColorHex() { return accentColorHex; }
    public double getWaterBias() { return waterBias; }
    public double getMudBias() { return mudBias; }
    public double getTallGrassThreshold() { return tallGrassThreshold; }
    public double getTreeThreshold() { return treeThreshold; }
    public double getRockBias() { return rockBias; }
}
