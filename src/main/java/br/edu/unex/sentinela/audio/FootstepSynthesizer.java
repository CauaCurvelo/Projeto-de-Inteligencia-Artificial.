package br.edu.unex.sentinela.audio;

import java.util.Random;

import static br.edu.unex.sentinela.audio.AcousticResonator.*;
import static br.edu.unex.sentinela.audio.DspToolkit.*;

/**
 * Síntese multicamada de passos orgânicos (thump corporal, textura de solo, grãos e ressonância)
 * com múltiplas variantes por superfície para eliminar repetição mecânica e chiado 8-bit.
 */
final class FootstepSynthesizer {
    private static final int VARIANTS = 4;

    private FootstepSynthesizer() {}

    static boolean supports(SoundId id) {
        return id.name().startsWith("STEP_");
    }

    static float[][] createVariants(SoundId id) {
        float[][] out = new float[VARIANTS][];
        for (int v = 0; v < VARIANTS; v++) {
            Random rng = new Random(31_337L * (id.ordinal() + 1) + v * 7919L);
            out[v] = synthesize(id, v, rng);
        }
        return out;
    }

    private static float[] synthesize(SoundId id, int v, Random rng) {
        switch (id) {
            case STEP_GRASS:      return grassStep(v, rng);
            case STEP_TALL_GRASS: return tallGrassStep(v, rng);
            case STEP_DIRT:       return dirtStep(v, rng);
            case STEP_MUD:        return mudStep(v, rng);
            case STEP_WATER:      return waterStep(v, rng);
            case STEP_STONE:      return stoneStep(v, rng);
            case STEP_SNOW:       return snowStep(v, rng);
            case STEP_BOOT:       return bootStep(v, rng);
            case STEP_PAW:        return pawStep(v, rng);
            case STEP_HOOF:       return hoofStep(v, rng);
            case STEP_HEAVY:      return heavyStep(v, rng);
            default:              return grassStep(v, rng);
        }
    }

    private static float[] grassStep(int v, Random rng) {
        double dur = 0.095 + v * 0.008;
        float[] out = mixInto(buffer(dur), thump(0.06, 92 + v * 4, 42, 35, 2.8), 0.0, 0.36);
        float[] blade = biquadBandPass(pinkNoise(0.08, rng), 980 + rng.nextDouble() * 240, 1.3);
        float[] tip = biquadBandPass(pinkNoise(0.06, rng), 1850 + rng.nextDouble() * 320, 1.7);
        mixInto(out, envelope(blade, 0.007, 2.2), 0.003, 0.72);
        mixInto(out, envelope(tip, 0.008, 2.6), 0.012, 0.34);
        biquadLowPass(out, 2900, 0.7);
        return normalize(edgeFade(out, 0.004, 0.014), 0.42);
    }

    private static float[] tallGrassStep(int v, Random rng) {
        double dur = 0.22 + (v % 2) * 0.02;
        float[] out = mixInto(buffer(dur), thump(0.06, 78 + v * 3, 40, 32, 3.0), 0.0, 0.22);
        for (int s = 0; s < 2; s++) {
            float[] swish = biquadBandPass(pinkNoise(0.11, rng), 780 + s * 620 + v * 55, 1.4);
            envelope(swish, 0.016, 1.9);
            mixInto(out, swish, s * (0.048 + rng.nextDouble() * 0.018), s == 0 ? 0.75 : 0.55);
        }
        biquadLowPass(out, 2500, 0.75);
        return normalize(edgeFade(out, 0.008, 0.025), 0.23);
    }

    private static float[] dirtStep(int v, Random rng) {
        double dur = 0.095 + (v % 2) * 0.012;
        float[] out = mixInto(buffer(dur), thump(0.050, 118 + v * 5, 50, 44, 3.2), 0.0, 0.38);
        float[] crunch = biquadBandPass(pinkNoise(0.065, rng), 920 + v * 75, 1.1);
        mixInto(out, envelope(crunch, 0.004, 2.4), 0.002, 0.82);
        scatterGrains(out, 10, 0.006, 0.056, 1050, 2250, 0.34, rng);
        biquadLowPass(out, 3100, 0.7);
        return normalize(edgeFade(out, 0.003, 0.014), 0.46);
    }

