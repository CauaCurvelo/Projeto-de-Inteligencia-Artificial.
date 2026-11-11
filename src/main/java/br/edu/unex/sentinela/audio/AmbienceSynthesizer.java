package br.edu.unex.sentinela.audio;

import static br.edu.unex.sentinela.audio.DspToolkit.*;

/** Receitas dos loops de ambiente (vento, floresta, noite, rio, chuva, tempestade). */
final class AmbienceSynthesizer {
    private static final double LOOP_SEC = 6.0;

    private AmbienceSynthesizer() {}

    static float[] create(SoundId id) {
        switch (id) {
            case LOOP_WIND:       return wind(500, 0.35, 0.12);
            case LOOP_STORM_WIND: return wind(1100, 0.6, 0.3);
            case LOOP_FOREST:     return forest();
            case LOOP_NIGHT:      return night();
            case LOOP_RIVER:      return river();
            case LOOP_RAIN:       return rain();
            default:              return buffer(1);
        }
    }

    /** Vento: ruído filtrado com modulação lenta de amplitude (rajadas). */
    private static float[] wind(double cutoff, double peak, double gustRate) {
        float[] n = lowPass(lowPass(whiteNoise(LOOP_SEC + 1), cutoff), cutoff * 1.4);
        for (int i = 0; i < n.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            n[i] *= (float) (0.55 + 0.45 * Math.sin(2 * Math.PI * gustRate * t + Math.sin(t * 0.7)));
        }
        return normalize(loopFade(n, 1.0), peak);
    }

    /** Floresta de dia: folhagem suave contínua. */
    private static float[] forest() {
        float[] leaves = bandPass(whiteNoise(LOOP_SEC + 1), 1800, 6000);
        for (int i = 0; i < leaves.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            leaves[i] *= (float) (0.3 + 0.7 * Math.pow(Math.abs(Math.sin(t * 0.9) * Math.sin(t * 0.37)), 2));
        }
        return normalize(loopFade(leaves, 1.0), 0.18);
    }

    /** Noite: cama grave + coro denso de grilos. */
    private static float[] night() {
        float[] b = normalize(lowPass(whiteNoise(LOOP_SEC + 1), 250), 0.25);
        for (double t = 0; t < LOOP_SEC; t += 0.35 + Math.abs(rand()) * 0.4) {
            float[] c = DspToolkit.sweep(0.04, 4300 + rand() * 400, 4400);
            envelope(c, 0.004, 1);
            mixInto(b, c, t, 0.12);
        }
        return normalize(loopFade(b, 1.0), 0.3);
    }

    /** Rio: ruído marrom + borbulhas aleatórias. */
    private static float[] river() {
        float[] b = normalize(bandPass(brownNoise(LOOP_SEC + 1), 150, 1600), 1);
        mixInto(b, normalize(bandPass(whiteNoise(LOOP_SEC + 1), 1500, 4500), 0.25), 0, 1);
        for (double t = 0; t < LOOP_SEC; t += 0.05 + Math.abs(rand()) * 0.15) {
            double f = 500 + Math.abs(rand()) * 1200;
            mixInto(b, envelope(DspToolkit.sweep(0.03, f, f * 1.6), 0.002, 2), t, 0.15);
        }
        return normalize(loopFade(b, 1.0), 0.4);
    }

    /** Chuva: chiado + gotas granulares. */
    private static float[] rain() {
        float[] b = normalize(bandPass(whiteNoise(LOOP_SEC + 1), 600, 9000), 0.6);
        for (double t = 0; t < LOOP_SEC; t += 0.004 + Math.abs(rand()) * 0.02) {
            float[] drop = envelope(bandPass(whiteNoise(0.015), 2000, 8000), 0.0005, 3);
            mixInto(b, drop, t, 0.4 * Math.abs(rand()));
        }
        return normalize(loopFade(b, 1.0), 0.45);
    }
}
