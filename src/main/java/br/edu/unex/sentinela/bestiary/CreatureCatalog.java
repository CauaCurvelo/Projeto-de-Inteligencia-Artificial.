package br.edu.unex.sentinela.bestiary;

import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Catálogo completo de todas as criaturas da fauna e personagens do ecossistema.
 */
public final class CreatureCatalog {
    private static final List<CreatureInfo> CREATURES = new ArrayList<>();

    static {
        // 1. Raposa Silvestre
        CREATURES.add(new CreatureInfo(
            "FOX", "Raposa Silvestre", "Sentinela da Floresta (Você)",
            null, null,
            "Onívora (Maçãs e Carnes)", "Bosques, Clareiras e Tocas Subterrâneas",
            "Mordida Rápida (Espaço ou Clique)", 4, 3, 3,
            "Predadora astuta dotada de grande furtividade. Consegue se esconder em tocas e na mata alta para quebrar o rastro de cheiro e linha de visão dos inimigos.",
            "Use o sprint com moderação para não esgotar a fome rapidamente e mergulhe em rios para lavar seu rastro de cheiro."
        ));

        // 2. Caçador Humano
        CREATURES.add(new CreatureInfo(
            "HUNTER", "Caçador Humano", "Predador Racional com Arco Recurvo",
            EnemySkin.HUNTER_SNIPER, EnemyType.HUNTER,
            "Alimentos Humanos / Caça", "Trilhas de Terra e Clareiras",
            "Disparo Balístico de Flechas à Longa Distância", 2, 5, 2,
            "Patrulha trilhas armadas e dispara flechas mortais assim que avista o jogador. Frequentemente acompanhado por Cães Rastreadores.",
            "Desvie em zigue-zague ao ver a mira vermelha se fixando e utilize árvores como cobertura contra disparos."
        ));

        // 3. Cão Rastreador
        CREATURES.add(new CreatureInfo(
            "HOUND", "Cão Rastreador", "Sabujo de Faro Fino",
            EnemySkin.HOUND_TRACKER, EnemyType.HOUND,
            "Carnívoro (Rações e Caça)", "Arredores de Caçadores e Bosques Abertos",
            "Mordida e Alerta Sonoro aos Aliados", 5, 4, 2,
            "Fareja o rastro invisível deixado pelo jogador no solo. Quando detecta o cheiro, late alto alertando caçadores próximos e inicia perseguição veloz.",
            "Atravesse a água de rios ou poças de lama para dissolver instantaneamente seu rastro de cheiro."
        ));

        // 4. Lobo da Floresta
        CREATURES.add(new CreatureInfo(
            "WOLF", "Lobo da Floresta", "Caçador Noturno de Matilha",
            EnemySkin.WOLF, EnemyType.RIVAL,
            "Carnívoro Estrito (Carne Fresca)", "Florestas Densas e Bosques Escuros",
            "Investida Feroz e Dentição Cortante", 4, 4, 3,
            "Uiva ao detectar invasores e persegue presas vorazmente. Caça tanto a raposa quanto itens de carne caídos.",
            "Lute no momento em que ele recuar para respirar ou use tocas para fazê-lo perder o interesse."
        ));

        // 5. Javali Selvagem
        CREATURES.add(new CreatureInfo(
            "BOAR", "Javali Selvagem", "Besta Territorial de Presas Curvas",
            EnemySkin.BOAR, EnemyType.RIVAL,
            "Herbívoro e Raízes (Maçãs e Frutos)", "Lodaçais, Pântanos e Matas Fechadas",
            "Carga Frontal com Presas Esmagadoras", 4, 3, 3,
            "Extremamente territorial. Não tolera intrusos em seu território e compete ferozmente por maçãs caídas no chão.",
            "Esquive para o lado no momento em que ele iniciar a carga em linha reta; ele demora para manobrar."
        ));

        // 6. Texugo Feroz
        CREATURES.add(new CreatureInfo(
            "BADGER", "Texugo Feroz", "Guerreiro Compacto e Destemido",
            EnemySkin.BADGER, EnemyType.RIVAL,
            "Onívoro Oportunista", "Sub-bosques e Formações Rochosas",
            "Mordida Rápida com Garras Subterrâneas", 3, 2, 2,
            "Baixa estatura e temperamento explosivo. Defende tocas e alimentos até a morte com sibilos agressivos.",
            "Pode ser abatido rapidamente com dois ataques precisos antes que consiga contra-atacar."
        ));

        // 7. Urso Pardo Alfa (Chefe)
        CREATURES.add(new CreatureInfo(
            "BEAR", "Urso Pardo Alfa", "Soberano Titânico das Montanhas",
            EnemySkin.BEAR, EnemyType.BEAR_BOSS,
            "Onívoro Supremo (Carne, Peixes e Frutos)", "Colinas Rochosas e Grandes Paredões",
            "Patada Sísmica Esmagadora (Tremor na Tela)", 3, 5, 6,
            "Predador alfa colossal dotado de altíssima vitalidade. Seus ataques geram impacto devastador e eliminam a raposa em poucos golpes.",
            "Ataque pelas costas logo após ele errar uma patada e mantenha distância durante o golpe telegrafado."
        ));

        // 8. Pantera Negra (Chefe)
        CREATURES.add(new CreatureInfo(
            "PANTHER", "Pantera Negra", "Sombra Furtiva da Floresta",
            EnemySkin.PANTHER, EnemyType.BEAR_BOSS,
            "Carnívora Letal", "Copas Densas e Selva Profunda",
            "Salto Furtivo e Dilaceração", 5, 5, 5,
            "Velocidade incomparável. Circunda a presa silenciosamente antes de desferir um bote mortal.",
            "Escute com atenção os rosnados para identificar de qual lado ela está se aproximando."
        ));

        // 9. Crocodilo do Rio (Chefe)
        CREATURES.add(new CreatureInfo(
            "CROCODILE", "Crocodilo do Rio", "Terror Emboscador dos Rios",
            EnemySkin.CROCODILE, EnemyType.BEAR_BOSS,
            "Carnívoro Anfíbio", "Margens de Rios e Águas Rasas",
            "Mordida de Mandíbula de Aço", 3, 5, 6,
            "Habita as margens da água. Embora mais lento em terra firme, domina as zonas ribeirinhas com ataques brutais.",
            "Evite nadar perto de onde um crocodilo foi avistado; sua velocidade na água é perigosamente alta."
        ));
    }

    private CreatureCatalog() {}

    public static List<CreatureInfo> getAll() {
        return Collections.unmodifiableList(CREATURES);
    }

    public static CreatureInfo getById(String id) {
        for (CreatureInfo c : CREATURES) {
            if (c.getId().equalsIgnoreCase(id)) return c;
        }
        return CREATURES.get(0);
    }
}
