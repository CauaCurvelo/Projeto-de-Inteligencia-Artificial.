package br.edu.unex.sentinela.audio;

import br.edu.unex.sentinela.ai.AIState;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.world.World;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Controlador da playlist musical da partida: rotaciona entre 3 temas leves
 * com crossfade suave, atenuação tática em perseguição/toca/pausa e silêncio nos menus.
 */
final class MusicController {
    private static final SoundId[] TRACKS = {
        SoundId.MUSIC_CLEARING,
        SoundId.MUSIC_MEADOW,
        SoundId.MUSIC_WALTZ
    };
    private static final double BASE_MIX = 0.42;
    private static final double TRACK_ROTATE_SEC = 58.0;
    private static final double CROSSFADE_SEC = 2.5;

    private final Map<SoundId, Voice> voices = new EnumMap<>(SoundId.class);
    private final double[] weights = new double[TRACKS.length];
    private final Random rand = new Random();

    private int activeIndex = 0;
    private int lastDay = -1;
    private double playTimer = 0.0;
    private double lastSurvivalTime = 0.0;

    void update(World world, double dt) {
        AudioEngine engine = AudioEngine.get();
        if (!engine.isReady()) return;
        ensureInitialized(engine);

        if (world.isGameOver()) {
            silenceAll();
            lastSurvivalTime = world.getSurvivalTime();
            return;
        }
        if (world.getSurvivalTime() + 0.5 < lastSurvivalTime) {
            resetForNewMatch(world);
        }
        lastSurvivalTime = world.getSurvivalTime();

        if (lastDay < 0) {
            lastDay = world.getDayCycleNumber();
        }
        playTimer += dt;
        boolean dayTurned = world.getDayCycleNumber() > lastDay;
        if (dayTurned) {
            lastDay = world.getDayCycleNumber();
        }
        if (playTimer >= TRACK_ROTATE_SEC || (dayTurned && playTimer >= 22.0)) {
            advanceTrack();
        }

        stepCrossfade(dt);
        double duck = computeGameplayDuck(world);
        applyGains(BASE_MIX * duck);
    }

    void updateMenuOrPaused(World world, boolean inMatchMenu) {
        if (voices.isEmpty()) return;
        if (!inMatchMenu || world == null || world.isGameOver()) {
            silenceAll();
            if (!inMatchMenu) {
                playTimer = 0.0;
                lastDay = -1;
            }
            return;
        }
        double burrowedFactor = (world.getPlayer() != null && world.getPlayer().isBurrowed()) ? 0.55 : 1.0;
        applyGains(BASE_MIX * 0.35 * burrowedFactor);
    }

    void resetForNewMatch(World world) {
        playTimer = 0.0;
        lastDay = world != null ? world.getDayCycleNumber() : 1;
        activeIndex = rand.nextInt(TRACKS.length);
        for (int i = 0; i < weights.length; i++) {
            weights[i] = (i == activeIndex) ? 1.0 : 0.0;
        }
    }

    private void ensureInitialized(AudioEngine engine) {
        if (!voices.isEmpty()) return;
        for (SoundId id : TRACKS) {
            voices.put(id, engine.loop(id));
        }
        weights[activeIndex] = 1.0;
    }

    private void advanceTrack() {
        int step = 1 + rand.nextInt(TRACKS.length - 1);
        activeIndex = (activeIndex + step) % TRACKS.length;
        playTimer = 0.0;
    }

    private void stepCrossfade(double dt) {
        double delta = Math.max(0.0, dt) / CROSSFADE_SEC;
        for (int i = 0; i < TRACKS.length; i++) {
            double target = (i == activeIndex) ? 1.0 : 0.0;
            if (weights[i] < target) {
                weights[i] = Math.min(1.0, weights[i] + delta);
            } else if (weights[i] > target) {
                weights[i] = Math.max(0.0, weights[i] - delta);
            }
        }
    }

    private double computeGameplayDuck(World world) {
        if (world.getPlayer() != null && world.getPlayer().isBurrowed()) {
            return 0.55;
        }
        for (Enemy e : world.getEnemies()) {
            AIState s = e.getState();
            if (s == AIState.CHASE || s == AIState.AIMING || s == AIState.ROARING) {
                return 0.72;
            }
        }
        return 1.0;
    }

    private void applyGains(double masterMix) {
        double chVol = AudioEngine.get().getChannelVolume(AudioChannel.MUSIC);
        for (int i = 0; i < TRACKS.length; i++) {
            Voice v = voices.get(TRACKS[i]);
            if (v != null) {
                v.targetGain = Math.max(0.0, masterMix * weights[i] * chVol);
            }
        }
    }

    private void silenceAll() {
        for (SoundId id : TRACKS) {
            Voice v = voices.get(id);
            if (v != null) {
                v.targetGain = 0.0;
            }
        }
    }
}
