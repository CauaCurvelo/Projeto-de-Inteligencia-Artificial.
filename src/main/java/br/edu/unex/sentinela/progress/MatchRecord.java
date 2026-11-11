package br.edu.unex.sentinela.progress;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro detalhado de uma partida encerrada para exibição no Histórico de Jogos.
 */
public class MatchRecord {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String timestamp;
    private final double survivalSeconds;
    private final int score;
    private final int applesEaten;
    private final int enemiesDefeated;
    private final int daysSurvived;
    private final String causeOfDeath;

    public MatchRecord(double survivalSeconds, int score, int applesEaten, int enemiesDefeated, int daysSurvived, String causeOfDeath) {
        this(LocalDateTime.now().format(FORMATTER), survivalSeconds, score, applesEaten, enemiesDefeated, daysSurvived, causeOfDeath);
    }

    public MatchRecord(String timestamp, double survivalSeconds, int score, int applesEaten, int enemiesDefeated, int daysSurvived, String causeOfDeath) {
        this.timestamp = timestamp;
        this.survivalSeconds = survivalSeconds;
        this.score = score;
        this.applesEaten = applesEaten;
        this.enemiesDefeated = enemiesDefeated;
        this.daysSurvived = daysSurvived;
        this.causeOfDeath = (causeOfDeath != null && !causeOfDeath.isEmpty()) ? causeOfDeath : "Sobrevivência Encerrada";
    }

    public String getTimestamp() { return timestamp; }
    public double getSurvivalSeconds() { return survivalSeconds; }
    public int getScore() { return score; }
    public int getApplesEaten() { return applesEaten; }
    public int getEnemiesDefeated() { return enemiesDefeated; }
    public int getDaysSurvived() { return daysSurvived; }
    public String getCauseOfDeath() { return causeOfDeath; }

    public String getFormattedSurvivalTime() {
        int totalSec = (int) Math.floor(survivalSeconds);
        int mins = totalSec / 60;
        int secs = totalSec % 60;
        return String.format("%02dm %02ds", mins, secs);
    }

    public String toCsvLine() {
        return timestamp + ";" + survivalSeconds + ";" + score + ";" + applesEaten + ";" + enemiesDefeated + ";" + daysSurvived + ";" + causeOfDeath.replace(";", ",");
    }

    public static MatchRecord fromCsvLine(String line) {
        String[] parts = line.split(";", -1);
        if (parts.length < 7) return null;
        try {
            return new MatchRecord(
                parts[0],
                Double.parseDouble(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]),
                Integer.parseInt(parts[5]),
                parts[6]
            );
        } catch (Exception e) {
            return null;
        }
    }
}
