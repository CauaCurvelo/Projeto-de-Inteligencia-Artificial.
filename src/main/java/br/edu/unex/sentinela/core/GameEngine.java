package br.edu.unex.sentinela.core;

import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.rendering.Renderer;
import br.edu.unex.sentinela.world.World;
import javafx.animation.AnimationTimer;

public class GameEngine {
    private AnimationTimer loop;

    public GameEngine(World world, Renderer renderer, InputManager inputManager) {
        

        loop = new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }
                
                // Calculate deltaTime in seconds
                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                
                // Cap deltaTime to prevent huge jumps (e.g. when dragging the window)
                if (deltaTime > 0.05) deltaTime = 0.05;

                // Game Loop Order: Input, Update, Render
                inputManager.processInput();
                world.update(deltaTime, inputManager);
                renderer.render(world);
            }
        };
    }

    public void start() {
        loop.start();
    }

    public void stop() {
        loop.stop();
    }
}
