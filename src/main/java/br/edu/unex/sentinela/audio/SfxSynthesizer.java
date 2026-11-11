package br.edu.unex.sentinela.audio;

import static br.edu.unex.sentinela.audio.DspToolkit.*;

/** Receitas procedurais dos efeitos curtos (passos, combate, vozes, ações, UI). */
final class SfxSynthesizer {
    private SfxSynthesizer() {}

    static float[] create(SoundId id) {
        switch (id) {
            case STEP_GRASS:      return noiseHit(0.09, 1800, 6000, 0.002, 2.5, 0.45);
            case STEP_TALL_GRASS: return rustle(0.22, 0.5);
            case STEP_DIRT:       return noiseHit(0.08, 200, 1400, 0.002, 3.0, 0.5);
            case STEP_MUD:        return mudSquelch();
            case STEP_WATER:      return splash(0.25, 0.45);
            case STEP_STONE:      return mixInto(noiseHit(0.05, 2500, 9000, 0.001, 4, 0.35), tone(0.04, 900, 600, 4), 0, 0.2);
            case STEP_BOOT:       return mixInto(noiseHit(0.1, 100, 900, 0.002, 3, 0.6), tone(0.08, 110, 70, 4), 0, 0.5);
            case STEP_PAW:        return noiseHit(0.06, 300, 2200, 0.002, 3, 0.35);
            case STEP_HOOF:       return mixInto(noiseHit(0.06, 600, 3000, 0.001, 5, 0.5), tone(0.07, 380, 220, 5), 0, 0.4);
            case STEP_HEAVY:      return normalize(mixInto(tone(0.25, 70, 40, 2.5), noiseHit(0.2, 60, 500, 0.003, 2, 1), 0, 0.6), 0.8);
            case BITE_WHOOSH:     return bite();
            case HIT_IMPACT:      return normalize(drive(mixInto(tone(0.15, 160, 60, 3), noiseHit(0.1, 200, 4000, 0.001, 3, 1), 0, 0.7), 2.5), 0.85);
            case PLAYER_HURT:     return playerHurt();
            case ENEMY_DEATH:     return enemyDeath();
            case BOW_SHOT:        return bowShot();
            case ARROW_THUD:      return mixInto(tone(0.08, 220, 120, 4), noiseHit(0.06, 400, 3000, 0.001, 4, 1), 0, 0.5);
            case DOG_BARK:        return bark(0.0);
            case WOLF_HOWL:       return howl();
            case WOLF_SNARL:      return growl(0.5, 95, 1.0);
            case BOAR_GRUNT:      return grunt();
            case BADGER_HISS:     return hiss(0.4, 2500, 7000);
            case BEAR_ROAR:       return roar(1.1, 85, 60, 1100);
            case PANTHER_ROAR:    return roar(0.9, 140, 90, 1800);
            case CROC_HISS:       return mixInto(hiss(0.8, 600, 3500), growl(0.8, 45, 0.6), 0, 0.5);
            case HUNTER_SHOUT:    return hunterShout();
            case ALERT_STINGER:   return alertStinger();
            case SNIFF:           return sniff();
            case LOST_TARGET:     return normalize(mixInto(tone(0.25, 520, 390, 2), tone(0.3, 390, 330, 2), 0.12, 1), 0.35);
            case EAT_APPLE:       return crunch(3, 0.06);
            case EAT_MEAT:        return mixInto(crunch(4, 0.08), growl(0.35, 120, 0.3), 0, 0.3);
            case HEAL:            return arpeggio(new double[]{523, 659, 784, 1047}, 0.07, 0.4);
            case BURROW_ENTER:    return mixInto(noiseHit(0.35, 100, 900, 0.01, 1.5, 0.7), tone(0.3, 300, 120, 2), 0, 0.3);
            case BURROW_EXIT:     return mixInto(noiseHit(0.3, 150, 1500, 0.01, 1.5, 0.6), tone(0.25, 140, 320, 2), 0, 0.3);
            case SPLASH:          return splash(0.45, 0.7);
            case LEAVES_RUSTLE:   return rustle(0.4, 0.5);
            case HEARTBEAT:       return heartbeat();
            case HUNGER_GROWL:    return normalize(lowPass(sawSweep(0.8, 70, 55, 9, 0.25), 300), 0.45);
            case DAY_CHIME:       return arpeggio(new double[]{392, 523, 659, 784}, 0.15, 0.4);
            case WEATHER_SHIFT:   return normalize(envelope(bandPass(whiteNoise(1.5), 300, 2000), 0.6, 1.5), 0.3);
            case GAME_OVER:       return arpeggio(new double[]{392, 330, 262, 196}, 0.28, 0.5);
            case UI_CLICK:        return tone(0.04, 1400, 900, 4);
            case ACHIEVEMENT_UNLOCKED: return arpeggio(new double[]{523, 659, 784, 1046, 1318}, 0.08, 0.55);
            case BIRD_CHIRP:      return birdChirp();
            case OWL_HOOT:        return owlHoot();
            case CRICKET:         return cricket();
            case THUNDER:         return thunder();
            default:              return buffer(0.05);
        }
    }

