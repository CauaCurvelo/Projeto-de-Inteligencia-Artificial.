package br.edu.unex.sentinela.app;

import br.edu.unex.sentinela.core.GameEngine;
import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.rendering.Renderer;
import br.edu.unex.sentinela.world.World;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.Stage;

public class Main extends Application {
    private static final int INITIAL_WIDTH = 960;
    private static final int INITIAL_HEIGHT = 640;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Instinto Selvagem (Wild Instinct) - Projeto Final IA");

        Group root = new Group();
        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
        Canvas canvas = new Canvas(INITIAL_WIDTH, INITIAL_HEIGHT);
        root.getChildren().add(canvas);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        InputManager inputManager = new InputManager();
        inputManager.attachToScene(scene, primaryStage);

        World world = new World();
        Renderer renderer = new Renderer(gc, INITIAL_WIDTH, INITIAL_HEIGHT);

        // Bind canvas resize automatically when window resizes or goes Fullscreen (F11)
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            renderer.updateViewport(newVal.doubleValue(), canvas.getHeight());
            world.getCamera().updateViewport(newVal.doubleValue(), canvas.getHeight());
        });
        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            renderer.updateViewport(canvas.getWidth(), newVal.doubleValue());
            world.getCamera().updateViewport(canvas.getWidth(), newVal.doubleValue());
        });

        GameEngine engine = new GameEngine(world, renderer, inputManager);
        engine.start();

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
