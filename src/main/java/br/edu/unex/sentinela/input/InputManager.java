package br.edu.unex.sentinela.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import java.util.HashSet;
import java.util.Set;

public class InputManager {
    private final Set<KeyCode> activeKeys = new HashSet<>();
    private double dx = 0;
    private double dy = 0;
    private boolean sprinting = false;
    private boolean attackPressed = false;
    private boolean debugOverlay = true;
    private boolean restartRequested = false;
    private Stage stage;

    public void attachToScene(Scene scene, Stage stage) {
        this.stage = stage;
        scene.setOnKeyPressed(event -> {
            activeKeys.add(event.getCode());
            if (event.getCode() == KeyCode.H || event.getCode() == KeyCode.F3) {
                debugOverlay = !debugOverlay;
            }
            if (event.getCode() == KeyCode.R) {
                restartRequested = true;
            }
            if (event.getCode() == KeyCode.F11) {
                toggleFullScreen();
            }
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.J) {
                attackPressed = true;
            }
        });
        scene.setOnKeyReleased(event -> {
            activeKeys.remove(event.getCode());
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.J) {
                attackPressed = false;
            }
        });

        // Also allow Left Mouse Click to attack!
        scene.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
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
        if (activeKeys.contains(KeyCode.W) || activeKeys.contains(KeyCode.UP)) dy -= 1;
        if (activeKeys.contains(KeyCode.S) || activeKeys.contains(KeyCode.DOWN)) dy += 1;
        if (activeKeys.contains(KeyCode.A) || activeKeys.contains(KeyCode.LEFT)) dx -= 1;
        if (activeKeys.contains(KeyCode.D) || activeKeys.contains(KeyCode.RIGHT)) dx += 1;

        sprinting = activeKeys.contains(KeyCode.SHIFT);

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
    
    public boolean pollRestartRequested() {
        boolean req = restartRequested;
        restartRequested = false;
        return req;
    }
}
