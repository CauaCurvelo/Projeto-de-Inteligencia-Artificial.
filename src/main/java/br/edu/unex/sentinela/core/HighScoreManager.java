package br.edu.unex.sentinela.core;

import java.io.*;

public class HighScoreManager {
    private static final String FILE_NAME = "highscore.dat";
    private int bestScore = 0;
    private double bestSurvivalTime = 0.0;

    public HighScoreManager() {
        loadHighScore();
    }

    public void checkAndSave(int currentScore, double currentSurvivalTime) {
        boolean changed = false;
        if (currentScore > bestScore) {
            bestScore = currentScore;
            changed = true;
        }
        if (currentSurvivalTime > bestSurvivalTime) {
            bestSurvivalTime = currentSurvivalTime;
            changed = true;
        }
        if (changed) {
            saveHighScore();
        }
    }

    private void loadHighScore() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            bestScore = in.readInt();
            bestSurvivalTime = in.readDouble();
        } catch (IOException e) {
            // If file corrupted or first run, defaults remain 0
        }
    }

    private void saveHighScore() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(FILE_NAME))) {
            out.writeInt(bestScore);
            out.writeDouble(bestSurvivalTime);
        } catch (IOException e) {
            // Ignore write errors silently
        }
    }

    public int getBestScore() { return bestScore; }
    public double getBestSurvivalTime() { return bestSurvivalTime; }
}

