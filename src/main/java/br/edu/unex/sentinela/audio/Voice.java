package br.edu.unex.sentinela.audio;

/** Uma instância de som tocando no mixer (one-shot ou loop com ganho suavizado). */
final class Voice {
    final float[] data;
    final boolean loop;
    private double pos;
    private final double pitch;
    volatile double targetGain;
    volatile double pan; // -1 esquerda .. +1 direita
    private double gain;
    private boolean finished;

    Voice(float[] data, double gain, double pan, double pitch, boolean loop) {
        this.data = data;
        this.targetGain = gain;
        this.gain = loop ? 0.0 : gain; // loops entram com fade-in
        this.pan = pan;
        this.pitch = pitch;
        this.loop = loop;
        if (data == null || data.length == 0) {
            this.finished = true;
        }
    }

    /** Mistura `frames` quadros estéreo neste voice dentro de mix (intercalado L,R) com interpolação linear. */
    void render(float[] mix, int frames) {
        if (finished) return;
        int len = data.length;
        double l = Math.sqrt(0.5 * (1 - pan)), r = Math.sqrt(0.5 * (1 + pan));
        for (int f = 0; f < frames; f++) {
            gain += (targetGain - gain) * 0.0008; // smoothing (~30 ms)
            if (pos >= len) {
                if (!loop) { finished = true; return; }
                while (pos >= len) pos -= len;
            }
            int i0 = (int) pos;
            double frac = pos - i0;
            int i1 = i0 + 1;
            if (i1 >= len) {
                i1 = loop ? 0 : i0;
            }
            float sample = (float) ((data[i0] + frac * (data[i1] - data[i0])) * gain);
            mix[f * 2] += sample * l;
            mix[f * 2 + 1] += sample * r;
            pos += pitch;
        }
    }

    boolean isFinished() { return finished; }
    void stop() { finished = true; }
}
