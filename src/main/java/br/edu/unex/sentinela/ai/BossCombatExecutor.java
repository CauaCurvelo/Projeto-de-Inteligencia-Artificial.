package br.edu.unex.sentinela.ai;

import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.settings.GameSettings;
import br.edu.unex.sentinela.world.Camera;
import br.edu.unex.sentinela.world.Particle;
import br.edu.unex.sentinela.world.TileMap;

import java.util.List;
import java.util.Random;

/**
 * Resolve gatilhos, hitboxes diferenciadas, partículas de impacto e tremor de câmera
 * específicos dos ataques primários e especiais dos Chefes.
 */
public final class BossCombatExecutor {
    private static final Random RAND = new Random();

    private BossCombatExecutor() {}

    /**
     * Avalia preparação de ataque, efeitos visuais e colisão de golpe de um Chefe contra a Raposa.
     *
     * @return mensagem de derrota caso o jogador chegue a 0 vidas neste ataque, ou null caso contrário.
     */
    public static String processBossCombat(Enemy boss, Player player, List<Particle> particles,
                                           Camera camera, TileMap tileMap, double distToPlayer) {
        BossBehaviorController ctrl = boss.getBossController();
        if (ctrl == null || boss.getHp() <= 0) return null;

        if (ctrl.consumePendingRoarShake() && distToPlayer < 450.0) {
            spawnRoarParticles(boss, ctrl.getArchetype(), particles);
            if (GameSettings.get().isScreenShake()) {
                camera.triggerShake(4.2, 0.28);
            }
        }

        if (!ctrl.isRetreating() && !ctrl.isRoaring()) {
            BossAttackType chosen = ctrl.selectAttackForPlayer(boss, player, tileMap, distToPlayer);
            if (chosen != null) {
                boss.startBossAttackWindup(player.getX(), player.getY(), ctrl.getScaledWindupDuration(chosen));
            }
        }

        if (!boss.isLunging()) {
            return null;
        }

        BossAttackType atk = ctrl.getActiveAttack();
        if (!ctrl.isFirstBurstVfxDone()) {
            ctrl.markFirstBurstVfxDone();
            spawnAttackBurstParticles(boss, atk, ctrl.getArchetype(), particles);
            if (GameSettings.get().isScreenShake() && distToPlayer < 460.0) {
                if (atk == BossAttackType.BEAR_SEISMIC_SLAM || atk == BossAttackType.POLAR_FROST_CRUSH) {
                    camera.triggerShake(7.8, 0.36);
                } else if (atk == BossAttackType.CROC_DEATH_ROLL) {
                    camera.triggerShake(4.6, 0.24);
                }
            }
        }

        if (!shouldAttemptHitThisWindow(ctrl, atk)) {
            return null;
        }

        if (!isPlayerInsideAttackHitbox(boss, player, atk, distToPlayer)) {
            return null;
        }

        boolean struck = player.takeDamage(boss.getX(), boss.getY(), atk.getPlayerKnockback());
        if (!struck) {
            return null;
        }

        markHitExecuted(ctrl, atk);
        spawnHitSparks(player, ctrl.getArchetype().getParticleColorHex(), 13, particles);
        if (GameSettings.get().isScreenShake()) {
            camera.triggerShake(5.0, 0.20);
        }

        if (player.getLives() <= 0) {
            return boss.getSkin().getDisplayName() + " o subjugou com " + atk.getDisplayName() + "!";
        }
        return null;
    }

    private static boolean shouldAttemptHitThisWindow(BossBehaviorController ctrl, BossAttackType atk) {
        if (atk.isDoubleHit()) {
            if (!ctrl.isHitLandedFirst()) return true;
            return !ctrl.isHitLandedSecond() && ctrl.isSecondSlashWindowReady();
        }
        return !ctrl.isHitLandedFirst();
    }

    private static void markHitExecuted(BossBehaviorController ctrl, BossAttackType atk) {
        if (!ctrl.isHitLandedFirst()) {
            ctrl.markHitLandedFirst();
        } else if (atk.isDoubleHit()) {
            ctrl.markHitLandedSecond();
        }
    }

    private static boolean isPlayerInsideAttackHitbox(Enemy boss, Player player, BossAttackType atk, double distToPlayer) {
        if (player.isBurrowed() || distToPlayer > atk.getHitRange()) {
            return false;
        }
        if (atk.isAreaAttack() || distToPlayer < 16.0) {
            return true;
        }
        double angleToPlayer = Math.atan2(player.getY() - boss.getY(), player.getX() - boss.getX());
        double diff = normalizeAngle(angleToPlayer - boss.getFaceAngle());
        return Math.abs(diff) <= atk.getHitHalfArcRad();
    }

    private static void spawnRoarParticles(Enemy boss, BossArchetype archetype, List<Particle> particles) {
        for (int i = 0; i < 10; i++) {
            double a = (Math.PI * 2.0 * i) / 10.0;
            particles.add(new Particle(
                boss.getX() + Math.cos(a) * 14,
                boss.getY() + Math.sin(a) * 14,
                Math.cos(a) * 75.0,
                Math.sin(a) * 75.0,
                0.35,
                archetype.getAccentColorHex(),
                4.2
            ));
        }
    }

    private static void spawnAttackBurstParticles(Enemy boss, BossAttackType atk, BossArchetype archetype, List<Particle> particles) {
        if (atk == BossAttackType.BEAR_SEISMIC_SLAM) {
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2.0 * i) / 16.0;
                String hex = (i % 2 == 0) ? "#E67E22" : "#D7CCC8";
                particles.add(new Particle(boss.getX(), boss.getY(), Math.cos(a) * 145, Math.sin(a) * 145, 0.40, hex, 5.2));
            }
        } else if (atk == BossAttackType.POLAR_FROST_CRUSH) {
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2.0 * i) / 16.0;
                String hex = (i % 2 == 0) ? "#38BDF8" : "#F8FAFC";
                particles.add(new Particle(boss.getX(), boss.getY(), Math.cos(a) * 150, Math.sin(a) * 150, 0.42, hex, 5.4));
            }
        } else if (atk == BossAttackType.CROC_DEATH_ROLL) {
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12.0;
                particles.add(new Particle(boss.getX(), boss.getY(), Math.cos(a) * 110, Math.sin(a) * 110, 0.36, "#4DD0E1", 4.5));
            }
        } else if (atk == BossAttackType.PANTHER_SHADOW_POUNCE) {
            double back = boss.getFaceAngle() + Math.PI;
            for (int i = 0; i < 9; i++) {
                double spread = back + (RAND.nextDouble() - 0.5) * 0.9;
                particles.add(new Particle(boss.getX(), boss.getY(), Math.cos(spread) * 95, Math.sin(spread) * 95, 0.28, archetype.getAccentColorHex(), 3.8));
            }
        }
    }

    private static void spawnHitSparks(Player player, String colorHex, int count, List<Particle> particles) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                player.getX(),
                player.getY(),
                RAND.nextDouble() * 140 - 70,
                RAND.nextDouble() * 140 - 70,
                0.36,
                colorHex,
                4.8
            ));
        }
    }

    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= Math.PI * 2.0;
        while (angle < -Math.PI) angle += Math.PI * 2.0;
        return angle;
    }
}
