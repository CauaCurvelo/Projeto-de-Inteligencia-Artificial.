package br.edu.unex.sentinela.audio;

import static br.edu.unex.sentinela.audio.DspToolkit.*;

/** Receitas dos loops de ambiente (vento, floresta, noite, rio, chuva, tempestade). */
final class AmbienceSynthesizer {
    private static final double LOOP_SEC = 10.0;
    private static final double FADE_SEC = 1.5;

    private AmbienceSynthesizer() {}

    static float[] create(SoundId id) {
        switch (id) {
            case LOOP_WIND:       return wind(260, 0.022, 0.16, 0.10);
            case LOOP_STORM_WIND: return wind(380, 0.032, 0.22, 0.30);
            case LOOP_FOREST:     return forest();
            case LOOP_NIGHT:      return night();
            case LOOP_RIVER:      return river();
            case LOOP_RAIN:       return rain();
            default:              return buffer(1);
        }
    }

    /** Vento: ruído rosa profundamente filtrado com rajadas orgânicas lentas. */
    private static float[] wind(double cutoff, double targetRms, double maxPeak, double gustRate) {
        float[] n = smoothLowPass(pinkNoise(LOOP_SEC + FADE_SEC), cutoff, 3);
        for (int i = 0; i < n.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            n[i] *= (float) (0.60 + 0.40 * Math.sin(2 * Math.PI * gustRate * t + Math.sin(t * 0.55)));
        }
        return normalizeRms(loopFade(n, FADE_SEC), targetRms, maxPeak);
    }

    /** Floresta de dia: folhagem suave em médio-grave sem estática aguda. */
    private static float[] forest() {
        float[] leaves = smoothBandPass(pinkNoise(LOOP_SEC + FADE_SEC), 220, 1050, 3);
        for (int i = 0; i < leaves.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double swell = Math.sin(Math.PI * 0.20 * t) * Math.sin(t * 0.37);
            leaves[i] *= (float) (0.45 + 0.55 * swell * swell);
        }
        return normalizeRms(loopFade(leaves, FADE_SEC), 0.020, 0.15);
    }

    /** Noite: cama grave acolhedora com grilos esparsos suaves. */
    private static float[] night() {
        float[] b = normalizeRms(smoothLowPass(pinkNoise(LOOP_SEC + FADE_SEC), 180, 3), 0.018, 0.10);
        for (double t = 0.2; t < LOOP_SEC + FADE_SEC - 0.1; t += 0.45 + Math.abs(rand()) * 0.55) {
            float[] c = envelope(DspToolkit.sweep(0.038, 2900 + rand() * 200, 3100), 0.010, 1.4);
            mixInto(b, c, t, 0.032);
        }
        return normalizeRms(loopFade(b, FADE_SEC), 0.020, 0.16);
    }

    /** Rio: fluxo grave-médio quente com micro-borbulhas naturais. */
    private static float[] river() {
        float[] deep = normalizeRms(smoothBandPass(brownNoise(LOOP_SEC + FADE_SEC), 110, 800, 3), 0.030, 0.20);
        float[] shimmer = normalizeRms(smoothBandPass(pinkNoise(LOOP_SEC + FADE_SEC), 260, 900, 3), 0.012, 0.08);
        mixInto(deep, shimmer, 0, 1.0);
        for (double t = 0.05; t < LOOP_SEC + FADE_SEC - 0.05; t += 0.08 + Math.abs(rand()) * 0.18) {
            double f = 350 + Math.abs(rand()) * 420;
            mixInto(deep, envelope(DspToolkit.sweep(0.028, f, f * 1.4), 0.005, 2), t, 0.042);
        }
        return normalizeRms(loopFade(deep, FADE_SEC), 0.034, 0.24);
    }

    /** Chuva: ruído rosa quente sem agudos metálicos + gotas suavizadas. */
    private static float[] rain() {
        float[] b = normalizeRms(smoothBandPass(pinkNoise(LOOP_SEC + FADE_SEC), 180, 1900, 3), 0.036, 0.24);
        for (double t = 0.02; t < LOOP_SEC + FADE_SEC - 0.03; t += 0.012 + Math.abs(rand()) * 0.032) {
            float[] drop = envelope(smoothBandPass(pinkNoise(0.020), 650, 1900, 3), 0.002, 2.5);
            mixInto(b, drop, t, 0.09 * Math.abs(rand()));
        }
        return normalizeRms(loopFade(b, FADE_SEC), 0.040, 0.28);
    }
}
