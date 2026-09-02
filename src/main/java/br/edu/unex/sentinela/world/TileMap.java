package br.edu.unex.sentinela.world;

public class TileMap {
    public static final int TILE_SIZE = 32;
    private final int cols;
    private final int rows;
    private final TileType[][] map;

    public TileMap(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.map = new TileType[rows][cols];
        generateMap();
    }

    private void generateMap() {
        // Initialize with grass
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                map[row][col] = TileType.GRASS;
                
                // Borders as walls
                if (row == 0 || row == rows - 1 || col == 0 || col == cols - 1) {
                    map[row][col] = TileType.WALL;
                }
            }
        }

        // Add some obstacles (Walls)
        for (int i = 5; i < 10; i++) {
            map[5][i] = TileType.WALL;
            map[12][i + 5] = TileType.WALL;
        }

        // Add some mud
        for (int row = 8; row <= 10; row++) {
            for (int col = 18; col <= 21; col++) {
                map[row][col] = TileType.MUD;
            }
        }
        
        // Add some water
        for (int row = 14; row <= 16; row++) {
            for (int col = 3; col <= 6; col++) {
                map[row][col] = TileType.WATER;
            }
        }
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public TileType getTileAt(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return TileType.WALL; // Out of bounds is treated as wall
        }
        return map[row][col];
    }

    public boolean isWalkable(double worldX, double worldY) {
        int col = (int) Math.floor(worldX / TILE_SIZE);
        int row = (int) Math.floor(worldY / TILE_SIZE);
        return getTileAt(col, row).isWalkable();
    }
}
