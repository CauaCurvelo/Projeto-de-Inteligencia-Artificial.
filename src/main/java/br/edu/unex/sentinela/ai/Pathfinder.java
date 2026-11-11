package br.edu.unex.sentinela.ai;

import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Sistema de Pathfinding A* de 8 Direções (Octile Heuristic) com:
 * - Prevenção estrita de Corner-Cutting (não atravessa quinas de árvores ou rochas)
 * - Validação de destino bloqueado (encontra automaticamente o tile caminhável livre mais próximo)
 * - Algoritmo String-Pulling (Line-of-Sight Path Smoothing) para trajetos diagonais fluidos sem zigue-zague
 */
public class Pathfinder {

    private static final int[][] DIRECTIONS_8 = {
        { 0, -1, 10}, // N
        { 0,  1, 10}, // S
        {-1,  0, 10}, // W
        { 1,  0, 10}, // E
        {-1, -1, 14}, // NW
        { 1, -1, 14}, // NE
        {-1,  1, 14}, // SW
        { 1,  1, 14}  // SE
    };

    private static final int MAX_EXPLORED_NODES = 1100;

    public List<Node> findPath(TileMap map, int startCol, int startRow, int targetCol, int targetRow) {
        return findPath(map, startCol, startRow, targetCol, targetRow, false);
    }

    public List<Node> findPath(TileMap map, int startCol, int startRow, int targetCol, int targetRow, boolean isAquaticCrocodile) {
        // Se o ponto de destino cair em cima de uma árvore ou rocha, ajusta para o vizinho caminhável mais próximo
        if (!map.getTileAt(targetCol, targetRow).isWalkable()) {
            int[] validTarget = findNearestWalkableTile(map, targetCol, targetRow, 3);
            if (validTarget != null) {
                targetCol = validTarget[0];
                targetRow = validTarget[1];
            } else {
                return new ArrayList<>();
            }
        }

        if (startCol == targetCol && startRow == targetRow) {
            return new ArrayList<>();
        }

        Node startNode = new Node(startCol, startRow);
        Node targetNode = new Node(targetCol, targetRow);

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Long, Node> allNodes = new HashMap<>();
        Set<Long> closedSet = new HashSet<>();

        startNode.gCost = 0;
        startNode.hCost = getOctileDistance(startNode, targetNode);
        openSet.add(startNode);
        allNodes.put(nodeKey(startCol, startRow), startNode);

        Node closestNodeSoFar = startNode;
        int lowestH = startNode.hCost;
        int exploredCount = 0;

        while (!openSet.isEmpty()) {
            Node currentNode = openSet.poll();
            long currentKey = nodeKey(currentNode.col, currentNode.row);

            if (!closedSet.add(currentKey)) {
                continue;
            }
            exploredCount++;

            if (currentNode.hCost < lowestH) {
                lowestH = currentNode.hCost;
                closestNodeSoFar = currentNode;
            }

            if (currentNode.equals(targetNode)) {
                return smoothPath(map, retracePath(startNode, currentNode), isAquaticCrocodile);
            }

            if (exploredCount >= MAX_EXPLORED_NODES) {
                return smoothPath(map, retracePath(startNode, closestNodeSoFar), isAquaticCrocodile);
            }

            for (int[] dir : DIRECTIONS_8) {
                int dx = dir[0];
                int dy = dir[1];
                int baseStepCost = dir[2];

                int neighborCol = currentNode.col + dx;
                int neighborRow = currentNode.row + dy;
                long neighborKey = nodeKey(neighborCol, neighborRow);

                if (closedSet.contains(neighborKey)) {
                    continue;
                }

                TileType neighborTile = map.getTileAt(neighborCol, neighborRow);
                if (!neighborTile.isWalkable()) {
                    continue;
                }

                // Prevenção de Corner-Cutting em movimentos diagonais:
                // Só permite diagonal se ambos os blocos ortogonais adjacentes também forem caminháveis
                if (dx != 0 && dy != 0) {
                    TileType sideA = map.getTileAt(currentNode.col + dx, currentNode.row);
                    TileType sideB = map.getTileAt(currentNode.col, currentNode.row + dy);
                    if (!sideA.isWalkable() || !sideB.isWalkable()) {
                        continue;
                    }
                }

                int terrainMultiplier = computeTerrainWeight(neighborTile, isAquaticCrocodile);
                int moveCostToNeighbor = currentNode.gCost + (baseStepCost * terrainMultiplier);

                Node neighbor = allNodes.get(neighborKey);
                if (neighbor == null) {
                    neighbor = new Node(neighborCol, neighborRow);
                    neighbor.gCost = moveCostToNeighbor;
                    neighbor.hCost = getOctileDistance(neighbor, targetNode);
                    neighbor.parent = currentNode;
                    allNodes.put(neighborKey, neighbor);
                    openSet.add(neighbor);
                } else if (moveCostToNeighbor < neighbor.gCost) {
                    openSet.remove(neighbor);
                    neighbor.gCost = moveCostToNeighbor;
                    neighbor.parent = currentNode;
                    openSet.add(neighbor);
                }
            }
        }

        if (closestNodeSoFar != startNode) {
            return smoothPath(map, retracePath(startNode, closestNodeSoFar), isAquaticCrocodile);
        }

        return new ArrayList<>();
    }

