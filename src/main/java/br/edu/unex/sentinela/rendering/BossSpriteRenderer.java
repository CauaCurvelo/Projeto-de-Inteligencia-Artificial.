package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.ai.BossAttackType;
import br.edu.unex.sentinela.ai.BossBehaviorController;
import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.entity.Enemy;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Renderizador dedicado exclusivamente aos Chefes (Urso Pardo Alfa, Pantera Negra e Crocodilo do Rio),
 * aplicando expressão corporal dinâmica: abertura de mandíbula/rugido, agachamento de salto,
 * elevação sísmica, giro rotativo e destaque visual de fúria (< 50% HP) ou recuo.
 */
public class BossSpriteRenderer {
    private final GraphicsContext gc;
    private final CharacterSpriteRenderer quadrupedHelper;

    public BossSpriteRenderer(GraphicsContext gc, CharacterSpriteRenderer quadrupedHelper) {
        this.gc = gc;
        this.quadrupedHelper = quadrupedHelper;
    }

    public void renderBossSprite(Enemy enemy, double ex, double ey) {
        BossBehaviorController ctrl = enemy.getBossController();
        double walk = enemy.getWalkPhase();
        boolean moving = enemy.isMoving();
        boolean lunging = enemy.isLunging();
        boolean windup = enemy.isWindup();

        boolean enraged = ctrl != null && ctrl.isEnraged();
        boolean roaring = ctrl != null && ctrl.isRoaring();
        boolean retreating = ctrl != null && ctrl.isRetreating();
        double mouthOpen = ctrl != null ? ctrl.getMouthOpenRatio() : 0.0;
        double windupProg = ctrl != null ? ctrl.getWindupProgress() : 0.0;
        BossAttackType atk = ctrl != null ? ctrl.getActiveAttack() : null;

        double breathRate = enraged ? 5.2 : 3.4;
        double breathAmp = enraged ? 0.068 : 0.045;
        double breath = Math.sin(enemy.getBreathTimer() * breathRate) * breathAmp;

        double scaleX = ctrl != null ? ctrl.getBodyScaleX(lunging) : 1.0;
        double scaleY = ctrl != null ? ctrl.getBodyScaleY(lunging) : 1.0;
        double rollDeg = ctrl != null ? ctrl.getRollAngleOffsetDeg(lunging) : 0.0;
        double roarJitter = roaring ? Math.sin(enemy.getBreathTimer() * 48.0) * 1.9 : 0.0;

        gc.save();
        gc.translate(ex + roarJitter * 0.4, ey + roarJitter * 0.4);
        gc.rotate(Math.toDegrees(enemy.getFaceAngle()) + rollDeg);
        gc.scale(scaleX, scaleY);

        EnemySkin skin = enemy.getSkin();
        if (skin == EnemySkin.CROCODILE) {
            drawRiverCrocodile(walk, breath, moving, mouthOpen, enraged, roaring, retreating, atk, windup, lunging);
        } else if (skin == EnemySkin.PANTHER) {
            drawShadowPanther(walk, breath, moving, mouthOpen, enraged, roaring, retreating, atk, windup, lunging, windupProg);
        } else {
            drawAlphaBear(walk, breath, moving, mouthOpen, enraged, roaring, retreating, atk, windup, windupProg,
                    skin == EnemySkin.POLAR_BEAR);
        }

        gc.restore();
    }

