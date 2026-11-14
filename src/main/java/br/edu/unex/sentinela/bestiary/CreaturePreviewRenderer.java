package br.edu.unex.sentinela.bestiary;

import br.edu.unex.sentinela.ai.EnemyType;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.PassiveAnimal;
import br.edu.unex.sentinela.entity.PassiveAnimalType;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.rendering.CharacterSpriteRenderer;
import br.edu.unex.sentinela.rendering.PassiveAnimalRenderer;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Vitrine animada em Obsidian Spotlight com suporte a Jogador, Inimigos/Chefes e Fauna Passiva,
 * com silhueta misteriosa para criaturas ainda não desbloqueadas por abate.
 */
public class CreaturePreviewRenderer extends Canvas {
    private CreatureInfo currentCreature;
    private final Player previewPlayer = new Player(0, 0, 3);
    private Enemy previewEnemy;
    private PassiveAnimal previewPassive;
    private double animTime = 0.0;
    private AnimationTimer timer;

    public CreaturePreviewRenderer(double width, double height) {
        super(width, height);
        setupTimer();
    }

    public void setCreature(CreatureInfo info) {
        this.currentCreature = info;
        this.previewEnemy = null;
        this.previewPassive = null;

        if (info != null) {
            if (info.isPassive()) {
                this.previewPassive = new PassiveAnimal(info.getPassiveType(), 0, 0);
                this.previewPassive.setMoving(true);
            } else if (info.getSkin() != null && info.getType() != null) {
                this.previewEnemy = new Enemy(info.getType(), info.getSkin(), 0, 0, 1.0, 1.0);
                this.previewEnemy.setMoving(true);
            } else {
                this.previewPlayer.setMoving(true);
            }
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
        boolean isPassive = currentCreature != null && currentCreature.isPassive();
        boolean isUnlocked = currentCreature != null && BestiaryUnlockManager.get().isUnlocked(currentCreature.getId());

        Color ringAccent;
        if (!isUnlocked) {
            ringAccent = Color.web("#64748B", 0.28);
        } else if (isBoss) {
            ringAccent = Color.web("#F87171", 0.35);
        } else if (isPassive) {
            ringAccent = Color.web("#38BDF8", 0.32);
        } else {
            ringAccent = Color.web("#E8B948", 0.26);
        }

        RadialGradient bg = new RadialGradient(
                0, 0, w * 0.5, h * 0.48, w * 0.54, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, isUnlocked ? Color.web("#182520") : Color.web("#111827")),
                new Stop(0.65, isUnlocked ? Color.web("#0D1412") : Color.web("#0B0F17")),
                new Stop(1.0, Color.web("#070B0A"))
        );
        gc.setFill(bg);
        gc.fillRoundRect(0, 0, w, h, 14, 14);

        gc.setStroke(Color.web("#FFFFFF", isUnlocked ? 0.08 : 0.04));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(0.5, 0.5, w - 1.0, h - 1.0, 14, 14);

        if (currentCreature == null) return;

        double cx = w * 0.5;
        double cy = h * 0.51;
        double angle = -Math.PI * 0.25 + animTime * 0.58;

        double spriteScale = 2.15;
        if (isBoss) {
            spriteScale = 1.52;
        } else if (isPassive) {
            PassiveAnimalType pt = currentCreature.getPassiveType();
            if (pt == PassiveAnimalType.DEER) spriteScale = 1.62;
            else if (pt == PassiveAnimalType.RABBIT) spriteScale = 2.40;
            else if (pt == PassiveAnimalType.FROG) spriteScale = 2.50;
            else if (pt == PassiveAnimalType.BIRD) spriteScale = 2.35;
        }

        // Pedestal discreto iluminado
        gc.setFill(Color.color(0, 0, 0, 0.42));
        gc.fillOval(cx - 56, cy + 32, 112, 22);
        gc.setStroke(ringAccent);
        gc.setLineWidth(1.2);
        gc.strokeOval(cx - 56, cy + 32, 112, 22);

        gc.save();
        gc.translate(cx, cy);
        gc.scale(spriteScale, spriteScale);

        // Se a criatura ainda não foi derrotada pelo jogador, exibe em silhueta misteriosa
        if (!isUnlocked) {
            gc.setGlobalAlpha(0.20);
        }

        if (previewPassive != null) {
            PassiveAnimalRenderer passiveRenderer = new PassiveAnimalRenderer(gc);
            previewPassive.setVisualAngle(angle);
            previewPassive.setWalkPhase(animTime * 6.5);
            previewPassive.setBreathTimer(animTime);
            passiveRenderer.renderPassiveAnimal(previewPassive, 0, 0);
        } else if (previewEnemy != null) {
            CharacterSpriteRenderer spriteRenderer = new CharacterSpriteRenderer(gc);
            previewEnemy.setFaceAngle(angle);
            previewEnemy.setWalkPhase(animTime * 7.2);
            previewEnemy.setBreathTimer(animTime);
            spriteRenderer.renderEnemySprite(previewEnemy, 0, 0);
        } else {
            CharacterSpriteRenderer spriteRenderer = new CharacterSpriteRenderer(gc);
            previewPlayer.setVisualAngle(angle);
            previewPlayer.setWalkPhase(animTime * 8.2);
            previewPlayer.setBreathTimer(animTime);
            spriteRenderer.renderFox(previewPlayer, 0, 0);
        }

        gc.restore();

        // Emblema de cadeado / silhueta bloqueada quando não desbloqueado
        if (!isUnlocked) {
            gc.save();
            gc.setFill(Color.web("#0F172A", 0.65));
            gc.fillOval(cx - 18, cy - 18, 36, 36);
            gc.setStroke(Color.web("#64748B", 0.80));
            gc.setLineWidth(1.4);
            gc.strokeOval(cx - 18, cy - 18, 36, 36);

            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            gc.setFill(Color.web("#CBD5E1"));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("?", cx, cy + 6);
            gc.restore();
        }
    }
}
