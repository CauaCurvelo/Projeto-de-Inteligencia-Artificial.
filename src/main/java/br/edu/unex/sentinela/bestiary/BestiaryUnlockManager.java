package br.edu.unex.sentinela.bestiary;

import br.edu.unex.sentinela.core.SaveStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de desbloqueio do Bestiário por abate.
 * Cada criatura só é catalogada e revelada após o jogador derrotá-la ao menos uma vez.
 * A Raposa (personagem do jogador) é sempre desbloqueada por padrão.
 */
public final class BestiaryUnlockManager {
    private static final String FILE_NAME = "bestiary_unlocked.properties";
    private static final BestiaryUnlockManager INSTANCE = new BestiaryUnlockManager();

    private final Set<String> unlockedIds = ConcurrentHashMap.newKeySet();

    private BestiaryUnlockManager() {
        unlockedIds.add("fox"); // A Raposa (você) é sempre conhecida
        load();
    }

    public static BestiaryUnlockManager get() {
        return INSTANCE;
    }

    public boolean isUnlocked(String creatureId) {
        if (creatureId == null) return false;
        if ("fox".equalsIgnoreCase(creatureId)) return true;
        return unlockedIds.contains(creatureId.toLowerCase());
    }

    public synchronized boolean recordKill(String creatureId) {
        if (creatureId == null || creatureId.isBlank()) return false;
        String normalized = creatureId.toLowerCase().trim();
        if (unlockedIds.add(normalized)) {
            save();
            return true;
        }
        return false;
    }

    public Set<String> getUnlockedIds() {
        return Collections.unmodifiableSet(unlockedIds);
    }

    public int getUnlockedCount() {
        return unlockedIds.size();
    }

    public void load() {
        File f = SaveStorage.resolveFileForRead(FILE_NAME);
        if (!f.exists()) return;

        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(f)) {
            p.load(in);
            for (String key : p.stringPropertyNames()) {
                if (key.startsWith("unlocked.")) {
                    String id = key.substring("unlocked.".length()).trim().toLowerCase();
                    if (!id.isEmpty()) {
                        unlockedIds.add(id);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public synchronized void save() {
        Properties p = new Properties();
        for (String id : unlockedIds) {
            p.setProperty("unlocked." + id, "true");
        }
        File outFile = SaveStorage.resolveFileForWrite(FILE_NAME);
        try (FileOutputStream out = new FileOutputStream(outFile)) {
            p.store(out, "Instinto Selvagem - Criaturas Desbloqueadas no Bestiario");
        } catch (Exception ignored) {}
    }
}