    private void drawAlphaBear(double walk, double breath, boolean moving, double mouthOpen,
                                boolean enraged, boolean roaring, boolean retreating,
                                BossAttackType atk, boolean windup, double windupProg, boolean isPolar) {
        double stride = moving ? Math.sin(walk) * 6.8 : 0.0;
        double sway = moving ? Math.cos(walk) * 1.6 : 0.0;
        boolean slamPrep = windup && (atk == BossAttackType.BEAR_SEISMIC_SLAM || atk == BossAttackType.POLAR_FROST_CRUSH);
        double slamLift = slamPrep ? windupProg * 5.0 : 0.0;

        Color legCoat = Color.web(isPolar ? "#94A3B8" : "#2B1707");
        Color clawColor = Color.web(isPolar ? "#E0F7FA" : "#D7CCC8");
        Color outerCoat = Color.web(isPolar ? (enraged ? "#7DD3FC" : "#CBD5E1") : (enraged ? "#4A220A" : "#38200D"));
        Color midCoat = Color.web(isPolar ? (enraged ? "#BAE6FD" : "#E2E8F0") : (enraged ? "#6A3210" : "#4E2C12"));
        Color torsoCoat = Color.web(isPolar ? (enraged ? "#E0F2FE" : "#F8FAFC") : (enraged ? "#8D4618" : "#6E411E"));
        Color headCoat = Color.web(isPolar ? "#F1F5F9" : "#4A2910");
        Color muzzleCoat = Color.web(isPolar ? "#FFFFFF" : "#8D5B36");
        Color noseColor = Color.web(isPolar ? "#0F172A" : "#140A03");
        Color crestColor = Color.web(isPolar ? "#38BDF8" : "#FF6D00", 0.58);

        // Sombra pesada expande quando se ergue para esmagar
        gc.setFill(Color.color(0, 0, 0, 0.46));
        gc.fillOval(-22 - slamLift * 0.5, -15 - slamLift * 0.4, 44 + slamLift, 30 + slamLift * 0.8);

        // Patas colossais (dianteiras alargam na preparação do esmagamento)
        if (slamPrep) {
            gc.setFill(legCoat);
            gc.fillRoundRect(8, -17 - slamLift * 0.5, 11, 7, 3, 3);
            gc.fillRoundRect(8, 10 + slamLift * 0.5, 11, 7, 3, 3);
            gc.setFill(Color.web(isPolar ? "#38BDF8" : "#FFD54F"));
            gc.fillOval(17, -16.5 - slamLift * 0.5, 5, 5);
            gc.fillOval(17, 11.5 + slamLift * 0.5, 5, 5);
        }
        quadrupedHelper.drawQuadrupedLegs(stride, 10.5, 12.0, 9.5, 5.6, legCoat, clawColor);

        // Rabo curto redondo
        gc.setFill(outerCoat);
        gc.fillOval(-22, -4.0, 7, 8);

        // Corpo Maciço e Corcunda Muscular
        gc.scale(1.0 + breath * 0.5, 1.0 + breath);
        gc.setFill(outerCoat);
        gc.fillOval(-19, -14.0 + sway * 0.4, 36, 28.0);
        gc.setFill(midCoat);
        gc.fillOval(-16, -12.5 + sway * 0.4, 31, 25.0);
        gc.setFill(torsoCoat);
        gc.fillOval(-4, -11.5 + sway * 0.5, 19, 23.0);

        // Crista de pelo eriçado / geada no dorso quando em fúria ou rugindo
        if (enraged || roaring) {
            gc.setFill(crestColor);
            for (double rx = -10; rx <= 4; rx += 5) {
                gc.fillPolygon(new double[]{rx, rx + 3, rx - 1}, new double[]{-12, -16, -9}, 3);
                gc.fillPolygon(new double[]{rx, rx + 3, rx - 1}, new double[]{12, 16, 9}, 3);
            }
        }

        // Cabeça larga e focinho expressivo
        double headShift = mouthOpen * 2.5 + (retreating ? -1.5 : 0.0);
        gc.setFill(headCoat);
        gc.fillOval(7.0 + headShift, -9.5 + sway * 0.6, 15.0, 19.0);

        // Boca aberta em rugido com presas brancas
        if (mouthOpen > 0.12) {
            double jawW = 5.5 + mouthOpen * 4.5;
            double jawH = 4.0 + mouthOpen * 5.2;
            gc.setFill(Color.web("#4A0912"));
            gc.fillOval(19.0 + headShift, -jawH * 0.5 + sway * 0.6, jawW, jawH);
            gc.setFill(Color.web("#FFF8E1"));
            gc.fillPolygon(new double[]{21 + headShift, 24.5 + headShift, 21.5 + headShift},
                           new double[]{-jawH * 0.45, -jawH * 0.15, -jawH * 0.05}, 3);
            gc.fillPolygon(new double[]{21 + headShift, 24.5 + headShift, 21.5 + headShift},
                           new double[]{jawH * 0.45, jawH * 0.15, jawH * 0.05}, 3);
        }

        // Focinho claro / branco glacial
        gc.setFill(muzzleCoat);
        gc.fillRoundRect(16.5 + headShift, -5.0 + sway * 0.6, 8.5, 10.0, 5, 5);
        gc.setFill(noseColor);
        gc.fillOval(22.5 + headShift, -2.5 + sway * 0.6, 3.8, 5.0);

        // Orelhas (achatam levemente em fúria ou recuo)
        double earPin = (enraged || retreating) ? 1.5 : 0.0;
        gc.setFill(legCoat);
        gc.fillOval(8.5 + headShift - earPin, -12.5 + sway * 0.6 + earPin * 0.5, 6.5, 6.5);
        gc.fillOval(8.5 + headShift - earPin, 6.0 + sway * 0.6 - earPin * 0.5, 6.5, 6.5);

        // Olhos expressivos de predador alfa
        Color eyeColor = retreating ? Color.web("#90A4AE")
                : (isPolar ? (enraged ? Color.web("#00E5FF") : Color.web("#0284C7"))
                           : (enraged ? Color.web("#FFEA00") : Color.web("#FF3D00")));
        double eyeSize = (enraged || roaring) ? 3.5 : 2.8;
        gc.setFill(eyeColor);
        gc.fillOval(16.5 + headShift, -5.2 + sway * 0.6, eyeSize, 2.5);
        gc.fillOval(16.5 + headShift, 2.8 + sway * 0.6, eyeSize, 2.5);
    }

