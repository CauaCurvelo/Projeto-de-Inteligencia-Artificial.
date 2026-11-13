package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.settings.GameAction;
import br.edu.unex.sentinela.settings.GameSettings;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Botão keycap interativo que captura uma tecla do jogador para remapeamento.
 */
public class KeyRebindButton extends Button {
    private final GameAction action;
    private boolean listening = false;
    private Runnable onChanged;

    public KeyRebindButton(GameAction action, Runnable onChanged) {
        this.action = action;
        this.onChanged = onChanged;
        setPrefSize(112, 30);
        getStyleClass().add("keycap-btn");
        UiTypography.bind(this, UiTypography.TypoRole.KEYCAP);
        refreshText();

        setOnAction(e -> startListening());

        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!listening) return;
            e.consume();
            KeyCode code = e.getCode();
            if (code == KeyCode.ESCAPE) {
                stopListening();
                return;
            }
            if (code != KeyCode.UNDEFINED) {
                GameSettings.get().getKeyBindings().rebind(this.action, code);
                GameSettings.get().save();
                stopListening();
                if (this.onChanged != null) this.onChanged.run();
            }
        });

        focusedProperty().addListener((obs, was, now) -> {
            if (!now && listening) stopListening();
        });
    }

    public void startListening() {
        listening = true;
        setText("[TECLA...]");
        getStyleClass().remove("keycap-btn");
        if (!getStyleClass().contains("keycap-btn-listening")) {
            getStyleClass().add("keycap-btn-listening");
        }
        requestFocus();
    }

    public void stopListening() {
        listening = false;
        getStyleClass().remove("keycap-btn-listening");
        if (!getStyleClass().contains("keycap-btn")) {
            getStyleClass().add("keycap-btn");
        }
        refreshText();
    }

    public void refreshText() {
        KeyCode key = GameSettings.get().getKeyBindings().getKey(action);
        setText(key != null ? formatKeyName(key) : "—");
    }

    private static String formatKeyName(KeyCode key) {
        return switch (key) {
            case SPACE -> "ESPAÇO";
            case SHIFT -> "SHIFT";
            case EQUALS -> "+";
            case MINUS -> "-";
            default -> key.getName().toUpperCase();
        };
    }
}
