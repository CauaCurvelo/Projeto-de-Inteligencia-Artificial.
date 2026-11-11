package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.settings.GameAction;
import br.edu.unex.sentinela.settings.GameSettings;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Botão interativo para captura e remapeamento de teclas nas configurações.
 */
public class KeyRebindButton extends Button {
    private final GameAction action;
    private boolean listening = false;
    private EventHandler<KeyEvent> keyListener;
    private final Runnable onReboundCallback;

    public KeyRebindButton(GameAction action, Runnable onReboundCallback) {
        this.action = action;
        this.onReboundCallback = onReboundCallback;
        setPrefSize(140, 32);
        setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        updateLabel();
        applyNormalStyle();

        setOnAction(e -> startListening());
    }

    public void updateLabel() {
        KeyCode code = GameSettings.get().getKeyBindings().getKey(action);
        setText(code != null ? code.getName() : "Nenhuma");
    }

    private void startListening() {
        if (listening) return;
        listening = true;
        setText("[Pressione...]");
        setStyle(
            "-fx-background-color: #E67E22;" +
            "-fx-text-fill: #FFFFFF;" +
            "-fx-background-radius: 6;" +
            "-fx-border-color: #F39C12;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 6;"
        );

        keyListener = event -> {
            KeyCode newKey = event.getCode();
            event.consume();
            if (newKey != KeyCode.ESCAPE) {
                GameSettings.get().getKeyBindings().rebind(action, newKey);
                GameSettings.get().save();
            }
            stopListening();
            if (onReboundCallback != null) {
                onReboundCallback.run();
            }
        };

        if (getScene() != null) {
            getScene().addEventFilter(KeyEvent.KEY_PRESSED, keyListener);
        }
    }

    private void stopListening() {
        listening = false;
        if (getScene() != null && keyListener != null) {
            getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keyListener);
        }
        updateLabel();
        applyNormalStyle();
    }

    private void applyNormalStyle() {
        setStyle(
            "-fx-background-color: rgba(50, 65, 55, 0.9);" +
            "-fx-text-fill: #F1C40F;" +
            "-fx-background-radius: 6;" +
            "-fx-border-color: rgba(212, 172, 13, 0.6);" +
            "-fx-border-width: 1.2;" +
            "-fx-border-radius: 6;" +
            "-fx-cursor: hand;"
        );
    }
}
