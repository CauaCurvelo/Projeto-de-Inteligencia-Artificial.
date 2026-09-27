package br.edu.unex.sentinela.world;

public enum TileType {
    GRASS(true, 1),
    TALL_GRASS(true, 1),
    DIRT_PATH(true, 1), // Fast natural paths
    MUD(true, 3),       // Heavy swamp terrain, washes scent
    WATER(true, 4),     // Swimmable river/lakes, completely clears scent
    TREE(false, 999),   // Impassable obstacle & blocks vision
    WALL(false, 999);   // Granite cliffs and border mountains

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
