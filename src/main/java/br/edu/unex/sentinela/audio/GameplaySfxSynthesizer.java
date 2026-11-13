package br.edu.unex.sentinela.audio;

import java.util.Random;

import static br.edu.unex.sentinela.audio.AcousticResonator.*;
import static br.edu.unex.sentinela.audio.DspToolkit.*;

/**
 * Síntese orgânica dos efeitos de combate, coleta, ações de sobrevivência, UI e fauna pontual.
 */
final class GameplaySfxSynthesizer {
    private GameplaySfxSynthesizer() {}

    static float[][] createVariants(SoundId id) {
        int count = variantCount(id);
        float[][] out = new float[count][];
        for (int v = 0; v < count; v++) {
            Random rng = new Random(55_217L * (id.ordinal() + 1) + v * 4999L);
            out[v] = synthesize(id, v, rng);
        }
        return out;
    }

    private static int variantCount(SoundId id) {
        switch (id) {
            case BITE_WHOOSH:
            case HIT_IMPACT:
            case BOW_SHOT:
            case EAT_APPLE:
            case EAT_MEAT:
            case LEAVES_RUSTLE:
            case BIRD_CHIRP:
            case CRICKET:
                return 3;
            case ARROW_THUD:
            case OWL_HOOT:
                return 2;
            default:
                return 1;
        }
    }

    private static float[] synthesize(SoundId id, int v, Random rng) {
        switch (id) {
            case BITE_WHOOSH:  return biteWhoosh(v, rng);
            case HIT_IMPACT:   return hitImpact(v, rng);
            case PLAYER_HURT:  return playerHurt(rng);
            case ENEMY_DEATH:  return enemyDeath(rng);
            case BOW_SHOT:     return bowShot(v, rng);
            case ARROW_THUD:   return arrowThud(v, rng);
            case EAT_APPLE:    return eatApple(v, rng);
            case EAT_MEAT:     return eatMeat(v, rng);
            case HEAL:         return acousticArpeggio(new int[]{72, 76, 79, 84}, 0.065, 0.36, true);
            case BURROW_ENTER: return burrow(true, rng);
            case BURROW_EXIT:  return burrow(false, rng);
            case SPLASH:       return waterSplash(rng);
            case LEAVES_RUSTLE:return rustle(0.38, 0.24, rng);
            case HEARTBEAT:    return heartbeat();
            case HUNGER_GROWL: return hungerGrowl(rng);
            case ALERT_STINGER:return alertStinger(rng);
            case LOST_TARGET:  return lostTarget();
            case DAY_CHIME:    return acousticArpeggio(new int[]{67, 72, 76, 79}, 0.14, 0.34, false);
            case WEATHER_SHIFT:return normalizeRms(envelope(smoothBandPass(pinkNoise(1.5, rng), 220, 950, 2), 0.6, 1.5), 0.028, 0.16);
            case GAME_OVER:    return acousticArpeggio(new int[]{67, 64, 60, 55}, 0.25, 0.44, false);
            case UI_CLICK:     return woodClick();
            case ACHIEVEMENT_UNLOCKED: return acousticArpeggio(new int[]{72, 76, 79, 84, 88}, 0.075, 0.46, true);
            case BIRD_CHIRP:   return birdChirp(v, rng);
            case OWL_HOOT:     return owlHoot(v, rng);
            case CRICKET:      return cricket(v, rng);
            case THUNDER:      return thunder(rng);
            default:           return buffer(0.05);
        }
    }

    private static float[] biteWhoosh(int v, Random rng) {
        float[] air = biquadBandPass(pinkNoise(0.18, rng), 540 + v * 35, 1.1);
        float[] snap = biquadBandPass(envelope(pinkNoise(0.05, rng), 0.002, 3.8), 1150, 2.2);
        float[] out = buffer(0.20);
        mixInto(out, envelope(air, 0.04, 1.7), 0.0, 0.72);
        mixInto(out, thump(0.05, 155, 58, 45, 3.2), 0.105, 0.55);
        mixInto(out, snap, 0.11, 0.85);
        biquadLowPass(out, 2700, 0.7);
        return normalize(edgeFade(out, 0.004, 0.018), 0.68);
    }

    private static float[] hitImpact(int v, Random rng) {
        float[] out = mixInto(buffer(0.16), thump(0.13, 142 + v * 10, 44, 34, 2.8), 0.0, 0.76);
        float[] slap = biquadBandPass(pinkNoise(0.055, rng), 780 + v * 60, 1.3);
        mixInto(out, envelope(slap, 0.002, 3.4), 0.0, 0.92);
        drive(out, 1.28);
        biquadLowPass(out, 2400, 0.7);
        airSpace(out, 0.16);
        return normalize(edgeFade(out, 0.002, 0.02), 0.78);
    }

