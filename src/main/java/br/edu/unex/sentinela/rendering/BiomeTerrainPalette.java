package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.world.BiomeType;
import javafx.scene.paint.Color;

/**
 * Paletas procedurais de solo, água, lama e flora rasteira por bioma,
 * mantendo o cache de chunks rápido e desacoplado de condicionais espalhados.
 */
public final class BiomeTerrainPalette {
    public final double grassHueBase;
    public final double grassSatBase;
    public final double grassBriOpen;
    public final double grassBriShade;
    public final double tallGrassHue;
    public final double tallGrassSat;
    public final double dirtHue;
    public final double dirtSat;
    public final double dirtBri;
    public final double mudHue;
    public final double mudSat;
    public final double mudBri;

    public final Color grassEdge;
    public final Color tallGrassStem;
    public final Color tallGrassTipA;
    public final Color tallGrassTipB;
    public final Color waterDeep;
    public final Color waterShallow;
    public final Color waterAbyss;
    public final Color waterSandTint;
    public final Color waterFoam;
    public final Color waterRimLight;
    public final Color flowerPrimary;
    public final Color flowerSecondary;
    public final Color flowerCenter;
    public final Color cliffMossDark;
    public final Color cliffMossLight;
    public final double flowerDensityThreshold;

    private BiomeTerrainPalette(
            double grassHueBase, double grassSatBase, double grassBriOpen, double grassBriShade,
            double tallGrassHue, double tallGrassSat, double dirtHue, double dirtSat, double dirtBri,
            double mudHue, double mudSat, double mudBri,
            Color grassEdge, Color tallGrassStem, Color tallGrassTipA, Color tallGrassTipB,
            Color waterDeep, Color waterShallow, Color waterAbyss, Color waterSandTint,
            Color waterFoam, Color waterRimLight, Color flowerPrimary, Color flowerSecondary,
            Color flowerCenter, Color cliffMossDark, Color cliffMossLight, double flowerDensityThreshold) {
        this.grassHueBase = grassHueBase;
        this.grassSatBase = grassSatBase;
        this.grassBriOpen = grassBriOpen;
        this.grassBriShade = grassBriShade;
        this.tallGrassHue = tallGrassHue;
        this.tallGrassSat = tallGrassSat;
        this.dirtHue = dirtHue;
        this.dirtSat = dirtSat;
        this.dirtBri = dirtBri;
        this.mudHue = mudHue;
        this.mudSat = mudSat;
        this.mudBri = mudBri;
        this.grassEdge = grassEdge;
        this.tallGrassStem = tallGrassStem;
        this.tallGrassTipA = tallGrassTipA;
        this.tallGrassTipB = tallGrassTipB;
        this.waterDeep = waterDeep;
        this.waterShallow = waterShallow;
        this.waterAbyss = waterAbyss;
        this.waterSandTint = waterSandTint;
        this.waterFoam = waterFoam;
        this.waterRimLight = waterRimLight;
        this.flowerPrimary = flowerPrimary;
        this.flowerSecondary = flowerSecondary;
        this.flowerCenter = flowerCenter;
        this.cliffMossDark = cliffMossDark;
        this.cliffMossLight = cliffMossLight;
        this.flowerDensityThreshold = flowerDensityThreshold;
    }

    private static final BiomeTerrainPalette MEADOW = new BiomeTerrainPalette(
        104.0, 0.48, 0.46, 0.36, 108.0, 0.55,
        28.0, 0.46, 0.48, 25.0, 0.58, 0.28,
        Color.web("#3D732E", 0.85), Color.web("#1E4618"), Color.web("#4E8C3A"), Color.web("#66A64F"),
        Color.web("#124675"), Color.web("#1B639E"), Color.web("#0D3357", 0.38), Color.web("#2980B9", 0.24),
        Color.web("#E1F5FE", 0.42), Color.web("#81D4FA", 0.35),
        Color.web("#FFF59D"), Color.web("#E1BEE7"), Color.web("#FF8F00"),
        Color.web("#2E5A27", 0.85), Color.web("#689F38", 0.85), 0.86
    );

