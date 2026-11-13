package br.edu.unex.sentinela.input;

import br.edu.unex.sentinela.settings.GameAction;
import br.edu.unex.sentinela.settings.GameSettings;
import br.edu.unex.sentinela.settings.KeyBindings;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class InputManager {
    private final Set<KeyCode> activeKeys = new HashSet<>();
    private double dx = 0;
    private double dy = 0;
    private boolean sprinting = false;
    private boolean attackPressed = false;
    private boolean debugOverlay = false;
    private boolean zoomInRequested = false;
    private boolean zoomOutRequested = false;
    private Stage stage;
    private Supplier<Boolean> gameplayInputEnabled = () -> true;

    public void setGameplayInputEnabled(Supplier<Boolean> gameplayInputEnabled) {
        this.gameplayInputEnabled = gameplayInputEnabled != null ? gameplayInputEnabled : () -> true;
    }

    /** Limpa teclas e gatilhos transitórios (chamado ao pausar ou abrir menus). */
    public void clearInputs() {
        activeKeys.clear();
        dx = 0;
        dy = 0;
        sprinting = false;
        attackPressed = false;
        zoomInRequested = false;
        zoomOutRequested = false;
    }

    public void attachToScene(Scene scene, Stage stage) {
        this.stage = stage;
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                toggleFullScreen();
                return;
            }
            if (!Boolean.TRUE.equals(gameplayInputEnabled.get())) {
                return;
            }

            activeKeys.add(event.getCode());
            KeyBindings kb = GameSettings.get().getKeyBindings();

            if (kb.isActionTriggered(GameAction.DEBUG_OVERLAY, activeKeys)) {
                debugOverlay = !debugOverlay;
            }
            if (kb.isActionTriggered(GameAction.ZOOM_IN, activeKeys)) {
                zoomInRequested = true;
            }
            if (kb.isActionTriggered(GameAction.ZOOM_OUT, activeKeys)) {
                zoomOutRequested = true;
            }
            if (kb.isActionTriggered(GameAction.ATTACK, activeKeys)) {
                attackPressed = true;
            }
            if (kb.isActionTriggered(GameAction.MUTE_AUDIO, activeKeys)) {
                br.edu.unex.sentinela.audio.AudioEngine.get().toggleMute();
            }
            if (event.getCode() == KeyCode.OPEN_BRACKET) {
                br.edu.unex.sentinela.audio.AudioEngine.get().changeVolume(-0.1);
            }
            if (event.getCode() == KeyCode.CLOSE_BRACKET) {
                br.edu.unex.sentinela.audio.AudioEngine.get().changeVolume(0.1);
                br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.UI_CLICK, 0.5, 0, 1);
            }
        });
        scene.setOnKeyReleased(event -> {
            activeKeys.remove(event.getCode());
            KeyBindings kb = GameSettings.get().getKeyBindings();
            if (!kb.isActionTriggered(GameAction.ATTACK, activeKeys)) {
                attackPressed = false;
            }
        });

        // Also allow Left Mouse Click to attack!
        scene.setOnMousePressed(event -> {
            if (Boolean.TRUE.equals(gameplayInputEnabled.get()) && event.getButton() == MouseButton.PRIMARY) {
                attackPressed = true;
            }
        });
        scene.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                attackPressed = false;
            }
        });
    }

    public void toggleFullScreen() {
        if (stage != null) {
            stage.setFullScreen(!stage.isFullScreen());
        }
    }

    public void processInput() {
        dx = 0;
        dy = 0;
        KeyBindings kb = GameSettings.get().getKeyBindings();

        if (kb.isActionTriggered(GameAction.MOVE_UP, activeKeys)) dy -= 1;
        if (kb.isActionTriggered(GameAction.MOVE_DOWN, activeKeys)) dy += 1;
        if (kb.isActionTriggered(GameAction.MOVE_LEFT, activeKeys)) dx -= 1;
        if (kb.isActionTriggered(GameAction.MOVE_RIGHT, activeKeys)) dx += 1;

        sprinting = kb.isActionTriggered(GameAction.SPRINT, activeKeys);

        if (dx != 0 && dy != 0) {
            double length = Math.hypot(dx, dy);
            dx /= length;
            dy /= length;
        }
    }

    public double getDx() { return dx; }
    public double getDy() { return dy; }
    public boolean isSprinting() { return sprinting; }
    public boolean isAttackPressed() { return attackPressed; }
    public boolean isDebugOverlay() { return debugOverlay; }

    public boolean pollZoomInRequested() {
        boolean req = zoomInRequested;
        zoomInRequested = false;
        return req;
    }

    public boolean pollZoomOutRequested() {
        boolean req = zoomOutRequested;
        zoomOutRequested = false;
        return req;
    }
}
