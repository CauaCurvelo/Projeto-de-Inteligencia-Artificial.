package br.edu.unex.sentinela.audio;

import java.util.Arrays;
import java.util.Random;

import static br.edu.unex.sentinela.audio.DspToolkit.SAMPLE_RATE;

/**
 * Núcleo de modelagem acústica e síntese formântica orgânica:
 * filtros Biquad de 2ª ordem, excitação glotal fisiológica e reflexões espaciais leves.
 */
final class AcousticResonator {

    private AcousticResonator() {}

    /** Passa-faixa Biquad RBJ com ganho unitário no pico (retorna novo buffer para bancos paralelos). */
    static float[] biquadBandPass(float[] src, double centerHz, double q) {
        float[] out = new float[src.length];
        double fc = Math.max(25.0, Math.min(SAMPLE_RATE * 0.44, centerHz));
        double w0 = 2.0 * Math.PI * fc / SAMPLE_RATE;
        double alpha = Math.sin(w0) / (2.0 * Math.max(0.25, q));
        double a0 = 1.0 + alpha;
        double b0 = alpha / a0;
        double b2 = -alpha / a0;
        double a1 = (-2.0 * Math.cos(w0)) / a0;
        double a2 = (1.0 - alpha) / a0;

        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < src.length; i++) {
            double x0 = src[i];
            double y0 = b0 * x0 + b2 * x2 - a1 * y1 - a2 * y2;
            out[i] = (float) y0;
            x2 = x1; x1 = x0;
            y2 = y1; y1 = y0;
        }
        return out;
    }

    /** Passa-baixa Biquad (-12 dB/oitava) in-place para remover estridências agudas. */
    static float[] biquadLowPass(float[] buf, double cutoffHz, double q) {
        double fc = Math.max(30.0, Math.min(SAMPLE_RATE * 0.45, cutoffHz));
        double w0 = 2.0 * Math.PI * fc / SAMPLE_RATE;
        double cosW = Math.cos(w0);
        double alpha = Math.sin(w0) / (2.0 * Math.max(0.35, q));
        double a0 = 1.0 + alpha;
        double b1 = (1.0 - cosW) / a0;
        double b0 = 0.5 * b1;
        double b2 = b0;
        double a1 = (-2.0 * cosW) / a0;
        double a2 = (1.0 - alpha) / a0;

        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < buf.length; i++) {
            double x0 = buf[i];
            double y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            buf[i] = (float) y0;
            x2 = x1; x1 = x0;
            y2 = y1; y1 = y0;
        }
        return buf;
    }

    /** Passa-alta Biquad (-12 dB/oitava) in-place para limpar lama sub-harmônica e DC. */
    static float[] biquadHighPass(float[] buf, double cutoffHz, double q) {
        double fc = Math.max(20.0, Math.min(SAMPLE_RATE * 0.44, cutoffHz));
        double w0 = 2.0 * Math.PI * fc / SAMPLE_RATE;
        double cosW = Math.cos(w0);
        double alpha = Math.sin(w0) / (2.0 * Math.max(0.35, q));
        double a0 = 1.0 + alpha;
        double onePlus = 1.0 + cosW;
        double b0 = (0.5 * onePlus) / a0;
        double b1 = (-onePlus) / a0;
        double b2 = b0;
        double a1 = (-2.0 * cosW) / a0;
        double a2 = (1.0 - alpha) / a0;

        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < buf.length; i++) {
            double x0 = buf[i];
            double y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            buf[i] = (float) y0;
            x2 = x1; x1 = x0;
            y2 = y1; y1 = y0;
        }
        return buf;
    }

    /**
     * Pulso glotal fisiológico (Rosenberg suavizado) centrado em média zero.
     * Evita quinas instantâneas da dente-de-serra e soa orgânico sem aliasing metálico.
     */
    static double glottalSample(double phase01, double openRatio) {
        double p = phase01 - Math.floor(phase01);
        double open = Math.max(0.28, Math.min(0.68, openRatio));
        double close = open * 0.36;
        double v;
        if (p < open) {
            v = 0.5 * (1.0 - Math.cos(Math.PI * p / open));
        } else if (p < open + close) {
            double c = (p - open) / close;
            v = Math.cos(0.5 * Math.PI * c);
        } else {
            v = 0.0;
        }
        return (v - 0.34) * 1.45;
    }

    /**
     * Gera excitação laríngea orgânica (pulso glotal + micro-jitter + rugosidade sub-harmônica + sopro).
     */
    static float[] vocalExcitation(
            double seconds,
            double f0Start, double f0Mid, double f0End, double midPos,
            double vibHz, double vibDepth,
            double jitterAmt, double gritAmt, double breathMix,
            Random rng
    ) {
        int len = Math.max(1, (int) (seconds * SAMPLE_RATE));
        float[] out = new float[len];
        float[] breath = biquadLowPass(DspToolkit.pinkNoise(seconds, rng), 3200.0, 0.7);
        double phase = 0.0;
        double subPhase = 0.0;
        double jitterWalk = 0.0;
        double gritWalk = 0.0;

        for (int i = 0; i < len; i++) {
            double u = (double) i / len;
            double sec = (double) i / SAMPLE_RATE;
            double fBase = interpolatePitchCurve(u, f0Start, f0Mid, f0End, midPos);
            double vib = 1.0 + vibDepth * Math.sin(2.0 * Math.PI * vibHz * sec);

            if ((i & 31) == 0) {
                jitterWalk = jitterWalk * 0.65 + (rng.nextDouble() * 2.0 - 1.0) * 0.35;
                gritWalk = gritWalk * 0.55 + (rng.nextDouble() * 2.0 - 1.0) * 0.45;
            }
            double fInst = Math.max(24.0, fBase * vib * (1.0 + jitterAmt * jitterWalk));
            phase += fInst / SAMPLE_RATE;
            subPhase += (fInst * 0.5) / SAMPLE_RATE;

            double glot = glottalSample(phase, 0.46 + 0.08 * gritWalk);
            double sub = gritAmt * (0.65 * glottalSample(subPhase, 0.52) + 0.35 * glot * gritWalk);
            double raw = glot + sub;
            raw *= (1.0 - 0.25 * gritAmt * Math.max(0.0, gritWalk));
            out[i] = (float) (raw + breath[i] * breathMix);
        }
        return out;
    }

    /** Aplica banco paralelo de 3 formantes vocais para simular trato oral/faríngeo realista. */
    static float[] applyFormants(
            float[] excitation,
            double f1, double q1, double g1,
            double f2, double q2, double g2,
            double f3, double q3, double g3,
            double chestBodyGain
    ) {
        float[] out = new float[excitation.length];
        float[] r1 = biquadBandPass(excitation, f1, q1);
        float[] r2 = biquadBandPass(excitation, f2, q2);
        float[] r3 = biquadBandPass(excitation, f3, q3);
        float[] chest = Arrays.copyOf(excitation, excitation.length);
        biquadLowPass(chest, Math.max(95.0, f1 * 0.65), 0.7);

        for (int i = 0; i < out.length; i++) {
            out[i] = (float) (r1[i] * g1 + r2[i] * g2 + r3[i] * g3 + chest[i] * chestBodyGain);
        }
        return out;
    }

    /** Bolha líquida orgânica (micro-cavidade subindo de pitch amortecida por textura rosa). */
    static float[] organicBubble(double seconds, double fStart, double fEnd, double noiseAmt, Random rng) {
        int len = Math.max(1, (int) (seconds * SAMPLE_RATE));
        float[] out = new float[len];
        float[] tex = biquadBandPass(DspToolkit.pinkNoise(seconds, rng), (fStart + fEnd) * 0.5, 1.4);
        double phase = 0.0;
        int att = Math.max(1, (int) (0.003 * SAMPLE_RATE));
        for (int i = 0; i < len; i++) {
            double u = (double) i / len;
            double f = fStart + (fEnd - fStart) * (1.0 - Math.exp(-u * 3.5));
            phase += 2.0 * Math.PI * f / SAMPLE_RATE;
            double aEnv = i < att ? 0.5 * (1.0 - Math.cos(Math.PI * i / att)) : 1.0;
            double dEnv = Math.exp(-u * 4.5) * (1.0 - u);
            out[i] = (float) ((Math.sin(phase) * (1.0 - noiseAmt) + tex[i] * noiseAmt) * aEnv * dEnv);
        }
        return out;
    }

    /** Adiciona micro-reflexões filtradas simulando ar livre entre árvores sem cauda metálica. */
    static float[] airSpace(float[] buf, double wet) {
        if (wet <= 0.001) return buf;
        int[] delays = { (int) (0.011 * SAMPLE_RATE), (int) (0.019 * SAMPLE_RATE), (int) (0.031 * SAMPLE_RATE), (int) (0.047 * SAMPLE_RATE) };
        double[] gains = { 0.34, 0.24, 0.16, 0.10 };
        float[] reflections = new float[buf.length];
        for (int t = 0; t < delays.length; t++) {
            int d = delays[t];
            double g = gains[t] * wet;
            for (int i = d; i < buf.length; i++) {
                reflections[i] += (float) (buf[i - d] * g);
            }
        }
        biquadLowPass(reflections, 1750.0, 0.65);
        for (int i = 0; i < buf.length; i++) {
            buf[i] += reflections[i];
        }
        return buf;
    }

    private static double interpolatePitchCurve(double u, double fStart, double fMid, double fEnd, double midPos) {
        double m = Math.max(0.05, Math.min(0.95, midPos));
        if (u < m) {
            double t = u / m;
            double k = 0.5 * (1.0 - Math.cos(Math.PI * t));
            return fStart + (fMid - fStart) * k;
        }
        double t = (u - m) / (1.0 - m);
        double k = 0.5 * (1.0 - Math.cos(Math.PI * t));
        return fMid + (fEnd - fMid) * k;
    }
}
