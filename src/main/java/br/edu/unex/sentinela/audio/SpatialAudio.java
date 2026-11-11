package br.edu.unex.sentinela.audio;

/** Áudio espacial 2D: atenuação por distância e pan estéreo relativos ao ouvinte (raposa). */
public final class SpatialAudio {
    public static final double HEARING_RADIUS = 18 * 32.0;
    private static final double PAN_WIDTH = 320.0;

    private double listenerX, listenerY;

    public void setListener(double x, double y) { listenerX = x; listenerY = y; }

    public double attenuation(double x, double y) {
        double d = Math.hypot(x - listenerX, y - listenerY);
        if (d >= HEARING_RADIUS) return 0;
        double k = 1 - d / HEARING_RADIUS;
        return k * k;
    }

    public double pan(double x) {
        return Math.max(-0.85, Math.min(0.85, (x - listenerX) / PAN_WIDTH));
    }

    /** Toca um som posicionado no mundo. */
    public void playAt(SoundId id, double x, double y, double gain, double pitch) {
        double a = attenuation(x, y);
        if (a <= 0) return;
        AudioEngine.get().play(id, gain * a, pan(x), pitch);
    }
}