    private void drawShadowPanther(double walk, double breath, boolean moving, double mouthOpen,
                                   boolean enraged, boolean roaring, boolean retreating,
                                   BossAttackType atk, boolean windup, boolean lunging, double windupProg) {
        double stride = moving ? Math.sin(walk) * 7.2 : (windup && atk == BossAttackType.PANTHER_SHADOW_POUNCE ? -windupProg * 4.5 : 0.0);
        double spineCurve = moving ? Math.cos(walk) * 1.8 : 0.0;
        double tailFreq = (enraged || windup) ? walk * 1.9 + 5.0 : (moving ? walk * 1.2 : breath * 9.0);
        double tailWhip = Math.sin(tailFreq) * ((windup || enraged) ? 8.0 : 6.2);

        gc.setFill(Color.color(0, 0, 0, 0.44));
        gc.fillOval(-20, -10, 40, 20);

        // Patas Musculosas (no salto esticam para frente/trás)
        if (lunging && atk == BossAttackType.PANTHER_SHADOW_POUNCE) {
            quadrupedHelper.drawQuadrupedLegs(6.5, 11.5, 8.2, 8.8, 3.8, Color.web("#121212"), Color.web("#FFD600"));
        } else {
            quadrupedHelper.drawQuadrupedLegs(stride, 10.5, 8.2, 7.5, 3.8, Color.web("#121212"), Color.web("#2C2C2C"));
        }

        // Cauda Felina Longa chicoteando em "S"
        gc.setStroke(Color.web(enraged ? "#311B92" : "#141414"));
        gc.setLineWidth(4.2);
        gc.strokeLine(-15, 0, -24, tailWhip * 0.5);
        gc.strokeLine(-24, tailWhip * 0.5, -32, -tailWhip * 0.45);

        // Corpo Felino Alongado
        gc.scale(1.0 + breath * 0.4, 1.0 + breath);
        gc.setFill(Color.web("#101014"));
        gc.fillOval(-17, -7.8 + spineCurve * 0.3, 24, 15.6);
        gc.fillOval(-6, -9.2 + spineCurve * 0.5, 21, 18.4);
        gc.setFill(Color.web(enraged ? "#4527A0" : "#263238", 0.78));
        gc.fillOval(-12, -5.5 + spineCurve * 0.4, 15, 11.0);
        gc.fillOval(-3, -6.8 + spineCurve * 0.5, 14, 13.6);

        // Cabeça Felina e Rosnado
        double headShift = mouthOpen * 2.0 + (lunging ? 2.5 : 0.0);
        gc.setFill(Color.web("#141418"));
        gc.fillOval(9.0 + headShift, -7.2 + spineCurve * 0.6, 12.5, 14.4);

        if (mouthOpen > 0.12) {
            gc.setFill(Color.web("#5E0B15"));
            gc.fillOval(18.5 + headShift, -3.5 - mouthOpen * 2.0 + spineCurve * 0.6, 5.5 + mouthOpen * 3.0, 7.0 + mouthOpen * 3.5);
            gc.setFill(Color.web("#FFFFFF"));
            gc.fillOval(21.5 + headShift, -3.2 + spineCurve * 0.6, 2.2, 2.0);
            gc.fillOval(21.5 + headShift, 1.2 + spineCurve * 0.6, 2.2, 2.0);
        }

        gc.setFill(Color.web("#1F1F24"));
        gc.fillRoundRect(16.5 + headShift, -4.5 + spineCurve * 0.6, 6.0, 9.0, 4, 4);

        // Orelhas pontudas dobram para trás quando agacha para o bote
        double earBack = (windup || enraged) ? -2.2 : 0.0;
        gc.setFill(Color.web("#08080A"));
        gc.fillOval(10.0 + headShift + earBack, -9.2 + spineCurve * 0.6, 5.0, 4.5);
        gc.fillOval(10.0 + headShift + earBack, 4.7 + spineCurve * 0.6, 5.0, 4.5);

        // Olhos Luminosos (brilham mais forte no ataque/fúria)
        Color eyeColor = retreating ? Color.web("#90A4AE") : (roaring || enraged ? Color.web("#FF6D00") : Color.web("#FFD600"));
        gc.setFill(eyeColor);
        gc.fillOval(16.0 + headShift, -4.5 + spineCurve * 0.6, 3.2, 2.4);
        gc.fillOval(16.0 + headShift, 2.1 + spineCurve * 0.6, 3.2, 2.4);
    }

