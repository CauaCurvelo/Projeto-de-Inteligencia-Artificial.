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
    }

    /** Mistura `frames` quadros estéreo neste voice dentro de mix (intercalado L,R). */
    void render(float[] mix, int frames) {
        if (finished) return;
        double l = Math.sqrt(0.5 * (1 - pan)), r = Math.sqrt(0.5 * (1 + pan));
        for (int f = 0; f < frames; f++) {
            gain += (targetGain - gain) * 0.0008; // smoothing (~30 ms)
            int i = (int) pos;
            if (i >= data.length) {
                if (!loop) { finished = true; return; }
                pos -= data.length; i = (int) pos;
            }
            float s = (float) (data[i] * gain);
            mix[f * 2] += s * l;
            mix[f * 2 + 1] += s * r;
            pos += pitch;
        }
    }

    boolean isFinished() { return finished; }
    void stop() { finished = true; }
}
