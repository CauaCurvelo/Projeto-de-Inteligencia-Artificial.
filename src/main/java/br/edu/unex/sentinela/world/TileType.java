package br.edu.unex.sentinela.world;

public enum TileType {
    GRASS(true, 1),
    MUD(true, 2),
    WALL(false, 999),
    WATER(false, 999);

    private final boolean walkable;
    private final int movementCost;

    TileType(boolean walkable, int movementCost) {
        this.walkable = walkable;
        this.movementCost = movementCost;
    }

    public boolean isWalkable() {
        return walkable;
    }

    public int getMovementCost() {
        return movementCost;
    }
}