    // ---- blocos de construção ----
    private static float[] noiseHit(double sec, double lo, double hi, double att, double curve, double peak) {
        return normalize(envelope(bandPass(whiteNoise(sec), lo, hi), att, curve), peak);
    }

    private static float[] tone(double sec, double f0, double f1, double curve) {
        return envelope(sweep(sec, f0, f1), 0.002, curve);
    }

    private static float[] rustle(double sec, double peak) {
        float[] n = bandPass(whiteNoise(sec), 2500, 8000);
        float[] am = buffer(sec);
        for (int i = 0; i < am.length; i++) am[i] = (float) Math.abs(Math.sin(i * 0.0012) * (0.6 + 0.4 * rand()));
        return normalize(envelope(multiply(n, am), 0.03, 1.5), peak);
    }

    private static float[] splash(double sec, double peak) {
        float[] b = noiseHit(sec, 400, 5000, 0.004, 2.2, 1);
        mixInto(b, tone(0.08, 600, 1600, 3), 0.01, 0.3);
        mixInto(b, tone(0.06, 900, 2000, 3), 0.07, 0.2);
        return normalize(b, peak);
    }

    private static float[] mudSquelch() {
        float[] b = noiseHit(0.18, 80, 700, 0.01, 1.5, 1);
        mixInto(b, lowPass(sawSweep(0.12, 90, 160, 30, 0.3), 600), 0.03, 0.4);
        return normalize(b, 0.5);
    }

    private static float[] bite() {
        float[] w = normalize(envelope(bandPass(whiteNoise(0.18), 500, 3000), 0.08, 1.5), 0.5);
        mixInto(w, noiseHit(0.05, 1500, 7000, 0.001, 5, 1), 0.13, 0.7);
        return normalize(w, 0.75);
    }

    private static float[] playerHurt() {
        float[] yelp = normalize(envelope(bandPass(sawSweep(0.28, 1100, 650, 18, 0.06), 400, 3500), 0.01, 1.8), 0.6);
        mixInto(yelp, tone(0.12, 140, 60, 3), 0, 0.5);
        return normalize(drive(yelp, 1.6), 0.85);
    }

    private static float[] enemyDeath() {
        float[] b = normalize(envelope(lowPass(sawSweep(0.7, 300, 80, 6, 0.1), 1200), 0.01, 1.2), 0.6);
        mixInto(b, noiseHit(0.4, 60, 500, 0.01, 2, 0.6), 0.25, 1);
        return normalize(b, 0.8);
    }

    private static float[] bowShot() {
        float[] twang = envelope(sawSweep(0.25, 180, 150, 40, 0.02), 0.001, 3);
        lowPass(twang, 1800);
        float[] whoosh = normalize(envelope(bandPass(whiteNoise(0.35), 1500, 6000), 0.15, 1.2), 1);
        mixInto(twang, whoosh, 0.02, 0.4);
        return normalize(twang, 0.6);
    }

    private static float[] bark(double variant) {
        float[] b = buffer(0.5);
        for (int k = 0; k < 2; k++) {
            float[] one = envelope(bandPass(sawSweep(0.14, 520 + variant, 300, 25, 0.08), 250, 2500), 0.005, 2);
            mixInto(one, noiseHit(0.1, 800, 3000, 0.002, 3, 0.4), 0, 1);
            mixInto(b, drive(one, 2), k * 0.2, 1);
        }
        return normalize(b, 0.8);
    }

    private static float[] howl() {
        float[] b = buffer(2.0);
        float[] rise = sawSweep(2.0, 330, 520, 5, 0.02);
        for (int i = 0; i < rise.length; i++) {
            double t = (double) i / rise.length;
            rise[i] *= (float) (Math.sin(Math.PI * Math.min(1, t * 1.3)) * (t < 0.3 ? t / 0.3 : 1));
        }
        mixInto(b, bandPass(rise, 300, 1600), 0, 1);
        return normalize(b, 0.55);
    }

    private static float[] growl(double sec, double f, double peak) {
        float[] g = sawSweep(sec, f, f * 0.9, 22, 0.3);
        multiply(g, normalize(lowPass(whiteNoise(sec), 40), 1));
        return normalize(envelope(drive(lowPass(g, 700), 2), 0.05, 1.2), peak * 0.7);
    }

