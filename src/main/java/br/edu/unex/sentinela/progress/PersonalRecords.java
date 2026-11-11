package br.edu.unex.sentinela.progress;

/**
 * Estatísticas consolidadas e recordes pessoais (Personal Records) do jogador.
 */
public class PersonalRecords {
    private int bestScore = 0;
    private double bestSurvivalTime = 0.0;
    private int maxKillsSingleRun = 0;
    private int maxApplesSingleRun = 0;
    private int totalMatchesPlayed = 0;
    private int totalKillsAccumulated = 0;
    private int totalApplesAccumulated = 0;
    private double totalTimePlayed = 0.0;

    public void updateWithMatch(MatchRecord match) {
        if (match == null) return;
        totalMatchesPlayed++;
        totalKillsAccumulated += match.getEnemiesDefeated();
        totalApplesAccumulated += match.getApplesEaten();
        totalTimePlayed += match.getSurvivalSeconds();

        if (match.getScore() > bestScore) {
            bestScore = match.getScore();
        }
        if (match.getSurvivalSeconds() > bestSurvivalTime) {
            bestSurvivalTime = match.getSurvivalSeconds();
        }
        if (match.getEnemiesDefeated() > maxKillsSingleRun) {
            maxKillsSingleRun = match.getEnemiesDefeated();
        }
        if (match.getApplesEaten() > maxApplesSingleRun) {
            maxApplesSingleRun = match.getApplesEaten();
        }
    }

    public int getBestScore() { return bestScore; }
    public void setBestScore(int v) { this.bestScore = v; }

    public double getBestSurvivalTime() { return bestSurvivalTime; }
    public void setBestSurvivalTime(double v) { this.bestSurvivalTime = v; }

    public int getMaxKillsSingleRun() { return maxKillsSingleRun; }
    public void setMaxKillsSingleRun(int v) { this.maxKillsSingleRun = v; }

    public int getMaxApplesSingleRun() { return maxApplesSingleRun; }
    public void setMaxApplesSingleRun(int v) { this.maxApplesSingleRun = v; }

    public int getTotalMatchesPlayed() { return totalMatchesPlayed; }
    public void setTotalMatchesPlayed(int v) { this.totalMatchesPlayed = v; }

    public int getTotalKillsAccumulated() { return totalKillsAccumulated; }
    public void setTotalKillsAccumulated(int v) { this.totalKillsAccumulated = v; }

    public int getTotalApplesAccumulated() { return totalApplesAccumulated; }
    public void setTotalApplesAccumulated(int v) { this.totalApplesAccumulated = v; }

    public double getTotalTimePlayed() { return totalTimePlayed; }
    public void setTotalTimePlayed(double v) { this.totalTimePlayed = v; }

    public String getFormattedBestTime() {
        int totalSec = (int) Math.floor(bestSurvivalTime);
        return String.format("%02dm %02ds", totalSec / 60, totalSec % 60);
    }

    public String getFormattedTotalTime() {
        int totalSec = (int) Math.floor(totalTimePlayed);
        int hours = totalSec / 3600;
        int mins = (totalSec % 3600) / 60;
        int secs = totalSec % 60;
        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, mins, secs);
        }
        return String.format("%02dm %02ds", mins, secs);
    }
}
