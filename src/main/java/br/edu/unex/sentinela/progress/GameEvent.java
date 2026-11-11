package br.edu.unex.sentinela.progress;

import br.edu.unex.sentinela.ai.EnemySkin;

/**
 * Eventos emitidos pela gameplay para tracking de estatísticas, conquistas e histórico.
 */
public class GameEvent {
    public enum Type {
        ENEMY_DEFEATED,
        BOSS_DEFEATED,
        APPLE_EATEN,
        MEAT_EATEN,
        BURROW_ENTERED,
        RIVER_SWAM,
        RAIN_SURVIVED,
        DAY_CYCLE_PASSED,
        CREATURE_SPOTTED,
        DAMAGE_TAKEN,
        GAME_OVER
    }

    private final Type type;
    private final EnemySkin targetSkin;
    private final int value;
    private final String message;

    public GameEvent(Type type) {
        this(type, null, 1, "");
    }

    public GameEvent(Type type, EnemySkin targetSkin, int value, String message) {
        this.type = type;
        this.targetSkin = targetSkin;
        this.value = value;
        this.message = message;
    }

    public Type getType() { return type; }
    public EnemySkin getTargetSkin() { return targetSkin; }
    public int getValue() { return value; }
    public String getMessage() { return message; }
}
