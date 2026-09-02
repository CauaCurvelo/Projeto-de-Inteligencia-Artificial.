package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.entity.Agent;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.input.InputManager;

public class World {
    private Player player;
    private Agent agent;
    private TileMap tileMap;

    public World() {
        this.tileMap = new TileMap(25, 19); // 800/32 = 25, 600/32 = 18.75 -> 19
        this.player = new Player(400, 300); // Start in middle of 800x600 screen
        
        // Start agent at (2, 2) and go to (20, 15)
        this.agent = new Agent(2 * TileMap.TILE_SIZE + 16, 2 * TileMap.TILE_SIZE + 16, 20, 15, this.tileMap);
    }

    public void update(double deltaTime, InputManager inputManager) {
        player.move(inputManager.getDx(), inputManager.getDy(), deltaTime, tileMap);
        agent.update(deltaTime);
    }

    public Player getPlayer() {
        return player;
    }
    
    public Agent getAgent() {
        return agent;
    }

    public TileMap getTileMap() {
        return tileMap;
    }
}
