package br.edu.unex.sentinela.audio;

import java.util.Random;

import static br.edu.unex.sentinela.audio.AcousticResonator.*;
import static br.edu.unex.sentinela.audio.DspToolkit.*;

/**
 * Síntese vocal animal e humana baseada em excitação glotal fisiológica,
 * ressonadores formânticos (F1/F2/F3), sopro pulmonar e sub-harmônicos orgânicos.
 */
final class CreatureVoiceSynthesizer {
    private CreatureVoiceSynthesizer() {}

    static boolean supports(SoundId id) {
        switch (id) {
            case DOG_BARK:
            case WOLF_HOWL:
            case WOLF_SNARL:
            case BOAR_GRUNT:
            case BADGER_HISS:
            case BEAR_ROAR:
            case PANTHER_ROAR:
            case CROC_HISS:
            case HUNTER_SHOUT:
            case SNIFF:
                return true;
            default:
                return false;
        }
    }

    static float[][] createVariants(SoundId id) {
        int count = (id == SoundId.WOLF_HOWL || id == SoundId.BEAR_ROAR
                || id == SoundId.PANTHER_ROAR || id == SoundId.CROC_HISS) ? 2 : 3;
        float[][] out = new float[count][];
        for (int v = 0; v < count; v++) {
            Random rng = new Random(91_813L * (id.ordinal() + 1) + v * 6151L);
            out[v] = synthesize(id, v, rng);
        }
        return out;
    }

    private static float[] synthesize(SoundId id, int v, Random rng) {
        switch (id) {
            case DOG_BARK:     return dogBark(v, rng);
            case WOLF_HOWL:    return wolfHowl(v, rng);
            case WOLF_SNARL:   return wolfSnarl(v, rng);
            case BOAR_GRUNT:   return boarGrunt(v, rng);
            case BADGER_HISS:  return badgerHiss(v, rng);
            case BEAR_ROAR:    return bearRoar(v, rng);
            case PANTHER_ROAR: return pantherRoar(v, rng);
            case CROC_HISS:    return crocHiss(v, rng);
            case HUNTER_SHOUT: return hunterShout(v, rng);
            case SNIFF:        return sniff(v, rng);
            default:           return buffer(0.1);
        }
    }

    private static float[] dogBark(int v, Random rng) {
        float[] out = buffer(0.48);
        for (int k = 0; k < 2; k++) {
            double dur = (k == 0 ? 0.135 : 0.12) + v * 0.005;
            double fBase = (k == 0 ? 255.0 : 280.0) + v * 14.0;
            float[] exc = vocalExcitation(dur, fBase, fBase * 1.48, fBase * 0.74, 0.24, 18.0, 0.025, 0.018, 0.18, 0.22, rng);
            float[] bark = applyFormants(exc, 495 + v * 18, 3.0, 1.0, 1180, 3.6, 0.65, 2260, 4.0, 0.26, 0.35);
            envelope(bark, 0.008, 2.2);
            mixInto(out, bark, k * (0.185 + v * 0.01), k == 0 ? 1.0 : 0.86);
        }
        biquadHighPass(out, 95, 0.7);
        biquadLowPass(out, 3400, 0.7);
        airSpace(out, 0.22);
        return normalize(edgeFade(out, 0.004, 0.03), 0.72);
    }

    private static float[] wolfHowl(int v, Random rng) {
        double dur = 1.95;
        double f0 = 235.0 + v * 18.0;
        float[] exc = vocalExcitation(dur, f0, f0 * 1.78, f0 * 1.15, 0.36, 4.6, 0.018, 0.009, 0.04, 0.18, rng);
        float[] howl = applyFormants(exc, 385, 4.2, 1.0, 860, 4.6, 0.55, 1960, 5.0, 0.18, 0.28);
        for (int i = 0; i < howl.length; i++) {
            double u = (double) i / howl.length;
            double swell = Math.sin(Math.PI * Math.pow(u, 0.72));
            howl[i] *= (float) Math.max(0.0, swell);
        }
        biquadHighPass(howl, 120, 0.7);
        biquadLowPass(howl, 2900, 0.7);
        airSpace(howl, 0.36);
        return normalize(edgeFade(howl, 0.04, 0.12), 0.54);
    }

