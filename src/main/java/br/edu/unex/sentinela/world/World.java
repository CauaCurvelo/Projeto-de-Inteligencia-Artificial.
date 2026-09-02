package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.input.InputManager;

public class World {
    private Player player;
    private TileMap tileMap;

    public World() {
        this.tileMap = new TileMap(25, 19); // 800/32 = 25, 600/32 = 18.75 -> 19
        this.player = new Player(400, 300); // Start in middle of 800x600 screen
    }

    public void update(double deltaTime, InputManager inputManager) {
        player.move(inputManager.getDx(), inputManager.getDy(), deltaTime, tileMap);
    }

    public Player getPlayer() {
        return player;
    }

    public TileMap getTileMap() {
        return tileMap;
    }
}