    private static float[] playerHurt(Random rng) {
        float[] exc = vocalExcitation(0.24, 620, 860, 430, 0.22, 16, 0.03, 0.02, 0.14, 0.20, rng);
        float[] yelp = applyFormants(exc, 790, 3.4, 1.0, 1860, 4.0, 0.58, 2900, 4.2, 0.18, 0.22);
        envelope(yelp, 0.008, 1.8);
        mixInto(yelp, thump(0.08, 145, 52, 40, 3.0), 0.0, 0.45);
        biquadHighPass(yelp, 120, 0.7);
        biquadLowPass(yelp, 3400, 0.7);
        airSpace(yelp, 0.20);
        return normalize(edgeFade(yelp, 0.004, 0.025), 0.74);
    }

    private static float[] enemyDeath(Random rng) {
        float[] out = buffer(0.68);
        float[] exc = vocalExcitation(0.48, 185, 132, 68, 0.25, 6.0, 0.025, 0.025, 0.35, 0.45, rng);
        float[] sigh = applyFormants(exc, 390, 3.0, 1.0, 960, 3.4, 0.55, 1900, 3.8, 0.16, 0.40);
        mixInto(out, envelope(sigh, 0.025, 1.4), 0.0, 0.85);
        mixInto(out, thump(0.18, 82, 34, 24, 2.5), 0.24, 0.65);
        mixInto(out, rustle(0.22, 0.35, rng), 0.25, 0.45);
        biquadLowPass(out, 2300, 0.7);
        airSpace(out, 0.22);
        return normalize(edgeFade(out, 0.01, 0.05), 0.72);
    }

    private static float[] bowShot(int v, Random rng) {
        float[] out = buffer(0.32);
        float[] woodExc = envelope(pinkNoise(0.07, rng), 0.001, 4.2);
        mixInto(out, biquadBandPass(woodExc, 175 + v * 12, 3.8), 0.0, 0.75);
        mixInto(out, biquadBandPass(woodExc, 340 + v * 15, 4.2), 0.0, 0.48);
        float[] arrowAir = biquadBandPass(pinkNoise(0.26, rng), 1150 + v * 70, 1.3);
        mixInto(out, envelope(arrowAir, 0.02, 1.9), 0.018, 0.58);
        biquadLowPass(out, 2800, 0.7);
        airSpace(out, 0.18);
        return normalize(edgeFade(out, 0.002, 0.03), 0.58);
    }

    private static float[] arrowThud(int v, Random rng) {
        float[] out = mixInto(buffer(0.11), thump(0.08, 195 + v * 15, 68, 45, 3.4), 0.0, 0.72);
        float[] woodExc = envelope(pinkNoise(0.065, rng), 0.001, 4.2);
        mixInto(out, biquadBandPass(woodExc, 420 + v * 25, 4.6), 0.0, 0.65);
        mixInto(out, biquadBandPass(woodExc, 1080 + v * 35, 4.2), 0.0, 0.42);
        biquadLowPass(out, 2500, 0.7);
        return normalize(edgeFade(out, 0.002, 0.016), 0.62);
    }

    private static float[] eatApple(int v, Random rng) {
        float[] out = buffer(0.28);
        for (int k = 0; k < 3; k++) {
            double off = k * (0.068 + v * 0.004);
            mixInto(out, thump(0.035, 130, 58, 48, 3.2), off, 0.32);
            float[] crisp = biquadBandPass(pinkNoise(0.045, rng), 920 + k * 90 + v * 45, 1.8);
            float[] juice = biquadBandPass(pinkNoise(0.045, rng), 1980 + v * 70, 2.0);
            mixInto(out, envelope(crisp, 0.002, 3.4), off, 0.75);
            mixInto(out, envelope(juice, 0.003, 3.6), off + 0.003, 0.42);
        }
        biquadLowPass(out, 3000, 0.7);
        return normalize(edgeFade(out, 0.004, 0.02), 0.50);
    }

    private static float[] eatMeat(int v, Random rng) {
        float[] out = buffer(0.38);
        for (int k = 0; k < 3; k++) {
            double off = k * (0.095 + v * 0.005);
            mixInto(out, thump(0.06, 98, 42, 30, 2.8), off, 0.55);
            float[] chew = biquadBandPass(pinkNoise(0.07, rng), 430 + k * 45 + v * 25, 1.7);
            float[] wet = biquadBandPass(pinkNoise(0.055, rng), 1080 + v * 40, 2.0);
            mixInto(out, envelope(chew, 0.006, 2.4), off, 0.70);
            mixInto(out, envelope(wet, 0.008, 2.8), off + 0.012, 0.45);
        }
        biquadLowPass(out, 2400, 0.7);
        return normalize(edgeFade(out, 0.005, 0.025), 0.55);
    }