    private static float[] wolfSnarl(int v, Random rng) {
        double dur = 0.54;
        double f0 = 92.0 + v * 5.0;
        float[] exc = vocalExcitation(dur, f0 * 1.12, f0, f0 * 0.78, 0.35, 14.0, 0.04, 0.032, 0.54, 0.34, rng);
        float[] snarl = applyFormants(exc, 345, 3.0, 0.90, 920, 3.2, 0.85, 1820, 3.8, 0.38, 0.24);
        for (int i = 0; i < snarl.length; i++) {
            double sec = (double) i / SAMPLE_RATE;
            snarl[i] *= (float) (0.72 + 0.28 * Math.sin(2.0 * Math.PI * 6.5 * sec));
        }
        drive(envelope(snarl, 0.045, 1.3), 1.35);
        biquadHighPass(snarl, 60, 0.7);
        biquadLowPass(snarl, 2700, 0.7);
        airSpace(snarl, 0.20);
        return normalize(edgeFade(snarl, 0.01, 0.04), 0.62);
    }

    private static float[] boarGrunt(int v, Random rng) {
        float[] out = buffer(0.58);
        int hits = 2 + (v % 2);
        for (int k = 0; k < hits; k++) {
            double f0 = (106.0 - k * 9.0) + v * 6.0;
            float[] exc = vocalExcitation(0.145, f0 * 1.18, f0 * 0.92, f0 * 0.68, 0.32, 22.0, 0.05, 0.036, 0.66, 0.30, rng);
            float[] grunt = applyFormants(exc, 275, 3.2, 0.92, 760, 3.3, 0.82, 1620, 3.8, 0.30, 0.28);
            envelope(grunt, 0.012, 1.9);
            mixInto(out, grunt, k * (hits == 2 ? 0.20 : 0.155), k == 0 ? 1.0 : 0.85);
        }
        drive(out, 1.3);
        biquadHighPass(out, 60, 0.7);
        biquadLowPass(out, 2300, 0.7);
        airSpace(out, 0.18);
        return normalize(edgeFade(out, 0.008, 0.03), 0.64);
    }

    private static float[] badgerHiss(int v, Random rng) {
        double dur = 0.42;
        float[] puff = biquadBandPass(pinkNoise(dur, rng), 980 + v * 50, 2.0);
        float[] sibilance = biquadBandPass(pinkNoise(dur, rng), 1860 + v * 70, 2.5);
        float[] exc = vocalExcitation(0.13, 142, 118, 86, 0.3, 24, 0.05, 0.035, 0.55, 0.45, rng);
        float[] throat = applyFormants(exc, 440, 2.8, 0.85, 1180, 3.2, 0.6, 2050, 3.5, 0.18, 0.25);

        float[] out = buffer(dur);
        mixInto(out, envelope(throat, 0.01, 2.0), 0.0, 0.72);
        mixInto(out, envelope(puff, 0.025, 1.5), 0.015, 0.92);
        mixInto(out, envelope(sibilance, 0.030, 1.6), 0.020, 0.58);
        biquadHighPass(out, 110, 0.7);
        biquadLowPass(out, 3100, 0.75);
        airSpace(out, 0.18);
        return normalize(edgeFade(out, 0.008, 0.035), 0.48);
    }

