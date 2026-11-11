package br.edu.unex.sentinela.bestiary;

import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;

/**
 * Informações e ficha técnica detalhada de cada animal e personagem do jogo.
 */
public class CreatureInfo {
    private final String id;
    private final String name;
    private final String title;
    private final EnemySkin skin; // null para a Raposa
    private final EnemyType type; // null para a Raposa
    private final String diet;
    private final String habitat;
    private final String attackType;
    private final int speedStars;   // 1 a 5
    private final int dangerStars;  // 1 a 5
    private final int healthPoints;
    private final String behaviorDescription;
    private final String survivalTip;

    public CreatureInfo(String id, String name, String title, EnemySkin skin, EnemyType type,
                        String diet, String habitat, String attackType,
                        int speedStars, int dangerStars, int healthPoints,
                        String behaviorDescription, String survivalTip) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.skin = skin;
        this.type = type;
        this.diet = diet;
        this.habitat = habitat;
        this.attackType = attackType;
        this.speedStars = speedStars;
        this.dangerStars = dangerStars;
        this.healthPoints = healthPoints;
        this.behaviorDescription = behaviorDescription;
        this.survivalTip = survivalTip;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTitle() { return title; }
    public EnemySkin getSkin() { return skin; }
    public EnemyType getType() { return type; }
    public String getDiet() { return diet; }
    public String getHabitat() { return habitat; }
    public String getAttackType() { return attackType; }
    public int getSpeedStars() { return speedStars; }
    public int getDangerStars() { return dangerStars; }
    public int getHealthPoints() { return healthPoints; }
    public String getBehaviorDescription() { return behaviorDescription; }
    public String getSurvivalTip() { return survivalTip; }
}
