package br.edu.unex.sentinela.bestiary;

import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;

import java.util.List;

/**
 * Catálogo conciso e escaneável de todas as espécies do mundo de Sentinela.
 */
public final class CreatureCatalog {
    private static final List<CreatureInfo> ALL = List.of(
        new CreatureInfo(
            "fox", "Raposa Silvestre", "Sentinela (Você)",
            null, null,
            "Onívora (Frutas e Carnes)",
            "Todos os Biomas e Tocas",
            "Mordida Rápida",
            4, 2, 3,
            "Furtiva em tocas e mata alta; frutas regionais também recuperam parte da stamina.",
            "Regule o sprint e explore pântanos, matas fechadas, serras e tundras nevadas."
        ),
        new CreatureInfo(
            "hunter", "Caçador Armado", "Patrulheiro · Variações Regionais",
            EnemySkin.HUNTER_SNIPER, EnemyType.HUNTER,
            "Humano",
            "Clareiras, Brejos, Matas, Serras e Neve",
            "Arco e Flecha",
            3, 4, 3,
            "Traja uniformes regionais (Mateiro, do Brejo, Alpino, Ártico) e dispara flechas precisas.",
            "Quebre a linha de visão usando árvores ou paredões de granito e flanqueie."
        ),
        new CreatureInfo(
            "hound", "Cão Rastreador", "Farejador · Variações Regionais",
            EnemySkin.HOUND_TRACKER, EnemyType.HOUND,
            "Carnívoro",
            "Matilhas (Brejo, Selva, Serra, Ártico)",
            "Perseguição e Latido",
            4, 3, 2,
            "Segue pegadas no solo; variedades do brejo e árticas ignoram a lentidão de lama/slush.",
            "Mergulhe em rios ou esconda-se na toca para cortar o rastro de cheiro."
        ),
        new CreatureInfo(
            "wolf", "Lobo Cinzento", "Predador · Variedades Regionais & Ártico",
            EnemySkin.WOLF, EnemyType.RIVAL,
            "Carnívoro",
            "Bosques, Selvas, Serras e Neve",
            "Investida Ágil",
            4, 4, 4,
            "Carnívoro puro de patrulha ampla; Lobos Brancos e Serranos são ágeis e têm faro aguçado.",
            "Evite combates frontais em campo aberto; ataque pelos flancos."
        ),
        new CreatureInfo(
            "boar", "Javali Selvagem", "Territorial · Lodo / Pintado / Neves",
            EnemySkin.BOAR, EnemyType.RIVAL,
            "Onívoro",
            "Pântanos, Matas, Bosques e Neve",
            "Carga de Presas",
            3, 4, 4,
            "Javalis do Lodo e das Neves cruzam terreno úmido sem lentidão; os Serranos têm couro duro.",
            "Desvie lateralmente da investida para expor o flanco."
        ),
        new CreatureInfo(
            "badger", "Texugo Feroz", "Guardião · Brejo / Granito / Polar",
            EnemySkin.BADGER, EnemyType.RIVAL,
            "Onívoro",
            "Tocas, Brejos, Serras e Neve",
            "Garrada Contínua",
            3, 3, 4,
            "Guardião solitário de tocas e rochas; Texugos de Granito e Polares têm vida extra.",
            "Mantenha rota de fuga livre contra encostas de rocha."
        ),
        new CreatureInfo(
            "bear", "Urso Pardo Alfa", "Chefe Nativo · Serra Granítica",
            EnemySkin.BEAR, EnemyType.BEAR_BOSS,
            "Onívoro",
            "Serra Granítica e Encostas",
            "Esmagamento Sísmico",
            3, 5, 10,
            "Rei das encostas rochosas; salta em onda sísmica devastadora ao defender o covil.",
            "Aguarde o impacto no solo e ataque na janela de recuperação."
        ),
        new CreatureInfo(
            "panther", "Pantera Negra", "Chefe Nativo · Mata Fechada",
            EnemySkin.PANTHER, EnemyType.BEAR_BOSS,
            "Carnívora",
            "Mata Fechada",
            "Bote Furtivo",
            5, 5, 10,
            "Soberana da Mata Fechada; quase invisível sob as copas, desfere botes duplos.",
            "Observe os movimentos na vegetação densa e não caminhe em linha reta."
        ),
        new CreatureInfo(
            "crocodile", "Crocodilo do Rio", "Chefe Nativo · Pântano Sombrio",
            EnemySkin.CROCODILE, EnemyType.BEAR_BOSS,
            "Carnívoro",
            "Pântano Sombrio e Rios",
            "Bote de Mandíbula",
            4, 5, 10,
            "Apex do Pântano; ganha altíssima velocidade em águas turvas e lamaçais.",
            "Lute apenas em terra firme e nunca cruze rios sob vigilância."
        ),
        new CreatureInfo(
            "polar_bear", "Urso Polar Alfa", "Chefe Nativo · Tundra Nevada",
            EnemySkin.POLAR_BEAR, EnemyType.BEAR_BOSS,
            "Carnívoro",
            "Tundra Nevada e Lagos Glaciais",
            "Impacto Glacial",
            4, 5, 10,
            "Colosso da nevasca; atravessa águas geladas com facilidade e esmaga o gelo ao redor.",
            "Afaste-se da onda de impacto e atraia-o para fora dos lagos congelados."
        )
    );

    private CreatureCatalog() {}

    public static List<CreatureInfo> getAll() {
        return ALL;
    }
}
