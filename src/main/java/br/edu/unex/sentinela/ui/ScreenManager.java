package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.AchievementTracker;
import br.edu.unex.sentinela.progress.MatchHistoryRepository;
import br.edu.unex.sentinela.progress.MatchRecord;
import br.edu.unex.sentinela.world.World;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

/**
 * Orquestrador da camada de interface do usuário, controlando a máquina de telas,
 * transições de navegação e atalhos globais (como ESC para pausar).
 */
public class ScreenManager extends StackPane {
    private ScreenState currentState = ScreenState.MAIN_MENU;
    private ScreenState previousState = ScreenState.MAIN_MENU;

    private final World world;
    private final AchievementToast toast = new AchievementToast();
    private final StackPane contentLayer = new StackPane();
    private BestiaryScreen activeBestiaryScreen;
    private Runnable onViewChanged;

    private final EventHandler<KeyEvent> sceneEscHandler = event -> {
        if (event.getCode() == KeyCode.ESCAPE && !event.isConsumed()) {
            event.consume();
            handleEscKey();
        }
    };

    public void setOnViewChanged(Runnable onViewChanged) {
        this.onViewChanged = onViewChanged;
    }

    public ScreenManager(World world) {
        this.world = world;

        getChildren().addAll(contentLayer, toast);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventHandler(KeyEvent.KEY_PRESSED, sceneEscHandler);
            }
            if (newScene != null) {
                UiTheme.applyGlobalTheme(newScene);
                newScene.addEventHandler(KeyEvent.KEY_PRESSED, sceneEscHandler);
            }
        });

        AchievementTracker.get().setUnlockListener(toast::showAchievement);

        // Inicia exibindo o Menu Principal
        updateScreenView();
    }

    public ScreenState getCurrentState() {
        return currentState;
    }

    public boolean isGameRunning() {
        return currentState == ScreenState.PLAYING;
    }

    /** Verdadeiro quando em pausa ou configurações abertas a partir de uma partida ativa. */
    public boolean isInMatchMenu() {
        return currentState != ScreenState.MAIN_MENU
                && currentState != ScreenState.PLAYING
                && (currentState == ScreenState.PAUSED || previousState == ScreenState.PAUSED);
    }

    public void handleEscKey() {
        if (currentState == ScreenState.PLAYING) {
            pauseGame();
        } else if (currentState == ScreenState.PAUSED) {
            resumeGame();
        } else if (currentState == ScreenState.GAME_OVER) {
            returnToMainMenu();
        } else if (currentState != ScreenState.MAIN_MENU) {
            navigateBack();
        }
    }

    public void showGameOver() {
        if (currentState != ScreenState.GAME_OVER) {
            previousState = ScreenState.PLAYING;
            currentState = ScreenState.GAME_OVER;
            updateScreenView();
        }
    }

    public void startGame() {
        world.reset();
        AchievementTracker.get().resetRunStats();
        previousState = ScreenState.MAIN_MENU;
        currentState = ScreenState.PLAYING;
        updateScreenView();
    }

    public void pauseGame() {
        if (currentState == ScreenState.PLAYING) {
            previousState = ScreenState.PLAYING;
            currentState = ScreenState.PAUSED;
            updateScreenView();
        }
    }

    public void resumeGame() {
        if (currentState == ScreenState.PAUSED) {
            currentState = ScreenState.PLAYING;
            updateScreenView();
        }
    }

    public void restartGame() {
        recordCurrentMatchIfApplicable("Reinício Manual");
        startGame();
    }

    public void returnToMainMenu() {
        recordCurrentMatchIfApplicable("Retorno ao Menu Principal");
        world.reset();
        previousState = ScreenState.MAIN_MENU;
        currentState = ScreenState.MAIN_MENU;
        updateScreenView();
    }

    public void navigate(ScreenState target) {
        this.previousState = this.currentState;
        this.currentState = target;
        updateScreenView();
    }

    public void navigateBack() {
        if (previousState == ScreenState.PAUSED) {
            currentState = ScreenState.PAUSED;
        } else {
            currentState = ScreenState.MAIN_MENU;
        }
        updateScreenView();
    }

    private void recordCurrentMatchIfApplicable(String reason) {
        // Partidas encerradas por morte já foram registradas em World.triggerGameOver()
        if (world.isGameOver()) {
            return;
        }
        if (world.getSurvivalTime() >= 3.0) {
            MatchRecord record = new MatchRecord(
                world.getSurvivalTime(),
                world.getScore(),
                world.getApplesEaten(),
                world.getEnemiesDefeated(),
                world.getDayCycleNumber(),
                reason
            );
            MatchHistoryRepository.get().recordMatch(record);
        }
    }

    private void updateScreenView() {
        if (activeBestiaryScreen != null) {
            activeBestiaryScreen.stopPreview();
            activeBestiaryScreen = null;
        }
        contentLayer.getChildren().clear();

        Node screenNode = null;
        switch (currentState) {
            case MAIN_MENU:
                screenNode = new MainMenuScreen(this);
                break;
            case PAUSED:
                screenNode = new PauseMenuScreen(this);
                break;
            case GAME_OVER:
                screenNode = new GameOverScreen(this, world);
                break;
            case SETTINGS:
                screenNode = new SettingsScreen(this);
                break;
            case HISTORY:
                screenNode = new HistoryScreen(this);
                break;
            case ACHIEVEMENTS:
                screenNode = new AchievementsScreen(this);
                break;
            case BESTIARY:
                activeBestiaryScreen = new BestiaryScreen(this);
                screenNode = activeBestiaryScreen;
                break;
            case PLAYING:
            default:
                // Em jogo, a camada de menus fica vazia, permitindo visão total do canvas
                break;
        }

        toast.setVisible(currentState == ScreenState.PLAYING || currentState == ScreenState.PAUSED);

        if (screenNode != null) {
            contentLayer.getChildren().add(screenNode);
            screenNode.requestFocus();
        } else if (getScene() != null) {
            // Garante foco válido na camada principal quando o menu é fechado durante a partida
            requestFocus();
        }

        if (onViewChanged != null) {
            onViewChanged.run();
        }
    }
}