    private static float[] bearRoar(int v, Random rng) {
        double dur = 1.15;
        double f0 = 94.0 + v * 6.0;
        float[] exc = vocalExcitation(dur, f0, f0 * 1.18, f0 * 0.60, 0.28, 11.0, 0.045, 0.038, 0.72, 0.38, rng);
        float[] roar = applyFormants(exc, 290, 2.7, 0.92, 690, 2.9, 0.88, 1480, 3.4, 0.40, 0.38);
        envelope(roar, 0.14, 1.35);
        drive(roar, 1.38);
        biquadHighPass(roar, 45, 0.7);
        biquadLowPass(roar, 2600, 0.7);
        airSpace(roar, 0.32);
        return normalize(edgeFade(roar, 0.025, 0.09), 0.84);
    }

    private static float[] pantherRoar(int v, Random rng) {
        double dur = 0.92;
        double f0 = 154.0 + v * 10.0;
        float[] exc = vocalExcitation(dur, f0, f0 * 1.34, f0 * 0.62, 0.24, 15.0, 0.05, 0.036, 0.68, 0.28, rng);
        float[] roar = applyFormants(exc, 450, 2.9, 1.0, 1140, 3.3, 0.74, 2320, 3.8, 0.28, 0.34);
        envelope(roar, 0.085, 1.45);
        drive(roar, 1.35);
        biquadHighPass(roar, 65, 0.7);
        biquadLowPass(roar, 3200, 0.7);
        airSpace(roar, 0.28);
        return normalize(edgeFade(roar, 0.015, 0.07), 0.80);
    }

    private static float[] crocHiss(int v, Random rng) {
        double dur = 0.86;
        float[] lowBreath = biquadBandPass(pinkNoise(dur, rng), 390 + v * 25, 1.7);
        float[] midBreath = biquadBandPass(pinkNoise(dur, rng), 1080 + v * 45, 2.0);
        float[] exc = vocalExcitation(dur, 56 + v * 3, 48, 36, 0.35, 9.0, 0.06, 0.04, 0.70, 0.35, rng);
        float[] gut = applyFormants(exc, 195, 2.8, 0.9, 480, 3.2, 0.55, 980, 3.6, 0.18, 0.55);

        float[] out = buffer(dur);
        mixInto(out, envelope(gut, 0.06, 1.3), 0.0, 0.85);
        mixInto(out, envelope(lowBreath, 0.05, 1.4), 0.02, 0.85);
        mixInto(out, envelope(midBreath, 0.06, 1.6), 0.03, 0.55);
        biquadHighPass(out, 40, 0.7);
        biquadLowPass(out, 2400, 0.7);
        airSpace(out, 0.24);
        return normalize(edgeFade(out, 0.015, 0.06), 0.62);
    }

    private static float[] hunterShout(int v, Random rng) {
        double dur = 0.42;
        double f0 = 156.0 + v * 11.0;
        float[] exc = vocalExcitation(dur, f0, f0 * 1.24, f0 * 0.78, 0.26, 5.2, 0.018, 0.015, 0.12, 0.18, rng);
        float[] voice = applyFormants(exc, 620, 3.8, 1.0, 1260, 4.2, 0.68, 2420, 4.5, 0.22, 0.25);
        envelope(voice, 0.022, 1.5);
        biquadHighPass(voice, 95, 0.7);
        biquadLowPass(voice, 3300, 0.7);
        airSpace(voice, 0.22);
        return normalize(edgeFade(voice, 0.008, 0.04), 0.58);
    }

    private static float[] sniff(int v, Random rng) {
        float[] out = buffer(0.46);
        for (int k = 0; k < 3; k++) {
            float[] air = pinkNoise(0.085, rng);
            float[] f1 = biquadBandPass(air, 720 + v * 40, 2.4);
            float[] f2 = biquadBandPass(air, 1740 + v * 65, 2.2);
            float[] burst = buffer(0.085);
            mixInto(burst, f1, 0, 0.85);
            mixInto(burst, f2, 0, 0.55);
            envelope(burst, 0.024, 1.6);
            mixInto(out, burst, k * 0.122 + v * 0.004, 0.72 + k * 0.14);
        }
        biquadLowPass(out, 2800, 0.7);
        return normalize(edgeFade(out, 0.006, 0.02), 0.36);
    }
}
