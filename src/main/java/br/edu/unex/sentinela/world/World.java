package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.input.InputManager;

public class World {
    private Player player;

    public World() {
        this.player = new Player(400, 300); // Start in middle of 800x600 screen
    }

    public void update(double deltaTime, InputManager inputManager) {
        player.move(inputManager.getDx(), inputManager.getDy(), deltaTime);
    }

    public Player getPlayer() {
        return player;
    }
}
