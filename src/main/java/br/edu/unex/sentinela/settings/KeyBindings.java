package br.edu.unex.sentinela.settings;

import javafx.scene.input.KeyCode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Gerenciador de teclas com suporte a remapeamento dinâmico e resolução de conflitos.
 */
public class KeyBindings {
    private final Map<GameAction, KeyCode> bindings = new EnumMap<>(GameAction.class);

    public KeyBindings() {
        resetToDefaults();
    }

    public void resetToDefaults() {
        bindings.put(GameAction.MOVE_UP, KeyCode.W);
        bindings.put(GameAction.MOVE_DOWN, KeyCode.S);
        bindings.put(GameAction.MOVE_LEFT, KeyCode.A);
        bindings.put(GameAction.MOVE_RIGHT, KeyCode.D);
        bindings.put(GameAction.SPRINT, KeyCode.SHIFT);
        bindings.put(GameAction.ATTACK, KeyCode.SPACE);
        bindings.put(GameAction.ZOOM_IN, KeyCode.EQUALS);
        bindings.put(GameAction.ZOOM_OUT, KeyCode.MINUS);
        bindings.put(GameAction.DEBUG_OVERLAY, KeyCode.F3);
        bindings.put(GameAction.MUTE_AUDIO, KeyCode.M);
    }

    public KeyCode getKey(GameAction action) {
        return bindings.get(action);
    }

    public void rebind(GameAction action, KeyCode newKey) {
        if (newKey == null) return;
        // Se outra ação já usava essa tecla, inverte as teclas para evitar bloqueio
        for (Map.Entry<GameAction, KeyCode> entry : bindings.entrySet()) {
            if (entry.getValue() == newKey && entry.getKey() != action) {
                entry.setValue(bindings.get(action));
                break;
            }
        }
        bindings.put(action, newKey);
    }

    public boolean isActionTriggered(GameAction action, Set<KeyCode> activeKeys) {
        KeyCode bound = bindings.get(action);
        if (bound != null && activeKeys.contains(bound)) {
            return true;
        }
        // Teclas secundárias naturais para navegação acessível
        if (action == GameAction.MOVE_UP && activeKeys.contains(KeyCode.UP)) return true;
        if (action == GameAction.MOVE_DOWN && activeKeys.contains(KeyCode.DOWN)) return true;
        if (action == GameAction.MOVE_LEFT && activeKeys.contains(KeyCode.LEFT)) return true;
        if (action == GameAction.MOVE_RIGHT && activeKeys.contains(KeyCode.RIGHT)) return true;
        if (action == GameAction.ATTACK && activeKeys.contains(KeyCode.J)) return true;
        if (action == GameAction.DEBUG_OVERLAY && activeKeys.contains(KeyCode.H)) return true;
        return false;
    }

    public void loadFromProperties(Properties props) {
        for (GameAction action : GameAction.values()) {
            String val = props.getProperty("key." + action.name());
            if (val != null) {
                try {
                    bindings.put(action, KeyCode.valueOf(val));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveToProperties(Properties props) {
        for (Map.Entry<GameAction, KeyCode> entry : bindings.entrySet()) {
            props.setProperty("key." + entry.getKey().name(), entry.getValue().name());
        }
    }
}
