package br.edu.unex.sentinela.progress;

import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.audio.AudioEngine;
import br.edu.unex.sentinela.audio.SoundId;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Monitor e gerenciador de conquistas em tempo real.
 */
public final class AchievementTracker {
    private static final String FILE_NAME = "achievements.properties";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final AchievementTracker INSTANCE = new AchievementTracker();

    private final Map<Achievement, String> unlocked = new ConcurrentHashMap<>();
    private final Set<EnemySkin> spottedSpecies = Collections.synchronizedSet(new HashSet<>());
    private Consumer<Achievement> unlockListener;

    // Estatísticas da corrida atual
    private int runKills = 0;
    private int runApples = 0;
    private int runMeat = 0;
    private int runScore = 0;
    private double runTime = 0.0;
    private int runLives = 3;

    private AchievementTracker() {
        load();
        GameEventBus.get().subscribe(this::handleEvent);
    }

    public static AchievementTracker get() {
        return INSTANCE;
    }

    public void setUnlockListener(Consumer<Achievement> listener) {
        this.unlockListener = listener;
    }

    public void resetRunStats() {
        runKills = 0;
        runApples = 0;
        runMeat = 0;
        runScore = 0;
        runTime = 0.0;
        runLives = 3;
    }

    public void updateTick(double survivalTime, int score, int lives) {
        this.runTime = survivalTime;
        this.runScore = score;
        this.runLives = lives;

        if (runTime >= 300.0) {
            unlock(Achievement.SURVIVOR_5MIN);
        }
        if (runScore >= 1000) {
            unlock(Achievement.SCORE_1000);
        }
        if (runScore >= 5000) {
            unlock(Achievement.SCORE_5000);
        }
    }

    private void handleEvent(GameEvent e) {
        switch (e.getType()) {
            case ENEMY_DEFEATED:
                runKills++;
                unlock(Achievement.FIRST_BITE);
                if (runKills >= 5) unlock(Achievement.HUNTER_OF_HUNTERS);
                if (runLives <= 1) unlock(Achievement.NEAR_DEATH);
                break;
            case BOSS_DEFEATED:
                unlock(Achievement.APEX_SLAYER);
                break;
            case APPLE_EATEN:
                runApples++;
                if (runApples >= 10) unlock(Achievement.FORAGER);
                break;
            case MEAT_EATEN:
                runMeat++;
                if (runMeat >= 5) unlock(Achievement.CARNIVORE);
                break;
            case BURROW_ENTERED:
                unlock(Achievement.GHOST_FOX);
                break;
            case RIVER_SWAM:
                unlock(Achievement.AQUATIC);
                break;
            case RAIN_SURVIVED:
                unlock(Achievement.RAIN_SURVIVOR);
                break;
            case DAY_CYCLE_PASSED:
                unlock(Achievement.NIGHT_OWL);
                break;
            case CREATURE_SPOTTED:
                if (e.getTargetSkin() != null) {
                    spottedSpecies.add(e.getTargetSkin());
                    if (spottedSpecies.size() >= 5) {
                        unlock(Achievement.BESTIARY_OBSERVER);
                    }
                }
                break;
            case GAME_OVER:
                resetRunStats();
                break;
            default:
                break;
        }
    }

    public synchronized void unlock(Achievement a) {
        if (unlocked.containsKey(a)) return;
        String dateStr = LocalDateTime.now().format(FORMATTER);
        unlocked.put(a, dateStr);
        save();

        AudioEngine.get().play(SoundId.ACHIEVEMENT_UNLOCKED, 1.0, 0.0, 1.0);

        if (unlockListener != null) {
            unlockListener.accept(a);
        }
    }

    public boolean isUnlocked(Achievement a) {
        return unlocked.containsKey(a);
    }

    public String getUnlockDate(Achievement a) {
        return unlocked.get(a);
    }

    public int getUnlockedCount() {
        return unlocked.size();
    }

    public Set<EnemySkin> getSpottedSpecies() {
        return Collections.unmodifiableSet(spottedSpecies);
    }

    public void load() {
        File f = new File(FILE_NAME);
        if (!f.exists()) return;
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(f)) {
            p.load(in);
            for (String key : p.stringPropertyNames()) {
                if (key.startsWith("seen.")) {
                    try {
                        spottedSpecies.add(EnemySkin.valueOf(key.substring(5)));
                    } catch (Exception ignored) {}
                } else {
                    try {
                        Achievement a = Achievement.valueOf(key);
                        unlocked.put(a, p.getProperty(key));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    public void save() {
        Properties p = new Properties();
        for (Map.Entry<Achievement, String> entry : unlocked.entrySet()) {
            p.setProperty(entry.getKey().name(), entry.getValue());
        }
        for (EnemySkin skin : spottedSpecies) {
            p.setProperty("seen." + skin.name(), "true");
        }
        try (FileOutputStream out = new FileOutputStream(FILE_NAME)) {
            p.store(out, "Instinto Selvagem - Conquistas");
        } catch (Exception ignored) {}
    }
}
