package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.audio.AudioEngine;
import br.edu.unex.sentinela.audio.SoundId;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.PassiveAnimal;
import br.edu.unex.sentinela.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Representa a queda de um raio no mundo durante eventos de Tempestade.
 *
 * Possui 3 estágios com transições suaves:
 * 1. WARNING (1.35s): Telégrafo visual de carga eletrostática no chão (círculo pulsante, faíscas azuis/amarelas
 *    e arcos convergentes). Dá tempo do jogador e animais saírem da área se quiserem.
 * 2. STRIKING (0.22s): O raio atinge o solo com ramificações zigue-zague procedurais, som estrondoso de trovão,
 *    flash eletrizante na tela e dano massivo na área de impacto.
 * 3. LINGERING (0.65s): Ponto chamuscado no solo, faíscas residuais e fumaça que dissipa gradualmente.
 */
public class LightningStrike {

    public enum Stage {
        WARNING,
        STRIKING,
        LINGERING,
        DEAD
    }

    private static final Random RAND = new Random();

    private final double x;
    private final double y;
    private Stage stage = Stage.WARNING;

    private static final double WARNING_DURATION = 1.35;
    private static final double STRIKE_DURATION = 0.22;
    private static final double LINGER_DURATION = 0.65;

    private double timer = WARNING_DURATION;
    private boolean damageApplied = false;

    // Vértices pré-calculados do traçado do raio para o estágio STRIKING
    private final List<double[]> mainBoltPoints = new ArrayList<>();
    private final List<List<double[]>> branchBolts = new ArrayList<>();

    // Partículas elétricas pré-geradas para o estágio WARNING
    private final List<double[]> sparkOffsets = new ArrayList<>();
    private boolean playerWasInDangerZone = false;

    public LightningStrike(double x, double y) {
        this.x = x;
        this.y = y;

        // Gera 8 a 12 micro faíscas angulares para o aviso de carga
        int sparkCount = 10;
        for (int i = 0; i < sparkCount; i++) {
            double angle = RAND.nextDouble() * 2.0 * Math.PI;
            double radius = 12.0 + RAND.nextDouble() * 32.0;
            sparkOffsets.add(new double[]{angle, radius, 1.5 + RAND.nextDouble() * 2.5});
        }
    }

    public void update(double deltaTime, World world) {
        timer -= deltaTime;

        switch (stage) {
            case WARNING:
                Player checkPlayer = world.getPlayer();
                if (checkPlayer != null && Math.hypot(checkPlayer.getX() - x, checkPlayer.getY() - y) <= 68.0) {
                    playerWasInDangerZone = true;
                }
                if (timer <= 0) {
                    stage = Stage.STRIKING;
                    timer = STRIKE_DURATION;
                    generateBoltGeometry();
                    applyDamageAndEffects(world);
                }
                break;

            case STRIKING:
                if (timer <= 0) {
                    stage = Stage.LINGERING;
                    timer = LINGER_DURATION;
                }
                break;

            case LINGERING:
                if (timer <= 0) {
                    stage = Stage.DEAD;
                }
                break;

            case DEAD:
                break;
        }
    }

    private void generateBoltGeometry() {
        mainBoltPoints.clear();
        branchBolts.clear();

        // Raio desce do céu (aprox. 550px acima) até o solo (x, y)
        double curX = x + (RAND.nextDouble() - 0.5) * 60.0;
        double curY = y - 560.0;
        mainBoltPoints.add(new double[]{curX, curY});

        int segments = 14;
        double dy = 560.0 / segments;

        for (int i = 1; i < segments; i++) {
            curY += dy;
            // Tendência de ir em direção ao alvo com zigue-zague
            double targetWeight = (double) i / segments;
            double idealX = mainBoltPoints.get(0)[0] + (x - mainBoltPoints.get(0)[0]) * targetWeight;
            curX = idealX + (RAND.nextDouble() - 0.5) * 36.0;
            mainBoltPoints.add(new double[]{curX, curY});

            // 35% de chance de gerar um galho elétrico bifurcado
            if (RAND.nextDouble() < 0.35 && i < segments - 2) {
                List<double[]> branch = new ArrayList<>();
                branch.add(new double[]{curX, curY});
                double bX = curX;
                double bY = curY;
                double bDir = RAND.nextBoolean() ? 1.0 : -1.0;
                int branchSegments = 3 + RAND.nextInt(3);
                for (int b = 0; b < branchSegments; b++) {
                    bX += bDir * (12.0 + RAND.nextDouble() * 18.0);
                    bY += dy * 0.7 + (RAND.nextDouble() - 0.3) * 12.0;
                    branch.add(new double[]{bX, bY});
                }
                branchBolts.add(branch);
            }
        }
        mainBoltPoints.add(new double[]{x, y});
    }

