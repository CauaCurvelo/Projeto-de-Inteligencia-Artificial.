package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.world.World;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import br.edu.unex.sentinela.entity.Agent;
import br.edu.unex.sentinela.ai.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;

public class Renderer {
    private GraphicsContext gc;
    private double width;
    private double height;

    public Renderer(GraphicsContext gc, double width, double height) {
        this.gc = gc;
        this.width = width;
        this.height = height;
    }

    public void render(World world) {
        // Clear screen
        gc.clearRect(0, 0, width, height);
        
        TileMap tileMap = world.getTileMap();
        int ts = TileMap.TILE_SIZE;
        
        if (tileMap != null) {
            for (int row = 0; row < tileMap.getRows(); row++) {
                for (int col = 0; col < tileMap.getCols(); col++) {
                    TileType type = tileMap.getTileAt(col, row);
                    switch (type) {
                        case GRASS: gc.setFill(Color.web("#7EC850")); break;
                        case WALL:  gc.setFill(Color.web("#555555")); break;
                        case MUD:   gc.setFill(Color.web("#70543E")); break;
                        case WATER: gc.setFill(Color.web("#3B83BD")); break;
                    }
                    gc.fillRect(col * ts, row * ts, ts, ts);
                    
                    // Optional grid outline
                    gc.setStroke(Color.web("#3A6339"));
                    gc.setLineWidth(1);
                    gc.strokeRect(col * ts, row * ts, ts, ts);
                }
            }
        }

        Agent agent = world.getAgent();
        if (agent != null) {
            // Draw Target Destination
            gc.setFill(Color.web("#FFD700", 0.5)); // Gold / Yellow
            gc.fillRect(agent.getTargetCol() * ts, agent.getTargetRow() * ts, ts, ts);
            
            // Draw Path
            List<Node> path = agent.getCurrentPath();
            if (path != null) {
                gc.setFill(Color.web("#FFFF00", 0.4)); // Semi-transparent yellow
                for (Node node : path) {
                    gc.fillRect(node.col * ts, node.row * ts, ts, ts);
                }
            }
            
            // Draw Agent
            double ax = agent.getX();
            double ay = agent.getY();
            double size = 24.0;
            gc.setFill(Color.web("#CC4444")); // Red Agent
            gc.fillRoundRect(ax - size/2, ay - size/2, size, size, 8, 8);
        }

        // Draw Player
        double px = world.getPlayer().getX();
        double py = world.getPlayer().getY();
        double size = 32.0;
        
        // Player Body
        gc.setFill(Color.web("#88CC88")); // Light green player
        gc.fillRoundRect(px - size/2, py - size/2, size, size, 8, 8); // slightly rounded
        
        // Player direction indicator (Eyes / Visor)
        gc.setFill(Color.web("#FFFFFF"));
        double faceDx = world.getPlayer().getFaceDx();
        double faceDy = world.getPlayer().getFaceDy();
        
        // Offset for the visor based on direction
        double visorX = px + faceDx * 8;
        double visorY = py + faceDy * 8;
        gc.fillOval(visorX - 4, visorY - 4, 8, 8);
    }
}