    private static float[] burrow(boolean entering, Random rng) {
        float[] out = buffer(0.36);
        for (int k = 0; k < 4; k++) {
            double off = k * 0.062;
            double f = entering ? (760 - k * 110) : (480 + k * 120);
            float[] dig = biquadBandPass(pinkNoise(0.065, rng), f, 1.3);
            mixInto(out, envelope(dig, 0.005, 2.5), off, 0.65);
            mixInto(out, thump(0.045, 92, 42, 34, 3.0), off, 0.35);
        }
        biquadLowPass(out, entering ? 1350 : 2100, 0.75);
        airSpace(out, entering ? 0.26 : 0.14);
        return normalize(edgeFade(out, 0.006, 0.03), 0.52);
    }

    private static float[] waterSplash(Random rng) {
        float[] out = mixInto(buffer(0.46), thump(0.10, 95, 38, 24, 2.6), 0.0, 0.42);
        mixInto(out, envelope(biquadBandPass(pinkNoise(0.36, rng), 480, 1.0), 0.012, 1.9), 0.0, 0.75);
        mixInto(out, envelope(biquadBandPass(pinkNoise(0.28, rng), 1320, 1.2), 0.018, 2.2), 0.015, 0.48);
        for (int d = 0; d < 10; d++) {
            double f0 = 320 + rng.nextDouble() * 580;
            mixInto(out, organicBubble(0.024 + rng.nextDouble() * 0.016, f0, f0 * 1.5, 0.48, rng),
                    0.02 + d * 0.032 + rng.nextDouble() * 0.015, 0.18);
        }
        biquadLowPass(out, 3200, 0.7);
        airSpace(out, 0.22);
        return normalize(edgeFade(out, 0.006, 0.04), 0.58);
    }

    private static float[] rustle(double sec, double maxPeak, Random rng) {
        float[] n = biquadBandPass(pinkNoise(sec, rng), 1080, 1.1);
        float[] am = buffer(sec);
        for (int i = 0; i < am.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            am[i] = (float) (0.55 + 0.45 * Math.sin(2.0 * Math.PI * 11.0 * t + 0.6 * rng.nextDouble()));
        }
        float[] out = envelope(multiply(n, am), 0.03, 1.6);
        biquadLowPass(out, 2600, 0.7);
        return normalize(edgeFade(out, 0.01, 0.03), Math.min(maxPeak, 0.24));
    }

    private static float[] heartbeat() {
        float[] out = buffer(0.58);
        mixInto(out, thump(0.14, 54, 28, 22, 2.2), 0.0, 1.0);
        mixInto(out, thump(0.13, 46, 26, 22, 2.4), 0.21, 0.72);
        biquadLowPass(out, 130, 0.7);
        return normalize(edgeFade(out, 0.005, 0.03), 0.66);
    }

    private static float[] hungerGrowl(Random rng) {
        float[] out = buffer(0.82);
        for (int k = 0; k < 3; k++) {
            double fStart = 54 + rng.nextDouble() * 24;
            double fEnd = fStart * (k == 1 ? 1.45 : 0.78);
            float[] rumble = organicBubble(0.20, fStart, fEnd, 0.38, rng);
            mixInto(out, biquadBandPass(rumble, 82 + k * 14, 1.8), k * 0.22, 0.85);
        }
        biquadLowPass(out, 240, 0.7);
        return normalize(edgeFade(out, 0.015, 0.05), 0.44);
    }

    private static float[] alertStinger(Random rng) {
        float[] out = mixInto(buffer(0.52), thump(0.22, 96, 44, 22, 2.2), 0.0, 0.78);
        mixInto(out, MusicInstrumentToolkit.harp(47, 0.45, 0.42), 0.01, 0.80);
        mixInto(out, MusicInstrumentToolkit.harp(53, 0.45, 0.34), 0.025, 0.65);
        float[] puff = biquadBandPass(pinkNoise(0.14, rng), 980, 1.4);
        mixInto(out, envelope(puff, 0.01, 2.4), 0.0, 0.26);
        biquadLowPass(out, 2400, 0.7);
        airSpace(out, 0.26);
        return normalize(edgeFade(out, 0.004, 0.05), 0.48);
    }

    private static float[] lostTarget() {
        float[] out = buffer(0.52);
        mixInto(out, MusicInstrumentToolkit.ocarina(64, 0.16, 0.40), 0.0, 0.9);
        mixInto(out, MusicInstrumentToolkit.ocarina(59, 0.22, 0.36), 0.15, 0.85);
        airSpace(out, 0.28);
        return normalize(edgeFade(out, 0.01, 0.06), 0.30);
    }

