package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.world.World;
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
        // Clear screen and draw a nicer background (Grass-like color)
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.web("#2E4F2D")); // Dark green background
        gc.fillRect(0, 0, width, height);

        // Grid effect for a classic feel
        gc.setStroke(Color.web("#3A6339"));
        gc.setLineWidth(1);
        for (int i = 0; i < width; i += 32) gc.strokeLine(i, 0, i, height);
        for (int i = 0; i < height; i += 32) gc.strokeLine(0, i, width, i);

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
