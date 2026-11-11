package br.edu.unex.sentinela.audio;

import java.util.Random;

/**
 * Primitivas de síntese/DSP mono em float (-1..1) a 44.1 kHz.
 * Todas as funções operam sobre buffers e são puras (sem estado global além do RNG).
 */
public final class DspToolkit {
    public static final int SAMPLE_RATE = 44100;
    private static final Random RNG = new Random(1337);

    private DspToolkit() {}

    public static float[] buffer(double seconds) {
        return new float[Math.max(1, (int) (seconds * SAMPLE_RATE))];
    }

    public static double rand() { return RNG.nextDouble() * 2.0 - 1.0; }

    public static float[] whiteNoise(double seconds) {
        float[] b = buffer(seconds);
        for (int i = 0; i < b.length; i++) b[i] = (float) rand();
        return b;
    }

    /** Ruído marrom (random walk) — base para vento, rio, rosnados. */
    public static float[] brownNoise(double seconds) {
        float[] b = buffer(seconds);
        double v = 0;
        for (int i = 0; i < b.length; i++) {
            v = (v + rand() * 0.02) * 0.998;
            b[i] = (float) (v * 3.5);
        }
        return b;
    }

    /** Oscilador senoidal com varredura linear de frequência (f0 -> f1). */
    public static float[] sweep(double seconds, double f0, double f1) {
        float[] b = buffer(seconds);
        double phase = 0;
        for (int i = 0; i < b.length; i++) {
            double t = (double) i / b.length;
            phase += 2 * Math.PI * (f0 + (f1 - f0) * t) / SAMPLE_RATE;
            b[i] = (float) Math.sin(phase);
        }
        return b;
    }

    /** Onda dente-de-serra com varredura — vozes animais ásperas. */
    public static float[] sawSweep(double seconds, double f0, double f1, double vibratoHz, double vibratoDepth) {
        float[] b = buffer(seconds);
        double phase = 0;
        for (int i = 0; i < b.length; i++) {
            double t = (double) i / b.length;
            double sec = (double) i / SAMPLE_RATE;
            double f = (f0 + (f1 - f0) * t) * (1 + vibratoDepth * Math.sin(2 * Math.PI * vibratoHz * sec));
            phase += f / SAMPLE_RATE;
            b[i] = (float) (2 * (phase - Math.floor(phase + 0.5)));
        }
        return b;
    }

    /** Filtro passa-baixa de 1 polo (in-place). cutoff em Hz. */
    public static float[] lowPass(float[] b, double cutoff) {
        double a = 1 - Math.exp(-2 * Math.PI * cutoff / SAMPLE_RATE);
        double y = 0;
        for (int i = 0; i < b.length; i++) { y += a * (b[i] - y); b[i] = (float) y; }
        return b;
    }

    /** Filtro passa-alta de 1 polo (in-place). */
    public static float[] highPass(float[] b, double cutoff) {
        double a = 1 - Math.exp(-2 * Math.PI * cutoff / SAMPLE_RATE);
        double low = 0;
        for (int i = 0; i < b.length; i++) { low += a * (b[i] - low); b[i] = (float) (b[i] - low); }
        return b;
    }

    /** Passa-faixa simples (HP seguido de LP). */
    public static float[] bandPass(float[] b, double lo, double hi) {
        return lowPass(highPass(b, lo), hi);
    }

    /** Envelope percussivo: ataque linear + decaimento exponencial. */
    public static float[] envelope(float[] b, double attackSec, double decayCurve) {
        int att = Math.max(1, (int) (attackSec * SAMPLE_RATE));
        for (int i = 0; i < b.length; i++) {
            double g = i < att ? (double) i / att
                    : Math.pow(1.0 - (double) (i - att) / Math.max(1, b.length - att), decayCurve);
            b[i] *= (float) g;
        }
        return b;
    }

    /** Fade-in/out nas bordas para loops sem clique. */
    public static float[] loopFade(float[] b, double fadeSec) {
        int f = Math.min(b.length / 2, (int) (fadeSec * SAMPLE_RATE));
        for (int i = 0; i < f; i++) {
            float k = (float) i / f;
            float head = b[i], tail = b[b.length - f + i];
            b[i] = head * k + tail * (1 - k);
        }
        float[] out = new float[b.length - f];
        System.arraycopy(b, 0, out, 0, out.length);
        return out;
    }

    /** Soma b em a a partir do offset (em segundos) com ganho. */
    public static float[] mixInto(float[] a, float[] b, double offsetSec, double gain) {
        int off = (int) (offsetSec * SAMPLE_RATE);
        for (int i = 0; i < b.length && off + i < a.length; i++) a[off + i] += (float) (b[i] * gain);
        return a;
    }

    public static float[] multiply(float[] a, float[] b) {
        for (int i = 0; i < a.length && i < b.length; i++) a[i] *= b[i];
        return a;
    }

    /** Saturação suave (tanh) — dá corpo a rosnados e impactos. */
    public static float[] drive(float[] b, double amount) {
        for (int i = 0; i < b.length; i++) b[i] = (float) Math.tanh(b[i] * amount);
        return b;
    }

    public static float[] normalize(float[] b, double peak) {
        float max = 1e-6f;
        for (float v : b) max = Math.max(max, Math.abs(v));
        float k = (float) (peak / max);
        for (int i = 0; i < b.length; i++) b[i] *= k;
        return b;
    }
}
