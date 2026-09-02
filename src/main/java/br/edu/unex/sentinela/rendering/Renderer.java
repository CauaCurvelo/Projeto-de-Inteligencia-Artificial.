package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.world.World;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

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
        if (tileMap != null) {
            int ts = TileMap.TILE_SIZE;
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
