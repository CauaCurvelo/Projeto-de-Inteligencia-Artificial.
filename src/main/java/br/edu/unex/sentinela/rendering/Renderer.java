package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.world.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Orquestrador principal do pipeline de renderização em múltiplas camadas de profundidade (2.5D):
 * 1. Ground & Biome Transitions + Ambient Occlusion (TerrainRenderer)
 * 2. Directional Cast Shadows (FoliageAndCliffRenderer)
 * 3. Tactical Vision Cones, Smooth Tracking Trails & Sonar Auras (TacticalAuraRenderer)
 * 4. Ground Details: Burrows, Footprints, Particles, Items (EntityRenderer)
 * 5. 2.5D Granite Cliffs & Tree Trunks (FoliageAndCliffRenderer)
 * 6. Characters, Projectiles & Modern Nameplates (EntityRenderer)
 * 7. Overhead Volumetric Tree Canopies (FoliageAndCliffRenderer)
 * 8. Dynamic Sun/Night Lighting, Vignette & Weather (LightingAndWeatherRenderer)
 * 9. Arcade HUD (HudRenderer)
 */
public class Renderer {
    private final GraphicsContext gc;
    private double width;
    private double height;

    private final TerrainRenderer terrainRenderer;
    private final FoliageAndCliffRenderer foliageAndCliffRenderer;
    private final TacticalAuraRenderer tacticalAuraRenderer;
    private final EntityRenderer entityRenderer;
    private final LightingAndWeatherRenderer lightingAndWeatherRenderer;
    private final HudRenderer hudRenderer;

    public Renderer(GraphicsContext gc, double width, double height) {
        this.gc = gc;
        this.width = width;
        this.height = height;
        this.terrainRenderer = new TerrainRenderer(gc);
        this.foliageAndCliffRenderer = new FoliageAndCliffRenderer(gc);
        this.tacticalAuraRenderer = new TacticalAuraRenderer(gc);
        this.entityRenderer = new EntityRenderer(gc);
        this.lightingAndWeatherRenderer = new LightingAndWeatherRenderer(gc);
        this.hudRenderer = new HudRenderer(gc);
    }

    public void updateViewport(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void render(World world, InputManager inputManager) {
        Camera cam = world.getCamera();
        double zoom = cam.getZoom();
        double camX = cam.getX();
        double camY = cam.getY();
        double animTime = world.getSurvivalTime();

        double cycleProgress = world.getCycleProgressTimer() / world.getCycleDuration();
        double sunOffsetX = 6.0 + Math.cos(cycleProgress * Math.PI) * 3.5;
        double sunOffsetY = 5.5 + Math.sin(cycleProgress * Math.PI) * 2.5;

        double visibleW = width / zoom;
        double visibleH = height / zoom;

        // 0. Fundo base
        gc.setFill(Color.web("#101A0D"));
        gc.fillRect(0, 0, visibleW, visibleH);

        gc.save();
        gc.scale(zoom, zoom);

        TileMap tileMap = world.getTileMap();
        int ts = TileMap.TILE_SIZE;

        int startCol = (int) Math.floor(camX / ts) - 1;
        int endCol = (int) Math.ceil((camX + visibleW) / ts) + 1;
        int startRow = (int) Math.floor(camY / ts) - 1;
        int endRow = (int) Math.ceil((camY + visibleH) / ts) + 1;

        // PASSO 1: Terreno detalhado, micro-texturas e Oclusão de Ambiente (AO)
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                TileType type = tileMap.getTileAt(col, row);
                double sx = col * ts - camX;
                double sy = row * ts - camY;
                terrainRenderer.renderGroundTile(type, sx, sy, ts, col, row, tileMap, animTime);
            }
        }

