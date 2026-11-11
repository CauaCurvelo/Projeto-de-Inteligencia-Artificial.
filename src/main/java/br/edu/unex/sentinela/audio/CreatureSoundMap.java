package br.edu.unex.sentinela.audio;

import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.world.TileType;

/** Mapeia espécie/terreno para o som apropriado (passos, voz de ataque, voz de alerta). */
final class CreatureSoundMap {
    private CreatureSoundMap() {}

    static SoundId footstep(EnemySkin skin, TileType tile) {
        if (tile == TileType.WATER) return SoundId.STEP_WATER;
        if (tile == TileType.MUD) return SoundId.STEP_MUD;
        switch (skin) {
            case HUNTER_SNIPER: return SoundId.STEP_BOOT;
            case BOAR:          return SoundId.STEP_HOOF;
            case BEAR:
            case CROCODILE:     return SoundId.STEP_HEAVY;
            default:            return SoundId.STEP_PAW;
        }
    }

    static SoundId playerFootstep(TileType tile) {
        if (tile == null) return SoundId.STEP_GRASS;
        switch (tile) {
            case TALL_GRASS: return SoundId.STEP_TALL_GRASS;
            case DIRT_PATH:  return SoundId.STEP_DIRT;
            case MUD:        return SoundId.STEP_MUD;
            case WATER:      return SoundId.STEP_WATER;
            case WALL:       return SoundId.STEP_STONE;
            default:         return SoundId.STEP_GRASS;
        }
    }

    /** Voz emitida quando o inimigo prepara um ataque. */
    static SoundId attackVoice(EnemySkin skin) {
        switch (skin) {
            case HOUND_TRACKER: return SoundId.DOG_BARK;
            case WOLF:          return SoundId.WOLF_SNARL;
            case BOAR:          return SoundId.BOAR_GRUNT;
            case BADGER:        return SoundId.BADGER_HISS;
            case BEAR:          return SoundId.BEAR_ROAR;
            case PANTHER:       return SoundId.PANTHER_ROAR;
            case CROCODILE:     return SoundId.CROC_HISS;
            default:            return SoundId.HUNTER_SHOUT;
        }
    }

    /** Voz emitida ao avistar a raposa (entrar em perseguição). */
    static SoundId alertVoice(EnemySkin skin) {
        switch (skin) {
            case HOUND_TRACKER: return SoundId.DOG_BARK;
            case WOLF:          return SoundId.WOLF_HOWL;
            case HUNTER_SNIPER: return SoundId.HUNTER_SHOUT;
            default:            return attackVoice(skin);
        }
    }

    static double footstepGain(EnemySkin skin) {
        switch (skin) {
            case BEAR: case CROCODILE: return 0.9;
            case HUNTER_SNIPER: case BOAR: return 0.6;
            default: return 0.4;
        }
    }
}
