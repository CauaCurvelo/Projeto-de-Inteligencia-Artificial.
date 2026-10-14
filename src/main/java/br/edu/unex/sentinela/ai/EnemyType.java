package br.edu.unex.sentinela.ai;

public enum EnemyType {
    HUNTER("Caçador", "#E53935", 3, 200),          // 3 HP, 200 pts
    HOUND("Cão de Caça", "#8E24AA", 2, 150),       // 2 HP, 150 pts
    RIVAL("Javali Territorial", "#FB8C00", 4, 300), // 4 HP, 300 pts
    BEAR_BOSS("Urso Alfa (Superpredador)", "#8D6E63", 10, 1000); // 10 HP, 1000 pts!

    private final String displayName;
    private final String hexColor;
    private final int maxHp;
    private final int defeatScore;

    EnemyType(String displayName, String hexColor, int maxHp, int defeatScore) {
        this.displayName = displayName;
        this.hexColor = hexColor;
        this.maxHp = maxHp;
        this.defeatScore = defeatScore;
    }

    public String getDisplayName() { return displayName; }
    public String getHexColor() { return hexColor; }
    public int getMaxHp() { return maxHp; }
    public int getDefeatScore() { return defeatScore; }
}
