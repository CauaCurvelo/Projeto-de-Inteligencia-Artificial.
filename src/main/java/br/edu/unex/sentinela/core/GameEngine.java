package br.edu.unex.sentinela.core;

import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.rendering.Renderer;
import br.edu.unex.sentinela.world.World;
import javafx.animation.AnimationTimer;

public class GameEngine {
    private final World world;
    private final Renderer renderer;
    private final InputManager inputManager;
    private long lastTime = 0;
    private AnimationTimer timer;

    public GameEngine(World world, Renderer renderer, InputManager inputManager) {
        this.world = world;
        this.renderer = renderer;
        this.inputManager = inputManager;
    }

    public void start() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                // Cap deltaTime to prevent huge jumps on tab switch / lag
                if (deltaTime > 0.1) {
                    deltaTime = 0.1;
                }

                update(deltaTime);
                render();
            }
        };
        timer.start();
    }

    private void update(double deltaTime) {
        inputManager.processInput();
        world.update(deltaTime, inputManager);
    }

    private void render() {
        renderer.render(world, inputManager);
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }
}
