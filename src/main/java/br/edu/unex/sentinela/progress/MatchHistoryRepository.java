package br.edu.unex.sentinela.progress;

import br.edu.unex.sentinela.core.SaveStorage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repositório de persistência do histórico de partidas e recordes pessoais.
 */
public final class MatchHistoryRepository {
    private static final String HISTORY_FILE = "match_history.csv";
    private static final String LEGACY_HIGHSCORE = "highscore.dat";
    private static final int MAX_HISTORY_ENTRIES = 50;

    private static final MatchHistoryRepository INSTANCE = new MatchHistoryRepository();

    private final List<MatchRecord> history = new ArrayList<>();
    private final PersonalRecords personalRecords = new PersonalRecords();

    private MatchHistoryRepository() {
        load();
        migrateLegacyHighScoreIfEmpty();
    }

    public static MatchHistoryRepository get() {
        return INSTANCE;
    }

    public synchronized void recordMatch(MatchRecord record) {
        if (record == null || record.getSurvivalSeconds() < 2.0) return;
        history.add(0, record);
        while (history.size() > MAX_HISTORY_ENTRIES) {
            history.remove(history.size() - 1);
        }
        personalRecords.updateWithMatch(record);
        save();
    }

    public synchronized List<MatchRecord> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public synchronized PersonalRecords getPersonalRecords() {
        return personalRecords;
    }

    private void migrateLegacyHighScoreIfEmpty() {
        if (personalRecords.getBestScore() > 0) return;
        File f = SaveStorage.resolveFileForRead(LEGACY_HIGHSCORE);
        if (!f.exists()) return;
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            int score = in.readInt();
            double time = in.readDouble();
            if (score > 0 || time > 0) {
                personalRecords.setBestScore(score);
                personalRecords.setBestSurvivalTime(time);
            }
        } catch (Exception ignored) {}
    }

    public synchronized void load() {
        File f = SaveStorage.resolveFileForRead(HISTORY_FILE);
        if (!f.exists()) return;
        history.clear();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                MatchRecord record = MatchRecord.fromCsvLine(line);
                if (record != null) {
                    history.add(record);
                    personalRecords.updateWithMatch(record);
                }
            }
        } catch (Exception ignored) {}
    }

    public synchronized void save() {
        File f = SaveStorage.resolveFileForWrite(HISTORY_FILE);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            writer.println("# Timestamp;SurvivalSec;Score;Apples;Kills;Days;Cause");
            for (MatchRecord r : history) {
                writer.println(r.toCsvLine());
            }
        } catch (Exception ignored) {}
    }
}
