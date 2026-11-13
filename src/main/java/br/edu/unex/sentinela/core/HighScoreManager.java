package br.edu.unex.sentinela.core;

import java.io.*;

public class HighScoreManager {
    private static final String FILE_NAME = "highscore.dat";
    private static final long SAVE_DEBOUNCE_NS = 5_000_000_000L;
    private int bestScore = 0;
    private double bestSurvivalTime = 0.0;
    private boolean dirty = false;
    private long lastSaveNs = 0;

    public HighScoreManager() {
        loadHighScore();
    }

    public void checkAndSave(int currentScore, double currentSurvivalTime) {
        if (currentScore > bestScore) {
            bestScore = currentScore;
            dirty = true;
        }
        if (currentSurvivalTime > bestSurvivalTime) {
            bestSurvivalTime = currentSurvivalTime;
            dirty = true;
        }
        flushIfNeeded();
    }

    public void flushIfNeeded() {
        if (!dirty) return;
        long now = System.nanoTime();
        if (now - lastSaveNs >= SAVE_DEBOUNCE_NS) {
            saveHighScore();
            dirty = false;
            lastSaveNs = now;
        }
    }

    public void flushNow() {
        if (!dirty) return;
        saveHighScore();
        dirty = false;
        lastSaveNs = System.nanoTime();
    }

    private void loadHighScore() {
        File file = SaveStorage.resolveFileForRead(FILE_NAME);
        if (!file.exists()) return;

        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            bestScore = in.readInt();
            bestSurvivalTime = in.readDouble();
        } catch (IOException e) {
            // If file corrupted or first run, defaults remain 0
        }
    }

    private void saveHighScore() {
        File file = SaveStorage.resolveFileForWrite(FILE_NAME);
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeInt(bestScore);
            out.writeDouble(bestSurvivalTime);
        } catch (IOException e) {
            // Ignore write errors silently
        }
    }

    public int getBestScore() { return bestScore; }
    public double getBestSurvivalTime() { return bestSurvivalTime; }
}

