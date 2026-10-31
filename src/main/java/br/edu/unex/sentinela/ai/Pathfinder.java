package br.edu.unex.sentinela.ai;

import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class Pathfinder {
    
    private static final int[][] DIRECTIONS = {
        {0, -1}, // UP
        {0, 1},  // DOWN
        {-1, 0}, // LEFT
        {1, 0}   // RIGHT
    };

    private static final int MAX_EXPLORED_NODES = 800;

    public List<Node> findPath(TileMap map, int startCol, int startRow, int targetCol, int targetRow) {
        Node startNode = new Node(startCol, startRow);
        Node targetNode = new Node(targetCol, targetRow);
        
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Node> closedSet = new HashSet<>();
        
        openSet.add(startNode);
        Node closestNodeSoFar = startNode;
        int lowestH = getManhattanDistance(startNode, targetNode);
        int exploredCount = 0;
        
        while (!openSet.isEmpty()) {
            Node currentNode = openSet.poll();
            closedSet.add(currentNode);
            exploredCount++;

            int currentH = getManhattanDistance(currentNode, targetNode);
            if (currentH < lowestH) {
                lowestH = currentH;
                closestNodeSoFar = currentNode;
            }
            
            if (currentNode.equals(targetNode)) {
                return retracePath(startNode, currentNode);
            }

            if (exploredCount >= MAX_EXPLORED_NODES) {
                return retracePath(startNode, closestNodeSoFar);
            }
            
            for (int[] dir : DIRECTIONS) {
                int neighborCol = currentNode.col + dir[0];
                int neighborRow = currentNode.row + dir[1];
                
                TileType neighborTile = map.getTileAt(neighborCol, neighborRow);
                
                if (!neighborTile.isWalkable()) {
                    continue;
                }
                
                Node neighbor = new Node(neighborCol, neighborRow);
                if (closedSet.contains(neighbor)) {
                    continue;
                }
                
                int moveCostToNeighbor = currentNode.gCost + neighborTile.getMovementCost();
                
                boolean inOpenSet = false;
                for (Node n : openSet) {
                    if (n.equals(neighbor)) {
                        neighbor = n;
                        inOpenSet = true;
                        break;
                    }
                }
                
                if (moveCostToNeighbor < neighbor.gCost || !inOpenSet) {
                    neighbor.gCost = moveCostToNeighbor;
                    neighbor.hCost = getManhattanDistance(neighbor, targetNode);
                    neighbor.parent = currentNode;
                    
                    if (!inOpenSet) {
                        openSet.add(neighbor);
                    } else {
                        openSet.remove(neighbor);
                        openSet.add(neighbor);
                    }
                }
            }
        }
        
        if (closestNodeSoFar != startNode) {
            return retracePath(startNode, closestNodeSoFar);
        }

        return new ArrayList<>();
    }
    
    private int getManhattanDistance(Node nodeA, Node nodeB) {
        int distX = Math.abs(nodeA.col - nodeB.col);
        int distY = Math.abs(nodeA.row - nodeB.row);
        return distX + distY;
    }
    
    private List<Node> retracePath(Node startNode, Node endNode) {
        List<Node> path = new ArrayList<>();
        Node currentNode = endNode;
        
        while (currentNode != null && !currentNode.equals(startNode)) {
            path.add(currentNode);
            currentNode = currentNode.parent;
        }
        
        Collections.reverse(path);
        return path;
    }
}
