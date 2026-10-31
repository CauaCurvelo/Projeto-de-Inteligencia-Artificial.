package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.core.HighScoreManager;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.input.InputManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class World {
    private TileMap tileMap;
    private Player player;
    private Camera camera;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<FoodItem> foodItems = new ArrayList<>();
    private final List<MeatItem> meatItems = new ArrayList<>();
    private final List<Burrow> burrows = new ArrayList<>();
    private final List<Footprint> scentTrail = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();

    // Persistent High Score Manager
    private final HighScoreManager highScoreManager = new HighScoreManager();

    // Dynamic Weather System
    private WeatherType currentWeather = WeatherType.CLEAR;
    private double weatherTimer = 0.0;
    private final double weatherDuration = 45.0;
    private final List<WeatherDrop> rainDrops = new ArrayList<>();

    // Infinite Survival & Scoring Mechanics
    private double survivalTime = 0.0;
    private int score = 0;
    private int applesEaten = 0;
    private int enemiesDefeated = 0;
    private int dayCycleNumber = 1;
    private double cycleProgressTimer = 0.0;
    private final double cycleDuration = 90.0;

    // Boss & Reinforcement spawning
    private double foodRespawnTimer = 0.0;
    private double enemyReinforcementTimer = 0.0;
    private double bossSpawnTimer = 0.0;
    private static final Random rand = new Random();

    private boolean gameOver = false;
    private String gameOverReason = "";

    public World() {
        initRainDrops(90);
        initWorld();
    }

    private void initRainDrops(int count) {
        rainDrops.clear();
        for (int i = 0; i < count; i++) {
            rainDrops.add(new WeatherDrop(
                Math.random() * 960,
                Math.random() * 640,
                -40.0 + (Math.random() * 20 - 10),
                280.0 + (Math.random() * 80),
                12.0 + Math.random() * 8
            ));
        }
    }

    public void reset() {
        enemies.clear();
        foodItems.clear();
        meatItems.clear();
        burrows.clear();
        scentTrail.clear();
        particles.clear();
        projectiles.clear();
        survivalTime = 0.0;
        score = 0;
        applesEaten = 0;
        enemiesDefeated = 0;
        dayCycleNumber = 1;
        cycleProgressTimer = 0.0;
        foodRespawnTimer = 0.0;
        enemyReinforcementTimer = 0.0;
        bossSpawnTimer = 0.0;
        currentWeather = WeatherType.CLEAR;
        weatherTimer = 0.0;
        gameOver = false;
        gameOverReason = "";
        initWorld();
    }

    private void initWorld() {
        this.tileMap = new TileMap();
        this.player = new Player(0, 0, 3);
        double vpW = (camera != null) ? camera.getViewportWidth() : 960;
        double vpH = (camera != null) ? camera.getViewportHeight() : 640;
        this.camera = new Camera(vpW, vpH);
        spawnInitialSurroundings();
    }

    private void spawnInitialSurroundings() {
        for (int i = 0; i < 12; i++) {
            spawnFoodNearPlayer();
        }

        for (int i = 0; i < 5; i++) {
            int c = rand.nextInt(40) - 20;
            int r = rand.nextInt(40) - 20;
            TileType t = tileMap.getTileAt(c, r);
            if (t == TileType.GRASS || t == TileType.TALL_GRASS) {
                burrows.add(new Burrow(c, r));
            }
        }

        spawnEnemyAt(EnemyType.HUNTER, EnemySkin.BOAR, -20, -20);
        spawnEnemyAt(EnemyType.HUNTER, EnemySkin.BOAR, 22, 22);
        spawnEnemyAt(EnemyType.HOUND, EnemySkin.WOLF, 25, -20);
        spawnEnemyAt(EnemyType.HOUND, EnemySkin.WOLF, -20, 25);
        
        // Rivals with varied skins (Boar, Wolf, Badger)
        spawnEnemyAt(EnemyType.RIVAL, EnemySkin.BOAR, 15, 12);
        spawnEnemyAt(EnemyType.RIVAL, EnemySkin.WOLF, -15, -15);
        spawnEnemyAt(EnemyType.RIVAL, EnemySkin.BADGER, 18, -18);
    }

    private void spawnFoodNearPlayer() {
        int playerCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
        int playerRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);

        // Berries only spawn naturally near Trees or inside Tall Grass (shrubs)
        for (int tries = 0; tries < 40; tries++) {
            int c = playerCol + (rand.nextInt(50) - 25);
            int r = playerRow + (rand.nextInt(50) - 25);
            TileType t = tileMap.getTileAt(c, r);

            if (t == TileType.GRASS || t == TileType.TALL_GRASS) {
                // Must be adjacent to a Tree or inside Tall Grass
                boolean isNearTreeOrShrub = (t == TileType.TALL_GRASS) ||
                    tileMap.getTileAt(c + 1, r) == TileType.TREE ||
                    tileMap.getTileAt(c - 1, r) == TileType.TREE ||
                    tileMap.getTileAt(c, r + 1) == TileType.TREE ||
                    tileMap.getTileAt(c, r - 1) == TileType.TREE;

                if (isNearTreeOrShrub) {
                    foodItems.add(new FoodItem(c, r));
                    break;
                }
            }
        }
    }

    private void spawnEnemyAt(EnemyType type, EnemySkin skin, int col, int row) {
        double scale = 1.0 + (survivalTime / 240.0);
        enemies.add(new Enemy(type, skin, col * TileMap.TILE_SIZE + 16, row * TileMap.TILE_SIZE + 16, scale, scale));
    }

    public void update(double deltaTime, InputManager inputManager) {
        if (inputManager.pollRestartRequested()) {
            reset();
            return;
        }

        if (gameOver) return;

        survivalTime += deltaTime;
        cycleProgressTimer += deltaTime;
        score += (int) (deltaTime * 10);

        if (cycleProgressTimer >= cycleDuration) {
            cycleProgressTimer = 0.0;
            dayCycleNumber++;
            score += 500;
        }

        highScoreManager.checkAndSave(score, survivalTime);

        // Weather transitions
        weatherTimer += deltaTime;
        if (weatherTimer >= weatherDuration) {
            weatherTimer = 0.0;
            switch (currentWeather) {
                case CLEAR: currentWeather = rand.nextBoolean() ? WeatherType.RAIN : WeatherType.FOG; break;
                case RAIN: currentWeather = rand.nextBoolean() ? WeatherType.CLEAR : WeatherType.FOG; break;
                case FOG: currentWeather = rand.nextBoolean() ? WeatherType.CLEAR : WeatherType.RAIN; break;
            }
        }

        if (currentWeather == WeatherType.RAIN) {
            for (WeatherDrop drop : rainDrops) {
                drop.update(deltaTime, camera.getViewportWidth(), camera.getViewportHeight());
            }
        }

        // Procedural food streaming (limited density, only near trees or bushes)
        foodRespawnTimer += deltaTime;
        if (foodRespawnTimer >= 5.0) {
            foodRespawnTimer = 0.0;
            cleanupDistantFood();
            while (foodItems.size() < 14) {
                spawnFoodNearPlayer();
            }
        }

        // Reinforcements with varied Rival skins (every 30s instead of 45s)
        enemyReinforcementTimer += deltaTime;
        if (enemyReinforcementTimer >= 30.0) {
            enemyReinforcementTimer = 0.0;
            if (enemies.size() < 18) {
                int pCol = (int)(player.getX() / TileMap.TILE_SIZE);
                int pRow = (int)(player.getY() / TileMap.TILE_SIZE);
                int offC = (rand.nextBoolean() ? 1 : -1) * (20 + rand.nextInt(12));
                int offR = (rand.nextBoolean() ? 1 : -1) * (20 + rand.nextInt(12));
                
                int pick = rand.nextInt(3);
                if (pick == 0) {
                    spawnEnemyAt(EnemyType.HUNTER, EnemySkin.BOAR, pCol + offC, pRow + offR);
                } else if (pick == 1) {
                    spawnEnemyAt(EnemyType.HOUND, EnemySkin.WOLF, pCol + offC, pRow + offR);
                } else {
                    EnemySkin rivalSkin = rand.nextBoolean() ? EnemySkin.WOLF : EnemySkin.BADGER;
                    spawnEnemyAt(EnemyType.RIVAL, rivalSkin, pCol + offC, pRow + offR);
                }
            }
        }

        // Boss Spawn with Varied Skins (every 50s instead of 75s)
        bossSpawnTimer += deltaTime;
        if (bossSpawnTimer >= 50.0) {
            bossSpawnTimer = 0.0;
            int pCol = (int)(player.getX() / TileMap.TILE_SIZE);
            int pRow = (int)(player.getY() / TileMap.TILE_SIZE);
            int offC = (rand.nextBoolean() ? 1 : -1) * 22;
            int offR = (rand.nextBoolean() ? 1 : -1) * 22;

            int bossChoice = rand.nextInt(3);
            EnemySkin bossSkin;
            if (bossChoice == 0) bossSkin = EnemySkin.BEAR;
            else if (bossChoice == 1) bossSkin = EnemySkin.CROCODILE;
            else bossSkin = EnemySkin.PANTHER;

            spawnEnemyAt(EnemyType.BEAR_BOSS, bossSkin, pCol + offC, pRow + offR);
        }

        // Update Player
        player.update(inputManager.getDx(), inputManager.getDy(), inputManager.isSprinting(), 
                      inputManager.isAttackPressed(), deltaTime, tileMap, scentTrail, particles, burrows);

        if (player.getHunger() <= 0) {
            triggerGameOver("Você sucumbiu à inanição (Fome esgotada)!");
            return;
        }

        for (FoodItem food : foodItems) {
            if (!food.isEaten()) {
                double dist = Math.hypot(player.getX() - food.getX(), player.getY() - food.getY());
                if (dist < 20.0) {
                    food.setEaten(true);
                    player.feed(30.0);
                    applesEaten++;
                    score += 100;
                    highScoreManager.checkAndSave(score, survivalTime);
                }
            }
        }

        for (MeatItem meat : meatItems) {
            if (!meat.isEaten()) {
                meat.update(deltaTime);
                double dist = Math.hypot(player.getX() - meat.getX(), player.getY() - meat.getY());
                if (dist < 22.0) {
                    meat.setEaten(true);
                    player.feed(50.0);
                    player.healLife(1);
                    score += 250;
                }
            }
        }

        double scentDecayMultiplier = currentWeather.getScentWashMultiplier();
        Iterator<Footprint> fpIt = scentTrail.iterator();
        while (fpIt.hasNext()) {
            Footprint fp = fpIt.next();
            fp.update(deltaTime * scentDecayMultiplier);
            if (fp.isExpired()) {
                fpIt.remove();
            }
        }

        Iterator<Particle> pIt = particles.iterator();
        while (pIt.hasNext()) {
            Particle p = pIt.next();
            p.update(deltaTime);
            if (p.isExpired()) {
                pIt.remove();
            }
        }

        Iterator<Projectile> projIt = projectiles.iterator();
        while (projIt.hasNext()) {
            Projectile proj = projIt.next();
            proj.update(deltaTime, tileMap);

            if (proj.isActive()) {
                double dist = Math.hypot(proj.getX() - player.getX(), proj.getY() - player.getY());
                if (dist < 18.0) {
                    proj.deactivate();
                    if (player.takeDamage()) {
                        if (player.getLives() <= 0) {
                            triggerGameOver("O Caçador abateu a raposa com um disparo!");
                            return;
                        }
                    }
                }
            }

            if (!proj.isActive()) {
                projIt.remove();
            }
        }

        // Update Enemies & Combat
        double timeProgress = (survivalTime / 240.0);
        Iterator<Enemy> enemyIt = enemies.iterator();
        while (enemyIt.hasNext()) {
            Enemy enemy = enemyIt.next();
            enemy.update(deltaTime, player, foodItems, scentTrail, projectiles, enemies, tileMap, timeProgress, currentWeather);

            double distToPlayer = Math.hypot(enemy.getX() - player.getX(), enemy.getY() - player.getY());

            if (player.isAttacking()) {
                double attackRange = 36.0;
                double attackX = player.getX() + player.getFaceDx() * 18;
                double attackY = player.getY() + player.getFaceDy() * 18;
                double distToAttack = Math.hypot(enemy.getX() - attackX, enemy.getY() - attackY);

                if (distToAttack <= attackRange) {
                    boolean defeated = enemy.takeDamage(1);
                    enemy.takeDamage(0);

                    for (int k = 0; k < 4; k++) {
                        particles.add(new Particle(
                            enemy.getX(), enemy.getY(),
                            player.getFaceDx() * 60 + (rand.nextDouble() * 40 - 20),
                            player.getFaceDy() * 60 + (rand.nextDouble() * 40 - 20),
                            0.25, "#FFD700", 3.0
                        ));
                    }

                    if (defeated) {
                        enemiesDefeated++;
                        score += enemy.getType().getDefeatScore();
                        meatItems.add(new MeatItem(enemy.getX(), enemy.getY()));
                        enemyIt.remove();
                        continue;
                    }
                }
            }

            // Enemy Melee Attack Check against Player
            if (distToPlayer < 28.0 && enemy.getHp() > 0) {
                boolean hit = false;
                if (enemy.getType() == EnemyType.HUNTER || enemy.getType() == EnemyType.HOUND || enemy.getType() == EnemyType.BEAR_BOSS) {
                    hit = true;
                } else if (enemy.getType() == EnemyType.RIVAL) {
                    // Rivals always attack if close!
                    hit = true;
                }

                if (hit && player.takeDamage()) {
                    // Particle burst on player hit
                    for (int k = 0; k < 5; k++) {
                        particles.add(new Particle(
                            player.getX(), player.getY(),
                            (rand.nextDouble() * 80 - 40),
                            (rand.nextDouble() * 80 - 40),
                            0.3, "#E74C3C", 4.0
                        ));
                    }

                    if (player.getLives() <= 0) {
                        if (enemy.getType() == EnemyType.BEAR_BOSS) triggerGameOver(enemy.getSkin().getDisplayName() + " o subjugou!");
                        else if (enemy.getType() == EnemyType.HUNTER) triggerGameOver("O Caçador o encurralou e capturou!");
                        else if (enemy.getType() == EnemyType.HOUND) triggerGameOver("O Cão de Caça o interceptou!");
                        else triggerGameOver(enemy.getSkin().getDisplayName() + " o atacou!");
                        return;
                    }
                }
            }
        }

        camera.update(player.getX(), player.getY());
    }

    private void triggerGameOver(String reason) {
        this.gameOver = true;
        this.gameOverReason = reason;
        highScoreManager.checkAndSave(score, survivalTime);
    }

    private void cleanupDistantFood() {
        foodItems.removeIf(f -> Math.hypot(f.getX() - player.getX(), f.getY() - player.getY()) > 1800.0 || f.isEaten());
        meatItems.removeIf(m -> Math.hypot(m.getX() - player.getX(), m.getY() - player.getY()) > 1800.0 || m.isEaten());
        burrows.removeIf(b -> Math.hypot(b.getX() - player.getX(), b.getY() - player.getY()) > 2200.0);

        // Procedurally spawn burrows around player if few are nearby
        if (burrows.size() < 7) {
            int pCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
            int pRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);
            for (int t = 0; t < 15; t++) {
                int bc = pCol + (rand.nextInt(60) - 30);
                int br = pRow + (rand.nextInt(60) - 30);
                if (tileMap.getTileAt(bc, br) == TileType.GRASS) {
                    burrows.add(new Burrow(bc, br));
                    break;
                }
            }
        }
    }

    public TileMap getTileMap() { return tileMap; }
    public Player getPlayer() { return player; }
    public Camera getCamera() { return camera; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<FoodItem> getFoodItems() { return foodItems; }
    public List<MeatItem> getMeatItems() { return meatItems; }
    public List<Burrow> getBurrows() { return burrows; }
    public List<Footprint> getScentTrail() { return scentTrail; }
    public List<Particle> getParticles() { return particles; }
    public List<Projectile> getProjectiles() { return projectiles; }
    public WeatherType getCurrentWeather() { return currentWeather; }
    public List<WeatherDrop> getRainDrops() { return rainDrops; }
    public HighScoreManager getHighScoreManager() { return highScoreManager; }
    public double getSurvivalTime() { return survivalTime; }
    public int getScore() { return score; }
    public int getApplesEaten() { return applesEaten; }
    public int getEnemiesDefeated() { return enemiesDefeated; }
    public int getDayCycleNumber() { return dayCycleNumber; }
    public double getCycleProgressTimer() { return cycleProgressTimer; }
    public double getCycleDuration() { return cycleDuration; }
    public boolean isGameOver() { return gameOver; }
    public String getGameOverReason() { return gameOverReason; }
}