        // PASSO 2: Sombras Projetadas Direcionais no solo (Árvores e Paredões)
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                TileType type = tileMap.getTileAt(col, row);
                if (type == TileType.TREE || type == TileType.WALL) {
                    double sx = col * ts - camX;
                    double sy = row * ts - camY;
                    foliageAndCliffRenderer.renderCastShadow(type, sx, sy, ts, col, row, sunOffsetX, sunOffsetY);
                }
            }
        }

        // PASSO 3: Auras de Visão Táticas, Rotas de Rastreamento Fluidas e Sonar Acústico (projetados sobre o solo)
        tacticalAuraRenderer.renderTacticalAuras(world, camX, camY, visibleW, visibleH, animTime, inputManager.isDebugOverlay());

        // PASSO 4: Tocas, Pegadas/Rastro de Cheiro, Partículas, Arbustos de Frutas e Carne
        for (Burrow b : world.getBurrows()) {
            double bx = b.getX() - camX;
            double by = b.getY() - camY;
            if (bx >= -30 && bx <= visibleW + 30 && by >= -30 && by <= visibleH + 30) {
                entityRenderer.renderBurrow(b, bx, by);
            }
        }

        for (Footprint fp : world.getScentTrail()) {
            double fx = fp.getX() - camX;
            double fy = fp.getY() - camY;
            if (fx >= -12 && fx <= visibleW + 12 && fy >= -12 && fy <= visibleH + 12) {
                entityRenderer.renderFootprint(fp, fx, fy, inputManager.isDebugOverlay());
            }
        }

        for (Particle p : world.getParticles()) {
            double px = p.getX() - camX;
            double py = p.getY() - camY;
            if (px >= -10 && px <= visibleW + 10 && py >= -10 && py <= visibleH + 10) {
                gc.setFill(Color.web(p.getColorHex(), p.getAlpha()));
                gc.fillOval(px - p.getSize() / 2, py - p.getSize() / 2, p.getSize(), p.getSize());
            }
        }

        for (FoodItem food : world.getFoodItems()) {
            if (!food.isEaten()) {
                double fx = food.getX() - camX;
                double fy = food.getY() - camY;
                if (fx >= -25 && fx <= visibleW + 25 && fy >= -25 && fy <= visibleH + 25) {
                    entityRenderer.drawBerryBush(fx, fy, sunOffsetX, sunOffsetY);
                }
            }
        }

        for (MeatItem meat : world.getMeatItems()) {
            if (!meat.isEaten()) {
                double mx = meat.getX() - camX;
                double my = meat.getY() - camY;
                if (mx >= -25 && mx <= visibleW + 25 && my >= -25 && my <= visibleH + 25) {
                    entityRenderer.drawMeatDrop(mx, my);
                }
            }
        }

        // PASSO 5: Paredões de Rocha 2.5D e Troncos de Árvore
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                TileType type = tileMap.getTileAt(col, row);
                double sx = col * ts - camX;
                double sy = row * ts - camY;
                if (type == TileType.WALL) {
                    foliageAndCliffRenderer.renderGraniteWall25D(sx, sy, ts, col, row, tileMap);
                } else if (type == TileType.TREE) {
                    foliageAndCliffRenderer.renderTreeTrunk(sx, sy, ts, col, row);
                }
            }
        }

        // PASSO 6: Projéteis, Inimigos (com Nameplates Modernos) e Jogador
        for (Projectile proj : world.getProjectiles()) {
            if (proj.isActive()) {
                double prx = proj.getX() - camX;
                double pry = proj.getY() - camY;
                gc.setFill(Color.color(0, 0, 0, 0.35));
                gc.fillOval(prx - 3, pry + 4, 6, 4);
                gc.setFill(Color.web("#FFD700"));
                gc.fillOval(prx - 4, pry - 4, 8, 8);
                gc.setFill(Color.web("#FF4444"));
                gc.fillOval(prx - 2, pry - 2, 4, 4);
            }
        }

        for (Enemy enemy : world.getEnemies()) {
            double ex = enemy.getX() - camX;
            double ey = enemy.getY() - camY;
            if (ex < -60 || ex > visibleW + 60 || ey < -60 || ey > visibleH + 60) continue;
            entityRenderer.renderEnemy(enemy, ex, ey);
        }

        Player player = world.getPlayer();
        double px = player.getX() - camX;
        double py = player.getY() - camY;

        entityRenderer.drawPlayerFox(player, px, py);

        if (player.isAttacking()) {
            entityRenderer.drawClawSlash(player, px, py);
        }

        // PASSO 7: Copas de Árvores Volumétricas 2.5D (Sobrepondo troncos e bordas de auras no solo)
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (tileMap.getTileAt(col, row) == TileType.TREE) {
                    double sx = col * ts - camX;
                    double sy = row * ts - camY;
                    foliageAndCliffRenderer.renderTreeCanopy(sx, sy, ts, col, row, animTime);
                }
            }
        }

        // PASSO 8: Efeitos Climáticos, Iluminação Solar/Noturna Dinâmica e Vinheta de Profundidade
        lightingAndWeatherRenderer.renderWeatherVisuals(world, visibleW, visibleH, animTime);
        lightingAndWeatherRenderer.renderDynamicLightingAndVignette(world, px, py, visibleW, visibleH);

        gc.restore();

        // PASSO 9: Interface Arcade Fixa (HUD)
        hudRenderer.renderInfiniteHUD(world, inputManager, width, height);
    }
}