    private static float[] mudStep(int v, Random rng) {
        double dur = 0.20 + v * 0.012;
        float[] out = mixInto(buffer(dur), thump(0.08, 76 + v * 4, 34, 24, 2.4), 0.0, 0.50);
        float[] squelchLow = biquadBandPass(pinkNoise(0.14, rng), 290 + v * 25, 2.2);
        float[] squelchWet = biquadBandPass(pinkNoise(0.11, rng), 780 + v * 45, 1.9);
        mixInto(out, envelope(squelchLow, 0.014, 1.7), 0.008, 0.85);
        mixInto(out, envelope(squelchWet, 0.022, 1.9), 0.032, 0.52);
        for (int b = 0; b < 3; b++) {
            double f0 = 150 + rng.nextDouble() * 140;
            float[] bubble = organicBubble(0.028 + rng.nextDouble() * 0.014, f0, f0 * 1.9, 0.42, rng);
            mixInto(out, bubble, 0.035 + b * 0.034 + rng.nextDouble() * 0.012, 0.20);
        }
        biquadLowPass(out, 2100, 0.75);
        return normalize(edgeFade(out, 0.006, 0.024), 0.48);
    }

    private static float[] waterStep(int v, Random rng) {
        double dur = 0.24 + (v % 2) * 0.02;
        float[] out = mixInto(buffer(dur), thump(0.06, 86, 44, 30, 3.0), 0.0, 0.25);
        float[] wash = biquadBandPass(pinkNoise(0.18, rng), 620 + v * 50, 1.0);
        float[] spray = biquadBandPass(pinkNoise(0.13, rng), 1480 + v * 80, 1.2);
        mixInto(out, envelope(wash, 0.010, 2.1), 0.002, 0.78);
        mixInto(out, envelope(spray, 0.012, 2.4), 0.012, 0.42);
        for (int d = 0; d < 6; d++) {
            double f0 = 360 + rng.nextDouble() * 520;
            float[] drop = organicBubble(0.020 + rng.nextDouble() * 0.014, f0, f0 * 1.45, 0.52, rng);
            mixInto(out, drop, 0.018 + d * 0.028 + rng.nextDouble() * 0.014, 0.16);
        }
        biquadLowPass(out, 3200, 0.7);
        return normalize(edgeFade(out, 0.004, 0.024), 0.44);
    }

    private static float[] stoneStep(int v, Random rng) {
        double dur = 0.068;
        float[] out = mixInto(buffer(dur), thump(0.028, 175 + v * 12, 82, 65, 3.8), 0.0, 0.18);
        float[] exc = envelope(pinkNoise(0.036, rng), 0.001, 4.2);
        mixInto(out, biquadBandPass(exc, 740 + v * 45, 3.8), 0.0, 0.62);
        mixInto(out, biquadBandPass(exc, 1460 + v * 60, 4.2), 0.0, 0.52);
        scatterGrains(out, 5, 0.005, 0.026, 1300, 2400, 0.28, rng);
        biquadLowPass(out, 3200, 0.7);
        return normalize(edgeFade(out, 0.002, 0.010), 0.38);
    }

    private static float[] snowStep(int v, Random rng) {
        double dur = 0.145 + (v % 2) * 0.012;
        float[] out = mixInto(buffer(dur), thump(0.068, 72 + v * 3, 34, 24, 2.5), 0.0, 0.30);
        float[] pack = biquadBandPass(pinkNoise(0.11, rng), 940 + v * 65, 1.15);
        float[] iceFrost = biquadBandPass(pinkNoise(0.095, rng), 2150 + rng.nextDouble() * 340, 1.45);
        mixInto(out, envelope(pack, 0.010, 2.1), 0.004, 0.80);
        mixInto(out, envelope(iceFrost, 0.012, 2.4), 0.010, 0.56);
        scatterGrains(out, 12, 0.008, 0.088, 1300, 2700, 0.28, rng);
        biquadLowPass(out, 3100, 0.7);
        return normalize(edgeFade(out, 0.005, 0.020), 0.44);
    }

