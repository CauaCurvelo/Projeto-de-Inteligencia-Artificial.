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
     * Avalia preparação de ataque, efeitos visuais e colisão de golpe de um Chefe contra a Raposa e outros mobs.
     *
     * @return mensagem de derrota caso o jogador chegue a 0 vidas neste ataque, ou null caso contrário.
     */
    public static String processBossCombat(Enemy boss, Player player, List<Enemy> allEnemies, List<Particle> particles,
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
            // Se o chefe estiver perseguindo o jogador, avalia ataque contra ele
            if (boss.getState() == AIState.CHASE && distToPlayer < 260.0) {
                BossAttackType chosen = ctrl.selectAttackForPlayer(boss, player, tileMap, distToPlayer);
                if (chosen != null) {
                    boss.startBossAttackWindup(player.getX(), player.getY(), ctrl.getScaledWindupDuration(chosen));
                }
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

        boolean hitLandedOnAny = false;
        String gameOverReason = null;

        // 1. Conexão física do golpe com a Raposa (Player)
        if (isPlayerInsideAttackHitbox(boss, player, atk, distToPlayer)) {
            boolean struck = player.takeDamage(boss.getX(), boss.getY(), atk.getPlayerKnockback());
            if (struck) {
                hitLandedOnAny = true;
                spawnHitSparks(player.getX(), player.getY(), ctrl.getArchetype().getParticleColorHex(), 13, particles);
                if (GameSettings.get().isScreenShake()) {
                    camera.triggerShake(5.0, 0.20);
                }
                if (player.getLives() <= 0) {
                    gameOverReason = boss.getSkin().getDisplayName() + " o subjugou com " + atk.getDisplayName() + "!";
                }
            }
        }

        // 2. Conexão física de área com outros Inimigos/Fauna hostil
        if (allEnemies != null) {
            for (Enemy other : allEnemies) {
                if (other == boss || other.getHp() <= 0 || other.getType() == EnemyType.BEAR_BOSS) continue;
                double distOther = Math.hypot(other.getX() - boss.getX(), other.getY() - boss.getY());
                if (isTargetInsideAttackHitbox(boss, other.getX(), other.getY(), atk, distOther)) {
                    int damage = (atk.isAreaAttack() || atk.isDoubleHit()) ? 3 : 2;
                    other.takeDamageFrom(damage, boss, boss.getX(), boss.getY());
                    double knock = atk.getPlayerKnockback() * 0.90;
                    other.applyKnockback(boss.getX(), boss.getY(), knock);
                    hitLandedOnAny = true;
                    spawnHitSparks(other.getX(), other.getY(), "#E53935", 8, particles);
                    br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.HIT_IMPACT, 0.85, 0, 1.05);
                }
            }
        }

        if (hitLandedOnAny) {
            markHitExecuted(ctrl, atk);
        }

        return gameOverReason;
    }

    public static String processBossCombat(Enemy boss, Player player, List<Particle> particles,
                                           Camera camera, TileMap tileMap, double distToPlayer) {
        return processBossCombat(boss, player, null, particles, camera, tileMap, distToPlayer);
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

    private static boolean isTargetInsideAttackHitbox(Enemy boss, double targetX, double targetY, BossAttackType atk, double distToTarget) {
        if (distToTarget > atk.getHitRange()) {
            return false;
        }
        if (atk.isAreaAttack() || distToTarget < 20.0) {
            return true;
        }
        double angleToTarget = Math.atan2(targetY - boss.getY(), targetX - boss.getX());
        double diff = normalizeAngle(angleToTarget - boss.getFaceAngle());
        return Math.abs(diff) <= atk.getHitHalfArcRad();
    }

    private static boolean isPlayerInsideAttackHitbox(Enemy boss, Player player, BossAttackType atk, double distToPlayer) {
        if (player.isBurrowed()) {
            return false;
        }
        return isTargetInsideAttackHitbox(boss, player.getX(), player.getY(), atk, distToPlayer);
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

    private static void spawnHitSparks(double x, double y, String colorHex, int count, List<Particle> particles) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                x,
                y,
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