    private int computeTerrainWeight(TileType tile, boolean isAquaticCrocodile) {
        if (isAquaticCrocodile) {
            if (tile == TileType.WATER) return 1;
            if (tile == TileType.MUD) return 2;
            return 4;
        }
        return tile.getMovementCost();
    }

    /**
     * Heurística Octile exata para grades de 8 direções (10 para cardeal, 14 para diagonal).
     */
    private int getOctileDistance(Node a, Node b) {
        int dx = Math.abs(a.col - b.col);
        int dy = Math.abs(a.row - b.row);
        return 10 * (dx + dy) + (14 - 2 * 10) * Math.min(dx, dy);
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

    /**
     * String-Pulling (Suavização de Rota):
     * Remove curvas em "escadinha" desnecessárias, mantendo apenas os pontos de inflexão reais
     * onde o terreno exige desvio de obstáculo ou mudança de custo de bioma.
     */
    private List<Node> smoothPath(TileMap map, List<Node> rawPath, boolean isAquaticCrocodile) {
        if (rawPath == null || rawPath.size() <= 2) {
            return rawPath;
        }

        List<Node> smoothed = new ArrayList<>();
        int currentIdx = 0;
        smoothed.add(rawPath.get(0));

        while (currentIdx < rawPath.size() - 1) {
            int furthestVisible = currentIdx + 1;
            // Testa até 6 nós à frente para manter espaçamento elegante de waypoints
            int maxLookAhead = Math.min(rawPath.size() - 1, currentIdx + 5);
            for (int testIdx = maxLookAhead; testIdx > currentIdx + 1; testIdx--) {
                if (hasClearWalkableCorridor(map, rawPath.get(currentIdx), rawPath.get(testIdx), isAquaticCrocodile)) {
                    furthestVisible = testIdx;
                    break;
                }
            }
            smoothed.add(rawPath.get(furthestVisible));
            currentIdx = furthestVisible;
        }

        return smoothed;
    }

    /**
     * Verifica se a linha reta entre dois nós está totalmente livre de obstáculos (incluindo margem de largura do corpo)
     * e não atravessa água profunda/lama sem necessidade para criaturas terrestres.
     */
    public static boolean hasClearWalkableCorridor(TileMap map, Node a, Node b, boolean isAquaticCrocodile) {
        double x1 = a.col * TileMap.TILE_SIZE + (TileMap.TILE_SIZE * 0.5);
        double y1 = a.row * TileMap.TILE_SIZE + (TileMap.TILE_SIZE * 0.5);
        double x2 = b.col * TileMap.TILE_SIZE + (TileMap.TILE_SIZE * 0.5);
        double y2 = b.row * TileMap.TILE_SIZE + (TileMap.TILE_SIZE * 0.5);
        return hasClearWorldCorridor(map, x1, y1, x2, y2, isAquaticCrocodile);
    }

    public static boolean hasClearWorldCorridor(TileMap map, double x1, double y1, double x2, double y2, boolean isAquaticCrocodile) {
        double dist = Math.hypot(x2 - x1, y2 - y1);
        int steps = (int) Math.ceil(dist / 10.0);
        if (steps <= 0) return true;

        double bodyMargin = 10.0; // Margem lateral para o corpo não raspar em quina de árvore/rocha

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double cx = x1 + (x2 - x1) * t;
            double cy = y1 + (y2 - y1) * t;

            // Checa o centro e as 4 extremidades da caixa do personagem
            if (!isPointFavorable(map, cx, cy, isAquaticCrocodile) ||
                !map.isWalkable(cx - bodyMargin, cy - bodyMargin) ||
                !map.isWalkable(cx + bodyMargin, cy - bodyMargin) ||
                !map.isWalkable(cx - bodyMargin, cy + bodyMargin) ||
                !map.isWalkable(cx + bodyMargin, cy + bodyMargin)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPointFavorable(TileMap map, double wx, double wy, boolean isAquaticCrocodile) {
        int c = (int) Math.floor(wx / TileMap.TILE_SIZE);
        int r = (int) Math.floor(wy / TileMap.TILE_SIZE);
        TileType type = map.getTileAt(c, r);
        if (!type.isWalkable()) return false;
        if (!isAquaticCrocodile && type == TileType.WATER) {
            return false; // Evita que o string-pulling jogue animais terrestres dentro do rio se o A* contornou pela ponte
        }
        return true;
    }

    public static int[] findNearestWalkableTile(TileMap map, int centerCol, int centerRow, int maxRadius) {
        if (map.getTileAt(centerCol, centerRow).isWalkable()) {
            return new int[]{centerCol, centerRow};
        }
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dr = -radius; dr <= radius; dr++) {
                for (int dc = -radius; dc <= radius; dc++) {
                    if (Math.abs(dc) != radius && Math.abs(dr) != radius) continue;
                    int c = centerCol + dc;
                    int r = centerRow + dr;
                    if (map.getTileAt(c, r).isWalkable()) {
                        return new int[]{c, r};
                    }
                }
            }
        }
        return null;
    }

    private long nodeKey(int col, int row) {
        return (((long) col) << 32) | (row & 0xFFFFFFFFL);
    }
}
