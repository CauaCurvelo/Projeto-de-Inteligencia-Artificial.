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
    private long nextRenderNs = 0;
    private boolean backdropDirty = true;
    private boolean wasGameRunning = false;
    private AnimationTimer timer;
    private final GameAudioDirector audioDirector = new GameAudioDirector();

    public GameEngine(World world, Renderer renderer, InputManager inputManager, ScreenManager screenManager) {
        this.world = world;
        this.renderer = renderer;
        this.inputManager = inputManager;
        this.screenManager = screenManager;
    }

    public void markBackdropDirty() {
        this.backdropDirty = true;
    }

    public void start() {
        AudioEngine.get().start();
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    nextRenderNs = now;
                    return;
                }

                int targetFps = GameSettings.get().getTargetFps();
                if (targetFps > 0) {
                    long frameIntervalNs = 1_000_000_000L / targetFps;
                    // Tolerância de 2.5ms evita descartar pulsos V-Sync de 60Hz e derrubar o FPS pela metade
                    if (now + 2_500_000L < nextRenderNs) {
                        return;
                    }
                    if (now - nextRenderNs > frameIntervalNs) {
                        nextRenderNs = now + frameIntervalNs;
                    } else {
                        nextRenderNs += frameIntervalNs;
                    }
                } else {
                    nextRenderNs = now;
                }

                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                // Cap deltaTime to prevent huge jumps on tab switch / lag
                if (deltaTime > 0.1) {
                    deltaTime = 0.1;
                }

                boolean running = (screenManager == null || screenManager.isGameRunning());
                if (running) {
                    update(deltaTime);
                    if (screenManager != null && world.isGameOver()) {
                        screenManager.showGameOver();
                    }
                    render();
                    backdropDirty = false;
                } else {
                    if (wasGameRunning || backdropDirty) {
                        render();
                        backdropDirty = false;
                    }
                    audioDirector.updateMenuOrPaused(world, screenManager != null && screenManager.isInMatchMenu());
                }
                wasGameRunning = running;
            }
        };
        timer.start();
    }

    private void update(double deltaTime) {
        inputManager.processInput();
        world.update(deltaTime, inputManager);
        audioDirector.update(world, deltaTime);
    }

    private void render() {
        boolean showHud = (screenManager == null || screenManager.isGameRunning() || screenManager.isInMatchMenu());
        renderer.setRenderHud(showHud);
        renderer.render(world, inputManager);
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
        world.getHighScoreManager().flushNow();
        AudioEngine.get().shutdown();
    }
}