    private static float[] woodClick() {
        float[] out = buffer(0.032);
        mixInto(out, thump(0.024, 540, 320, 120, 4.5), 0.0, 0.68);
        mixInto(out, thump(0.018, 1120, 760, 150, 5.2), 0.0, 0.28);
        biquadLowPass(out, 2600, 0.7);
        return normalize(edgeFade(out, 0.002, 0.008), 0.32);
    }

    private static float[] acousticArpeggio(int[] midiNotes, double stepSec, double peak, boolean addOcarina) {
        double tail = 0.75;
        float[] out = buffer(midiNotes.length * stepSec + tail);
        for (int i = 0; i < midiNotes.length; i++) {
            double off = i * stepSec;
            mixInto(out, MusicInstrumentToolkit.harp(midiNotes[i], 0.55, 0.45), off, 1.0);
            if (addOcarina && i == midiNotes.length - 1) {
                mixInto(out, MusicInstrumentToolkit.ocarina(midiNotes[i], 0.28, 0.25), off + 0.01, 1.0);
            }
        }
        airSpace(out, 0.30);
        return normalize(edgeFade(out, 0.005, 0.09), peak);
    }

    private static float[] birdChirp(int v, Random rng) {
        float[] out = buffer(0.58);
        int notes = 2 + (v % 2);
        double base = 1380 + v * 115;
        for (int k = 0; k < notes; k++) {
            double dur = 0.088 + rng.nextDouble() * 0.024;
            int len = Math.max(1, (int) (dur * SAMPLE_RATE));
            float[] note = new float[len];
            float[] breath = biquadBandPass(pinkNoise(dur, rng), base * 1.05, 2.2);
            double phase = 0.0;
            for (int i = 0; i < len; i++) {
                double u = (double) i / len;
                double sweep = Math.sin(Math.PI * Math.pow(u, 0.80));
                double f = base * (0.94 + k * 0.07 + 0.22 * sweep);
                phase += 2.0 * Math.PI * f / SAMPLE_RATE;
                double wave = Math.sin(phase) + 0.03 * Math.sin(2.0 * phase);
                double env = Math.sin(Math.PI * u);
                note[i] = (float) ((wave + breath[i] * 0.16) * env);
            }
            mixInto(out, note, k * 0.130, k == 0 ? 1.0 : 0.84);
        }
        biquadHighPass(out, 750, 0.7);
        biquadLowPass(out, 2400, 0.85);
        airSpace(out, 0.30);
        return normalize(edgeFade(out, 0.008, 0.04), 0.21);
    }

    private static float[] owlHoot(int v, Random rng) {
        float[] out = buffer(1.25);
        double f0 = 365 + v * 18;
        float[] first = applyFormants(vocalExcitation(0.32, f0, f0 * 1.04, f0 * 0.92, 0.35, 4.2, 0.012, 0.008, 0.0, 0.28, rng),
                380, 4.0, 1.0, 760, 4.0, 0.20, 1400, 3.0, 0.04, 0.30);
        float[] second = applyFormants(vocalExcitation(0.46, f0 * 0.98, f0 * 1.03, f0 * 0.84, 0.30, 4.5, 0.014, 0.008, 0.0, 0.26, rng),
                365, 4.0, 1.0, 730, 4.0, 0.18, 1350, 3.0, 0.04, 0.30);
        mixInto(out, envelope(first, 0.075, 1.3), 0.0, 0.90);
        mixInto(out, envelope(second, 0.070, 1.3), 0.48, 1.0);
        biquadLowPass(out, 1100, 0.7);
        airSpace(out, 0.38);
        return normalize(edgeFade(out, 0.02, 0.09), 0.32);
    }

    private static float[] cricket(int v, Random rng) {
        float[] out = buffer(0.48);
        double f = 2420 + v * 90;
        for (int k = 0; k < 4; k++) {
            float[] chirp = envelope(DspToolkit.sweep(0.034, f, f * 0.96), 0.005, 1.8);
            float[] wing = biquadBandPass(pinkNoise(0.034, rng), f, 3.8);
            mixInto(chirp, wing, 0.0, 0.28);
            mixInto(out, chirp, k * 0.068, 0.85 + 0.15 * (k % 2));
        }
        biquadLowPass(out, 2850, 0.75);
        airSpace(out, 0.25);
        return normalize(edgeFade(out, 0.005, 0.03), 0.15);
    }

    private static float[] thunder(Random rng) {
        float[] b = normalize(smoothLowPass(brownNoise(3.5), 180, 2), 1);
        float[] clap = normalize(envelope(smoothBandPass(pinkNoise(0.35, rng), 140, 1100, 2), 0.008, 2.0), 0.35);
        mixInto(b, clap, 0, 1);
        return normalize(edgeFade(envelope(drive(b, 1.3), 0.04, 1.4), 0.02, 0.15), 0.55);
    }
}
