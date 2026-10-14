package br.edu.unex.sentinela.ai;

public enum EnemySkin {
    // Rivals
    BOAR("Javali Selvagem", "#5D4037"),
    WOLF("Lobo da Floresta", "#546E7A"),
    BADGER("Texugo Feroz", "#37474F"),

    // Bosses
    BEAR("Urso Pardo Alfa", "#4A2E16"),
    CROCODILE("Crocodilo do Rio", "#1B5E20"),
    PANTHER("Pantera Negra", "#212121");

    private final String displayName;
    private final String hexColor;

    EnemySkin(String displayName, String hexColor) {
        this.displayName = displayName;
        this.hexColor = hexColor;
    }

    public String getDisplayName() { return displayName; }
    public String getHexColor() { return hexColor; }
}

