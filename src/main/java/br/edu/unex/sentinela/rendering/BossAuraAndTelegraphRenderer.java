package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.ai.AIState;
import br.edu.unex.sentinela.ai.BossArchetype;
import br.edu.unex.sentinela.ai.BossAttackType;
import br.edu.unex.sentinela.ai.BossBehaviorController;
import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.settings.GameSettings;
import br.edu.unex.sentinela.settings.GraphicsQuality;
import br.edu.unex.sentinela.world.World;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;

/**
 * Renderiza no terreno as Áreas de Domínio dos Chefes, o Limite Máximo de Perseguição (Leash),
 * as ondas sonoras de rugido e as telegrafias visuais específicas de cada golpe especial.
 */
public class BossAuraAndTelegraphRenderer {
    private final GraphicsContext gc;

    public BossAuraAndTelegraphRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    /** Desenha sempre no solo a fronteira do domínio e a zona de limite de perseguição dos chefes próximos. */
    public void renderBossTerritories(World world, double camX, double camY, double visibleW, double visibleH, double animTime) {
        Player player = world.getPlayer();
        GraphicsQuality quality = GameSettings.get().getGraphicsQuality();

        for (Enemy enemy : world.getEnemies()) {
            if (enemy.getType() != EnemyType.BEAR_BOSS || enemy.getBossController() == null) continue;
            BossBehaviorController ctrl = enemy.getBossController();
            BossArchetype arch = ctrl.getArchetype();

            double hx = ctrl.getHomeX() - camX;
            double hy = ctrl.getHomeY() - camY;
            double tr = arch.getTerritoryRadius();
            double lr = arch.getLeashRadius();

            if (hx < -lr - 60 || hx > visibleW + lr + 60 || hy < -lr - 60 || hy > visibleH + lr + 60) {
                continue;
            }

            double playerDistHome = Math.hypot(player.getX() - ctrl.getHomeX(), player.getY() - ctrl.getHomeY());
            boolean playerInside = playerDistHome <= tr;
            boolean activeFight = enemy.getState() == AIState.CHASE || enemy.getState() == AIState.ROARING || ctrl.isEnraged();
            Color domainColor = Color.web(arch.getTerritoryColorHex());

            gc.save();
            // Preenchimento sutil de borda do domínio em qualidade média/alta
            if (quality != GraphicsQuality.LOW) {
                double fillAlpha = activeFight ? 0.16 : (playerInside ? 0.13 : 0.08);
                RadialGradient aura = new RadialGradient(
                    0, 0, hx, hy, tr, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.TRANSPARENT),
                    new Stop(0.75, domainColor.deriveColor(0, 1.0, 1.0, fillAlpha * 0.45)),
                    new Stop(0.96, domainColor.deriveColor(0, 1.0, 1.15, fillAlpha)),
                    new Stop(1.0, Color.TRANSPARENT)
                );
                gc.setFill(aura);
                gc.fillOval(hx - tr, hy - tr, tr * 2, tr * 2);
            }

            // Anel delimitador do Território do Boss
            double pulse = 0.5 + 0.5 * Math.sin(animTime * 3.5);
            double ringAlpha = activeFight ? (0.48 + pulse * 0.18) : (playerInside ? 0.42 : 0.25);
            gc.setStroke(domainColor.deriveColor(0, 1.0, 1.2, ringAlpha));
            gc.setLineWidth(activeFight ? 2.2 : 1.5);
            gc.setLineDashes(12.0, 8.0);
            gc.setLineDashOffset(-animTime * 18.0);
            gc.strokeOval(hx - tr, hy - tr, tr * 2, tr * 2);

            // Marcadores cardeais orgânicos na borda do território
            gc.setLineDashes(null);
            gc.setFill(domainColor.deriveColor(0, 1.0, 1.25, ringAlpha + 0.15));
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 0.5 * i) + animTime * 0.12;
                double mx = hx + Math.cos(a) * tr;
                double my = hy + Math.sin(a) * tr;
                gc.fillOval(mx - 3.2, my - 3.2, 6.4, 6.4);
            }

            // Anel externo de Limite Máximo de Perseguição (visível quando em combate ou perto da borda)
            boolean showLeash = activeFight || ctrl.isRetreating() || ctrl.isNearLeashLimit(enemy.getX(), enemy.getY()) || playerDistHome >= tr * 0.85;
            if (showLeash) {
                boolean breaking = ctrl.isRetreating() || ctrl.isNearLeashLimit(enemy.getX(), enemy.getY());
                Color leashColor = breaking ? Color.web("#FF8A65") : Color.web("#FFB74D");
                double leashAlpha = breaking ? (0.45 + pulse * 0.22) : 0.24;
                gc.setStroke(leashColor.deriveColor(0, 1.0, 1.0, leashAlpha));
                gc.setLineWidth(breaking ? 1.8 : 1.2);
                gc.setLineDashes(6.0, 10.0);
                gc.strokeOval(hx - lr, hy - lr, lr * 2, lr * 2);
            }
            gc.restore();
        }
    }

    /** Renderiza o aviso de rugido e a telegrafia única no chão para o golpe preparado pelo Chefe. */
    public void renderBossCombatTelegraph(Enemy boss, double ex, double ey) {
        BossBehaviorController ctrl = boss.getBossController();
        if (ctrl == null) return;

        if (ctrl.isRoaring()) {
            renderRoarShockwaves(ex, ey, ctrl.getRoarProgress(), Color.web(ctrl.getArchetype().getAccentColorHex()));
            return;
        }

        if (!boss.isWindup() && !boss.isLunging()) return;

        BossAttackType atk = ctrl.getActiveAttack();
        double prog = boss.isWindup() ? ctrl.getWindupProgress() : 1.0;
        double aimAngle = Math.atan2(boss.getTargetAimY() - boss.getY(), boss.getTargetAimX() - boss.getX());
        double deg = -Math.toDegrees(aimAngle);

        gc.save();
        switch (atk) {
            case BEAR_SEISMIC_SLAM:
                renderSeismicSlamTelegraph(ex, ey, atk.getHitRange(), prog, boss.isLunging());
                break;
            case PANTHER_SHADOW_POUNCE:
                renderPounceLaneTelegraph(ex, ey, aimAngle, prog, boss.isLunging());
                break;
            case PANTHER_DOUBLE_SLASH:
                renderDoubleSlashTelegraph(ex, ey, deg, prog, boss.isLunging());
                break;
            case CROC_DEATH_ROLL:
                renderDeathRollVortex(ex, ey, atk.getHitRange(), prog, boss.isLunging(), ctrl.getActiveProgress());
                break;
            case CROC_AMBUSH_LUNGE:
                renderCrocAmbushCone(ex, ey, deg, prog);
                break;
            case BEAR_CLAW_SWIPE:
            default:
                renderClawSwipeArc(ex, ey, deg, atk.getHitRange(), prog);
                break;
        }
        gc.restore();
    }

    private void renderRoarShockwaves(double ex, double ey, double progress, Color accent) {
        gc.save();
        for (int i = 0; i < 2; i++) {
            double waveP = (progress + i * 0.38) % 1.0;
            double r = 22.0 + waveP * 54.0;
            gc.setStroke(accent.deriveColor(0, 1.0, 1.1, (1.0 - waveP) * 0.78));
            gc.setLineWidth(2.6 * (1.0 - waveP * 0.45));
            gc.strokeOval(ex - r, ey - r, r * 2, r * 2);
        }
        gc.restore();
    }

    private void renderSeismicSlamTelegraph(double ex, double ey, double radius, double prog, boolean lunging) {
        double innerR = Math.max(12.0, radius * prog);
        gc.setFill(Color.web("#FF6D00", lunging ? 0.34 : 0.14 + prog * 0.14));
        gc.fillOval(ex - innerR, ey - innerR, innerR * 2, innerR * 2);

        gc.setStroke(Color.web("#FFD54F", lunging ? 0.90 : 0.55 + prog * 0.35));
        gc.setLineWidth(lunging ? 3.2 : 2.0);
        gc.setLineDashes(lunging ? null : new double[]{8.0, 5.0});
        gc.strokeOval(ex - radius, ey - radius, radius * 2, radius * 2);
    }

    private void renderPounceLaneTelegraph(double ex, double ey, double angleRad, double prog, boolean lunging) {
        double len = 175.0;
        double endX = ex + Math.cos(angleRad) * len;
        double endY = ey + Math.sin(angleRad) * len;

        gc.setStroke(Color.web("#FFD600", lunging ? 0.75 : 0.32 + prog * 0.45));
        gc.setLineWidth(24.0);
        gc.strokeLine(ex, ey, endX, endY);

        gc.setStroke(Color.web("#FF3D00", 0.75));
        gc.setLineWidth(2.0);
        gc.setLineDashes(9.0, 6.0);
        gc.strokeLine(ex, ey, endX, endY);
    }

    private void renderDoubleSlashTelegraph(double ex, double ey, double deg, double prog, boolean lunging) {
        double r = 46.0;
        gc.setStroke(Color.web("#FFD700", lunging ? 0.95 : 0.55 + prog * 0.35));
        gc.setLineWidth(lunging ? 3.6 : 2.4);
        gc.strokeArc(ex - r, ey - r, r * 2, r * 2, deg - 48, 96, ArcType.OPEN);
        gc.setStroke(Color.web("#FFFFFF", 0.75));
        gc.setLineWidth(1.6);
        gc.strokeArc(ex - r * 0.78, ey - r * 0.78, r * 1.56, r * 1.56, deg - 36, 72, ArcType.OPEN);
    }

    private void renderDeathRollVortex(double ex, double ey, double radius, double prog, boolean lunging, double activeProg) {
        double spinStart = lunging ? activeProg * 540.0 : prog * 180.0;
        gc.setStroke(Color.web("#00E676", lunging ? 0.88 : 0.52 + prog * 0.30));
        gc.setLineWidth(lunging ? 3.0 : 2.0);
        gc.strokeArc(ex - radius, ey - radius, radius * 2, radius * 2, spinStart, 115, ArcType.OPEN);
        gc.strokeArc(ex - radius, ey - radius, radius * 2, radius * 2, spinStart + 180, 115, ArcType.OPEN);
    }

    private void renderCrocAmbushCone(double ex, double ey, double deg, double prog) {
        double r = 56.0;
        gc.setFill(Color.web("#00E676", 0.18 + prog * 0.20));
        gc.fillArc(ex - r, ey - r, r * 2, r * 2, deg - 32, 64, ArcType.ROUND);
        gc.setStroke(Color.web("#00E676", 0.75));
        gc.setLineWidth(2.0);
        gc.strokeArc(ex - r, ey - r, r * 2, r * 2, deg - 32, 64, ArcType.OPEN);
    }

    private void renderClawSwipeArc(double ex, double ey, double deg, double radius, double prog) {
        gc.setFill(Color.web("#FF3D00", 0.16 + prog * 0.22));
        gc.fillArc(ex - radius, ey - radius, radius * 2, radius * 2, deg - 58, 116, ArcType.ROUND);
        gc.setStroke(Color.web("#FF6D00", 0.80));
        gc.setLineWidth(2.5);
        gc.strokeArc(ex - radius, ey - radius, radius * 2, radius * 2, deg - 58, 116, ArcType.OPEN);
    }
}
