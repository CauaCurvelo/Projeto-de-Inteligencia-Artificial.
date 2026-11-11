package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.ai.AIState;
import br.edu.unex.sentinela.ai.Node;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.World;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.List;

/**
 * Responsável pela renderização moderna de percepção tática e furtividade (Stealth Tactical UI):
 * - Cones de visão com atenuação RadialGradient suave e resposta dinâmica ao estado da IA
 * - Trilha de rastreamento fluida (substituindo os antigos quadrados 32x32) com pulsos direcionais animados
 * - Ondas de sonar acústico e aura de camuflagem da raposa integradas ao solo
 */
public class TacticalAuraRenderer {
    private final GraphicsContext gc;

    public TacticalAuraRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    public void renderTacticalAuras(World world, double camX, double camY, double visibleW, double visibleH, double animTime, boolean showTacticalOverlay) {
        if (!showTacticalOverlay) return;

        boolean showTrails = br.edu.unex.sentinela.settings.GameSettings.get().isShowPathTrails();
        boolean showCones = br.edu.unex.sentinela.settings.GameSettings.get().isShowVisionCones();

        int ts = TileMap.TILE_SIZE;

        // 1. Renderiza Trilhas de Perseguição e Cones de Visão dos Inimigos no solo
        for (Enemy enemy : world.getEnemies()) {
            double ex = enemy.getX() - camX;
            double ey = enemy.getY() - camY;

            // Culling amplo para não cortar cones longos
            double maxRange = enemy.getVisionRange() + 80;
            if (ex < -maxRange || ex > visibleW + maxRange || ey < -maxRange || ey > visibleH + maxRange) {
                continue;
            }

            Color stateColor = resolveTacticalStateColor(enemy.getState());

            // A. Linha de Rastreamento / Rota até o Personagem (Fluida e Moderna, sem quadrados de grade)
            if (showTrails) {
                renderSmoothTrackingTrail(enemy, ex, ey, camX, camY, ts, stateColor, animTime);
            }

            // B. Cone de Visão com Gradiente Radial Suave (Stealth FOV Cone)
            if (showCones) {
                renderModernVisionCone(enemy, ex, ey, stateColor, animTime);
            }
        }

        // 2. Renderiza Ondas Acústicas de Ruído da Raposa (Sonar Ripple)
        Player player = world.getPlayer();
        double px = player.getX() - camX;
        double py = player.getY() - camY;
        renderPlayerNoiseSonar(player, px, py, animTime);
    }

    /**
     * Desenha uma trilha orgânica pontilhada/animada ligando o inimigo ao seu destino,
     * com chevrons direcionais fluindo ao longo do caminho e um pequeno alvo no ponto final.
     */
    private void renderSmoothTrackingTrail(Enemy enemy, double ex, double ey, double camX, double camY, int ts, Color stateColor, double animTime) {
        List<Node> path = enemy.getCurrentPath();
        int startIdx = enemy.getCurrentPathIndex();
        if (path == null || startIdx >= path.size()) return;

        gc.save();
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        int remaining = path.size() - startIdx;
        double[] ptsX = new double[remaining + 1];
        double[] ptsY = new double[remaining + 1];

        ptsX[0] = ex;
        ptsY[0] = ey + 4; // Ancorado nos pés do inimigo

        for (int i = 0; i < remaining; i++) {
            Node node = path.get(startIdx + i);
            ptsX[i + 1] = node.col * ts + (ts * 0.5) - camX;
            ptsY[i + 1] = node.row * ts + (ts * 0.5) - camY;
        }

        // 1. Sombra/Brilho externo suave da trilha no chão (Glow de base)
        gc.setStroke(stateColor.deriveColor(0, 1.0, 0.8, 0.16));
        gc.setLineWidth(5.0);
        gc.strokePolyline(ptsX, ptsY, ptsX.length);

        // 2. Linha tática tracejada fina com animação de fluxo (Marching ants suaves)
        gc.setStroke(stateColor.deriveColor(0, 1.0, 1.15, 0.52));
        gc.setLineWidth(1.8);
        gc.setLineDashes(6.0, 7.0);
        gc.setLineDashOffset(-(animTime * 28.0));
        gc.strokePolyline(ptsX, ptsY, ptsX.length);
        gc.setLineDashes(null);

        // 3. Pequenos Chevrons (Setas direcionais '›') fluindo nos segmentos da rota
        gc.setStroke(stateColor.deriveColor(0, 0.9, 1.25, 0.72));
        gc.setLineWidth(1.6);
        for (int i = 0; i < remaining; i++) {
            double segDx = ptsX[i + 1] - ptsX[i];
            double segDy = ptsY[i + 1] - ptsY[i];
            double segLen = Math.hypot(segDx, segDy);
            if (segLen < 8) continue;

            double segAngle = Math.atan2(segDy, segDx);
            double flowPhase = (animTime * 2.2 + i * 0.35) % 1.0;
            double cx = ptsX[i] + segDx * flowPhase;
            double cy = ptsY[i] + segDy * flowPhase;

            double wing1 = segAngle + Math.PI * 0.78;
            double wing2 = segAngle - Math.PI * 0.78;
            gc.strokeLine(cx, cy, cx + Math.cos(wing1) * 4.5, cy + Math.sin(wing1) * 4.5);
            gc.strokeLine(cx, cy, cx + Math.cos(wing2) * 4.5, cy + Math.sin(wing2) * 4.5);
        }

        // 4. Marcador Tático no Ponto de Destino Final (Target Waypoint Ring)
        double endX = ptsX[remaining];
        double endY = ptsY[remaining];
        double pulse = 1.0 + Math.sin(animTime * 5.0) * 0.18;
        double r = 5.5 * pulse;

        gc.setFill(stateColor.deriveColor(0, 1.0, 1.1, 0.25));
        gc.fillOval(endX - r, endY - r, r * 2, r * 2);
        gc.setStroke(stateColor.deriveColor(0, 1.0, 1.2, 0.75));
        gc.setLineWidth(1.4);
        gc.strokeOval(endX - r, endY - r, r * 2, r * 2);

        gc.restore();
    }

