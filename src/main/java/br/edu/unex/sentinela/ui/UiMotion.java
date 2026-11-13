package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.audio.AudioEngine;
import br.edu.unex.sentinela.audio.SoundId;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.Random;

/**
 * Micro-interações cinéticas da UI: entradas suaves, feedback tátil e atmosfera de vagalumes.
 */
public final class UiMotion {
    private UiMotion() {}

    /**
     * Anima a entrada de um painel ou aba com fade + elevação sutil.
     */
    public static void animateEntrance(Node node) {
        if (node == null) return;
        node.setOpacity(0.0);
        node.setTranslateY(10.0);

        FadeTransition fade = new FadeTransition(Duration.millis(190), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(190), node);
        slide.setFromY(10.0);
        slide.setToY(0.0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();
    }

    /**
     * Aplica deslizamento horizontal sutil no hover (ideal para trilho lateral do menu).
     */
    public static void attachHoverSlide(Node node, double shiftX) {
        if (node == null) return;
        node.hoverProperty().addListener((obs, was, hovered) -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(120), node);
            tt.setToX(hovered ? shiftX : 0.0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            tt.play();
        });
    }

    /**
     * Feedback tátil de pressão + clique sonoro sutil.
     */
    public static void attachPressFeedback(Node node, boolean playSound) {
        if (node == null) return;
        node.setOnMousePressed(e -> scaleTo(node, 0.975, 65));
        node.setOnMouseReleased(e -> {
            scaleTo(node, 1.0, 95);
            if (playSound && node.isHover()) {
                AudioEngine.get().play(SoundId.UI_CLICK, 0.24, 0.0, 1.05);
            }
        });
    }

    private static void scaleTo(Node node, double target, double millis) {
        ScaleTransition st = new ScaleTransition(Duration.millis(millis), node);
        st.setToX(target);
        st.setToY(target);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }

    /**
     * Canvas leve de vagalumes da floresta para o fundo do Menu Principal.
     * Pausa automaticamente quando removido da Scene para 0% de custo em partida.
     */
    public static Canvas createFireflyBackdrop() {
        Canvas canvas = new Canvas(960, 640);
        canvas.setMouseTransparent(true);

        final int count = 24;
        final double[] x = new double[count];
        final double[] y = new double[count];
        final double[] speed = new double[count];
        final double[] phase = new double[count];
        final double[] radius = new double[count];
        final boolean[] emerald = new boolean[count];

        Random rng = new Random(42L);
        for (int i = 0; i < count; i++) {
            x[i] = 0.28 + rng.nextDouble() * 0.70;
            y[i] = 0.10 + rng.nextDouble() * 0.85;
            speed[i] = 0.008 + rng.nextDouble() * 0.016;
            phase[i] = rng.nextDouble() * Math.PI * 2.0;
            radius[i] = 1.6 + rng.nextDouble() * 2.4;
            emerald[i] = rng.nextDouble() < 0.32;
        }

        AnimationTimer timer = new AnimationTimer() {
            private long lastNs = 0L;
            private double elapsed = 0.0;

            @Override
            public void handle(long now) {
                if (lastNs == 0L) lastNs = now;
                double dt = Math.min(0.05, (now - lastNs) * 1e-9);
                lastNs = now;
                elapsed += dt;

                double w = canvas.getWidth();
                double h = canvas.getHeight();
                if (w <= 0 || h <= 0) return;

                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.clearRect(0, 0, w, h);

                for (int i = 0; i < count; i++) {
                    y[i] -= speed[i] * dt;
                    if (y[i] < -0.05) y[i] = 1.05;
                    double px = (x[i] + Math.sin(elapsed * 0.65 + phase[i]) * 0.022) * w;
                    double py = y[i] * h;
                    double pulse = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(elapsed * 2.1 + phase[i] * 1.7));
                    double r = radius[i];

                    Color core = emerald[i]
                            ? Color.web("#34D399", 0.68 * pulse)
                            : Color.web("#FCD34D", 0.72 * pulse);
                    Color halo = emerald[i]
                            ? Color.web("#34D399", 0.14 * pulse)
                            : Color.web("#E8B948", 0.16 * pulse);

                    gc.setFill(halo);
                    gc.fillOval(px - r * 3.0, py - r * 3.0, r * 6.0, r * 6.0);
                    gc.setFill(core);
                    gc.fillOval(px - r * 0.65, py - r * 0.65, r * 1.3, r * 1.3);
                }
            }
        };

        canvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) timer.start();
            else timer.stop();
        });
        return canvas;
    }
}
