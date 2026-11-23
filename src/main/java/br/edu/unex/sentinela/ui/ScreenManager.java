package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.AchievementTracker;
import br.edu.unex.sentinela.progress.MatchHistoryRepository;
import br.edu.unex.sentinela.progress.MatchRecord;
import br.edu.unex.sentinela.world.World;
import javafx.event.EventHandler;
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
    private boolean matchPaused = false;

    private final World world;
    private final AchievementToast toast = new AchievementToast();
    private final StackPane contentLayer = new StackPane();
    private BestiaryScreen activeBestiaryScreen;
    private Runnable onViewChanged;
    private final EventHandler<KeyEvent> sceneKeyFilter;

    public void setOnViewChanged(Runnable onViewChanged) {
        this.onViewChanged = onViewChanged;
    }

    public ScreenManager(World world) {
        this.world = world;

        this.sceneKeyFilter = event -> {
            if (event.isConsumed()) return;
            KeyCode code = event.getCode();
            if (code == KeyCode.ESCAPE) {
                event.consume();
                handleEscKey();
            } else if (code == KeyCode.R || code == KeyCode.ENTER) {
                if (currentState == ScreenState.GAME_OVER || world.isGameOver()) {
                    event.consume();
                    startGame();
                }
            }
        };

        contentLayer.prefWidthProperty().bind(widthProperty());
        contentLayer.prefHeightProperty().bind(heightProperty());

        toast.setManaged(true);
        StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 24, 24, 0));
        getChildren().addAll(contentLayer, toast);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, sceneKeyFilter);
            }
            if (newScene != null) {
                UiTheme.applyGlobalTheme(newScene);
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, sceneKeyFilter);
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

    public boolean isMainMenu() {
        return currentState == ScreenState.MAIN_MENU;
    }

    /** Verdadeiro quando em pausa ou telas (configurações, bestiário, conquistas) abertas durante uma partida ativa. */
    public boolean isInMatchMenu() {
        return matchPaused && currentState != ScreenState.PLAYING && currentState != ScreenState.MAIN_MENU;
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
            matchPaused = false;
            currentState = ScreenState.GAME_OVER;
            updateScreenView();
            requestFocus();
        }
    }

    public void startGame() {
        matchPaused = false;
        world.reset();
        AchievementTracker.get().resetRunStats();
        currentState = ScreenState.PLAYING;
        updateScreenView();
    }

    public void pauseGame() {
        if (currentState == ScreenState.PLAYING) {
            matchPaused = true;
            currentState = ScreenState.PAUSED;
            updateScreenView();
        }
    }

    public void resumeGame() {
        if (currentState == ScreenState.PAUSED) {
            matchPaused = false;
            currentState = ScreenState.PLAYING;
            updateScreenView();
        }
    }

    public void restartGame() {
        matchPaused = false;
        recordCurrentMatchIfApplicable("Reinício Manual");
        startGame();
    }

    public void returnToMainMenu() {
        matchPaused = false;
        recordCurrentMatchIfApplicable("Retorno ao Menu Principal");
        world.reset();
        currentState = ScreenState.MAIN_MENU;
        updateScreenView();
    }

    public void navigate(ScreenState target) {
        this.currentState = target;
        updateScreenView();
    }

    public void navigateBack() {
        if (matchPaused) {
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

        boolean inGame = (currentState == ScreenState.PLAYING || currentState == ScreenState.PAUSED);
        toast.setVisible(inGame && toast.isDisplayingToast());

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

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
    }

    public World getWorld() {
        return world;
    }
}
