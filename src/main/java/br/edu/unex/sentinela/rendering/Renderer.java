package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.ai.Node;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.input.InputManager;
import br.edu.unex.sentinela.world.*;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;

public class Renderer {
    private final GraphicsContext gc;
    private double width;
    private double height;
    
    // Typography
    private final Font miniFont = Font.font("Consolas", FontWeight.BOLD, 12);
    private final Font titleFont = Font.font("Arial", FontWeight.BOLD, 30);
    private final Font tagFont = Font.font("Arial", FontWeight.BOLD, 10);
    private final Font symbolFont = Font.font("Arial", FontWeight.BOLD, 16);
    private final Font scoreFont = Font.font("Consolas", FontWeight.BOLD, 14);

    public Renderer(GraphicsContext gc, double width, double height) {
        this.gc = gc;
        this.width = width;
        this.height = height;
    }

    public void updateViewport(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void render(World world, InputManager inputManager) {
        Camera cam = world.getCamera();
        double camX = cam.getX();
        double camY = cam.getY();

        // 1. Background Fill
        gc.setFill(Color.web("#141E10"));
        gc.fillRect(0, 0, width, height);

        // 2. Render Visible Geographic Tiles
        TileMap tileMap = world.getTileMap();
        int ts = TileMap.TILE_SIZE;

        int startCol = (int) Math.floor(camX / ts);
        int endCol = (int) Math.ceil((camX + width) / ts);
        int startRow = (int) Math.floor(camY / ts);
        int endRow = (int) Math.ceil((camY + height) / ts);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                TileType type = tileMap.getTileAt(col, row);
                double sx = col * ts - camX;
                double sy = row * ts - camY;

                renderGeographicTile(type, sx, sy, ts, col, row, tileMap);
            }
        }

        // 3. Render Burrows (Fox Dens)
        for (Burrow b : world.getBurrows()) {
            double bx = b.getX() - camX;
            double by = b.getY() - camY;
            if (bx >= -20 && bx <= width + 20 && by >= -20 && by <= height + 20) {
                gc.setFill(Color.web("#6B4D30"));
                gc.fillOval(bx - 14, by - 14, 28, 28);
                gc.setFill(Color.web("#1A1108"));
                gc.fillOval(bx - 9, by - 9, 18, 18);
            }
        }

        // 4. Render Footprints / Scent Trail
        for (Footprint fp : world.getScentTrail()) {
            double fx = fp.getX() - camX;
            double fy = fp.getY() - camY;
            if (fx >= -10 && fx <= width + 10 && fy >= -10 && fy <= height + 10) {
                double alpha = fp.getAlpha() * 0.45;
                gc.setFill(Color.web("#3A2B1D", alpha));
                gc.fillOval(fx - 3, fy - 2, 4, 3);
                gc.fillOval(fx + 2, fy + 2, 4, 3);
                
                if (inputManager.isDebugOverlay()) {
                    gc.setFill(Color.web("#8E24AA", fp.getAlpha() * 0.35));
                    gc.fillOval(fx - 4, fy - 4, 8, 8);
                }
            }
        }

        // 5. Render Particles
        for (Particle p : world.getParticles()) {
            double px = p.getX() - camX;
            double py = p.getY() - camY;
            if (px >= -10 && px <= width + 10 && py >= -10 && py <= height + 10) {
                gc.setFill(Color.web(p.getColorHex(), p.getAlpha()));
                gc.fillOval(px - p.getSize()/2, py - p.getSize()/2, p.getSize(), p.getSize());
            }
        }

        // 6. Render Food Items (Berries)
        for (FoodItem food : world.getFoodItems()) {
            if (!food.isEaten()) {
                double fx = food.getX() - camX;
                double fy = food.getY() - camY;
                if (fx >= -20 && fx <= width + 20 && fy >= -20 && fy <= height + 20) {
                    drawBerryBush(fx, fy);
                }
            }
        }

        // 7. Render Dropped Meat
        for (MeatItem meat : world.getMeatItems()) {
            if (!meat.isEaten()) {
                double mx = meat.getX() - camX;
                double my = meat.getY() - camY;
                if (mx >= -20 && mx <= width + 20 && my >= -20 && my <= height + 20) {
                    drawMeatDrop(mx, my);
                }
            }
        }

        // 8. Debug Overlay
        if (inputManager.isDebugOverlay()) {
            renderDebugOverlay(world, camX, camY);
        }

        // 9. Render Projectiles
        for (Projectile proj : world.getProjectiles()) {
            if (proj.isActive()) {
                double prx = proj.getX() - camX;
                double pry = proj.getY() - camY;
                gc.setFill(Color.web("#FFD700"));
                gc.fillOval(prx - 4, pry - 4, 8, 8);
                gc.setFill(Color.web("#FF4444"));
                gc.fillOval(prx - 2, pry - 2, 4, 4);
            }
        }

