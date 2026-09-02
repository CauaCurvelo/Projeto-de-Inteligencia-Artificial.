package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.ai.Node;
import br.edu.unex.sentinela.ai.Pathfinder;
import br.edu.unex.sentinela.world.TileMap;

import java.util.List;

public class Agent {
    private double x;
    private double y;
    private double speed = 100.0;
    
    private List<Node> currentPath;
    private int currentPathIndex = 0;
    
    // Position tracking for destination
    private int targetCol;
    private int targetRow;
    
    public Agent(double startX, double startY, int targetCol, int targetRow, TileMap tileMap) {
        this.x = startX;
        this.y = startY;
        this.targetCol = targetCol;
        this.targetRow = targetRow;
        
        int startCol = (int) Math.floor(startX / TileMap.TILE_SIZE);
        int startRow = (int) Math.floor(startY / TileMap.TILE_SIZE);
        
        Pathfinder pathfinder = new Pathfinder();
        this.currentPath = pathfinder.findPath(tileMap, startCol, startRow, targetCol, targetRow);
    }
    
    public void update(double deltaTime) {
        if (currentPath == null || currentPathIndex >= currentPath.size()) {
            return; // Destination reached or no path
        }
        
        Node nextNode = currentPath.get(currentPathIndex);
        
        // Target center of the tile
        double targetX = (nextNode.col * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0);
        double targetY = (nextNode.row * TileMap.TILE_SIZE) + (TileMap.TILE_SIZE / 2.0);
        
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 2.0) {
            // Close enough to node, move to next
            x = targetX;
            y = targetY;
            currentPathIndex++;
        } else {
            // Move towards node
            double moveDist = speed * deltaTime;
            if (moveDist > distance) {
                moveDist = distance;
            }
            
            x += (dx / distance) * moveDist;
            y += (dy / distance) * moveDist;
        }
    }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public List<Node> getCurrentPath() { return currentPath; }
    public int getTargetCol() { return targetCol; }
    public int getTargetRow() { return targetRow; }
}
