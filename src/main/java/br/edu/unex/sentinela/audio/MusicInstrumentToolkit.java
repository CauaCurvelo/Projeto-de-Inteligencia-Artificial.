package br.edu.unex.sentinela.audio;

import static br.edu.unex.sentinela.audio.DspToolkit.SAMPLE_RATE;

/**
 * Síntese de instrumentos acústicos leves e expressivos (ocarina, harpa e baixo macio)
 * para composição procedural sem chiado ou poluição sonora.
 */
final class MusicInstrumentToolkit {

    private MusicInstrumentToolkit() {}

    static double midiToHz(int midiNote) {
        return 440.0 * Math.pow(2.0, (midiNote - 69) / 12.0);
    }

    /** Ocarina / flauta de madeira macia com vibrato lento e ataque sem estalos. */
    static float[] ocarina(int midiNote, double durationSec, double amp) {
        double freq = midiToHz(midiNote);
        double totalSec = Math.max(0.08, durationSec + 0.04);
        int len = Math.max(1, (int) (totalSec * SAMPLE_RATE));
        float[] b = new float[len];

        double attackSec = Math.min(0.026, totalSec * 0.3);
        double releaseSec = Math.min(0.085, totalSec * 0.4);
        double phase = 0.0;

        for (int i = 0; i < len; i++) {
            double t = (double) i / SAMPLE_RATE;
            double vibRamp = Math.min(1.0, Math.max(0.0, (t - 0.08) / 0.14));
            double fInst = freq * (1.0 + 0.0042 * vibRamp * Math.sin(2.0 * Math.PI * 5.0 * t));
            phase += 2.0 * Math.PI * fInst / SAMPLE_RATE;

            double wave = Math.sin(phase)
                    + 0.11 * Math.sin(2.0 * phase)
                    + 0.025 * Math.sin(3.0 * phase);

            double env = cosineEnvelope(t, totalSec, attackSec, releaseSec);
            b[i] = (float) (wave * env * amp);
        }
        return b;
    }

    /** Harpa / alaúde dedilhado com decaimento natural mais rápido nos harmônicos agudos. */
    static float[] harp(int midiNote, double sustainSec, double amp) {
        double freq = midiToHz(midiNote);
        double ringSec = Math.max(sustainSec, 0.52);
        int len = Math.max(1, (int) (ringSec * SAMPLE_RATE));
        float[] b = new float[len];

        double attackSec = 0.004;
        double phaseStep = 2.0 * Math.PI * freq / SAMPLE_RATE;
        double phase = 0.0;

        for (int i = 0; i < len; i++) {
            double t = (double) i / SAMPLE_RATE;
            phase += phaseStep;

            double h1 = 0.72 * Math.sin(phase) * Math.exp(-t * 3.1);
            double h2 = 0.22 * Math.sin(2.0 * phase) * Math.exp(-t * 6.0);
            double h3 = 0.07 * Math.sin(3.0 * phase) * Math.exp(-t * 10.5);

            double attack = t < attackSec ? 0.5 * (1.0 - Math.cos(Math.PI * t / attackSec)) : 1.0;
            double tailFade = (ringSec - t) < 0.05 ? Math.max(0.0, (ringSec - t) / 0.05) : 1.0;
            b[i] = (float) ((h1 + h2 + h3) * attack * tailFade * amp);
        }
        return b;
    }

    /** Baixo acústico redondo e discreto nos tempos estruturais. */
    static float[] warmBass(int midiNote, double durationSec, double amp) {
        double freq = midiToHz(midiNote);
        double totalSec = Math.max(0.18, durationSec);
        int len = Math.max(1, (int) (totalSec * SAMPLE_RATE));
        float[] b = new float[len];

        double attackSec = 0.008;
        double phaseStep = 2.0 * Math.PI * freq / SAMPLE_RATE;
        double phase = 0.0;

        for (int i = 0; i < len; i++) {
            double t = (double) i / SAMPLE_RATE;
            phase += phaseStep;

            double wave = 0.84 * Math.sin(phase)
                    + 0.16 * Math.sin(2.0 * phase) * Math.exp(-t * 3.5);
            double env = cosineEnvelope(t, totalSec, attackSec, Math.min(0.06, totalSec * 0.3))
                    * Math.exp(-t * 3.2);
            b[i] = (float) (wave * env * amp);
        }
        return b;
    }