        // 10. Render Enemies (with Zelda HP bars above head)
        for (Enemy enemy : world.getEnemies()) {
            double ex = enemy.getX() - camX;
            double ey = enemy.getY() - camY;

            if (ex < -50 || ex > width + 50 || ey < -50 || ey > height + 50) continue;

            renderEnemy(enemy, ex, ey);
        }

        // 11. Render Player (Fox) & Claw Attack Slash
        Player player = world.getPlayer();
        double px = player.getX() - camX;
        double py = player.getY() - camY;

        if (inputManager.isDebugOverlay() && player.getNoiseRadius() > 0) {
            gc.setStroke(Color.web("#FFFFFF", 0.35));
            gc.setLineWidth(1.8);
            gc.strokeOval(px - player.getNoiseRadius(), py - player.getNoiseRadius(), player.getNoiseRadius() * 2, player.getNoiseRadius() * 2);
        }

        drawPlayerFox(player, px, py);

        // Render Zelda-Style Claw Slash Animation
        if (player.isAttacking()) {
            drawClawSlash(player, px, py);
        }

        // 12. Weather Effects
        renderWeatherVisuals(world);

        // 13. Dynamic Dusk/Night Lighting Filter
        renderDayNightLighting(world);

        // 14. Infinite Arcade HUD
        renderInfiniteHUD(world, inputManager);
    }

    private void drawMeatDrop(double mx, double my) {
        // Juicy roast meat piece with bone
        gc.setFill(Color.web("#8E1600"));
        gc.fillOval(mx - 8, my - 6, 16, 12);
        gc.setFill(Color.web("#D9381E"));
        gc.fillOval(mx - 6, my - 5, 12, 10);
        // Bone ends
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(mx - 10, my - 3, 4, 6);
        gc.fillOval(mx + 6, my - 3, 4, 6);
    }

    private void drawClawSlash(Player player, double px, double py) {
        double angle = Math.atan2(player.getFaceDy(), player.getFaceDx());
        double slashX = px + Math.cos(angle) * 18;
        double slashY = py + Math.sin(angle) * 18;

        // Glowing white-gold swipe arc
        gc.setStroke(Color.web("#FFF275", 0.90));
        gc.setLineWidth(3.5);
        gc.strokeArc(slashX - 16, slashY - 16, 32, 32, -Math.toDegrees(angle) - 50, 100, ArcType.OPEN);

        gc.setStroke(Color.web("#FFFFFF"));
        gc.setLineWidth(2.0);
        gc.strokeArc(slashX - 14, slashY - 14, 28, 28, -Math.toDegrees(angle) - 40, 80, ArcType.OPEN);
    }

    private void renderWeatherVisuals(World world) {
        WeatherType weather = world.getCurrentWeather();

        if (weather == WeatherType.RAIN) {
            gc.setStroke(Color.web("#A0C4E8", 0.55));
            gc.setLineWidth(1.2);
            for (WeatherDrop drop : world.getRainDrops()) {
                gc.strokeLine(drop.getX(), drop.getY(), drop.getX() - 4, drop.getY() + drop.getLength());
            }

            gc.setFill(Color.color(0.08, 0.15, 0.28, 0.15));
            gc.fillRect(0, 0, width, height);

        } else if (weather == WeatherType.FOG) {
            gc.setFill(Color.color(0.85, 0.90, 0.95, 0.38));
            gc.fillRect(0, 0, width, height);
            
            gc.setFill(Color.color(0.70, 0.75, 0.82, 0.22));
            gc.fillOval(-width*0.2, -height*0.2, width * 1.4, height * 1.4);
        }
    }

    private void renderGeographicTile(TileType type, double sx, double sy, int ts, int col, int row, TileMap tileMap) {
        switch (type) {
            case GRASS:
                gc.setFill(Color.web("#448033"));
                gc.fillRect(sx, sy, ts, ts);

                // Subtle organic texture shading (no more weird white dots)
                int hash = (col * 31 + row * 17) & 0x7FFFFFFF;
                if (hash % 11 == 0) {
                    gc.setFill(Color.web("#3E742E"));
                    gc.fillRect(sx + 8, sy + 14, 2, 4);
                    gc.fillRect(sx + 12, sy + 12, 2, 6);
                } else if (hash % 17 == 0) {
                    gc.setFill(Color.web("#4B8B38"));
                    gc.fillRect(sx + 18, sy + 8, 3, 3);
                }
                break;

            case DIRT_PATH:
                gc.setFill(Color.web("#8A6844"));
                gc.fillRect(sx, sy, ts, ts);
                gc.setFill(Color.web("#755737"));
                gc.fillOval(sx + 4, sy + 8, ts - 8, ts - 16);
                if ((col + row) % 3 == 0) {
                    gc.setFill(Color.web("#A8947D"));
                    gc.fillOval(sx + 12, sy + 10, 3, 2);
                    gc.fillOval(sx + 20, sy + 18, 2, 2);
                }
                break;

            case TALL_GRASS:
                gc.setFill(Color.web("#316124"));
                gc.fillRect(sx, sy, ts, ts);
                gc.setFill(Color.web("#244E19"));
                gc.fillRect(sx + 4, sy + 4, 3, 22);
                gc.fillRect(sx + 12, sy + 2, 4, 26);
                gc.fillRect(sx + 21, sy + 6, 3, 20);
                gc.setFill(Color.web("#427A33"));
                gc.fillRect(sx + 7, sy + 8, 2, 16);
                break;

            case MUD:
                gc.setFill(Color.web("#543924"));
                gc.fillRect(sx, sy, ts, ts);
                gc.setFill(Color.web("#402B1B"));
                gc.fillOval(sx + 3, sy + 5, ts - 6, ts - 10);
                gc.fillRect(sx + 14, sy + 8, 2, 10);
                break;

            case WATER:
                gc.setFill(Color.web("#26659E"));
                gc.fillRect(sx, sy, ts, ts);
                gc.setFill(Color.web("#387EB8"));
                gc.fillRect(sx + 4, sy + 8, 12, 2);
                gc.fillRect(sx + 16, sy + 20, 10, 2);
                if (tileMap.getTileAt(col, row - 1) != TileType.WATER ||
                    tileMap.getTileAt(col, row + 1) != TileType.WATER) {
                    gc.setFill(Color.web("#90C8EB", 0.40));
                    gc.fillRect(sx, sy + 1, ts, 2);
                }
                break;

            case TREE:
                // Rich detailed canopy with natural trunk and drop shadow
                gc.setFill(Color.web("#386B2A"));
                gc.fillRect(sx, sy, ts, ts);

                // Trunk ground shadow
                gc.setFill(Color.web("#142B10", 0.65));
                gc.fillOval(sx + 2, sy + 16, ts - 4, 14);

                // Wood trunk
                gc.setFill(Color.web("#4A2E16"));
                gc.fillRect(sx + 12, sy + 12, 8, 14);

                // Layered vibrant leafy canopy
                gc.setFill(Color.web("#1B4519"));
                gc.fillOval(sx - 2, sy - 4, ts + 4, ts + 2);
                gc.setFill(Color.web("#276324"));
                gc.fillOval(sx + 1, sy - 2, ts - 2, ts - 4);
                gc.setFill(Color.web("#378233"));
                gc.fillOval(sx + 4, sy, ts - 8, ts - 10);
                gc.setFill(Color.web("#4CA147", 0.85));
                gc.fillOval(sx + 7, sy + 2, ts - 14, ts - 16);
                break;

            case WALL:
                gc.setFill(Color.web("#474C52"));
                gc.fillRect(sx, sy, ts, ts);

                gc.setFill(Color.web("#2B2E33"));
                gc.fillRect(sx, sy + ts - 4, ts, 4);

                gc.setStroke(Color.web("#34383D"));
                gc.setLineWidth(1.5);
                gc.strokeLine(sx + 2, sy + 16, sx + 16, sy + 16);
                gc.strokeLine(sx + 16, sy + 4, sx + 16, sy + 28);
                gc.strokeLine(sx + 16, sy + 20, sx + 30, sy + 20);

                gc.setStroke(Color.web("#6C737D"));
                gc.strokeLine(sx + 1, sy + 2, sx + ts - 1, sy + 2);
                break;
        }

        gc.setStroke(Color.web("#000000", 0.04));
        gc.setLineWidth(1);
        gc.strokeRect(sx, sy, ts, ts);
    }

    private void drawBerryBush(double fx, double fy) {
        // Shrub shadow
        gc.setFill(Color.web("#142B10", 0.50));
        gc.fillOval(fx - 14, fy - 6, 28, 16);

        // Lush green wild shrub foliage
        gc.setFill(Color.web("#1A4D1A"));
        gc.fillOval(fx - 14, fy - 14, 28, 26);
        gc.setFill(Color.web("#266B26"));
        gc.fillOval(fx - 11, fy - 12, 22, 22);
        gc.setFill(Color.web("#388E3C"));
        gc.fillOval(fx - 7, fy - 10, 15, 15);

        // Bright red ripe berries
        gc.setFill(Color.web("#D32F2F"));
        gc.fillOval(fx - 7, fy - 5, 6, 6);
        gc.fillOval(fx + 2, fy - 4, 6, 6);
        gc.fillOval(fx - 2, fy + 2, 6, 6);

        // Glossy light highlights on berries
        gc.setFill(Color.web("#FFFFFF", 0.85));
        gc.fillOval(fx - 6, fy - 4, 2, 2);
        gc.fillOval(fx + 3, fy - 3, 2, 2);
        gc.fillOval(fx - 1, fy + 3, 2, 2);
    }

    private void drawPlayerFox(Player player, double px, double py) {
        if (player.isBurrowed()) {
            gc.setFill(Color.web("#F39C12", 0.60));
            gc.fillOval(px - 10, py - 10, 20, 20);
            return;
        }

        double angle = Math.atan2(player.getFaceDy(), player.getFaceDx());

        if (player.isInvulnerable()) {
            if ((System.currentTimeMillis() / 120) % 2 == 0) {
                gc.setFill(Color.web("#E74C3C", 0.40));
                gc.fillOval(px - 18, py - 18, 36, 36);
            }
        }

        if (player.isSwimming()) {
            gc.setFill(Color.web("#3399FF", 0.50));
            gc.fillOval(px - 17, py - 17, 34, 34);
            gc.setStroke(Color.web("#FFFFFF", 0.70));
            gc.setLineWidth(1.5);
            gc.strokeOval(px - 17, py - 17, 34, 34);
        } else if (player.isHiddenInGrass()) {
            gc.setFill(Color.web("#2ECC71", 0.35));
            gc.fillOval(px - 16, py - 16, 32, 32);
        }

        double alpha = player.isHiddenInGrass() ? 0.65 : 1.0;

        double tailAngle = angle + Math.PI;
        double tx = px + Math.cos(tailAngle) * 11;
        double ty = py + Math.sin(tailAngle) * 11;
        gc.setFill(Color.web("#D35400", alpha));
        gc.fillOval(tx - 6, ty - 6, 12, 12);
        gc.setFill(Color.web("#FFFFFF", alpha));
        gc.fillOval(tx + Math.cos(tailAngle)*3 - 3, ty + Math.sin(tailAngle)*3 - 3, 6, 6);

        gc.setFill(Color.web("#E67E22", alpha));
        gc.fillOval(px - 11, py - 11, 22, 22);

        double earDist = 9.0;
        double e1x = px + Math.cos(angle + 0.9) * earDist;
        double e1y = py + Math.sin(angle + 0.9) * earDist;
        double e2x = px + Math.cos(angle - 0.9) * earDist;
        double e2y = py + Math.sin(angle - 0.9) * earDist;

        gc.setFill(Color.web("#BA4A00", alpha));
        gc.fillOval(e1x - 4, e1y - 4, 8, 8);
        gc.fillOval(e2x - 4, e2y - 4, 8, 8);
        gc.setFill(Color.web("#FFFFFF", alpha));
        gc.fillOval(e1x - 2, e1y - 2, 4, 4);
        gc.fillOval(e2x - 2, e2y - 2, 4, 4);

        double snoutX = px + Math.cos(angle) * 8;
        double snoutY = py + Math.sin(angle) * 8;
        gc.setFill(Color.web("#2C3E50", alpha));
        gc.fillOval(snoutX - 2.5, snoutY - 2.5, 5, 5);

        double eye1X = px + Math.cos(angle + 0.45) * 6;
        double eye1Y = py + Math.sin(angle + 0.45) * 6;
        double eye2X = px + Math.cos(angle - 0.45) * 6;
        double eye2Y = py + Math.sin(angle - 0.45) * 6;
        gc.setFill(Color.web("#FFFFFF", alpha));
        gc.fillOval(eye1X - 2, eye1Y - 2, 4, 4);
        gc.fillOval(eye2X - 2, eye2Y - 2, 4, 4);
        gc.setFill(Color.web("#000000", alpha));
        gc.fillOval(eye1X - 1, eye1Y - 1, 2, 2);
        gc.fillOval(eye2X - 1, eye2Y - 1, 2, 2);
    }

    private void renderEnemy(Enemy enemy, double ex, double ey) {
        double angle = enemy.getFaceAngle();

        // Flash red/white when struck
        if (enemy.isFlashing()) {
            gc.setFill(Color.web("#FFFFFF", 0.90));
            gc.fillOval(ex - 18, ey - 18, 36, 36);
        }

        switch (enemy.getType()) {
            case BEAR_BOSS:
                renderBossBySkin(enemy.getSkin(), ex, ey, angle);
                break;

            case HUNTER:
                gc.setFill(Color.web("#802020"));
                gc.fillOval(ex - 13, ey - 13, 26, 26);
                gc.setFill(Color.web("#C0392B"));
                gc.fillOval(ex - 10, ey - 10, 20, 20);

                double bowX = ex + Math.cos(angle) * 12;
                double bowY = ey + Math.sin(angle) * 12;
                gc.setStroke(Color.web("#F1C40F"));
                gc.setLineWidth(2.5);
                gc.strokeArc(bowX - 8, bowY - 8, 16, 16, -Math.toDegrees(angle) - 60, 120, ArcType.OPEN);

                gc.setFill(Color.web("#FFFFFF"));
                gc.fillOval(bowX - 2, bowY - 2, 4, 4);
                break;

            case HOUND:
                gc.setFill(Color.web("#4A235A"));
                gc.fillOval(ex - 12, ey - 12, 24, 24);
                gc.setFill(Color.web("#6C3483"));
                gc.fillOval(ex - 9, ey - 9, 18, 18);

                double ear1x = ex + Math.cos(angle + 1.2) * 10;
                double ear1y = ey + Math.sin(angle + 1.2) * 10;
                double ear2x = ex + Math.cos(angle - 1.2) * 10;
                double ear2y = ey + Math.sin(angle - 1.2) * 10;
                gc.setFill(Color.web("#2E1236"));
                gc.fillOval(ear1x - 3.5, ear1y - 3.5, 7, 7);
                gc.fillOval(ear2x - 3.5, ear2y - 3.5, 7, 7);

                double muzzleX = ex + Math.cos(angle) * 9;
                double muzzleY = ey + Math.sin(angle) * 9;
                gc.setFill(Color.web("#17202A"));
                gc.fillOval(muzzleX - 3, muzzleY - 3, 6, 6);

                gc.setStroke(Color.web("#E74C3C"));
                gc.setLineWidth(2.0);
                gc.strokeOval(ex - 6, ey - 6, 12, 12);
                break;

            case RIVAL:
                renderRivalBySkin(enemy.getSkin(), ex, ey, angle);
                break;
        }

        // Suspicion / Alert Icon
        if (enemy.getState() == br.edu.unex.sentinela.ai.AIState.SUSPICIOUS) {
            gc.setFont(symbolFont);
            gc.setFill(Color.web("#F1C40F"));
            gc.fillText("❓", ex - 6, ey - 32);
        } else if (enemy.getState() == br.edu.unex.sentinela.ai.AIState.CHASE || 
                   enemy.getState() == br.edu.unex.sentinela.ai.AIState.AIMING ||
                   enemy.getState() == br.edu.unex.sentinela.ai.AIState.FIGHTING_ENEMY) {
            gc.setFont(symbolFont);
            gc.setFill(Color.web("#E74C3C"));
            gc.fillText("❗", ex - 4, ey - 32);
        }

        // Zelda HP Bar above enemy
        double hpBarW = (enemy.getType() == EnemyType.BEAR_BOSS) ? 46.0 : 28.0;
        double hpPct = (double) enemy.getHp() / enemy.getMaxHp();
        gc.setFill(Color.web("#222222"));
        gc.fillRect(ex - hpBarW/2, ey - 22, hpBarW, 4);
        gc.setFill(enemy.getType() == EnemyType.BEAR_BOSS ? Color.web("#9B59B6") : Color.web("#E74C3C"));
        gc.fillRect(ex - hpBarW/2, ey - 22, hpBarW * hpPct, 4);

        // Head State & Skin Tag
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
        gc.setFont(tagFont);
        gc.setFill(Color.web("#000000", 0.75));
        String skinName = enemy.getSkin() != null ? enemy.getSkin().getDisplayName() : enemy.getType().name();
        String tag = "[" + skinName + " | " + enemy.getState().getDescription() + "]";
        double tagWidth = Math.max(90, tag.length() * 5.8);
        gc.fillRect(ex - tagWidth / 2.0, ey - 36, tagWidth, 14);
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillText(tag, ex - tagWidth / 2.0 + 3, ey - 25);
    }

    private void renderBossBySkin(EnemySkin skin, double ex, double ey, double angle) {
        if (skin == EnemySkin.CROCODILE) {
            // Crocodilo do Rio: Long green body, scaly snout, serrated spine
            gc.setFill(Color.web("#143D19"));
            gc.fillOval(ex - 22, ey - 14, 44, 28);
            gc.setFill(Color.web("#2E7D32"));
            gc.fillOval(ex - 18, ey - 11, 36, 22);

            // Elongated snout with teeth dots
            double snoutX = ex + Math.cos(angle) * 18;
            double snoutY = ey + Math.sin(angle) * 18;
            gc.setFill(Color.web("#1B5E20"));
            gc.fillOval(snoutX - 7, snoutY - 7, 14, 14);
            gc.setFill(Color.web("#FFFFFF"));
            gc.fillOval(snoutX + Math.cos(angle)*4 - 1.5, snoutY + Math.sin(angle)*4 - 1.5, 3, 3);

            // Serrated back scales
            double tailAngle = angle + Math.PI;
            double tailX = ex + Math.cos(tailAngle) * 18;
            double tailY = ey + Math.sin(tailAngle) * 18;
            gc.setFill(Color.web("#0E2E12"));
            gc.fillOval(tailX - 6, tailY - 6, 12, 12);
            return;
        } else if (skin == EnemySkin.PANTHER) {
            // Pantera Negra: Sleek dark obsidian body, sharp predatory ears, golden glowing eyes
            gc.setFill(Color.web("#171717"));
            gc.fillOval(ex - 18, ey - 18, 36, 36);
            gc.setFill(Color.web("#262626"));
            gc.fillOval(ex - 14, ey - 14, 28, 28);

            // Sleek Panther Ears
            double pEar1x = ex + Math.cos(angle + 1.0) * 14;
            double pEar1y = ey + Math.sin(angle + 1.0) * 14;
            double pEar2x = ex + Math.cos(angle - 1.0) * 14;
            double pEar2y = ey + Math.sin(angle - 1.0) * 14;
            gc.setFill(Color.web("#0A0A0A"));
            gc.fillOval(pEar1x - 4, pEar1y - 4, 8, 8);
            gc.fillOval(pEar2x - 4, pEar2y - 4, 8, 8);

            // Glowing yellow apex predator eyes
            double eye1x = ex + Math.cos(angle + 0.4) * 10;
            double eye1y = ey + Math.sin(angle + 0.4) * 10;
            double eye2x = ex + Math.cos(angle - 0.4) * 10;
            double eye2y = ey + Math.sin(angle - 0.4) * 10;
            gc.setFill(Color.web("#F1C40F"));
            gc.fillOval(eye1x - 2.5, eye1y - 2.5, 5, 5);
            gc.fillOval(eye2x - 2.5, eye2y - 2.5, 5, 5);
            return;
        }

        // Default: Urso Pardo Alfa (BEAR)
        gc.setFill(Color.web("#4A2E16"));
        gc.fillOval(ex - 20, ey - 20, 40, 40);
        gc.setFill(Color.web("#5D3A1A"));
        gc.fillOval(ex - 16, ey - 16, 32, 32);

        // Big Bear Ears
        double bearEar1x = ex + Math.cos(angle + 1.1) * 16;
        double bearEar1y = ey + Math.sin(angle + 1.1) * 16;
        double bearEar2x = ex + Math.cos(angle - 1.1) * 16;
        double bearEar2y = ey + Math.sin(angle - 1.1) * 16;
        gc.setFill(Color.web("#2B1707"));
        gc.fillOval(bearEar1x - 5, bearEar1y - 5, 10, 10);
        gc.fillOval(bearEar2x - 5, bearEar2y - 5, 10, 10);

        // Heavy Snout
        double bearSnoutX = ex + Math.cos(angle) * 14;
        double bearSnoutY = ey + Math.sin(angle) * 14;
        gc.setFill(Color.web("#140A03"));
        gc.fillOval(bearSnoutX - 5, bearSnoutY - 5, 10, 10);
    }

    private void renderRivalBySkin(EnemySkin skin, double ex, double ey, double angle) {
        if (skin == EnemySkin.WOLF) {
            // Lobo da Floresta: Steel blue-grey fur, sharp pointed ears, long muzzle
            gc.setFill(Color.web("#37474F"));
            gc.fillOval(ex - 13, ey - 13, 26, 26);
            gc.setFill(Color.web("#607D8B"));
            gc.fillOval(ex - 10, ey - 10, 20, 20);

            // Pointed Wolf Ears
            double wEar1x = ex + Math.cos(angle + 1.0) * 12;
            double wEar1y = ey + Math.sin(angle + 1.0) * 12;
            double wEar2x = ex + Math.cos(angle - 1.0) * 12;
            double wEar2y = ey + Math.sin(angle - 1.0) * 12;
            gc.setFill(Color.web("#263238"));
            gc.fillOval(wEar1x - 3.5, wEar1y - 3.5, 7, 7);
            gc.fillOval(wEar2x - 3.5, wEar2y - 3.5, 7, 7);

            // Snout
            double wSnoutX = ex + Math.cos(angle) * 10;
            double wSnoutY = ey + Math.sin(angle) * 10;
            gc.setFill(Color.web("#212121"));
            gc.fillOval(wSnoutX - 3, wSnoutY - 3, 6, 6);
            return;
        } else if (skin == EnemySkin.BADGER) {
            // Texugo Feroz: Charcoal body with iconic white facial stripes
            gc.setFill(Color.web("#263238"));
            gc.fillOval(ex - 13, ey - 13, 26, 26);
            gc.setFill(Color.web("#455A64"));
            gc.fillOval(ex - 10, ey - 10, 20, 20);

            // White face stripes
            double stripe1x = ex + Math.cos(angle + 0.5) * 8;
            double stripe1y = ey + Math.sin(angle + 0.5) * 8;
            double stripe2x = ex + Math.cos(angle - 0.5) * 8;
            double stripe2y = ey + Math.sin(angle - 0.5) * 8;
            gc.setFill(Color.web("#FFFFFF"));
            gc.fillOval(stripe1x - 2, stripe1y - 2, 4, 7);
            gc.fillOval(stripe2x - 2, stripe2y - 2, 4, 7);

            double bSnoutX = ex + Math.cos(angle) * 9;
            double bSnoutY = ey + Math.sin(angle) * 9;
            gc.setFill(Color.web("#102027"));
            gc.fillOval(bSnoutX - 3, bSnoutY - 3, 6, 6);
            return;
        }

        // Default: Javali Selvagem (BOAR) with tusks
        gc.setFill(Color.web("#5D4037"));
        gc.fillOval(ex - 14, ey - 14, 28, 28);
        gc.setFill(Color.web("#795548"));
        gc.fillOval(ex - 11, ey - 11, 22, 22);

        double snoutX = ex + Math.cos(angle) * 10;
        double snoutY = ey + Math.sin(angle) * 10;
        gc.setFill(Color.web("#3E2723"));
        gc.fillOval(snoutX - 4, snoutY - 4, 8, 8);

        double tusk1x = ex + Math.cos(angle + 0.6) * 11;
        double tusk1y = ey + Math.sin(angle + 0.6) * 11;
        double tusk2x = ex + Math.cos(angle - 0.6) * 11;
        double tusk2y = ey + Math.sin(angle - 0.6) * 11;
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillOval(tusk1x - 2, tusk1y - 2, 4, 4);
        gc.fillOval(tusk2x - 2, tusk2y - 2, 4, 4);
    }

    private void renderDebugOverlay(World world, double camX, double camY) {
        int ts = TileMap.TILE_SIZE;

        for (Enemy enemy : world.getEnemies()) {
            double ex = enemy.getX() - camX;
            double ey = enemy.getY() - camY;

            List<Node> path = enemy.getCurrentPath();
            if (path != null && !path.isEmpty()) {
                gc.setFill(Color.web(enemy.getType().getHexColor(), 0.35));
                for (Node n : path) {
                    double nx = n.col * ts - camX;
                    double ny = n.row * ts - camY;
                    if (nx >= -ts && nx <= width + ts && ny >= -ts && ny <= height + ts) {
                        gc.fillRect(nx, ny, ts, ts);
                    }
                }
            }

            double range = enemy.getVisionRange();
            double fovDeg = Math.toDegrees(enemy.getFovAngle());
            double startAngle = -Math.toDegrees(enemy.getFaceAngle()) - (fovDeg / 2.0);

            gc.setFill(Color.web(enemy.getType().getHexColor(), 0.12));
            gc.fillArc(ex - range, ey - range, range * 2, range * 2, startAngle, fovDeg, ArcType.ROUND);
            gc.setStroke(Color.web(enemy.getType().getHexColor(), 0.40));
            gc.strokeArc(ex - range, ey - range, range * 2, range * 2, startAngle, fovDeg, ArcType.ROUND);
        }
    }

    private void renderDayNightLighting(World world) {
        double cycleProgress = world.getCycleProgressTimer() / world.getCycleDuration();
        double nightFactor = Math.sin(cycleProgress * Math.PI);
        
        if (nightFactor > 0.10) {
            double alpha = Math.min(0.68, (nightFactor - 0.10) * 0.75);
            gc.setFill(Color.color(0.04, 0.06, 0.22, alpha));
            gc.fillRect(0, 0, width, height);
        }
    }



    private void renderInfiniteHUD(World world, InputManager inputManager) {
        Player player = world.getPlayer();

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);

        // Top-Left Status Panel
        gc.setFill(Color.web("#000000", 0.72));
        gc.fillRoundRect(16, 16, 260, 72, 8, 8);
        gc.setStroke(Color.web("#FFFFFF", 0.20));
        gc.strokeRoundRect(16, 16, 260, 72, 8, 8);

        // Hearts
        gc.setFont(miniFont);
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillText("VIDAS: ", 26, 32);

        for (int i = 0; i < player.getMaxLives(); i++) {
            if (i < player.getLives()) {
                gc.setFill(Color.web("#E74C3C"));
                gc.fillText("♥", 75 + i * 14, 32);
            } else {
                gc.setFill(Color.web("#555555"));
                gc.fillText("♡", 75 + i * 14, 32);
            }
        }

        gc.setFill(Color.web("#F1C40F"));
        gc.fillText("DIA " + world.getDayCycleNumber(), 180, 32);

        // Hunger Bar
        double hungerPct = player.getHunger() / player.getMaxHunger();
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillText("FOME", 26, 52);

        gc.setFill(Color.web("#222222"));
        gc.fillRoundRect(70, 42, 160, 12, 4, 4);
        Color hCol = hungerPct > 0.4 ? Color.web("#2ECC71") : (hungerPct > 0.2 ? Color.web("#F39C12") : Color.web("#E74C3C"));
        gc.setFill(hCol);
        gc.fillRoundRect(70, 42, 160 * hungerPct, 12, 4, 4);

        // Weather Badge & Status
        gc.setFill(Color.web("#85C1E9"));
        String weatherStr = "CLIMA: " + world.getCurrentWeather().getDisplayName();
        if (player.isBurrowed()) {
            gc.setFill(Color.web("#F39C12"));
            gc.fillText("[NA TOCA] | " + weatherStr, 26, 76);
        } else if (player.isSwimming()) {
            gc.setFill(Color.web("#3399FF"));
            gc.fillText("[NADANDO] | " + weatherStr, 26, 76);
        } else if (player.isHiddenInGrass()) {
            gc.setFill(Color.web("#2ECC71"));
            gc.fillText("[CAMUFLADO] | " + weatherStr, 26, 76);
        } else {
            gc.setFill(Color.web("#BDC3C7"));
            gc.fillText(weatherStr, 26, 76);
        }

        // Top-Center Score Banner
        gc.setFill(Color.web("#000000", 0.72));
        gc.fillRoundRect(width / 2 - 140, 16, 280, 44, 8, 8);
        gc.setStroke(Color.web("#F1C40F", 0.60));
        gc.strokeRoundRect(width / 2 - 140, 16, 280, 44, 8, 8);

        gc.setFont(scoreFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#F1C40F"));
        gc.fillText(String.format("PONTOS: %06d", world.getScore()), width / 2, 33);
        
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        gc.setFill(Color.web("#E67E22"));
        gc.fillText(String.format("RECORDE: %06d", world.getHighScoreManager().getBestScore()), width / 2, 50);

        // Top-Right Hints & Stats (with Combat Info)
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.web("#000000", 0.55));
        gc.fillRoundRect(width - 180, 16, 164, 60, 6, 6);
        gc.setFont(Font.font("Consolas", 10));
        gc.setFill(Color.web("#DDDDDD"));
        int mins = (int) world.getSurvivalTime() / 60;
        int secs = (int) world.getSurvivalTime() % 60;
        gc.fillText(String.format("TEMPO: %02d:%02d | ABATES: %d", mins, secs, world.getEnemiesDefeated()), width - 172, 30);
        gc.fillText("[ESPAÇO/CLIQUE]: Morder/Atacar", width - 172, 45);
        gc.fillText("[F11] Tela | [H] IA | [R]", width - 172, 60);

        // Game Over Overlay
        if (world.isGameOver()) {
            gc.setFill(Color.web("#000000", 0.88));
            gc.fillRect(0, 0, width, height);
            gc.setFont(titleFont);
            gc.setTextAlign(TextAlignment.CENTER);

            gc.setFill(Color.web("#E74C3C"));
            gc.fillText("FIM DE JOGO", width / 2, height / 2 - 50);

            gc.setFont(miniFont);
            gc.setFill(Color.web("#FFFFFF"));
            gc.fillText(world.getGameOverReason(), width / 2, height / 2 - 20);

            gc.setFont(scoreFont);
            gc.setFill(Color.web("#F1C40F"));
            gc.fillText("PONTUAÇÃO FINAL: " + world.getScore(), width / 2, height / 2 + 10);
            
            gc.setFont(miniFont);
            gc.setFill(Color.web("#E67E22"));
            gc.fillText("MELHOR RECORDE HISTÓRICO: " + world.getHighScoreManager().getBestScore(), width / 2, height / 2 + 32);

            gc.setFill(Color.web("#BDC3C7"));
            gc.fillText(String.format("Sobreviveu por %02d:%02d (Dia %d) | %d Frutas | %d Inimigos Derrotados", 
                        mins, secs, world.getDayCycleNumber(), world.getApplesEaten(), world.getEnemiesDefeated()), width / 2, height / 2 + 54);

            gc.setFill(Color.web("#2ECC71"));
            gc.fillText("Pressione [R] para jogar novamente", width / 2, height / 2 + 82);
        }
    }
}
