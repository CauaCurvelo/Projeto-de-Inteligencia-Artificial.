package br.edu.unex.sentinela.app;

import br.edu.unex.sentinela.core.GameEngine;
import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.rendering.Renderer;
import br.edu.unex.sentinela.settings.GameSettings;
import br.edu.unex.sentinela.ui.ScreenManager;
import br.edu.unex.sentinela.world.World;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.InputStream;

public class Main extends Application {
    public Main() {}

    private static final int INITIAL_WIDTH = 960;
    private static final int INITIAL_HEIGHT = 640;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Instinto Selvagem (Wild Instinct) - Projeto Final IA");
        applyWindowIcon(primaryStage);

        StackPane root = new StackPane();
        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
        br.edu.unex.sentinela.ui.UiTheme.applyGlobalTheme(scene);
        Canvas canvas = new Canvas(INITIAL_WIDTH, INITIAL_HEIGHT);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        InputManager inputManager = new InputManager();
        inputManager.attachToScene(scene, primaryStage);

        World world = new World();
        Renderer renderer = new Renderer(gc, INITIAL_WIDTH, INITIAL_HEIGHT);
        ScreenManager screenManager = new ScreenManager(world);
        inputManager.setGameplayInputEnabled(screenManager::isGameRunning);

        root.getChildren().addAll(canvas, screenManager);

        GameEngine engine = new GameEngine(world, renderer, inputManager, screenManager);
        screenManager.setOnViewChanged(() -> {
            if (!screenManager.isGameRunning()) {
                inputManager.clearInputs();
            }
            engine.markBackdropDirty();
        });

        // Bind canvas resize automatically when window resizes or goes Fullscreen (F11)
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            renderer.updateViewport(newVal.doubleValue(), canvas.getHeight());
            world.getCamera().updateViewport(newVal.doubleValue(), canvas.getHeight());
            engine.markBackdropDirty();
        });
        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            renderer.updateViewport(canvas.getWidth(), newVal.doubleValue());
            world.getCamera().updateViewport(canvas.getWidth(), newVal.doubleValue());
            engine.markBackdropDirty();
        });

        // Mantém ESC livre para pausar a partida em vez de fechar o modo tela cheia (F11 alterna fullscreen)
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.setFullScreenExitHint("");

        if (GameSettings.get().isFullscreen()) {
            primaryStage.setFullScreen(true);
        }

        engine.start();
        primaryStage.setOnCloseRequest(e -> engine.stop());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static void applyWindowIcon(Stage stage) {
        try (InputStream stream = Main.class.getResourceAsStream("/ui/app_icon.png")) {
            if (stream != null) {
                stage.getIcons().add(new Image(stream));
            }
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        launch(args);
    }
}