    private static float[] grunt() {
        float[] b = buffer(0.6);
        for (int k = 0; k < 3; k++) mixInto(b, envelope(lowPass(sawSweep(0.12, 160, 100, 35, 0.4), 900), 0.01, 2), k * 0.16, 1);
        return normalize(drive(b, 2.2), 0.7);
    }

    private static float[] hiss(double sec, double lo, double hi) {
        return normalize(envelope(bandPass(whiteNoise(sec), lo, hi), 0.06, 1.3), 0.5);
    }

    private static float[] roar(double sec, double f0, double f1, double bright) {
        float[] r = sawSweep(sec, f0, f1, 14, 0.15);
        mixInto(r, bandPass(whiteNoise(sec), 200, bright), 0, 0.5);
        return normalize(envelope(drive(lowPass(r, bright), 3), 0.12, 1.3), 0.9);
    }

    private static float[] hunterShout() {
        float[] v = bandPass(sawSweep(0.45, 180, 140, 6, 0.04), 300, 2500);
        float[] formant = bandPass(sawSweep(0.45, 180, 140, 6, 0.04), 800, 1200);
        mixInto(v, formant, 0, 1.2);
        return normalize(envelope(drive(v, 1.8), 0.03, 1.4), 0.6);
    }

    private static float[] alertStinger() {
        float[] b = buffer(0.6);
        mixInto(b, envelope(sawSweep(0.5, 220, 233, 0, 0), 0.005, 1.6), 0, 0.5);
        mixInto(b, envelope(sawSweep(0.5, 311, 330, 0, 0), 0.005, 1.6), 0, 0.4);
        mixInto(b, noiseHit(0.15, 3000, 9000, 0.001, 3, 0.3), 0, 1);
        return normalize(lowPass(b, 3500), 0.55);
    }

    private static float[] sniff() {
        float[] b = buffer(0.45);
        for (int k = 0; k < 3; k++) mixInto(b, noiseHit(0.08, 1500, 5000, 0.03, 1.5, 1), k * 0.12, 1);
        return normalize(b, 0.4);
    }

    private static float[] crunch(int bites, double gap) {
        float[] b = buffer(bites * gap + 0.15);
        for (int k = 0; k < bites; k++) mixInto(b, noiseHit(0.07, 1000 + rand() * 300, 7000, 0.001, 4, 1), k * gap, 0.8);
        return normalize(b, 0.6);
    }

    private static float[] arpeggio(double[] notes, double step, double peak) {
        float[] b = buffer(notes.length * step + 0.6);
        for (int k = 0; k < notes.length; k++) {
            float[] n = envelope(sweep(0.6, notes[k], notes[k]), 0.005, 2.5);
            mixInto(n, envelope(sweep(0.6, notes[k] * 2, notes[k] * 2), 0.005, 3), 0, 0.25);
            mixInto(b, n, k * step, 1);
        }
        return normalize(b, peak);
    }

    private static float[] heartbeat() {
        float[] b = buffer(0.55);
        mixInto(b, tone(0.12, 70, 45, 2.5), 0, 1);
        mixInto(b, tone(0.12, 62, 40, 2.5), 0.2, 0.7);
        return normalize(lowPass(b, 200), 0.7);
    }

    private static float[] birdChirp() {
        float[] b = buffer(0.6);
        int n = 2 + (int) ((rand() + 1) * 2);
        double base = 2800 + rand() * 600;
        for (int k = 0; k < n; k++) mixInto(b, envelope(DspToolkit.sweep(0.08, base, base * 1.4), 0.005, 1.5), k * 0.11, 1);
        return normalize(b, 0.35);
    }

    private static float[] owlHoot() {
        float[] b = buffer(1.2);
        mixInto(b, envelope(DspToolkit.sweep(0.35, 400, 370), 0.08, 1.2), 0, 1);
        mixInto(b, envelope(DspToolkit.sweep(0.5, 390, 340), 0.08, 1.2), 0.5, 0.9);
        return normalize(lowPass(b, 900), 0.35);
    }

    private static float[] cricket() {
        float[] b = buffer(0.5);
        for (int k = 0; k < 4; k++) {
            float[] chirp = DspToolkit.sweep(0.05, 4600, 4600);
            float[] am = DspToolkit.sweep(0.05, 90, 90);
            for (int i = 0; i < chirp.length; i++) chirp[i] *= Math.abs(am[i]);
            mixInto(b, envelope(chirp, 0.005, 1), k * 0.065, 1);
        }
        return normalize(b, 0.18);
    }

    private static float[] thunder() {
        float[] b = normalize(lowPass(brownNoise(3.5), 300), 1);
        mixInto(b, noiseHit(0.3, 200, 4000, 0.001, 2, 0.6), 0, 1);
        return normalize(envelope(drive(b, 1.5), 0.02, 1.4), 0.85);
    }
}
