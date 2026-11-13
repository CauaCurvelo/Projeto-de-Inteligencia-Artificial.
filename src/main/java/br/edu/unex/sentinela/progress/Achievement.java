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
        "Caçador de Caçadores",
        "Derrube 5 inimigos em uma única corrida.",
        "🏹",
        "Combate"
    ),
    APEX_SLAYER(
        "Matador de Gigantes",
        "Derrote qualquer predador alfa dos biomas.",
        "👑",
        "Combate"
    ),
    NIGHT_OWL(
        "Coruja Noturna",
        "Sobreviva ao seu primeiro ciclo de noite completa.",
        "🌙",
        "Sobrevivência"
    ),
    FORAGER(
        "Fruticultor da Selva",
        "Colha 10 frutas silvestres em uma única corrida.",
        "🍎",
        "Nutrição"
    ),
    CARNIVORE(
        "Banquete Carnívoro",
        "Consuma 5 pedaços de carne deixados por rivais.",
        "🍖",
        "Nutrição"
    ),
    GHOST_FOX(
        "Raposa Fantasma",
        "Esconda-se em uma toca para despistar predadores.",
        "🕳️",
        "Furtividade"
    ),
    AQUATIC(
        "Nadador Veloz",
        "Atravesse as correntes de um rio nadando.",
        "💧",
        "Exploração"
    ),
    RAIN_SURVIVOR(
        "Sob a Tempestade",
        "Sobreviva a um período de chuva torrencial.",
        "⛈️",
        "Sobrevivência"
    ),
    SURVIVOR_5MIN(
        "Sobrevivente Nato",
        "Resista por mais de 5 minutos na natureza selvagem.",
        "⏱️",
        "Sobrevivência"
    ),
    SCORE_1000(
        "Mestre da Trilha",
        "Alcance 1.000 pontos em uma partida.",
        "⭐",
        "Pontuação"
    ),
    SCORE_5000(
        "Lenda da Floresta",
        "Alcance 5.000 pontos em uma partida lendária.",
        "🌟",
        "Pontuação"
    ),
    NEAR_DEATH(
        "Por um Fio",
        "Vença um combate com apenas 1 coração restante.",
        "❤️",
        "Combate"
    ),
    BESTIARY_OBSERVER(
        "Olho da Sentinela",
        "Aviste 5 espécies diferentes de animais e inimigos.",
        "📜",
        "Exploração"
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