    /**
     * Renderiza uma faixa completa em formato declarativo (melodia + acompanhamento de acordes)
     * e embrulha a cauda de reverberação sobre o início para loop contínuo no ritmo exato.
     */
    static float[] renderTrack(
            double bpm, int stepsPerBar, int totalSteps,
            int[][] melody, int[][] chords, double harpAmp
    ) {
        double stepSec = (60.0 / bpm) / 4.0; // semicolcheia
        double loopSec = totalSteps * stepSec;
        int loopSamples = (int) Math.round(loopSec * SAMPLE_RATE);
        int tailPad = (int) (1.6 * SAMPLE_RATE);
        float[] work = new float[loopSamples + tailPad];

        renderAccompaniment(work, stepSec, stepsPerBar, chords, harpAmp);
        for (int[] note : melody) {
            double offset = note[0] * stepSec;
            double dur = note[1] * stepSec;
            double vel = note.length > 3 ? note[3] / 100.0 : 1.0;
            mixUnclamped(work, ocarina(note[2], dur, 0.34 * vel), offset);
        }

        addSoftSpace(work, stepSec * 2.0, 0.14);

        float[] loop = new float[loopSamples];
        System.arraycopy(work, 0, loop, 0, loopSamples);
        for (int i = 0; i < tailPad && i < loopSamples; i++) {
            loop[i] += work[loopSamples + i];
        }
        return DspToolkit.normalizeRms(loop, 0.038, 0.22);
    }

    private static void renderAccompaniment(
            float[] dst, double stepSec, int stepsPerBar, int[][] chords, double harpAmp
    ) {
        for (int[] ch : chords) {
            int start = ch[0], dur = ch[1], bass = ch[2], n1 = ch[3], n2 = ch[4], n3 = ch[5];
            mixUnclamped(dst, warmBass(bass, Math.min(dur, 7) * stepSec, 0.23), start * stepSec);
            mixUnclamped(dst, harp(n1, 5.0 * stepSec, harpAmp), start * stepSec);

            if (stepsPerBar == 12) {
                mixUnclamped(dst, harp(n2, 4.0 * stepSec, harpAmp * 0.80), (start + 4) * stepSec);
                mixUnclamped(dst, harp(n3, 4.0 * stepSec, harpAmp * 0.72), (start + 4) * stepSec);
                mixUnclamped(dst, harp(n2, 4.0 * stepSec, harpAmp * 0.76), (start + 8) * stepSec);
                mixUnclamped(dst, harp(n3, 4.0 * stepSec, harpAmp * 0.68), (start + 8) * stepSec);
            } else if (dur >= 16) {
                mixUnclamped(dst, harp(n2, 4.0 * stepSec, harpAmp * 0.82), (start + 4) * stepSec);
                mixUnclamped(dst, warmBass(bass + 7, 5.0 * stepSec, 0.16), (start + 8) * stepSec);
                mixUnclamped(dst, harp(n3, 4.0 * stepSec, harpAmp * 0.90), (start + 8) * stepSec);
                mixUnclamped(dst, harp(n2, 4.0 * stepSec, harpAmp * 0.78), (start + 12) * stepSec);
            } else {
                mixUnclamped(dst, harp(n2, 4.0 * stepSec, harpAmp * 0.80), (start + 2) * stepSec);
                mixUnclamped(dst, harp(n3, 4.0 * stepSec, harpAmp * 0.86), (start + 4) * stepSec);
            }
        }
    }

    private static void addSoftSpace(float[] buf, double delaySec, double wet) {
        float[] echo = new float[buf.length];
        int delay = Math.max(1, (int) (delaySec * SAMPLE_RATE));
        for (int i = delay; i < buf.length; i++) {
            echo[i] = buf[i - delay];
        }
        DspToolkit.smoothLowPass(echo, 1350, 2);
        for (int i = 0; i < buf.length; i++) {
            buf[i] += (float) (echo[i] * wet);
        }
    }

    private static void mixUnclamped(float[] dst, float[] src, double offsetSec) {
        int off = Math.max(0, (int) Math.round(offsetSec * SAMPLE_RATE));
        for (int i = 0; i < src.length && off + i < dst.length; i++) {
            dst[off + i] += src[i];
        }
    }

    private static double cosineEnvelope(double t, double totalSec, double attackSec, double releaseSec) {
        double atk = t < attackSec ? 0.5 * (1.0 - Math.cos(Math.PI * t / attackSec)) : 1.0;
        double rem = totalSec - t;
        double rel = rem < releaseSec ? Math.max(0.0, 0.5 * (1.0 - Math.cos(Math.PI * rem / releaseSec))) : 1.0;
        return atk * rel;
    }
}
