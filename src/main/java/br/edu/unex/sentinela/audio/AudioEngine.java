package br.edu.unex.sentinela.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Motor de áudio: sintetiza o banco de sons em background, mixa vozes numa thread
 * dedicada e escreve PCM 16-bit estéreo numa SourceDataLine. Falha de dispositivo = jogo mudo.
 */
public final class AudioEngine {
    private static final AudioEngine INSTANCE = new AudioEngine();
    private static final int FRAMES = 512;
    private static final int MAX_VOICES = 28;

    private final Map<SoundId, float[]> bank = new EnumMap<>(SoundId.class);
    private final Map<AudioChannel, Double> channelVolumes = new EnumMap<>(AudioChannel.class);
    private final ConcurrentLinkedQueue<Voice> pending = new ConcurrentLinkedQueue<>();
    private final List<Voice> active = new ArrayList<>();
    private volatile boolean running;
    private volatile boolean ready;
    private volatile boolean muted;
    private volatile double masterVolume = 0.8;

    private AudioEngine() {
        for (AudioChannel ch : AudioChannel.values()) {
            channelVolumes.put(ch, 1.0);
        }
    }

    public static AudioEngine get() { return INSTANCE; }

    public void start() {
        if (running) return;
        running = true;
        Thread t = new Thread(this::runLoop, "audio-mixer");
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    public void shutdown() { running = false; }

    private void synthesizeBank() {
        for (SoundId id : SoundId.values()) {
            bank.put(id, id.name().startsWith("LOOP_") ? AmbienceSynthesizer.create(id) : SfxSynthesizer.create(id));
        }
        ready = true;
    }

    /** Toca um efeito one-shot. Retorna silenciosamente se o banco ainda não estiver pronto. */
    public void play(SoundId id, double gain, double pan, double pitch) {
        if (!ready || muted) return;
        double chVol = getChannelVolume(id.getChannel());
        double finalGain = gain * chVol;
        if (finalGain < 0.01) return;
        pending.add(new Voice(bank.get(id), finalGain, clamp(pan), pitch, false));
    }

    /** Inicia um loop e devolve o handle (ou null se ainda não estiver pronto). */
    public Voice loop(SoundId id) {
        if (!ready) return null;
        Voice v = new Voice(bank.get(id), 0.0, 0.0, 1.0, true);
        pending.add(v);
        return v;
    }

    public boolean isReady() { return ready; }
    public boolean isMuted() { return muted; }
    public void toggleMute() { muted = !muted; }
    public double getMasterVolume() { return masterVolume; }
    public void setMasterVolume(double volume) { masterVolume = Math.max(0, Math.min(1, volume)); }
    public void changeVolume(double delta) { setMasterVolume(masterVolume + delta); }

    public double getChannelVolume(AudioChannel channel) {
        if (channel == null) return 1.0;
        return channelVolumes.getOrDefault(channel, 1.0);
    }

    public void setChannelVolume(AudioChannel channel, double volume) {
        if (channel == null) return;
        channelVolumes.put(channel, Math.max(0, Math.min(1.0, volume)));
    }

    private void runLoop() {
        synthesizeBank();
        AudioFormat fmt = new AudioFormat(DspToolkit.SAMPLE_RATE, 16, 2, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
            line.open(fmt, FRAMES * 4 * 4);
            line.start();
            float[] mix = new float[FRAMES * 2];
            byte[] out = new byte[FRAMES * 4];
            while (running) {
                mixBlock(mix);
                toPcm(mix, out);
                line.write(out, 0, out.length);
            }
            line.drain();
        } catch (Exception e) {
            System.err.println("[Audio] Dispositivo de som indisponível, jogo seguirá mudo: " + e.getMessage());
            running = false;
        }
    }

    private void mixBlock(float[] mix) {
        java.util.Arrays.fill(mix, 0f);
        Voice v;
        while ((v = pending.poll()) != null) {
            if (active.size() >= MAX_VOICES) dropOldestOneShot();
            active.add(v);
        }
        int frames = mix.length / 2;
        for (int i = active.size() - 1; i >= 0; i--) {
            Voice voice = active.get(i);
            voice.render(mix, frames);
            if (voice.isFinished()) active.remove(i);
        }
    }

    private void dropOldestOneShot() {
        for (int i = 0; i < active.size(); i++) {
            if (!active.get(i).loop) { active.remove(i); return; }
        }
    }

    private void toPcm(float[] mix, byte[] out) {
        double g = muted ? 0 : masterVolume;
        for (int i = 0; i < mix.length; i++) {
            double s = Math.tanh(mix[i] * g); // limitador suave anti-clipping
            int v = (int) (s * 32000);
            out[i * 2] = (byte) v;
            out[i * 2 + 1] = (byte) (v >> 8);
        }
    }

    private static double clamp(double p) { return Math.max(-1, Math.min(1, p)); }
}
