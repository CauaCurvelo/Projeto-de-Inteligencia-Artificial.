package br.edu.unex.sentinela.core;

import br.edu.unex.sentinela.audio.AudioEngine;
import br.edu.unex.sentinela.audio.GameAudioDirector;
import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.rendering.Renderer;
import br.edu.unex.sentinela.settings.GameSettings;
import br.edu.unex.sentinela.ui.ScreenManager;
import br.edu.unex.sentinela.world.World;
import javafx.animation.AnimationTimer;

public class GameEngine {
    private final World world;
    private final Renderer renderer;
    private final InputManager inputManager;
    private final ScreenManager screenManager;
    private long lastTime = 0;
    private AnimationTimer timer;
    private final GameAudioDirector audioDirector = new GameAudioDirector();

    public GameEngine(World world, Renderer renderer, InputManager inputManager, ScreenManager screenManager) {
        this.world = world;
        this.renderer = renderer;
        this.inputManager = inputManager;
        this.screenManager = screenManager;
    }

    public void start() {
        AudioEngine.get().start();
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double deltaTime = (now - lastTime) / 1_000_000_000.0;

                int targetFps = GameSettings.get().getTargetFps();
                if (targetFps > 0) {
                    double minFrameTime = 1.0 / targetFps;
                    if (deltaTime < minFrameTime) {
                        return;
                    }
                }

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
        if (screenManager != null && screenManager.isGameRunning()) {
            inputManager.processInput();
            world.update(deltaTime, inputManager);
            audioDirector.update(world, deltaTime);
        }
    }

    private void render() {
        renderer.render(world, inputManager);
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
        AudioEngine.get().shutdown();
    }
}