    private static final BiomeTerrainPalette SWAMP = new BiomeTerrainPalette(
        88.0, 0.43, 0.36, 0.28, 92.0, 0.48,
        36.0, 0.36, 0.34, 42.0, 0.44, 0.22,
        Color.web("#2F4D2A", 0.85), Color.web("#1D331B"), Color.web("#557A39"), Color.web("#7B8C45"),
        Color.web("#133831"), Color.web("#1B4C40"), Color.web("#0D2B24", 0.36), Color.web("#2E6B56", 0.22),
        Color.web("#A7FFEB", 0.28), Color.web("#4DB6AC", 0.30),
        Color.web("#F8BBD0"), Color.web("#DCEDC8"), Color.web("#6D4C41"),
        Color.web("#1B4332", 0.90), Color.web("#40916C", 0.85), 0.82
    );

    private static final BiomeTerrainPalette DENSE_FOREST = new BiomeTerrainPalette(
        128.0, 0.54, 0.35, 0.26, 134.0, 0.62,
        22.0, 0.50, 0.36, 20.0, 0.60, 0.24,
        Color.web("#1B5E32", 0.88), Color.web("#0E3B1C"), Color.web("#2E7D32"), Color.web("#00C853"),
        Color.web("#0E3D42"), Color.web("#155E63"), Color.web("#072629", 0.36), Color.web("#26A69A", 0.22),
        Color.web("#B2EBF2", 0.36), Color.web("#4DD0E1", 0.32),
        Color.web("#FF4081"), Color.web("#40C4FF"), Color.web("#FFF59D"),
        Color.web("#145A32", 0.92), Color.web("#00E676", 0.72), 0.80
    );

    private static final BiomeTerrainPalette HIGHLAND = new BiomeTerrainPalette(
        74.0, 0.36, 0.48, 0.38, 78.0, 0.40,
        32.0, 0.24, 0.52, 28.0, 0.32, 0.34,
        Color.web("#6C7A46", 0.84), Color.web("#47542A"), Color.web("#8D9952"), Color.web("#B0B865"),
        Color.web("#155085"), Color.web("#2170AD"), Color.web("#0D365C", 0.36), Color.web("#64B5F6", 0.24),
        Color.web("#E3F2FD", 0.48), Color.web("#90CAF9", 0.38),
        Color.web("#FFD54F"), Color.web("#90CAF9"), Color.web("#FFF8E1"),
        Color.web("#455A64", 0.72), Color.web("#90A4AE", 0.68), 0.85
    );

    private static final BiomeTerrainPalette SNOW = new BiomeTerrainPalette(
        204.0, 0.11, 0.84, 0.68, 198.0, 0.30,
        212.0, 0.18, 0.44, 206.0, 0.24, 0.32,
        Color.web("#D8E7F2", 0.88), Color.web("#475569"), Color.web("#7DD3FC"), Color.web("#E0F2FE"),
        Color.web("#0369A1"), Color.web("#0EA5E9"), Color.web("#0C4A6E", 0.36), Color.web("#7DD3FC", 0.28),
        Color.web("#F0F9FF", 0.60), Color.web("#BAE6FD", 0.48),
        Color.web("#BAE6FD"), Color.web("#F8FAFC"), Color.web("#7DD3FC"),
        Color.web("#64748B", 0.78), Color.web("#E0F2FE", 0.86), 0.89
    );

    public static BiomeTerrainPalette of(BiomeType biome) {
        if (biome == null) return MEADOW;
        switch (biome) {
            case SWAMP: return SWAMP;
            case DENSE_FOREST: return DENSE_FOREST;
            case HIGHLAND: return HIGHLAND;
            case SNOW: return SNOW;
            case MEADOW:
            default: return MEADOW;
        }
    }
}