    private static float[] pawStep(int v, Random rng) {
        double dur = 0.075;
        float[] out = mixInto(buffer(dur), thump(0.045, 92 + v * 5, 46, 38, 3.0), 0.0, 0.36);
        float[] pad = biquadBandPass(pinkNoise(0.055, rng), 780 + rng.nextDouble() * 220, 1.1);
        mixInto(out, envelope(pad, 0.004, 2.5), 0.002, 0.72);
        biquadLowPass(out, 2500, 0.7);
        return normalize(edgeFade(out, 0.003, 0.012), 0.36);
    }

    private static float[] bootStep(int v, Random rng) {
        double dur = 0.12 + (v % 2) * 0.014;
        double fStart = 92 + v * 12;
        float[] out = mixInto(buffer(dur), thump(0.065, fStart, 42 + v * 2, 36, 2.9), 0.0, 0.42 + (v % 2) * 0.12);
        float[] heel = biquadBandPass(envelope(pinkNoise(0.045, rng), 0.002, 3.2), 420 + v * 35, 1.4);
        float[] toe = biquadBandPass(envelope(pinkNoise(0.065, rng), 0.004, 2.5), 1040 + rng.nextDouble() * 320, 1.2);
        mixInto(out, heel, 0.0, 0.62);
        mixInto(out, toe, 0.016 + v * 0.006, 0.68 + (1 - (v % 2)) * 0.14);
        scatterGrains(out, 7, 0.014, 0.070, 950, 2000, 0.24, rng);
        biquadLowPass(out, 2700, 0.7);
        return normalize(edgeFade(out, 0.003, 0.016), 0.52);
    }

    private static float[] hoofStep(int v, Random rng) {
        double dur = 0.085;
        float[] out = mixInto(buffer(dur), thump(0.036, 155 + v * 8, 68, 56, 3.6), 0.0, 0.26);
        float[] exc = envelope(pinkNoise(0.045, rng), 0.001, 4.0);
        mixInto(out, biquadBandPass(exc, 440 + v * 22, 3.4), 0.0, 0.62);
        mixInto(out, biquadBandPass(exc, 1120 + v * 40, 3.8), 0.0, 0.58);
        mixInto(out, biquadBandPass(exc, 1720, 3.6), 0.0, 0.28);
        scatterGrains(out, 5, 0.010, 0.036, 1100, 2200, 0.24, rng);
        biquadLowPass(out, 3000, 0.7);
        return normalize(edgeFade(out, 0.002, 0.012), 0.46);
    }

    private static float[] heavyStep(int v, Random rng) {
        double dur = 0.26 + (v % 2) * 0.02;
        float[] out = mixInto(buffer(dur), thump(0.22, 54 + v * 6, 26 + v * 1.5, 18, 2.2), 0.0, 0.72 + (v % 2) * 0.12);
        mixInto(out, thump(0.11, 108 + v * 10, 44, 30, 2.8), 0.004 + v * 0.003, 0.48);
        float[] crush = biquadBandPass(pinkNoise(0.18, rng), 250 + v * 35, 1.0);
        mixInto(out, envelope(crush, 0.010, 2.2), 0.006, 0.62);
        scatterGrains(out, 7, 0.015, 0.12, 420, 1250, 0.18, rng);
        biquadLowPass(out, 1600, 0.7);
        airSpace(out, 0.18);
        return normalize(edgeFade(out, 0.006, 0.035), 0.72);
    }

    private static void scatterGrains(
            float[] dest, int count, double startSec, double endSec,
            double minHz, double maxHz, double gain, Random rng
    ) {
        for (int i = 0; i < count; i++) {
            double when = startSec + rng.nextDouble() * Math.max(0.005, endSec - startSec);
            double f = minHz + rng.nextDouble() * (maxHz - minHz);
            float[] g = biquadBandPass(pinkNoise(0.012, rng), f, 2.2);
            envelope(g, 0.0015, 3.0);
            mixInto(dest, g, when, gain * (0.6 + 0.4 * rng.nextDouble()));
        }
    }
}
