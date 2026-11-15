package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.ai.BossCombatExecutor;
import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.bestiary.BestiaryUnlockManager;
import br.edu.unex.sentinela.core.HighScoreManager;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.PassiveAnimal;
import br.edu.unex.sentinela.entity.PassiveAnimalType;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.progress.AchievementTracker;
import br.edu.unex.sentinela.progress.GameEvent;
import br.edu.unex.sentinela.progress.GameEventBus;
import br.edu.unex.sentinela.progress.MatchHistoryRepository;
import br.edu.unex.sentinela.progress.MatchRecord;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class World {
    private TileMap tileMap;
    private Player player;
    private Camera camera;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<PassiveAnimal> passiveAnimals = new ArrayList<>();
    private final List<Firefly> fireflies = new ArrayList<>();
    private final List<BearTrap> bearTraps = new ArrayList<>();
    private final List<WaterRipple> waterRipples = new ArrayList<>();
    private final List<FoodItem> foodItems = new ArrayList<>();
    private final List<MeatItem> meatItems = new ArrayList<>();
    private final List<Burrow> burrows = new ArrayList<>();
    private final List<Footprint> scentTrail = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();

    private double passiveAnimalStreamTimer = 0.0;
    private double fireflyStreamTimer = 0.0;
    private double playerWaterRippleTimer = 0.0;

    // Persistent High Score Manager
    private final HighScoreManager highScoreManager = new HighScoreManager();
    private final EcosystemSpawner ecosystemSpawner = new EcosystemSpawner();

    // Dynamic Weather & Wind System
    private WeatherType currentWeather = WeatherType.CLEAR;
    private double weatherTimer = 0.0;
    private final double weatherDuration = 45.0;
    private static final double WEATHER_TRANSITION_DURATION = 4.0;
    private double rainIntensity = 0.0;
    private double fogIntensity = 0.0;
    private double stormIntensity = 0.0;
    private double lightningCooldown = 5.0;
    private double lightningFlash = 0.0;
    private double lastDeltaTime = 0.016;
    private final List<WeatherDrop> rainDrops = new ArrayList<>();
    private final WindSystem windSystem = new WindSystem();
    private final List<LightningStrike> activeLightningStrikes = new ArrayList<>();

    // Infinite Survival & Scoring Mechanics
    private double survivalTime = 0.0;
    private int score = 0;
    private int applesEaten = 0;
    private int meatsEaten = 0;
    private int enemiesDefeated = 0;
    private int dayCycleNumber = 1;
    private double cycleProgressTimer = 0.0;
    private final double cycleDuration = 90.0;

    // Boss & Reinforcement spawning
    private static final SecureRandom SEED_GENERATOR = new SecureRandom();
    private long currentWorldSeed = generateRandomSeed();
    private double foodRespawnTimer = 0.0;
    private double enemyReinforcementTimer = 0.0;
    private double bossSpawnTimer = 0.0;
    private static final Random rand = new Random();

    private boolean gameOver = false;
    private String gameOverReason = "";

    public World() {
        initRainDrops(220);
        initWorld();
    }

    public World(long fixedWorldSeed) {
        this.currentWorldSeed = fixedWorldSeed;
        initRainDrops(220);
        initWorld();
    }

    private static long generateRandomSeed() {
        return SEED_GENERATOR.nextLong() ^ System.nanoTime();
    }

    public long getWorldSeed() {
        return currentWorldSeed;
    }

    private void initRainDrops(int count) {
        rainDrops.clear();
        for (int i = 0; i < count; i++) {
            rainDrops.add(new WeatherDrop(
                Math.random() * 1400 - 100,
                Math.random() * 900 - 100,
                -45.0 + (Math.random() * 16 - 8),
                600.0 + (Math.random() * 90),
                14.0 + Math.random() * 8
            ));
        }
    }

    public void reset() {
        highScoreManager.flushNow();
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
        meatsEaten = 0;
        enemiesDefeated = 0;
        dayCycleNumber = 1;
        cycleProgressTimer = 0.0;
        foodRespawnTimer = 0.0;
        enemyReinforcementTimer = 0.0;
        bossSpawnTimer = 0.0;
        currentWeather = WeatherType.CLEAR;
        weatherTimer = 0.0;
        rainIntensity = 0.0;
        fogIntensity = 0.0;
        stormIntensity = 0.0;
        lightningCooldown = 5.0;
        lightningFlash = 0.0;
        activeLightningStrikes.clear();
        initRainDrops(220);
        currentWorldSeed = generateRandomSeed();
        gameOver = false;
        gameOverReason = "";
        initWorld();
    }

    private void initWorld() {
        this.tileMap = new TileMap(currentWorldSeed);
        this.player = new Player(0, 0, 3);
        double vpW = (camera != null) ? camera.getViewportWidth() : 960;
        double vpH = (camera != null) ? camera.getViewportHeight() : 640;
        this.camera = new Camera(vpW, vpH);
        spawnInitialSurroundings();
    }

    private void spawnInitialSurroundings() {
        ecosystemSpawner.populateInitialEcosystem(tileMap, enemies, foodItems, burrows);
        for (int i = 0; i < 14; i++) {
            spawnPassiveAnimalNearPlayer();
        }
        // Popula a clareira cênica central com fauna pacífica e graciosa (coelhos, cervos, pássaros, sapos)
        passiveAnimals.add(new PassiveAnimal(PassiveAnimalType.RABBIT, 70.0, 45.0));
        passiveAnimals.add(new PassiveAnimal(PassiveAnimalType.RABBIT, 160.0, 110.0));
        passiveAnimals.add(new PassiveAnimal(PassiveAnimalType.DEER, 210.0, 75.0));
        passiveAnimals.add(new PassiveAnimal(PassiveAnimalType.BIRD, 95.0, 140.0));
        passiveAnimals.add(new PassiveAnimal(PassiveAnimalType.BIRD, 140.0, 30.0));
        passiveAnimals.add(new PassiveAnimal(PassiveAnimalType.FROG, 240.0, 160.0));
    }

    /** Atualiza o ecossistema vivo e a atmosfera ambiente em tempo real para a vitrine do Menu Principal. */
    public void updateMenuAmbient(double deltaTime) {
        survivalTime += deltaTime;
        cycleProgressTimer = (cycleProgressTimer + deltaTime) % cycleDuration;

        for (PassiveAnimal animal : passiveAnimals) {
            animal.update(deltaTime, null, null, passiveAnimals, foodItems, burrows, fireflies, particles, tileMap, waterRipples, currentWeather, 0.0);
        }

        Iterator<WaterRipple> ripIt = waterRipples.iterator();
        while (ripIt.hasNext()) {
            WaterRipple wr = ripIt.next();
            wr.update(deltaTime);
            if (wr.isExpired()) ripIt.remove();
        }

        streamFireflies(deltaTime);

        Iterator<Particle> pIt = particles.iterator();
        while (pIt.hasNext()) {
            Particle p = pIt.next();
            p.update(deltaTime);
            if (p.isExpired()) pIt.remove();
        }

        camera.updateMenuPan(survivalTime);
        windSystem.update(deltaTime, camera.getX(), camera.getY(), camera.getViewportWidth(), camera.getViewportHeight(), currentWeather, BiomeType.MEADOW);
    }

    private void spawnFoodNearPlayer() {
        int playerCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
        int playerRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);
        FoodItem newFood = ecosystemSpawner.spawnFoodNearPlayer(tileMap, foodItems, playerCol, playerRow);
        if (newFood != null) {
            // Emite partículas de broto vegetal e pólen ao germinar
            for (int i = 0; i < 5; i++) {
                double px = newFood.getX() + (rand.nextDouble() - 0.5) * 16.0;
                double py = newFood.getY() + (rand.nextDouble() - 0.5) * 12.0;
                double vx = (rand.nextDouble() - 0.5) * 18.0;
                double vy = -12.0 - rand.nextDouble() * 20.0;
                particles.add(new Particle(px, py, vx, vy, 0.45, "#81C784", 2.2));
            }
        }
    }

    public void update(double deltaTime, InputManager inputManager) {
        this.lastDeltaTime = deltaTime;

        if (inputManager.pollZoomInRequested()) {
            camera.zoomIn();
        }
        if (inputManager.pollZoomOutRequested()) {
            camera.zoomOut();
        }

        if (gameOver) return;

        survivalTime += deltaTime;
        cycleProgressTimer += deltaTime;
        score += (int) (deltaTime * 10);

        if (cycleProgressTimer >= cycleDuration) {
            cycleProgressTimer = 0.0;
            dayCycleNumber++;
            score += 500;
            GameEventBus.get().post(new GameEvent(GameEvent.Type.DAY_CYCLE_PASSED));
        }

        highScoreManager.checkAndSave(score, survivalTime);

        // Weather transitions & organic smoothing (transição gradual de ~4s)
        weatherTimer += deltaTime;
        if (weatherTimer >= weatherDuration) {
            weatherTimer = 0.0;
            switch (currentWeather) {
                case CLEAR:
                    currentWeather = (rand.nextDouble() < 0.50) ? WeatherType.RAIN : WeatherType.FOG;
                    break;
                case RAIN:
                    currentWeather = (rand.nextDouble() < 0.38) ? WeatherType.STORM : (rand.nextBoolean() ? WeatherType.CLEAR : WeatherType.FOG);
                    break;
                case FOG:
                    currentWeather = rand.nextBoolean() ? WeatherType.CLEAR : WeatherType.RAIN;
                    break;
                case STORM:
                    currentWeather = (rand.nextDouble() < 0.70) ? WeatherType.RAIN : WeatherType.CLEAR;
                    GameEventBus.get().post(new GameEvent(GameEvent.Type.STORM_SURVIVED));
                    break;
            }
        }

        double targetRain = (currentWeather == WeatherType.RAIN || currentWeather == WeatherType.STORM) ? 1.0 : 0.0;
        double targetFog = (currentWeather == WeatherType.FOG) ? 1.0 : 0.0;
        double targetStorm = (currentWeather == WeatherType.STORM) ? 1.0 : 0.0;
        double transStep = deltaTime / WEATHER_TRANSITION_DURATION;

        if (rainIntensity < targetRain) {
            rainIntensity = Math.min(targetRain, rainIntensity + transStep);
        } else if (rainIntensity > targetRain) {
            rainIntensity = Math.max(targetRain, rainIntensity - transStep);
        }

        if (fogIntensity < targetFog) {
            fogIntensity = Math.min(targetFog, fogIntensity + transStep);
        } else if (fogIntensity > targetFog) {
            fogIntensity = Math.max(targetFog, fogIntensity - transStep);
        }

        if (stormIntensity < targetStorm) {
            stormIntensity = Math.min(targetStorm, stormIntensity + transStep);
        } else if (stormIntensity > targetStorm) {
            stormIntensity = Math.max(targetStorm, stormIntensity - transStep);
        }

        // Atualização dinâmica do Sistema de Vento (suave e diegético)
        double camX = (camera != null) ? camera.getX() : 0.0;
        double camY = (camera != null) ? camera.getY() : 0.0;
        double vpW = (camera != null) ? camera.getViewportWidth() : 960.0;
        double vpH = (camera != null) ? camera.getViewportHeight() : 640.0;
        BiomeType currentBiome = BiomeType.MEADOW;
        if (player != null && tileMap != null) {
            currentBiome = tileMap.getBiomeAtWorld(player.getX(), player.getY());
        }
        windSystem.update(deltaTime, camX, camY, vpW, vpH, currentWeather, currentBiome);

        // Atualização dos raios ativos e quedas de raios em tempestade
        Iterator<LightningStrike> strikeIt = activeLightningStrikes.iterator();
        while (strikeIt.hasNext()) {
            LightningStrike strike = strikeIt.next();
            strike.update(deltaTime, this);
            if (strike.isDead()) {
                strikeIt.remove();
            }
        }

        if (stormIntensity > 0.25 || currentWeather == WeatherType.STORM) {
            lightningCooldown -= deltaTime;
            if (lightningCooldown <= 0) {
                lightningCooldown = 3.6 + rand.nextDouble() * (5.5 - stormIntensity * 2.5);
                spawnStormLightning(camX, camY, vpW, vpH);
            }
        }

        if (lightningFlash > 0.0) {
            lightningFlash = Math.max(0.0, lightningFlash - deltaTime * 4.5);
        }

        // Procedural entity & food streaming (limited density, only near trees or bushes)
        foodRespawnTimer += deltaTime;
        if (foodRespawnTimer >= 5.0) {
            foodRespawnTimer = 0.0;
            cleanupDistantEntities();
            while (foodItems.size() < 18) {
                spawnFoodNearPlayer();
            }
        }

        // Reinforcements with varied Rival skins (continuous dynamic population)
        enemyReinforcementTimer += deltaTime;
        if (enemyReinforcementTimer >= 18.0) {
            enemyReinforcementTimer = 0.0;
            if (enemies.size() < 24) {
                ecosystemSpawner.spawnReinforcementNearPlayer(tileMap, enemies, player, survivalTime, camera);
            }
        }

        // Boss Spawn with Varied Skins (every 40s)
        bossSpawnTimer += deltaTime;
        if (bossSpawnTimer >= 40.0) {
            bossSpawnTimer = 0.0;
            ecosystemSpawner.spawnApexBossNearPlayer(tileMap, enemies, player, survivalTime, camera);
        }

        // Update Player
        player.update(inputManager.getDx(), inputManager.getDy(), inputManager.isSprinting(), 
                      inputManager.isAttackPressed(), deltaTime, tileMap, scentTrail, particles, burrows,
                      windSystem.getWindVx(), windSystem.getWindVy(), windSystem.getIntensity(), rainIntensity);

        if (player.getHunger() <= 0) {
            triggerGameOver("Você sucumbiu à inanição (Fome esgotada)!");
            return;
        }

        for (FoodItem food : foodItems) {
            food.update(deltaTime);
            if (!food.isEaten()) {
                double dist = Math.hypot(player.getX() - food.getX(), player.getY() - food.getY());
                if (dist < 20.0) {
                    food.setEaten(true);
                    player.feed(food.getNutritionGain());
                    player.restoreStamina(food.getStaminaGain());
                    applesEaten++;
                    score += food.getScoreBonus();
                    for (int p = 0; p < 5; p++) {
                        particles.add(new Particle(
                            food.getX(), food.getY(),
                            (rand.nextDouble() - 0.5) * 70.0,
                            -18.0 - rand.nextDouble() * 42.0,
                            0.40, food.getVariant().getBerryColorHex(), 3.2
                        ));
                    }
                    highScoreManager.checkAndSave(score, survivalTime);
                    GameEventBus.get().post(new GameEvent(GameEvent.Type.APPLE_EATEN));
                }
            }
        }

        for (MeatItem meat : meatItems) {
            if (!meat.isEaten()) {
                meat.update(deltaTime);
                double dist = Math.hypot(player.getX() - meat.getX(), player.getY() - meat.getY());
                if (dist < 22.0) {
                    meat.setEaten(true);
                    MeatTier tier = meat.getTier();
                    player.feed(tier.getNutritionGain());
                    player.healLife(tier.getHealHearts());
                    meatsEaten++;
                    score += tier.getEatScoreBonus();
                    highScoreManager.checkAndSave(score, survivalTime);
                    GameEventBus.get().post(new GameEvent(GameEvent.Type.MEAT_EATEN));
                }
            }
        }

        if (player.isBurrowed()) {
            GameEventBus.get().post(new GameEvent(GameEvent.Type.BURROW_ENTERED));
        }
        if (player.isSwimming()) {
            GameEventBus.get().post(new GameEvent(GameEvent.Type.RIVER_SWAM));
        }
        if (currentWeather == WeatherType.RAIN) {
            GameEventBus.get().post(new GameEvent(GameEvent.Type.RAIN_SURVIVED));
        }
        currentBiome = tileMap.getBiomeAtWorld(player.getX(), player.getY());
        GameEventBus.get().post(new GameEvent(GameEvent.Type.BIOME_VISITED, currentBiome));

        AchievementTracker.get().updateTick(survivalTime, score, player.getLives());

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
            proj.update(deltaTime, tileMap, windSystem.getWindVx(), windSystem.getWindVy());

            if (proj.isActive()) {
                double dist = Math.hypot(proj.getX() - player.getX(), proj.getY() - player.getY());
                if (dist < 18.0) {
                    proj.deactivate();
                    if (player.takeDamage(proj.getX(), proj.getY())) {
                        if (player.getLives() <= 0) {
                            triggerGameOver("O Caçador abateu a raposa com um disparo!");
                            return;
                        }
                    }
                }

                // Flechas do caçador também acertam feras selvagens hostis em combate
                if (proj.isActive()) {
                    for (Enemy enemy : enemies) {
                        if (enemy.getHp() <= 0 || enemy.getType() == EnemyType.HUNTER || enemy.getType() == EnemyType.HOUND) continue;
                        double distE = Math.hypot(proj.getX() - enemy.getX(), proj.getY() - enemy.getY());
                        if (distE < 22.0) {
                            proj.deactivate();
                            enemy.takeDamage(1, proj.getX(), proj.getY());
                            br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.HIT_IMPACT, 0.85, 0, 1.1);
                            for (int k = 0; k < 6; k++) {
                                particles.add(new Particle(enemy.getX(), enemy.getY(),
                                    (rand.nextDouble() - 0.5) * 70, (rand.nextDouble() - 0.5) * 70,
                                    0.28, "#EF5350", 3.2));
                            }
                            break;
                        }
                    }
                }
            }

            if (!proj.isActive()) {
                projIt.remove();
            }
        }

        // 1. Atualizar e limpar ondulações de água (Water Ripples)
        Iterator<WaterRipple> ripIt = waterRipples.iterator();
        while (ripIt.hasNext()) {
            WaterRipple rip = ripIt.next();
            rip.update(deltaTime);
            if (rip.isExpired()) {
                ripIt.remove();
            }
        }

        // Jogador nadando na água gera ondulações
        playerWaterRippleTimer += deltaTime;
        if (player.isSwimming() && player.isMoving() && playerWaterRippleTimer >= 0.22) {
            playerWaterRippleTimer = 0.0;
            waterRipples.add(new WaterRipple(player.getX(), player.getY(), 18.0, 0.70));
        }
        if (player.isSwimming() && player.isAttacking() && player.getAttackTimer() > 0.17) {
            waterRipples.add(new WaterRipple(
                player.getX() + Math.cos(player.getVisualAngle()) * 16.0,
                player.getY() + Math.sin(player.getVisualAngle()) * 16.0,
                24.0, 0.88
            ));
        }

        // Gotas de chuva atingindo água geram micro-ondulações ocasionais
        if (rainIntensity > 0.20 && rand.nextDouble() < (0.12 * rainIntensity)) {
            double rx = player.getX() + (rand.nextDouble() - 0.5) * 440.0;
            double ry = player.getY() + (rand.nextDouble() - 0.5) * 340.0;
            int rc = (int) Math.floor(rx / TileMap.TILE_SIZE);
            int rr = (int) Math.floor(ry / TileMap.TILE_SIZE);
            if (tileMap.getTileAt(rc, rr) == TileType.WATER) {
                waterRipples.add(new WaterRipple(rx, ry, 10.0, 0.50));
            }
        }

        // 2. Atualizar armadilhas de urso (Bear Traps) e verificar acionamentos
        Iterator<BearTrap> trapIt = bearTraps.iterator();
        while (trapIt.hasNext()) {
            BearTrap trap = trapIt.next();
            trap.update(deltaTime);

            if (trap.isArmed()) {
                // Jogador pisando na armadilha
                double distPlayer = Math.hypot(player.getX() - trap.getX(), player.getY() - trap.getY());
                if (distPlayer < 14.0 && !player.isBurrowed()) {
                    trap.snap();
                    player.takeDamage(trap.getX(), trap.getY());
                    player.trapImmobilize(2.5);
                    GameEventBus.get().post(new GameEvent(GameEvent.Type.TRAP_TRIGGERED));
                    br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.HIT_IMPACT, 0.95, 0, 0.70);
                    for (int k = 0; k < 6; k++) {
                        particles.add(new Particle(trap.getX(), trap.getY(),
                            (rand.nextDouble() - 0.5) * 60, (rand.nextDouble() - 0.5) * 60,
                            0.35, "#B0BEC5", 3.0));
                    }
                }

                // Inimigos pisando na armadilha
                for (Enemy enemy : enemies) {
                    if (enemy.getHp() <= 0) continue;
                    double distEnemy = Math.hypot(enemy.getX() - trap.getX(), enemy.getY() - trap.getY());
                    if (distEnemy < 15.0) {
                        trap.snap();
                        enemy.takeDamage(2, trap.getX(), trap.getY());
                        enemy.trapImmobilize(3.5);
                        br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.HIT_IMPACT, 0.90, 0, 0.75);
                        for (int k = 0; k < 6; k++) {
                            particles.add(new Particle(trap.getX(), trap.getY(),
                                (rand.nextDouble() - 0.5) * 60, (rand.nextDouble() - 0.5) * 60,
                                0.35, "#B0BEC5", 3.0));
                        }
                        break;
                    }
                }
            }

            if (trap.isExpired()) {
                trapIt.remove();
            }
        }

        // 3. Streaming e atualização de fauna passiva silvestre
        passiveAnimalStreamTimer += deltaTime;
        if (passiveAnimalStreamTimer >= 4.0) {
            passiveAnimalStreamTimer = 0.0;
            passiveAnimals.removeIf(a -> {
                double dist = Math.hypot(a.getX() - player.getX(), a.getY() - player.getY());
                return dist > 1100.0 && (camera == null || !camera.isPointInView(a.getX(), a.getY(), 60.0));
            });
            while (passiveAnimals.size() < 14) {
                spawnPassiveAnimalNearPlayer();
            }
        }

        double timeProgress = (survivalTime / 240.0);
        Iterator<PassiveAnimal> passIt = passiveAnimals.iterator();
        while (passIt.hasNext()) {
            PassiveAnimal animal = passIt.next();
            animal.update(deltaTime, player, enemies, passiveAnimals, foodItems, burrows, fireflies, particles, tileMap, waterRipples, currentWeather, timeProgress, activeLightningStrikes);

            // Ataque da Raposa contra a fauna passiva
            if (player.isAttacking() && !player.hasProcessedAttackHit()) {
                double attackRange = 40.0;
                double attackX = player.getX() + Math.cos(player.getVisualAngle()) * 18.0;
                double attackY = player.getY() + Math.sin(player.getVisualAngle()) * 18.0;
                double distToAnimal = Math.hypot(animal.getX() - attackX, animal.getY() - attackY);

                if (distToAnimal <= attackRange) {
                    if (animal.isBurrowed()) {
                        player.markAttackHitProcessed();
                        animal.flushOutFromBurrow(attackX, attackY, particles);
                        triggerForestDisturbance(animal.getX(), animal.getY(), 160.0);
                        br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.BITE_WHOOSH, 0.8, 0, 1.25);
                        continue;
                    }
                    if (animal.isCatchable()) {
                        player.markAttackHitProcessed();
                        boolean dead = animal.takeDamage(1);
                        triggerForestDisturbance(animal.getX(), animal.getY(), 190.0);
                        br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.BITE_WHOOSH, 0.8, 0, 1.15);
                        for (int k = 0; k < 5; k++) {
                            particles.add(new Particle(animal.getX(), animal.getY(),
                                Math.cos(player.getVisualAngle()) * 50 + (rand.nextDouble() * 40 - 20),
                                Math.sin(player.getVisualAngle()) * 50 + (rand.nextDouble() * 40 - 20),
                                0.25, "#D7CCC8", 2.8));
                        }
                        if (player.isSwimming()) {
                            waterRipples.add(new WaterRipple(animal.getX(), animal.getY(), 22.0, 0.90));
                            for (int s = 0; s < 4; s++) {
                                particles.add(new Particle(animal.getX(), animal.getY(),
                                    (rand.nextDouble() - 0.5) * 60.0, -18.0 - rand.nextDouble() * 26.0,
                                    0.35, "#81D4FA", 2.6));
                            }
                        }
                        if (dead) {
                            meatItems.addAll(animal.createMorselDrops());
                            score += 50;
                            GameEventBus.get().post(new GameEvent(GameEvent.Type.PASSIVE_HUNTED));
                            if (animal.getType() == br.edu.unex.sentinela.entity.PassiveAnimalType.BIRD) {
                                GameEventBus.get().post(new GameEvent(GameEvent.Type.BIRD_CAUGHT));
                            }
                            recordBestiaryDefeatForPassive(animal);
                        }
                    }
                }
            }

            if (animal.isDespawned()) {
                if (animal.getHp() <= 0) {
                    meatItems.addAll(animal.createMorselDrops());
                }
                passIt.remove();
            }
        }

        // 4. Streaming e atualização de vagalumes noturnos
        streamFireflies(deltaTime);
        fireflies.removeIf(Firefly::isEaten);
        for (Firefly f : fireflies) {
            f.update(deltaTime);
        }

        // Update Enemies & Combat
        Iterator<Enemy> enemyIt = enemies.iterator();
        while (enemyIt.hasNext()) {
            Enemy enemy = enemyIt.next();
            if (enemy.getHp() <= 0) {
                meatItems.add(createMeatDrop(enemy));
                enemyIt.remove();
                continue;
            }
            enemy.update(deltaTime, player, foodItems, meatItems, passiveAnimals, bearTraps, scentTrail, projectiles, enemies, tileMap, waterRipples, timeProgress, currentWeather, windSystem.getWindVx(), windSystem.getWindVy(), windSystem.getIntensity(), activeLightningStrikes);

            double distToPlayer = Math.hypot(enemy.getX() - player.getX(), enemy.getY() - player.getY());
            if (distToPlayer < 460.0) {
                GameEventBus.get().post(new GameEvent(GameEvent.Type.CREATURE_SPOTTED, enemy.getSkin(), 1, ""));
            }

            if (player.isAttacking() && !player.hasProcessedAttackHit()) {
                double attackRange = 42.0;
                double attackX = player.getX() + Math.cos(player.getVisualAngle()) * 18.0;
                double attackY = player.getY() + Math.sin(player.getVisualAngle()) * 18.0;
                double distToAttack = Math.hypot(enemy.getX() - attackX, enemy.getY() - attackY);

                if (distToAttack <= attackRange) {
                    player.markAttackHitProcessed();
                    boolean defeated = enemy.takeDamage(1, player.getX(), player.getY());
                    triggerForestDisturbance(enemy.getX(), enemy.getY(), 190.0);

                    // Caçador atacado alerta cães sabujos próximos
                    if (enemy.getType() == EnemyType.HUNTER) {
                        for (Enemy ally : enemies) {
                            if (ally == enemy || ally.getHp() <= 0) continue;
                            if (ally.getType() == EnemyType.HOUND) {
                                double distHound = Math.hypot(ally.getX() - enemy.getX(), ally.getY() - enemy.getY());
                                if (distHound < 440.0) {
                                    ally.receiveHoundAlert(player.getX(), player.getY(), tileMap);
                                }
                            }
                        }
                    }

                    for (int k = 0; k < 6; k++) {
                        particles.add(new Particle(
                            enemy.getX(), enemy.getY(),
                            Math.cos(player.getVisualAngle()) * 80 + (rand.nextDouble() * 50 - 25),
                            Math.sin(player.getVisualAngle()) * 80 + (rand.nextDouble() * 50 - 25),
                            0.30, "#FFD700", 3.5
                        ));
                    }

                    if (player.isSwimming()) {
                        waterRipples.add(new WaterRipple(enemy.getX(), enemy.getY(), 24.0, 0.92));
                        for (int s = 0; s < 5; s++) {
                            particles.add(new Particle(enemy.getX(), enemy.getY(),
                                (rand.nextDouble() - 0.5) * 70.0, -20.0 - rand.nextDouble() * 30.0,
                                0.35, "#81D4FA", 2.8));
                        }
                    }

                    if (defeated) {
                        enemiesDefeated++;
                        score += enemy.getType().getDefeatScore();
                        meatItems.add(createMeatDrop(enemy));
                        if (enemy.getType() == EnemyType.BEAR_BOSS) {
                            GameEventBus.get().post(new GameEvent(GameEvent.Type.BOSS_DEFEATED, enemy.getSkin(), 1, "Chefe Derrotado"));
                        } else {
                            GameEventBus.get().post(new GameEvent(GameEvent.Type.ENEMY_DEFEATED, enemy.getSkin(), 1, "Inimigo Derrotado"));
                        }
                        recordBestiaryDefeatForEnemy(enemy);
                        enemyIt.remove();
                        continue;
                    }
                }
            }

            // Boss Combat: ataques primário e especial com hitbox própria, partículas e tremor de câmera
            if (enemy.getType() == EnemyType.BEAR_BOSS) {
                String bossDeathReason = BossCombatExecutor.processBossCombat(enemy, player, particles, camera, tileMap, distToPlayer);
                if (bossDeathReason != null) {
                    triggerGameOver(bossDeathReason);
                    return;
                }
                continue;
            }

            // Preparação de ataque e bote do inimigo contra o Jogador (em distância corpo a corpo natural)
            double strikePrepDist = (enemy.getType() == EnemyType.BEAR_BOSS || enemy.getSkin() == EnemySkin.BOAR) ? 48.0 : 42.0;
            if (distToPlayer <= strikePrepDist && enemy.getHp() > 0 && enemy.getType() != EnemyType.HUNTER) {
                if (enemy.getAttackCooldown() <= 0 && !enemy.isWindup() && !enemy.isLunging()) {
                    enemy.startAttackWindup(player.getX(), player.getY());
                }
            }

            // Conexão física do golpe (durante o bote, investida de javali ou contato imediato próximo)
            boolean isContactAttack = (distToPlayer <= 25.0 && enemy.getAttackCooldown() <= 0);
            boolean isLungeOrCharge = enemy.isLunging() || enemy.isCharging();
            if ((isLungeOrCharge || isContactAttack) && distToPlayer <= 42.0 && enemy.getHp() > 0 && enemy.getType() != EnemyType.HUNTER) {
                double impulse = enemy.isCharging() ? 350.0 : 260.0;
                if (player.takeDamage(enemy.getX(), enemy.getY(), impulse)) {
                    enemy.triggerAttackCooldownAfterHit();
                    // Impact sparks customized by enemy type
                    String particleColor = "#E74C3C";
                    int particleCount = 7;
                    if (enemy.getType() == EnemyType.BEAR_BOSS) {
                        particleColor = enemy.getSkin() == EnemySkin.CROCODILE ? "#00E676"
                                      : (enemy.getSkin() == EnemySkin.PANTHER ? "#FFD700"
                                      : (enemy.getSkin() == EnemySkin.POLAR_BEAR ? "#38BDF8" : "#E67E22"));
                        particleCount = 12;
                    } else if (enemy.getType() == EnemyType.HOUND) {
                        particleColor = "#9B59B6";
                    } else if (enemy.getType() == EnemyType.RIVAL) {
                        particleColor = "#E67E22";
                    }

                    for (int k = 0; k < particleCount; k++) {
                        particles.add(new Particle(
                            player.getX(), player.getY(),
                            (rand.nextDouble() * 120 - 60),
                            (rand.nextDouble() * 120 - 60),
                            0.35, particleColor, 4.5
                        ));
                    }

                    if (player.getLives() <= 0) {
                        if (enemy.getType() == EnemyType.BEAR_BOSS) triggerGameOver(enemy.getSkin().getDisplayName() + " o subjugou com um golpe fatal!");
                        else if (enemy.getType() == EnemyType.HUNTER) triggerGameOver("O Caçador o encurralou e capturou!");
                        else if (enemy.getType() == EnemyType.HOUND) triggerGameOver("O Cão de Caça o interceptou com uma mordida precisa!");
                        else triggerGameOver(enemy.getSkin().getDisplayName() + " o atacou brutalmente!");
                        return;
                    }
                }
            }
        }

        camera.updateShake(deltaTime);
        camera.update(player.getX(), player.getY());

        if (rainIntensity > 0.001) {
            double camDeltaX = camera.getDeltaX();
            double camDeltaY = camera.getDeltaY();
            double visW = camera.getVisibleWidth();
            double visH = camera.getVisibleHeight();
            double windVx = windSystem.getWindVx();
            double windVy = windSystem.getWindVy();
            for (WeatherDrop drop : rainDrops) {
                drop.update(deltaTime, visW, visH, camDeltaX, camDeltaY, windVx, windVy);
            }

            // Geração orgânica de poças/marolas de chuva no solo
            if (rainIntensity > 0.25 && rand.nextDouble() < 0.22 * rainIntensity) {
                double rx = camera.getX() + rand.nextDouble() * visW;
                double ry = camera.getY() + rand.nextDouble() * visH;
                int c = (int) Math.floor(rx / TileMap.TILE_SIZE);
                int r = (int) Math.floor(ry / TileMap.TILE_SIZE);
                TileType t = tileMap.getTileAt(c, r);
                if (t == TileType.WATER || t == TileType.MUD || t == TileType.DIRT_PATH) {
                    waterRipples.add(new WaterRipple(rx, ry, 7.0 + rand.nextDouble() * 8.0, 0.45));
                }
            }
        }
    }

    private void triggerGameOver(String reason) {
        this.gameOver = true;
        this.gameOverReason = reason;
        highScoreManager.checkAndSave(score, survivalTime);
        highScoreManager.flushNow();
        if (survivalTime >= 3.0) {
            MatchRecord record = new MatchRecord(survivalTime, score, applesEaten, enemiesDefeated, dayCycleNumber, reason);
            MatchHistoryRepository.get().recordMatch(record);
        }
        GameEventBus.get().post(new GameEvent(GameEvent.Type.GAME_OVER));
    }

    private void cleanupDistantEntities() {
        // Recycle distant static items
        foodItems.removeIf(f -> Math.hypot(f.getX() - player.getX(), f.getY() - player.getY()) > 1800.0 || f.isEaten());
        meatItems.removeIf(m -> Math.hypot(m.getX() - player.getX(), m.getY() - player.getY()) > 1800.0 || m.isEaten());
        burrows.removeIf(b -> Math.hypot(b.getX() - player.getX(), b.getY() - player.getY()) > 2200.0);

        // Despawn enemies that got left too far behind (preserva bosses próximos à sua arena territorial)
        enemies.removeIf(e -> {
            if (camera != null && camera.isPointInView(e.getX(), e.getY(), 70.0)) {
                return false; // Nunca despawna visivelmente na tela
            }
            double distEntity = Math.hypot(e.getX() - player.getX(), e.getY() - player.getY());
            if (e.getType() == EnemyType.BEAR_BOSS && e.getBossController() != null) {
                double distHome = Math.hypot(e.getBossController().getHomeX() - player.getX(), e.getBossController().getHomeY() - player.getY());
                return distEntity > 1950.0 && distHome > 1950.0;
            }
            return distEntity > 1700.0;
        });

        int pCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
        int pRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);

        int safetyAttempts = 0;
        while (enemies.size() < 12 && safetyAttempts < 8) {
            ecosystemSpawner.spawnReinforcementNearPlayer(tileMap, enemies, player, survivalTime, camera);
            safetyAttempts++;
        }

        if (burrows.size() < 7) {
            ecosystemSpawner.spawnBurrowInSector(tileMap, burrows, pCol, pRow, rand.nextDouble() * Math.PI * 2.0, 12, 32, camera);
        }
    }

    private MeatItem createMeatDrop(Enemy enemy) {
        return new MeatItem(
                enemy.getX(),
                enemy.getY(),
                MeatTier.fromEnemy(enemy.getType()),
                enemy.getSkin()
        );
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
    public double getRainIntensity() { return rainIntensity; }
    public double getFogIntensity() { return fogIntensity; }
    public double getLastDeltaTime() { return lastDeltaTime; }
    public List<WeatherDrop> getRainDrops() { return rainDrops; }
    public HighScoreManager getHighScoreManager() { return highScoreManager; }
    public double getSurvivalTime() { return survivalTime; }
    public int getScore() { return score; }
    public int getApplesEaten() { return applesEaten; }
    public int getMeatsEaten() { return meatsEaten; }
    public int getEnemiesDefeated() { return enemiesDefeated; }
    public int getDayCycleNumber() { return dayCycleNumber; }
    public double getCycleProgressTimer() { return cycleProgressTimer; }
    public double getCycleDuration() { return cycleDuration; }
    public boolean isGameOver() { return gameOver; }
    public String getGameOverReason() { return gameOverReason; }

    public boolean isNight() {
        return getNightFactor() > 0.45;
    }

    public double getNightFactor() {
        double cycleProgress = cycleProgressTimer / cycleDuration;
        return Math.sin(cycleProgress * Math.PI);
    }

    public boolean isDusk() {
        double f = getNightFactor();
        return f >= 0.20 && f <= 0.55;
    }

    public List<PassiveAnimal> getPassiveAnimals() { return passiveAnimals; }
    public List<Firefly> getFireflies() { return fireflies; }
    public List<BearTrap> getBearTraps() { return bearTraps; }

    public WindSystem getWindSystem() { return windSystem; }
    public double getWindVx() { return windSystem.getWindVx(); }
    public double getWindVy() { return windSystem.getWindVy(); }
    public double getWindIntensity() { return windSystem.getIntensity(); }
    public double getWindAngle() { return windSystem.getAngle(); }
    public List<LightningStrike> getActiveLightningStrikes() { return activeLightningStrikes; }
    public double getStormIntensity() { return stormIntensity; }
    public double getLightningFlash() { return lightningFlash; }
    public void triggerLightningFlash(double val) { this.lightningFlash = Math.max(this.lightningFlash, val); }

    private void spawnStormLightning(double camX, double camY, double vpW, double vpH) {
        double strikeX;
        double strikeY;

        if (rand.nextDouble() < 0.80) {
            List<double[]> potentialTargets = new ArrayList<>();
            if (player != null) {
                potentialTargets.add(new double[]{player.getX(), player.getY()});
            }
            for (Enemy e : enemies) {
                if (e.getHp() > 0 && camera != null && camera.isPointInView(e.getX(), e.getY(), 60.0)) {
                    potentialTargets.add(new double[]{e.getX(), e.getY()});
                }
            }
            for (PassiveAnimal a : passiveAnimals) {
                if (!a.isDead() && camera != null && camera.isPointInView(a.getX(), a.getY(), 60.0)) {
                    potentialTargets.add(new double[]{a.getX(), a.getY()});
                }
            }

            if (!potentialTargets.isEmpty()) {
                double[] target = potentialTargets.get(rand.nextInt(potentialTargets.size()));
                double angle = rand.nextDouble() * 2.0 * Math.PI;
                double offsetDist = 26.0 + rand.nextDouble() * 32.0;
                strikeX = target[0] + Math.cos(angle) * offsetDist;
                strikeY = target[1] + Math.sin(angle) * offsetDist;
            } else {
                strikeX = camX + 40.0 + rand.nextDouble() * (vpW - 80.0);
                strikeY = camY + 40.0 + rand.nextDouble() * (vpH - 80.0);
            }
        } else {
            strikeX = camX + 30.0 + rand.nextDouble() * (vpW - 60.0);
            strikeY = camY + 30.0 + rand.nextDouble() * (vpH - 60.0);
        }

        activeLightningStrikes.add(new LightningStrike(strikeX, strikeY));
    }
    public List<WaterRipple> getWaterRipples() { return waterRipples; }

    /**
     * Efeito em cadeia de perturbação na floresta:
     * Quando ocorre combate estrondoso, pássaros voam espantados e presas entram em alerta.
     */
    public void triggerForestDisturbance(double x, double y, double radius) {
        for (PassiveAnimal animal : passiveAnimals) {
            if (animal.isDespawned()) continue;
            double dist = Math.hypot(animal.getX() - x, animal.getY() - y);
            if (dist <= radius) {
                animal.triggerFlightBurst(x, y);
            }
        }
    }

    private void spawnPassiveAnimalNearPlayer() {
        int playerCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
        int playerRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);
        double margin = 80.0;

        for (int attempt = 0; attempt < 50; attempt++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double minOffscreen = (camera != null) ? camera.getMinOffscreenDistance(angle, margin) : 580.0;
            double distTiles = Math.ceil(minOffscreen / TileMap.TILE_SIZE) + 1.0 + rand.nextDouble() * 14.0;
            int col = playerCol + (int) Math.round(Math.cos(angle) * distTiles);
            int row = playerRow + (int) Math.round(Math.sin(angle) * distTiles);

            double wx = col * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2.0;
            double wy = row * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2.0;

            if (camera != null && camera.isPointInView(wx, wy, 60.0)) {
                continue; // Garante 100% que a fauna passiva NUNCA spawna na tela!
            }

            TileType tile = tileMap.getTileAt(col, row);
            BiomeType biome = tileMap.getBiomeAt(col, row);

            PassiveAnimalType type;
            if (tile == TileType.WATER || biome == BiomeType.SWAMP) {
                type = rand.nextDouble() < 0.65 ? PassiveAnimalType.FROG : PassiveAnimalType.BIRD;
            } else if (biome == BiomeType.DENSE_FOREST) {
                type = rand.nextDouble() < 0.60 ? PassiveAnimalType.DEER : (rand.nextBoolean() ? PassiveAnimalType.RABBIT : PassiveAnimalType.BIRD);
            } else if (biome == BiomeType.MEADOW) {
                type = rand.nextDouble() < 0.60 ? PassiveAnimalType.RABBIT : PassiveAnimalType.BIRD;
            } else {
                type = rand.nextBoolean() ? PassiveAnimalType.RABBIT : PassiveAnimalType.DEER;
            }

            // Validação estrita de terreno para nunca criar animais presos em árvores/rochedos ou aves submersas
            if (type == PassiveAnimalType.FROG) {
                if (!tile.isWalkable() && tile != TileType.WATER) continue;
            } else {
                if (!tile.isWalkable()) continue;
            }

            if (tile != TileType.WATER) {
                if (!tileMap.isWalkable(wx - 8.0, wy - 8.0) || !tileMap.isWalkable(wx + 8.0, wy + 8.0)
                        || !tileMap.isWalkable(wx - 8.0, wy + 8.0) || !tileMap.isWalkable(wx + 8.0, wy - 8.0)) {
                    continue;
                }
            }

            PassiveAnimal pa = new PassiveAnimal(type, wx, wy);
            if (type == PassiveAnimalType.RABBIT && !burrows.isEmpty() && rand.nextDouble() < 0.40) {
                Burrow closeBurrow = null;
                double closestDist = 160.0;
                for (Burrow b : burrows) {
                    double db = Math.hypot(b.getX() - wx, b.getY() - wy);
                    if (db < closestDist) {
                        closestDist = db;
                        closeBurrow = b;
                    }
                }
                if (closeBurrow != null) {
                    pa.enterBurrow(closeBurrow, null, 10.0 + rand.nextDouble() * 12.0);
                }
            }
            passiveAnimals.add(pa);
            break;
        }
    }

    private void streamFireflies(double dt) {
        // Vagalumes só surgem e permanecem estritamente de noite e sem chuva pesada
        if (!isNight() || rainIntensity > 0.20) {
            if (!fireflies.isEmpty()) {
                fireflies.clear();
            }
            return;
        }

        double refX = (player != null) ? player.getX() : (camera != null ? camera.getX() + camera.getViewportWidth() * 0.5 : 0.0);
        double refY = (player != null) ? player.getY() : (camera != null ? camera.getY() + camera.getViewportHeight() * 0.5 : 0.0);

        fireflies.removeIf(f -> Math.hypot(f.getX() - refX, f.getY() - refY) > 560.0);

        fireflyStreamTimer += dt;
        if (fireflyStreamTimer >= 0.35 && fireflies.size() < 34) {
            fireflyStreamTimer = 0.0;
            double ang = rand.nextDouble() * Math.PI * 2.0;
            double r = 60.0 + rand.nextDouble() * 320.0;
            double fx = refX + Math.cos(ang) * r;
            double fy = refY + Math.sin(ang) * r;
            int col = (int) Math.floor(fx / TileMap.TILE_SIZE);
            int row = (int) Math.floor(fy / TileMap.TILE_SIZE);
            BiomeType biome = tileMap.getBiomeAt(col, row);
            if (biome == BiomeType.DENSE_FOREST || biome == BiomeType.SWAMP || biome == BiomeType.MEADOW) {
                fireflies.add(new Firefly(fx, fy, biome));
            }
        }
    }

    private void recordBestiaryDefeatForEnemy(Enemy enemy) {
        if (enemy == null) return;
        String id = null;
        if (enemy.getType() == EnemyType.HUNTER || enemy.getSkin() == EnemySkin.HUNTER_SNIPER) {
            id = "hunter";
        } else if (enemy.getType() == EnemyType.HOUND || enemy.getSkin() == EnemySkin.HOUND_TRACKER) {
            id = "hound";
        } else if (enemy.getSkin() == EnemySkin.WOLF) {
            id = "wolf";
        } else if (enemy.getSkin() == EnemySkin.BOAR) {
            id = "boar";
        } else if (enemy.getSkin() == EnemySkin.BADGER) {
            id = "badger";
        } else if (enemy.getSkin() == EnemySkin.BEAR) {
            id = "bear";
        } else if (enemy.getSkin() == EnemySkin.PANTHER) {
            id = "panther";
        } else if (enemy.getSkin() == EnemySkin.CROCODILE) {
            id = "crocodile";
        } else if (enemy.getSkin() == EnemySkin.POLAR_BEAR) {
            id = "polar_bear";
        }
        if (id != null) {
            BestiaryUnlockManager.get().recordKill(id);
        }
    }

    private void recordBestiaryDefeatForPassive(PassiveAnimal animal) {
        if (animal == null) return;
        String id = null;
        if (animal.getType() == PassiveAnimalType.RABBIT) {
            id = "rabbit";
        } else if (animal.getType() == PassiveAnimalType.DEER) {
            id = "deer";
        } else if (animal.getType() == PassiveAnimalType.FROG) {
            id = "frog";
        } else if (animal.getType() == PassiveAnimalType.BIRD) {
            id = "bird";
        }
        if (id != null) {
            BestiaryUnlockManager.get().recordKill(id);
        }
    }
}
