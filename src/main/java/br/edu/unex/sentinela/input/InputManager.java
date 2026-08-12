package br.edu.unex.sentinela.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

public class InputManager {
    private Set<KeyCode> activeKeys = new HashSet<>();
    private double dx = 0;
    private double dy = 0;

    public void attachToScene(Scene scene) {
        scene.setOnKeyPressed(event -> activeKeys.add(event.getCode()));
        scene.setOnKeyReleased(event -> activeKeys.remove(event.getCode()));
    }

    public void processInput() {
        dx = 0;
        dy = 0;
        if (activeKeys.contains(KeyCode.W)) dy -= 1;
        if (activeKeys.contains(KeyCode.S)) dy += 1;
        if (activeKeys.contains(KeyCode.A)) dx -= 1;
        if (activeKeys.contains(KeyCode.D)) dx += 1;
        
        // Normalize vector for diagonal movement
        if (dx != 0 && dy != 0) {
            double length = Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
        }
    }

    public double getDx() { return dx; }
    public double getDy() { return dy; }
}
