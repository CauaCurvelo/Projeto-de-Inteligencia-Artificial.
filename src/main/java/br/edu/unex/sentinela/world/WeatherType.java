package br.edu.unex.sentinela.world;

public enum WeatherType {
    CLEAR("Céu Limpo", 1.0, 1.0, 1.0),       // Standard
    RAIN("Chuva Florestal", 0.85, 0.60, 2.5),  // Dampens sound, washes scent 2.5x faster, slightly reduces sight
    FOG("Neblina Densa", 0.55, 1.10, 0.8),    // Drastically cuts vision cone range by 45%!
    STORM("Tempestade Elétrica", 0.70, 0.40, 3.8); // Tempestade com ventania extrema, chuva torrencial e raios

    private final String displayName;
    private final double visionMultiplier;
    private final double soundMultiplier;
    private final double scentWashMultiplier;

    WeatherType(String displayName, double visionMultiplier, double soundMultiplier, double scentWashMultiplier) {
        this.displayName = displayName;
        this.visionMultiplier = visionMultiplier;
        this.soundMultiplier = soundMultiplier;
        this.scentWashMultiplier = scentWashMultiplier;
    }

    public String getDisplayName() { return displayName; }
    public double getVisionMultiplier() { return visionMultiplier; }
    public double getSoundMultiplier() { return soundMultiplier; }
    public double getScentWashMultiplier() { return scentWashMultiplier; }
    public boolean isRaining() { return this == RAIN || this == STORM; }
}

