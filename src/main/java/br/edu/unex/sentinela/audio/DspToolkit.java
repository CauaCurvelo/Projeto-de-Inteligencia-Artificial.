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

    /** Ruído rosa (-3 dB/oitava) via filtro de Paul Kellet para textura orgânica sem chiado. */
    public static float[] pinkNoise(double seconds) {
        return pinkNoise(seconds, RNG);
    }

    /** Variante determinística de ruído rosa usando um gerador específico. */
    public static float[] pinkNoise(double seconds, Random random) {
        float[] b = buffer(seconds);
        double b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0, b6 = 0;
        for (int i = 0; i < b.length; i++) {
            double w = random.nextDouble() * 2.0 - 1.0;
            b0 = 0.99886 * b0 + w * 0.0555179;
            b1 = 0.99332 * b1 + w * 0.0750759;
            b2 = 0.96900 * b2 + w * 0.1538520;
            b3 = 0.86650 * b3 + w * 0.3104856;
            b4 = 0.55000 * b4 + w * 0.5329522;
            b5 = -0.7616 * b5 - w * 0.0168980;
            double pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362;
            b6 = w * 0.115926;
            b[i] = (float) (pink * 0.11);
        }
        return b;
    }

    /** Passa-baixa em cascata para corte acentuado nos agudos sem estridência. */
    public static float[] smoothLowPass(float[] b, double cutoff, int stages) {
        for (int s = 0; s < stages; s++) {
            lowPass(b, cutoff);
        }
        return b;
    }

    /** Passa-faixa em cascata para isolar faixa quente sem vazamento de estática. */
    public static float[] smoothBandPass(float[] b, double lo, double hi, int stages) {
        for (int s = 0; s < stages; s++) {
            highPass(b, lo);
            lowPass(b, hi);
        }
        return b;
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

    /** Normaliza o nível contínuo (RMS) respeitando também um teto máximo de pico. */
    public static float[] normalizeRms(float[] b, double targetRms, double maxPeak) {
        if (b.length == 0) return b;
        double sumSq = 0;
        float max = 1e-6f;
        for (float v : b) {
            sumSq += (double) v * v;
            max = Math.max(max, Math.abs(v));
        }
        double rms = Math.sqrt(sumSq / b.length);
        if (rms < 1e-9) return b;
        double k = targetRms / rms;
        if (max * k > maxPeak) {
            k = maxPeak / max;
        }
        float gain = (float) k;
        for (int i = 0; i < b.length; i++) {
            b[i] *= gain;
        }
        return b;
    }

    /** Impacto corporal/membrana com queda exponencial rápida de frequência e ataque em cosseno. */
    public static float[] thump(double seconds, double fStart, double fEnd, double pitchDecayRate, double decayCurve) {
        float[] b = buffer(seconds);
        int att = Math.max(1, (int) (Math.min(0.008, seconds * 0.18) * SAMPLE_RATE));
        double phase = 0.0;
        for (int i = 0; i < b.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double f = fEnd + (fStart - fEnd) * Math.exp(-t * pitchDecayRate);
            phase += 2.0 * Math.PI * f / SAMPLE_RATE;
            double wave = Math.sin(phase) + 0.18 * Math.sin(2.0 * phase) * Math.exp(-t * 45.0);
            double aEnv = i < att ? 0.5 * (1.0 - Math.cos(Math.PI * i / att)) : 1.0;
            double dEnv = Math.pow(Math.max(0.0, 1.0 - (double) i / b.length), decayCurve);
            b[i] = (float) (wave * aEnv * dEnv);
        }
        return b;
    }

    /** Suaviza início e fim do buffer em cosseno para evitar cliques na reprodução. */
    public static float[] edgeFade(float[] b, double fadeInSec, double fadeOutSec) {
        int fi = Math.min(b.length / 2, (int) (fadeInSec * SAMPLE_RATE));
        int fo = Math.min(b.length / 2, (int) (fadeOutSec * SAMPLE_RATE));
        for (int i = 0; i < fi; i++) {
            b[i] *= (float) (0.5 * (1.0 - Math.cos(Math.PI * i / Math.max(1, fi))));
        }
        for (int i = 0; i < fo; i++) {
            int idx = b.length - 1 - i;
            b[idx] *= (float) (0.5 * (1.0 - Math.cos(Math.PI * i / Math.max(1, fo))));
        }
        return b;
    }
}