    private void applyDamageAndEffects(World world) {
        if (damageApplied) return;
        damageApplied = true;

        double impactRadius = 46.0;

        // 1. Flash eletrizante na tela
        world.triggerLightningFlash(0.92);

        // 2. Som de trovão e impacto elétrico
        Player player = world.getPlayer();
        double pan = 0.0;
        if (player != null) {
            double dx = x - player.getX();
            pan = Math.max(-1.0, Math.min(1.0, dx / 400.0));
        }
        AudioEngine.get().play(SoundId.THUNDER, 1.0, pan, 0.95 + RAND.nextDouble() * 0.15);

        // 3. Dano no jogador (2 corações de dano!)
        if (player != null) {
            double pDist = Math.hypot(player.getX() - x, player.getY() - y);
            if (pDist <= impactRadius) {
                player.takeDamage(x, y, 350.0, 2);
                if (player.getLives() > 0) {
                    // Sobreviveu ao impacto direto de um raio eletrizante!
                    br.edu.unex.sentinela.progress.GameEventBus.get().post(
                        new br.edu.unex.sentinela.progress.GameEvent(br.edu.unex.sentinela.progress.GameEvent.Type.LIGHTNING_DODGED)
                    );
                }
            } else if (playerWasInDangerZone) {
                // Jogador estava na mira do raio durante o aviso e escapou a tempo!
                br.edu.unex.sentinela.progress.GameEventBus.get().post(
                    new br.edu.unex.sentinela.progress.GameEvent(br.edu.unex.sentinela.progress.GameEvent.Type.LIGHTNING_DODGED)
                );
            }
        }

        // 4. Dano em Inimigos (predadores, caçadores, etc.)
        for (Enemy enemy : world.getEnemies()) {
            if (enemy.getHp() <= 0) continue;
            double eDist = Math.hypot(enemy.getX() - x, enemy.getY() - y);
            if (eDist <= impactRadius) {
                enemy.takeDamage(3, x, y);
            }
        }

        // 5. Dano em Animais Silvestres / Presas
        for (PassiveAnimal animal : world.getPassiveAnimals()) {
            if (animal.isDead()) continue;
            double aDist = Math.hypot(animal.getX() - x, animal.getY() - y);
            if (aDist <= impactRadius) {
                animal.takeDamage(2);
            }
        }
    }

    public boolean isWarning() {
        return stage == Stage.WARNING;
    }

    public boolean isStriking() {
        return stage == Stage.STRIKING;
    }

    public boolean isLingering() {
        return stage == Stage.LINGERING;
    }

    public boolean isDead() {
        return stage == Stage.DEAD;
    }

    public Stage getStage() {
        return stage;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWarningProgress() {
        if (stage != Stage.WARNING) return 1.0;
        return 1.0 - Math.max(0.0, timer / WARNING_DURATION);
    }

    public double getLingerProgress() {
        if (stage != Stage.LINGERING) return 0.0;
        return Math.max(0.0, timer / LINGER_DURATION);
    }

    public List<double[]> getMainBoltPoints() {
        return mainBoltPoints;
    }

    public List<List<double[]>> getBranchBolts() {
        return branchBolts;
    }

    public List<double[]> getSparkOffsets() {
        return sparkOffsets;
    }
}

