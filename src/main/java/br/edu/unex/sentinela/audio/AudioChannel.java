package br.edu.unex.sentinela.audio;

/**
 * Canais de mixagem de áudio independentes para ajuste refinado nas configurações.
 */
public enum AudioChannel {
    MASTER("Geral"),
    SFX("Efeitos Sonoros"),
    STEPS("Passos e Movimento"),
    CREATURES("Vozes e Criaturas"),
    AMBIENCE("Ambiente da Natureza"),
    WEATHER("Clima e Chuva"),
    MUSIC("Trilha Sonora"),
    UI("Interface e Menus");

    private final String displayName;

    AudioChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
