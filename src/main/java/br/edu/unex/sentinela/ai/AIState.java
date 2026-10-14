package br.edu.unex.sentinela.ai;

public enum AIState {
    PATROL("Patrulha"),
    SUSPICIOUS("Suspeito (?)"),
    ALERT("Alerta (!)"),
    SNIFFING_TRACK("Farejando Rastro"),
    CHASE("Perseguição"),
    AIMING("Mirando e Atirando"),
    MELEE_ATTACK("Golpe Corpo a Corpo"),
    FIGHTING_ENEMY("Lutando com Predador"),
    SEARCHING_FOOD("Buscando Alimento"),
    EATING("Alimentando-se"),
    TERRITORIAL_ATTACK("Ataque Territorial");

    private final String description;

    AIState(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
