package br.edu.unex.sentinela.progress;

/**
 * Conquistas do jogo com metadados para exibição na interface e notificação.
 */
public enum Achievement {
    FIRST_BITE(
        "Primeira Mordida",
        "Derrote seu primeiro inimigo na selva.",
        "🗡️",
        "Combate"
    ),
    HUNTER_OF_HUNTERS(
        "Caçador Implacável",
        "Derrube 10 inimigos em uma única corrida.",
        "🏹",
        "Combate"
    ),
    APEX_SLAYER(
        "Matador de Gigantes",
        "Derrote qualquer predador alfa dos biomas.",
        "👑",
        "Combate"
    ),
    FOUR_ALPHAS(
        "Soberano Supremo",
        "Derrote os 4 chefes alfa (Urso Pardo, Pantera, Crocodilo e Urso Polar).",
        "🏆",
        "Combate"
    ),
    NEAR_DEATH(
        "Por um Fio",
        "Vença um combate com apenas 1 coração restante.",
        "❤️",
        "Combate"
    ),
    PREY_HUNTER(
        "Garras Certeiras",
        "Capture 5 presas da fauna silvestre (coelho, cervo, sapo ou pássaro).",
        "🐾",
        "Combate"
    ),
    BIRD_IN_HAND(
        "Mais Rápido que o Vento",
        "Capture um pássaro canoro no solo antes que ele alce voo alto.",
        "🪶",
        "Furtividade"
    ),
    GHOST_FOX(
        "Raposa Fantasma",
        "Esconda-se em uma toca para despistar predadores.",
        "🕳️",
        "Furtividade"
    ),
    TRAP_SURVIVOR(
        "Passos Cautelosos",
        "Escape com vida de uma armadilha de urso armada por caçadores.",
        "⚙️",
        "Sobrevivência"
    ),
    NIGHT_OWL(
        "Coruja Noturna",
        "Sobreviva a 2 noites completas na selva.",
        "🌙",
        "Sobrevivência"
    ),
    RAIN_SURVIVOR(
        "Sob a Tempestade",
        "Sobreviva a 2 tempestades de chuva torrencial.",
        "⛈️",
        "Sobrevivência"
    ),
    SURVIVOR_8MIN(
        "Lorde da Sobrevivência",
        "Resista por mais de 8 minutos na natureza selvagem.",
        "⏱️",
        "Sobrevivência"
    ),
    FORAGER(
        "Fruticultor da Selva",
        "Colha 20 frutas silvestres em uma única corrida.",
        "🍎",
        "Nutrição"
    ),
    CARNIVORE(
        "Banquete Carnívoro",
        "Consuma 10 pedaços de carne deixados por presas ou rivais.",
        "🍖",
        "Nutrição"
    ),
    AQUATIC(
        "Nadador Veloz",
        "Atravesse as correntes de um rio nadando.",
        "💧",
        "Exploração"
    ),
    ICE_SKATER(
        "Drift Glacial",
        "Mantenha um drift em alta velocidade no gelo por 2 segundos contínuos.",
        "❄️",
        "Exploração"
    ),
    LIGHTNING_DODGER(
        "Reflexo de Faísca",
        "Esquive-se de um raio saindo do círculo de aviso elétrico antes do impacto.",
        "⚡",
        "Sobrevivência"
    ),
    WIND_RUNNER(
        "Cavalheiro dos Ventos",
        "Alcance velocidade máxima correndo com o impulso de uma forte ventania.",
        "🍃",
        "Exploração"
    ),
    RABBIT_FLUSHER(
        "Tocaia Perfeita",
        "Desentoca um coelho escondido em uma toca usando uma mordida precisa.",
        "🐰",
        "Combate"
    ),
    STORM_SURVIVOR(
        "Olho da Tempestade",
        "Sobreviva a uma tempestade elétrica completa sem sofrer dano de raio.",
        "⛈️",
        "Sobrevivência"
    ),
    BIOME_VOYAGER(
        "Mochileiro da Natureza",
        "Pise e explore todos os 5 biomas selvagens.",
        "🗺️",
        "Exploração"
    ),
    BESTIARY_OBSERVER(
        "Olho da Sentinela",
        "Aviste 8 espécies diferentes de animais e inimigos.",
        "📜",
        "Exploração"
    ),
    BESTIARY_MASTER(
        "Mestre do Bestiário",
        "Catalogue e derrote todas as 14 espécies da fauna silvestre, rivais e chefes no Bestiário.",
        "📖",
        "Exploração"
    ),
    SCORE_1500(
        "Mestre da Trilha",
        "Alcance 1.500 pontos em uma partida.",
        "⭐",
        "Pontuação"
    ),
    SCORE_7500(
        "Lenda da Floresta",
        "Alcance 7.500 pontos em uma partida lendária.",
        "🌟",
        "Pontuação"
    ),
    FOREST_SPIRIT(
        "Espírito da Floresta",
        "Desbloqueie todas as outras conquistas de Instinto Selvagem e alcance a transcendência eterna.",
        "✨",
        "Lendária"
    );

    private final String title;
    private final String description;
    private final String iconSymbol;
    private final String category;

    Achievement(String title, String description, String iconSymbol, String category) {
        this.title = title;
        this.description = description;
        this.iconSymbol = iconSymbol;
        this.category = category;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getIconSymbol() { return iconSymbol; }
    public String getCategory() { return category; }
}
