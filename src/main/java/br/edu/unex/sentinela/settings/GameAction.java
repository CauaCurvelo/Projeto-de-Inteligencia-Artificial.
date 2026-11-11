package br.edu.unex.sentinela.settings;

/**
 * Ações de comando do jogador mapeáveis a teclas do teclado.
 */
public enum GameAction {
    MOVE_UP("Mover para Cima"),
    MOVE_DOWN("Mover para Baixo"),
    MOVE_LEFT("Mover para a Esquerda"),
    MOVE_RIGHT("Mover para a Direita"),
    SPRINT("Disparar / Correr"),
    ATTACK("Ataque / Mordida"),
    ZOOM_IN("Aumentar Zoom (+)"),
    ZOOM_OUT("Diminuir Zoom (-)"),
    DEBUG_OVERLAY("Alternar Painel Debug (F3/H)"),
    MUTE_AUDIO("Silenciar Áudio (M)");

    private final String displayName;

    GameAction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
