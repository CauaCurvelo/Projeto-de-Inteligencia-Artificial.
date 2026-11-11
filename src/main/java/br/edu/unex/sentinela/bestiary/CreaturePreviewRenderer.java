package br.edu.unex.sentinela.bestiary;

import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.rendering.CharacterSpriteRenderer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderizador de vitrine / manequim animado para o Bestiário e Enciclopédia.
 */
public class CreaturePreviewRenderer {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final CharacterSpriteRenderer spriteRenderer;

    private final Player dummyPlayer = new Player(0, 0, 3);
    private final Map<String, Enemy> dummyEnemies = new HashMap<>();

    public CreaturePreviewRenderer(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.spriteRenderer = new CharacterSpriteRenderer(gc);
    }

    public void render(CreatureInfo info, double timeSec) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cx = w / 2.0;
        double cy = h / 2.0;

        gc.clearRect(0, 0, w, h);

        // Fundo com pedestal de pedra e iluminação focal
        RadialGradient bg = new RadialGradient(
            0, 0, cx, cy, Math.max(cx, cy), false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.rgb(38, 48, 40, 0.95)),
            new Stop(0.65, Color.rgb(20, 26, 22, 0.98)),
            new Stop(1.0, Color.rgb(10, 14, 12, 1.0))
        );
        gc.setFill(bg);
        gc.fillRoundRect(4, 4, w - 8, h - 8, 16, 16);

        // Borda dourada antiga do expositor
        gc.setStroke(Color.rgb(212, 172, 13, 0.65));
        gc.setLineWidth(2.5);
        gc.strokeRoundRect(4, 4, w - 8, h - 8, 16, 16);

        // Pedestal circular sombreado no chão da vitrine
        RadialGradient pedestal = new RadialGradient(
            0, 0, cx, cy + 30, 60, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.rgb(60, 75, 65, 0.7)),
            new Stop(0.7, Color.rgb(30, 40, 32, 0.8)),
            new Stop(1.0, Color.rgb(10, 14, 12, 0.0))
        );
        gc.setFill(pedestal);
        gc.fillOval(cx - 55, cy + 10, 110, 40);

        if (info == null) return;

        // Animação cíclica suave (rotação de 360° em 12s + caminhada constante)
        double angleRad = (timeSec * 0.45) % (Math.PI * 2.0) - Math.PI;
        double walk = timeSec * 4.5;
        double breath = timeSec * 2.8;

        if (info.getSkin() == null) {
            // Render da Raposa (Player)
            dummyPlayer.setVisualAngle(angleRad);
            dummyPlayer.setWalkPhase(walk);
            dummyPlayer.setBreathTimer(breath);
            dummyPlayer.setMoving(true);
            spriteRenderer.renderFox(dummyPlayer, cx, cy);
        } else {
            // Render de Inimigo / Animal do Ecossistema
            Enemy enemy = dummyEnemies.computeIfAbsent(info.getId(), id ->
                new Enemy(info.getType(), info.getSkin(), 0, 0, 1.0, 1.0)
            );
            enemy.setFaceAngle(angleRad);
            enemy.setWalkPhase(walk);
            enemy.setBreathTimer(breath);
            enemy.setMoving(true);
            spriteRenderer.renderEnemySprite(enemy, cx, cy);
        }
    }
}
