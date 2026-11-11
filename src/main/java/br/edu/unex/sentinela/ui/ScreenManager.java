package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.AchievementTracker;
import br.edu.unex.sentinela.progress.MatchHistoryRepository;
import br.edu.unex.sentinela.progress.MatchRecord;
import br.edu.unex.sentinela.world.World;
import javafx.scene.Node;
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

    public ScreenManager(World world) {
        this.world = world;

        getChildren().addAll(contentLayer, toast);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                UiTheme.applyGlobalTheme(newScene);
            }
        });

        AchievementTracker.get().setUnlockListener(toast::showAchievement);

        // Captura global de ESC para pausa e retorno
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                handleEscKey();
            }
        });

        // Inicia exibindo o Menu Principal
        updateScreenView();
    }

    public ScreenState getCurrentState() {
        return currentState;
    }

    public boolean isGameRunning() {
        return currentState == ScreenState.PLAYING;
    }

    public void handleEscKey() {
        if (currentState == ScreenState.PLAYING) {
            pauseGame();
        } else if (currentState == ScreenState.PAUSED) {
            resumeGame();
        } else if (currentState != ScreenState.MAIN_MENU) {
            navigateBack();
        }
    }

    public void startGame() {
        world.reset();
        AchievementTracker.get().resetRunStats();
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
        contentLayer.getChildren().clear();

        Node screenNode = null;
        switch (currentState) {
            case MAIN_MENU:
                screenNode = new MainMenuScreen(this);
                break;
            case PAUSED:
                screenNode = new PauseMenuScreen(this);
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
                screenNode = new BestiaryScreen(this);
                break;
            case PLAYING:
            default:
                // Em jogo, a camada de menus fica vazia, permitindo visão total do canvas
                break;
        }

        if (screenNode != null) {
            contentLayer.getChildren().add(screenNode);
            screenNode.requestFocus();
        }
    }
}
