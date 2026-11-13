package br.edu.unex.sentinela.core;

import java.io.File;
import java.util.Locale;

/**
 * Resolve o diretório persistente de saves e configurações do jogador.
 * Evita falhas de permissão em pacotes portáteis/instalados e mantém o progresso
 * separado dos arquivos de distribuição do jogo.
 */
public final class SaveStorage {
    private static final String APP_DIR_WIN = "InstintoSelvagem";
    private static final String APP_DIR_UNIX = ".instinto-selvagem";
    private static final String OVERRIDE_PROPERTY = "sentinela.data.dir";

    private SaveStorage() {}

    public static File resolveDataDir() {
        String override = System.getProperty(OVERRIDE_PROPERTY);
        File dir;
        if (override != null && !override.isBlank()) {
            dir = new File(override.trim());
        } else {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String localAppData = System.getenv("LOCALAPPDATA");
            if (os.contains("win") && localAppData != null && !localAppData.isBlank()) {
                dir = new File(localAppData, APP_DIR_WIN);
            } else {
                dir = new File(System.getProperty("user.home", "."), APP_DIR_UNIX);
            }
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** Retorna o arquivo no diretório de saves do usuário, garantindo a criação da pasta. */
    public static File resolveFileForWrite(String fileName) {
        return new File(resolveDataDir(), fileName);
    }

    /**
     * Procura primeiro no diretório persistente do usuário e faz fallback opcional para
     * o diretório de trabalho (compatibilidade com arquivos locais de desenvolvimento).
     */
    public static File resolveFileForRead(String fileName) {
        File primary = new File(resolveDataDir(), fileName);
        if (primary.exists()) {
            return primary;
        }
        String override = System.getProperty(OVERRIDE_PROPERTY);
        if ((override != null && !override.isBlank()) || System.getProperty("jpackage.app-path") != null) {
            return primary;
        }
        File legacy = new File(fileName);
        return legacy.exists() ? legacy : primary;
    }
}
