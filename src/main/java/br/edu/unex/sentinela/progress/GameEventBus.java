package br.edu.unex.sentinela.progress;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Barramento leve de eventos em memória para desacoplar a lógica do mundo de UI e conquistas.
 */
public final class GameEventBus {
    private static final GameEventBus INSTANCE = new GameEventBus();
    private final List<Consumer<GameEvent>> listeners = new CopyOnWriteArrayList<>();

    private GameEventBus() {}

    public static GameEventBus get() {
        return INSTANCE;
    }

    public void subscribe(Consumer<GameEvent> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<GameEvent> listener) {
        listeners.remove(listener);
    }

    public void post(GameEvent event) {
        for (Consumer<GameEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
