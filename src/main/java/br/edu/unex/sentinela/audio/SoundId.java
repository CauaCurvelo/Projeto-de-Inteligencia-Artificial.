package br.edu.unex.sentinela.audio;

/** Catálogo de todos os sons do jogo categorizados por canal de áudio. */
public enum SoundId {
    // Passos da raposa / terreno
    STEP_GRASS(AudioChannel.STEPS),
    STEP_TALL_GRASS(AudioChannel.STEPS),
    STEP_DIRT(AudioChannel.STEPS),
    STEP_MUD(AudioChannel.STEPS),
    STEP_WATER(AudioChannel.STEPS),
    STEP_STONE(AudioChannel.STEPS),
    STEP_SNOW(AudioChannel.STEPS),

    // Passos de inimigos
    STEP_BOOT(AudioChannel.STEPS),
    STEP_PAW(AudioChannel.STEPS),
    STEP_HOOF(AudioChannel.STEPS),
    STEP_HEAVY(AudioChannel.STEPS),

    // Combate
    BITE_WHOOSH(AudioChannel.SFX),
    HIT_IMPACT(AudioChannel.SFX),
    PLAYER_HURT(AudioChannel.SFX),
    ENEMY_DEATH(AudioChannel.SFX),
    BOW_SHOT(AudioChannel.SFX),
    ARROW_THUD(AudioChannel.SFX),

    // Vozes de animais/inimigos
    DOG_BARK(AudioChannel.CREATURES),
    WOLF_HOWL(AudioChannel.CREATURES),
    WOLF_SNARL(AudioChannel.CREATURES),
    BOAR_GRUNT(AudioChannel.CREATURES),
    BADGER_HISS(AudioChannel.CREATURES),
    BEAR_ROAR(AudioChannel.CREATURES),
    PANTHER_ROAR(AudioChannel.CREATURES),
    CROC_HISS(AudioChannel.CREATURES),
    HUNTER_SHOUT(AudioChannel.CREATURES),

    // IA / estados
    ALERT_STINGER(AudioChannel.SFX),
    SNIFF(AudioChannel.CREATURES),
    LOST_TARGET(AudioChannel.SFX),

    // Ações e avisos do jogador
    EAT_APPLE(AudioChannel.SFX),
    EAT_MEAT(AudioChannel.SFX),
    HEAL(AudioChannel.SFX),
    BURROW_ENTER(AudioChannel.SFX),
    BURROW_EXIT(AudioChannel.SFX),
    SPLASH(AudioChannel.SFX),
    LEAVES_RUSTLE(AudioChannel.SFX),
    HEARTBEAT(AudioChannel.SFX),
    HUNGER_GROWL(AudioChannel.SFX),

    // Interface / Pontuação / Notificações
    DAY_CHIME(AudioChannel.UI),
    WEATHER_SHIFT(AudioChannel.WEATHER),
    GAME_OVER(AudioChannel.UI),
    UI_CLICK(AudioChannel.UI),
    ACHIEVEMENT_UNLOCKED(AudioChannel.UI),

    // Ambiente pontual
    BIRD_CHIRP(AudioChannel.AMBIENCE),
    OWL_HOOT(AudioChannel.AMBIENCE),
    CRICKET(AudioChannel.AMBIENCE),
    THUNDER(AudioChannel.WEATHER),

    // Loops
    LOOP_WIND(AudioChannel.AMBIENCE),
    LOOP_FOREST(AudioChannel.AMBIENCE),
    LOOP_NIGHT(AudioChannel.AMBIENCE),
    LOOP_RIVER(AudioChannel.AMBIENCE),
    LOOP_RAIN(AudioChannel.WEATHER),
    LOOP_STORM_WIND(AudioChannel.WEATHER),

    // Música (Trilha Sonora procedural)
    MUSIC_CLEARING(AudioChannel.MUSIC),
    MUSIC_MEADOW(AudioChannel.MUSIC),
    MUSIC_WALTZ(AudioChannel.MUSIC);

    private final AudioChannel channel;

    SoundId(AudioChannel channel) {
        this.channel = channel;
    }

    public AudioChannel getChannel() {
        return channel;
    }
}
