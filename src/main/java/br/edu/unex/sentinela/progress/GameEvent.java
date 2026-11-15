package br.edu.unex.sentinela.progress;

import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.world.BiomeType;

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
        GAME_OVER,
        PASSIVE_HUNTED,
        BIRD_CAUGHT,
        ICE_DRIFT,
        LIGHTNING_DODGED,
        WIND_BOOSTED,
        RABBIT_FLUSHED,
        STORM_SURVIVED,
        TRAP_TRIGGERED,
        BIOME_VISITED
    }

    private final Type type;
    private final EnemySkin targetSkin;
    private final int value;
    private final String message;
    private final BiomeType biome;

    public GameEvent(Type type) {
        this(type, null, 1, "", null);
    }

    public GameEvent(Type type, EnemySkin targetSkin, int value, String message) {
        this(type, targetSkin, value, message, null);
    }

    public GameEvent(Type type, BiomeType biome) {
        this(type, null, 1, "", biome);
    }

    public GameEvent(Type type, EnemySkin targetSkin, int value, String message, BiomeType biome) {
        this.type = type;
        this.targetSkin = targetSkin;
        this.value = value;
        this.message = message;
        this.biome = biome;
    }

    public Type getType() { return type; }
    public EnemySkin getTargetSkin() { return targetSkin; }
    public int getValue() { return value; }
    public String getMessage() { return message; }
    public BiomeType getBiome() { return biome; }
}