    /**
     * Renderiza o Cone de Visão moderno com RadialGradient (desaparecendo suavemente na borda)
     * e linhas guia laterais sutis.
     */
    private void renderModernVisionCone(Enemy enemy, double ex, double ey, Color stateColor, double animTime) {
        double range = enemy.getVisionRange();
        double fovRad = enemy.getFovAngle();
        double fovDeg = Math.toDegrees(fovRad);
        double faceAngle = enemy.getFaceAngle();
        double startAngleDeg = -Math.toDegrees(faceAngle) - (fovDeg / 2.0);

        boolean isAlert = enemy.getState() == AIState.CHASE ||
                          enemy.getState() == AIState.AIMING ||
                          enemy.getState() == AIState.FIGHTING_ENEMY;
        boolean isSuspicious = enemy.getState() == AIState.SUSPICIOUS;

        double baseAlpha = isAlert ? 0.26 : (isSuspicious ? 0.20 : 0.13);
        if (isAlert) {
            baseAlpha += Math.sin(animTime * 8.0) * 0.04;
        }

        // 1. Preenchimento do Cone com Gradiente Radial (Forte na origem -> Transparente na borda externa)
        RadialGradient coneGradient = new RadialGradient(
            0, 0, ex, ey, range,
            false, CycleMethod.NO_CYCLE,
            new Stop(0.0,  stateColor.deriveColor(0, 1.0, 1.15, baseAlpha * 1.25)),
            new Stop(0.55, stateColor.deriveColor(0, 1.0, 1.0,  baseAlpha * 0.65)),
            new Stop(0.88, stateColor.deriveColor(0, 1.0, 0.9,  baseAlpha * 0.20)),
            new Stop(1.0,  Color.TRANSPARENT)
        );

        gc.setFill(coneGradient);
        gc.fillArc(ex - range, ey - range, range * 2, range * 2, startAngleDeg, fovDeg, ArcType.ROUND);

        // 2. Arco Frontal de Alcance (Apenas a borda curva externa tracejada sutil, sem fechar como fatia de pizza)
        gc.save();
        gc.setStroke(stateColor.deriveColor(0, 1.0, 1.2, isAlert ? 0.50 : 0.24));
        gc.setLineWidth(isAlert ? 1.6 : 1.1);
        gc.setLineDashes(8.0, 6.0);
        gc.strokeArc(ex - range * 0.96, ey - range * 0.96, range * 1.92, range * 1.92, startAngleDeg + 2, fovDeg - 4, ArcType.OPEN);
        gc.setLineDashes(null);

        // 3. Linhas Laterais de Limite Periférico (Curto alcance que some na metade do cone)
        double halfFov = fovRad * 0.5;
        double edgeLen = range * 0.72;
        RadialGradient edgeFade = new RadialGradient(
            0, 0, ex, ey, edgeLen,
            false, CycleMethod.NO_CYCLE,
            new Stop(0.0, stateColor.deriveColor(0, 1.0, 1.2, isAlert ? 0.55 : 0.30)),
            new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setStroke(edgeFade);
        gc.setLineWidth(1.3);
        gc.strokeLine(ex, ey, ex + Math.cos(faceAngle - halfFov) * edgeLen, ey + Math.sin(faceAngle - halfFov) * edgeLen);
        gc.strokeLine(ex, ey, ex + Math.cos(faceAngle + halfFov) * edgeLen, ey + Math.sin(faceAngle + halfFov) * edgeLen);

        gc.restore();
    }

    /**
     * Renderiza ondas concêntricas estilo Sonar Acústico quando a raposa emite ruído ao correr.
     */
    private void renderPlayerNoiseSonar(Player player, double px, double py, double animTime) {
        double radius = player.getNoiseRadius();
        if (radius <= 2.0) return;

        gc.save();
        // Anel limite externo tracejado suave
        gc.setStroke(Color.web("#E0F7FA", 0.22));
        gc.setLineWidth(1.2);
        gc.setLineDashes(5.0, 5.0);
        gc.strokeOval(px - radius, py - radius, radius * 2, radius * 2);
        gc.setLineDashes(null);

        // Duas ondas de som expansivas (Ripple Sonar)
        for (int i = 0; i < 2; i++) {
            double phase = (animTime * 1.4 + i * 0.5) % 1.0;
            double rWave = radius * phase;
            double alpha = (1.0 - phase) * 0.35;
            gc.setStroke(Color.web("#80DEEA", alpha));
            gc.setLineWidth(1.8 * (1.0 - phase * 0.5));
            gc.strokeOval(px - rWave, py - rWave, rWave * 2, rWave * 2);
        }
        gc.restore();
    }

    /**
     * Define a paleta tática moderna de acordo com o estado comportamental da IA.
     */
    public static Color resolveTacticalStateColor(AIState state) {
        if (state == null) return Color.web("#CFD8DC");
        switch (state) {
            case CHASE:
            case AIMING:
            case FIGHTING_ENEMY:
                return Color.web("#FF2A55"); // Vermelho Carmesim Tático (Perigo / Combate)
            case SUSPICIOUS:
                return Color.web("#FFB300"); // Âmbar Dourado (Alerta / Investigando)
            case PATROL:
            default:
                return Color.web("#FFF8E1"); // Luz Creme Suave (Calmo / Patrulha)
        }
    }
}