    private void drawRiverCrocodile(double walk, double breath, boolean moving, double mouthOpen,
                                    boolean enraged, boolean roaring, boolean retreating,
                                    BossAttackType atk, boolean windup, boolean lunging) {
        double stride = moving ? Math.sin(walk) * 5.2 : 0.0;
        double tailSpeed = (moving || windup || lunging) ? walk * 1.5 + 3.0 : breath * 8.0;
        double tailSway = Math.sin(tailSpeed) * ((windup || lunging) ? 7.5 : 5.5);

        gc.setFill(Color.color(0, 0, 0, 0.42));
        gc.fillOval(-26, -12, 52, 24);

        // 4 Patas Reptilianas Abertas
        gc.setFill(Color.web("#1B4D20"));
        gc.fillOval(6 + stride, -13.5, 7.5, 5.0);
        gc.fillOval(6 - stride, 8.5, 7.5, 5.0);
        gc.fillOval(-11 - stride, -13.0, 7.5, 5.0);
        gc.fillOval(-11 + stride, 8.0, 7.5, 5.0);

        // Cauda Escamosa Articulada
        gc.setFill(Color.web("#143D19"));
        gc.fillOval(-24, -5.5 + tailSway * 0.3, 13, 11.0);
        gc.fillOval(-32, -4.0 + tailSway * 0.7, 11, 8.0);
        gc.fillPolygon(new double[]{-29, -41, -29},
                       new double[]{-3.0 + tailSway * 0.7, tailSway * 1.15, 3.0 + tailSway * 0.7}, 3);

        // Torso Couraçado Alongado
        gc.scale(1.0 + breath * 0.3, 1.0 + breath);
        gc.setFill(Color.web("#143D19"));
        gc.fillOval(-16, -9.5, 32, 19.0);
        gc.setFill(Color.web(enraged ? "#1B5E20" : "#2E7D32"));
        gc.fillOval(-13, -7.8, 27, 15.6);

        // Osteodermos dorsais (realce esmeralda/dourado em fúria)
        gc.setFill(Color.web(enraged ? "#00E676" : "#0D2910"));
        for (int i = -12; i <= 8; i += 5) {
            gc.fillRect(i, -4.2, 3.2, 2.4);
            gc.fillRect(i, 1.8, 3.2, 2.4);
        }

        // Mandíbula articulada: abre em "V" durante rugido/emboscada expondo garganta carmesim e dentes
        double jawSpread = mouthOpen * 5.2;
        if (mouthOpen > 0.08) {
            gc.setFill(Color.web("#6A0D1C"));
            gc.fillPolygon(
                new double[]{12.0, 26.5, 26.5},
                new double[]{0.0, -5.0 - jawSpread, 5.0 + jawSpread},
                3
            );
        }

        // Metade superior & inferior do focinho (deslocam lateralmente simulando abertura top-down)
        gc.setFill(Color.web("#1B5E20"));
        gc.fillRoundRect(11.0, -6.2 - jawSpread * 0.65, 16.5, 6.8, 4, 4);
        gc.fillRoundRect(11.0, -0.6 + jawSpread * 0.65, 16.5, 6.8, 4, 4);
        gc.setFill(Color.web("#388E3C"));
        gc.fillRoundRect(12.0, -4.8 - jawSpread * 0.55, 14.5, 9.6, 4, 4);

        // Dentes brancos laterais acompahando abertura da mandíbula
        gc.setFill(Color.web("#FFFDE7"));
        for (int d = 14; d <= 23; d += 3) {
            gc.fillOval(d, -6.6 - jawSpread * 0.6, 2.2, 2.2);
            gc.fillOval(d, 4.4 + jawSpread * 0.6, 2.2, 2.2);
        }

        // Narinas e Olhos reptilianos
        gc.setFill(Color.web("#0A1F0D"));
        gc.fillOval(24.5, -3.2 - jawSpread * 0.35, 2.0, 1.8);
        gc.fillOval(24.5, 1.4 + jawSpread * 0.35, 2.0, 1.8);

        Color eyeColor = retreating ? Color.web("#90A4AE") : (enraged || roaring ? Color.web("#FF6D00") : Color.web("#FFEA00"));
        gc.setFill(eyeColor);
        gc.fillOval(11.5, -5.5 - jawSpread * 0.25, 3.2, 2.4);
        gc.fillOval(11.5, 3.1 + jawSpread * 0.25, 3.2, 2.4);
    }
}
