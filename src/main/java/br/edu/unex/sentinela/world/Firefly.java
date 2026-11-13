package br.edu.unex.sentinela.world;

/**
 * Vagalume noturno com bioluminescência orgânica pulsante.
 * Flutua em biomas úmidos e florestais durante o crepúsculo e a noite.
 */
public class Firefly {
    private double x;
    private double y;
    private final double startX;
    private final double startY;
    private double floatZ;
    private final double phase;
    private final double pulseSpeed;
    private double lifeTime;
    private final BiomeType biome;

    public Firefly(double x, double y, BiomeType biome) {
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.biome = biome;
        this.phase = Math.random() * Math.PI * 2.0;
        this.pulseSpeed = 1.8 + Math.random() * 1.6;
        this.lifeTime = Math.random() * 10.0;
        this.floatZ = 3.0 + Math.random() * 4.0;
    }

    public void update(double dt) {
        lifeTime += dt;
        x = startX + Math.sin(lifeTime * 0.75 + phase) * 22.0 + Math.cos(lifeTime * 0.35 + phase * 0.5) * 14.0;
        y = startY + Math.cos(lifeTime * 0.65 + phase) * 18.0 + Math.sin(lifeTime * 0.28 + phase * 0.7) * 10.0;
        floatZ = 3.5 + Math.sin(lifeTime * 1.8 + phase) * 2.5;
    }

    public double getGlowAlpha() {
        double raw = Math.sin(lifeTime * pulseSpeed + phase);
        // Suaviza a curva para imitar o acender lento e apagar suave do vagalume
        return Math.max(0.0, Math.pow(Math.max(0.0, raw), 1.6));
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getFloatZ() { return floatZ; }
    public BiomeType getBiome() { return biome; }
}
