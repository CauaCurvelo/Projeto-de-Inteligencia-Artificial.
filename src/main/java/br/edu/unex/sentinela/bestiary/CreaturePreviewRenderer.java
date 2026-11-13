package br.edu.unex.sentinela.bestiary;

import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.rendering.CharacterSpriteRenderer;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Vitrine animada em Obsidian Spotlight sem bordas pesadas e com escala ampliada.
 */
public class CreaturePreviewRenderer extends Canvas {
    private CreatureInfo currentCreature;
    private final Player previewPlayer = new Player(0, 0, 3);
    private Enemy previewEnemy;
    private double animTime = 0.0;
    private AnimationTimer timer;

    public CreaturePreviewRenderer(double width, double height) {
        super(width, height);
        setupTimer();
    }

    public void setCreature(CreatureInfo info) {
        this.currentCreature = info;
        if (info != null && info.getSkin() != null && info.getType() != null) {
            this.previewEnemy = new Enemy(info.getType(), info.getSkin(), 0, 0, 1.0, 1.0);
            this.previewEnemy.setMoving(true);
        } else {
            this.previewEnemy = null;
            this.previewPlayer.setMoving(true);
        }
        renderFrame();
    }

    public void start() {
        if (timer != null) timer.start();
    }

    public void stop() {
        if (timer != null) timer.stop();
    }

    private void setupTimer() {
        timer = new AnimationTimer() {
            private long lastNs = 0;

            @Override
            public void handle(long now) {
                if (lastNs == 0) lastNs = now;
                double dt = Math.min(0.05, (now - lastNs) * 1e-9);
                lastNs = now;
                animTime += dt;
                renderFrame();
            }
        };
    }

    private void renderFrame() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        gc.clearRect(0, 0, w, h);

        boolean isBoss = currentCreature != null && currentCreature.getType() == EnemyType.BEAR_BOSS;
        Color ringAccent = isBoss ? Color.web("#F87171", 0.30) : Color.web("#E8B948", 0.24);

        RadialGradient bg = new RadialGradient(
                0, 0, w * 0.5, h * 0.48, w * 0.54, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#182520")),
                new Stop(0.65, Color.web("#0D1412")),
                new Stop(1.0, Color.web("#070B0A"))
        );
        gc.setFill(bg);
        gc.fillRoundRect(0, 0, w, h, 14, 14);

        gc.setStroke(Color.web("#FFFFFF", 0.08));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(0.5, 0.5, w - 1.0, h - 1.0, 14, 14);

        if (currentCreature == null) return;

        double cx = w * 0.5;
        double cy = h * 0.51;
        double angle = -Math.PI * 0.25 + animTime * 0.58;
        double spriteScale = isBoss ? 1.52 : 2.15;

        // Pedestal discreto iluminado
        gc.setFill(Color.color(0, 0, 0, 0.42));
        gc.fillOval(cx - 56, cy + 32, 112, 22);
        gc.setStroke(ringAccent);
        gc.setLineWidth(1.2);
        gc.strokeOval(cx - 56, cy + 32, 112, 22);

        CharacterSpriteRenderer spriteRenderer = new CharacterSpriteRenderer(gc);
        gc.save();
        gc.translate(cx, cy);
        gc.scale(spriteScale, spriteScale);
        if (previewEnemy != null) {
            previewEnemy.setFaceAngle(angle);
            previewEnemy.setWalkPhase(animTime * 7.2);
            previewEnemy.setBreathTimer(animTime);
            spriteRenderer.renderEnemySprite(previewEnemy, 0, 0);
        } else {
            previewPlayer.setVisualAngle(angle);
            previewPlayer.setWalkPhase(animTime * 8.2);
            previewPlayer.setBreathTimer(animTime);
            spriteRenderer.renderFox(previewPlayer, 0, 0);
        }
        gc.restore();
    }
}
