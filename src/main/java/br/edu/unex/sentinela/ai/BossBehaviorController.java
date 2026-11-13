package br.edu.unex.sentinela.ai;

import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;

import java.util.List;
import java.util.Random;

/**
 * Controlador modular de comportamento dos Chefes (Urso, Pantera, Crocodilo):
 * - Gerencia âncora territorial fixa (homeX, homeY), patrulha confinada ao domínio e limite de perseguição (leash).
 * - Coordena estados expressivos: rugido de intimidação (ROARING), fúria (< 50% HP) e recuo ao domínio (RETREATING).
 * - Alterna ataques primários e especiais por arquétipo e expõe parâmetros de animação expressiva ao renderer.
 */
public class BossBehaviorController {
    private static final Random RAND = new Random();

    private final BossArchetype archetype;
    private final double homeX;
    private final double homeY;
    private final double speedMultiplier;

    private boolean retreating = false;
    private boolean engagedInCombat = false;
    private double retreatReaggroCooldown = 0.0;
    private double provokedTimer = 0.0;

    private double roarTimer = 0.0;
    private double roarTotalDuration = 0.5;
    private double roarFocusX;
    private double roarFocusY;
    private boolean pendingRoarShake = false;

    private boolean enraged = false;
    private boolean rageRoarTriggered = false;

    private BossAttackType activeAttack;
    private double specialCooldownTimer = 1.6;
    private double windupTotalDuration = 0.45;
    private double activeTotalDuration = 0.30;
    private double activeElapsed = 0.0;
    private double windupRemaining = 0.0;

    private boolean firstBurstVfxDone = false;
    private boolean hitLandedFirst = false;
    private boolean hitLandedSecond = false;
    private double secondSlashTimer = 0.0;

    public BossBehaviorController(BossArchetype archetype, double homeX, double homeY, double speedMultiplier) {
        this.archetype = archetype;
        this.homeX = homeX;
        this.homeY = homeY;
        this.speedMultiplier = speedMultiplier;
        this.activeAttack = archetype.getPrimaryAttack();
        this.roarFocusX = homeX;
        this.roarFocusY = homeY;
    }

    /** Atualiza temporizadores de fúria, rugido, recuo e progresso do ataque ativo. */
    public void tickTimers(double deltaTime, int hp, int maxHp, boolean isWindup, double windupLeft, boolean isLunging) {
        if (retreatReaggroCooldown > 0) retreatReaggroCooldown = Math.max(0.0, retreatReaggroCooldown - deltaTime);
        if (provokedTimer > 0) provokedTimer = Math.max(0.0, provokedTimer - deltaTime);
        if (specialCooldownTimer > 0) specialCooldownTimer = Math.max(0.0, specialCooldownTimer - deltaTime);
        if (roarTimer > 0) roarTimer = Math.max(0.0, roarTimer - deltaTime);
        if (secondSlashTimer > 0) secondSlashTimer = Math.max(0.0, secondSlashTimer - deltaTime);

        this.windupRemaining = isWindup ? windupLeft : 0.0;
        if (isLunging) {
            this.activeElapsed += deltaTime;
        } else {
            this.activeElapsed = 0.0;
        }

        if (!enraged && hp > 0 && hp <= Math.max(1, (int) Math.floor(maxHp * 0.5))) {
            this.enraged = true;
        }
    }

    /** Notifica provocação por dano do jogador, permitindo contra-ataque e entrada em fúria. */
    public void onDamaged(int hp, int maxHp, double attackerX, double attackerY, double selfX, double selfY) {
        this.provokedTimer = 4.2;
        if (hp > 0 && hp <= Math.max(1, (int) Math.floor(maxHp * 0.5))) {
            this.enraged = true;
            if (!rageRoarTriggered) {
                this.rageRoarTriggered = true;
                startRoar(archetype.getRoarDuration() * 1.15, attackerX, attackerY);
            }
        }
        double distHome = Math.hypot(selfX - homeX, selfY - homeY);
        if (retreating && distHome < archetype.getLeashRadius() * 0.88) {
            this.retreating = false;
            this.retreatReaggroCooldown = 0.0;
        }
    }

    /** Decide o estado da IA do chefe considerando território, limite de perseguição (leash) e rugidos. */
    public BossTickDecision evaluateBehavior(Enemy self, Player player, List<Enemy> allEnemies, TileMap tileMap,
                                             int curCol, int curRow, double deltaTime) {
        if (roarTimer > 0) {
            double targetAngle = Math.atan2(roarFocusY - self.getY(), roarFocusX - self.getX());
            return BossTickDecision.roar(targetAngle);
        }

        if (enraged && !rageRoarTriggered) {
            rageRoarTriggered = true;
            startRoar(archetype.getRoarDuration() * 1.15, player.getX(), player.getY());
            double targetAngle = Math.atan2(player.getY() - self.getY(), player.getX() - self.getX());
            return BossTickDecision.roar(targetAngle);
        }

        double distHome = Math.hypot(self.getX() - homeX, self.getY() - homeY);
        double playerDistHome = Math.hypot(player.getX() - homeX, player.getY() - homeY);
        double distToPlayer = Math.hypot(player.getX() - self.getX(), player.getY() - self.getY());
        boolean wasHostile = engagedInCombat || self.getState() == AIState.CHASE || self.getState() == AIState.ROARING;

        boolean brokeLeash = distHome > archetype.getLeashRadius() && playerDistHome > archetype.getLeashRadius() * 1.02;
        boolean targetHidOutsideCore = wasHostile && player.isBurrowed() && distHome > archetype.getTerritoryRadius() * 0.65;
        if (!retreating && (brokeLeash || targetHidOutsideCore)) {
            enterRetreatState(self);
        }

        if (retreating) {
            boolean enteredCore = playerDistHome <= archetype.getTerritoryRadius() * 0.72 && !player.isBurrowed();
            if (distHome <= archetype.getTerritoryRadius() * 0.45) {
                retreating = false;
                engagedInCombat = false;
            } else if ((provokedTimer > 0 || enteredCore) && retreatReaggroCooldown <= 0.0 && distHome < archetype.getLeashRadius() * 0.88) {
                retreating = false;
            } else {
                int homeCol = (int) Math.floor(homeX / TileMap.TILE_SIZE);
                int homeRow = (int) Math.floor(homeY / TileMap.TILE_SIZE);
                return BossTickDecision.moveTo(AIState.RETREATING, homeCol, homeRow);
            }
        }

        boolean insideTerritory = playerDistHome <= archetype.getTerritoryRadius() * 1.08;
        boolean insideLeashWhileChasing = wasHostile && playerDistHome <= archetype.getLeashRadius();
        boolean closeAggro = distToPlayer <= archetype.getAggroRadius() && distHome <= archetype.getLeashRadius() && retreatReaggroCooldown <= 0;
        boolean provokedInDomain = provokedTimer > 0 && playerDistHome <= archetype.getLeashRadius();

        if (!player.isBurrowed() && (insideTerritory || insideLeashWhileChasing || closeAggro || provokedInDomain)) {
            if (!wasHostile && retreatReaggroCooldown <= 0) {
                engagedInCombat = true;
                startRoar(archetype.getRoarDuration(), player.getX(), player.getY());
                double targetAngle = Math.atan2(player.getY() - self.getY(), player.getX() - self.getX());
                return BossTickDecision.roar(targetAngle);
            }
            engagedInCombat = true;
            int pCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
            int pRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);
            return BossTickDecision.moveTo(AIState.CHASE, pCol, pRow);
        }

        engagedInCombat = false;
        Enemy rival = findNearbyRival(self, allEnemies);
        if (rival != null) {
            int rCol = (int) Math.floor(rival.getX() / TileMap.TILE_SIZE);
            int rRow = (int) Math.floor(rival.getY() / TileMap.TILE_SIZE);
            return BossTickDecision.fightingEnemy(rival, rCol, rRow);
        }

        return BossTickDecision.patrol();
    }

    /** Escolhe um ponto caminhável dentro do território ancorado do chefe para patrulha orgânica. */
    public int[] pickTerritoryPatrolTile(TileMap tileMap, int curCol, int curRow) {
        double maxPatrolRadiusPx = archetype.getTerritoryRadius() * 0.88;
        int homeCol = (int) Math.floor(homeX / TileMap.TILE_SIZE);
        int homeRow = (int) Math.floor(homeY / TileMap.TILE_SIZE);
        double distFromHome = Math.hypot(curCol - homeCol, curRow - homeRow) * TileMap.TILE_SIZE;

        // Se saiu um pouco da zona interna ao patrulhar, prioriza retornar para dentro do domínio
        if (distFromHome > maxPatrolRadiusPx) {
            int[] backHome = Pathfinder.findNearestWalkableTile(tileMap, homeCol, homeRow, 3);
            if (backHome != null) return backHome;
        }

        double maxRadiusTiles = maxPatrolRadiusPx / TileMap.TILE_SIZE;
        for (int tries = 0; tries < 22; tries++) {
            double angle = RAND.nextDouble() * Math.PI * 2.0;
            double radius = (0.20 + RAND.nextDouble() * 0.75) * maxRadiusTiles;
            int candC = homeCol + (int) Math.round(Math.cos(angle) * radius);
            int candR = homeRow + (int) Math.round(Math.sin(angle) * radius);
            if (tileMap.getTileAt(candC, candR).isWalkable()) {
                return new int[]{candC, candR};
            }
        }
        return null;
    }

    /** Tenta iniciar o ataque primário ou especial apropriado à distância e linha de visão atual. */
    public BossAttackType selectAttackForPlayer(Enemy self, Player player, TileMap tileMap, double distToPlayer) {
        if (retreating || roarTimer > 0 || self.isWindup() || self.isLunging() || self.getAttackCooldown() > 0) {
            return null;
        }

        BossAttackType special = archetype.getSpecialAttack();
        if (specialCooldownTimer <= 0 && distToPlayer <= special.getTriggerRange() && distToPlayer >= archetype.getSpecialMinRange()) {
            boolean corridorClear = special != BossAttackType.PANTHER_SHADOW_POUNCE
                    || Pathfinder.hasClearWorldCorridor(tileMap, self.getX(), self.getY(), player.getX(), player.getY(), false);
            if (corridorClear) {
                specialCooldownTimer = archetype.getSpecialCooldown() * (enraged ? 0.78 : 1.0);
                prepareAttackState(special);
                return special;
            }
        }

        BossAttackType primary = archetype.getPrimaryAttack();
        if (distToPlayer <= primary.getTriggerRange()) {
            prepareAttackState(primary);
            return primary;
        }
        return null;
    }

    /** Prepara duração ajustada do windup (ligeiramente mais ágil em estado de fúria). */
    public double getScaledWindupDuration(BossAttackType attack) {
        return attack.getWindupDuration() * (enraged ? 0.88 : 1.0);
    }

    public void beginActiveStrike() {
        this.activeTotalDuration = activeAttack.getActiveDuration();
        this.activeElapsed = 0.0;
        this.firstBurstVfxDone = false;
        this.hitLandedFirst = false;
        this.hitLandedSecond = false;
        if (activeAttack.isDoubleHit()) {
            this.secondSlashTimer = activeAttack.getActiveDuration() * 0.50;
        }
    }

    /** Calcula a velocidade real do chefe combinando estado mental, fúria e terreno da espécie. */
    public double computeCurrentSpeed(AIState state, TileType tile, double duskBoost) {
        if (state == AIState.ROARING || roarTimer > 0) return 0.0;
        double base;
        if (state == AIState.RETREATING || retreating) {
            base = archetype.getChaseSpeed() * 1.12;
        } else if (state == AIState.CHASE || state == AIState.FIGHTING_ENEMY) {
            base = enraged ? archetype.getEnragedSpeed() : archetype.getChaseSpeed();
        } else {
            base = archetype.getPatrolSpeed();
        }
        return base * speedMultiplier * duskBoost * archetype.computeTerrainSpeedMultiplier(tile);
    }

    public boolean consumePendingRoarShake() {
        if (pendingRoarShake) {
            pendingRoarShake = false;
            return true;
        }
        return false;
    }

    private void prepareAttackState(BossAttackType chosen) {
        this.activeAttack = chosen;
        this.windupTotalDuration = getScaledWindupDuration(chosen);
        this.windupRemaining = this.windupTotalDuration;
        this.firstBurstVfxDone = false;
        this.hitLandedFirst = false;
        this.hitLandedSecond = false;
    }

    private void startRoar(double duration, double focusX, double focusY) {
        this.roarTotalDuration = Math.max(0.2, duration);
        this.roarTimer = this.roarTotalDuration;
        this.roarFocusX = focusX;
        this.roarFocusY = focusY;
        this.pendingRoarShake = true;
    }

    private void enterRetreatState(Enemy self) {
        this.retreating = true;
        this.engagedInCombat = false;
        this.retreatReaggroCooldown = 1.25;
        self.cancelActiveAttackForRetreat();
    }

    private Enemy findNearbyRival(Enemy self, List<Enemy> allEnemies) {
        Enemy nearest = null;
        double minEnemyDist = 220.0;
        for (Enemy other : allEnemies) {
            if (other != self && other.getHp() > 0 && other.getType() != EnemyType.BEAR_BOSS) {
                double d = Math.hypot(other.getX() - self.getX(), other.getY() - self.getY());
                double otherHomeDist = Math.hypot(other.getX() - homeX, other.getY() - homeY);
                if (d < minEnemyDist && otherHomeDist <= archetype.getLeashRadius() * 0.90) {
                    minEnemyDist = d;
                    nearest = other;
                }
            }
        }
        return nearest;
    }

    // --- Telemetria de Expressão & Animação para Renderizadores ---

    public double getWindupProgress() {
        if (windupTotalDuration <= 0.001 || windupRemaining <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, 1.0 - (windupRemaining / windupTotalDuration)));
    }

    public double getActiveProgress() {
        if (activeTotalDuration <= 0.001 || activeElapsed <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, activeElapsed / activeTotalDuration));
    }

    public double getRoarProgress() {
        if (roarTimer <= 0 || roarTotalDuration <= 0.001) return 0.0;
        return Math.max(0.0, Math.min(1.0, 1.0 - (roarTimer / roarTotalDuration)));
    }

    /** Abertura expressiva de mandíbula/boca (0 = fechada, 1 = escancarada). */
    public double getMouthOpenRatio() {
        if (roarTimer > 0) {
            double pulse = Math.sin(getRoarProgress() * Math.PI);
            return Math.min(1.0, 0.55 + pulse * 0.45);
        }
        if (windupRemaining > 0) {
            return 0.35 + getWindupProgress() * 0.65;
        }
        if (activeElapsed > 0) {
            return Math.max(0.15, 1.0 - getActiveProgress() * 0.85);
        }
        if (enraged && !retreating) return 0.25;
        return 0.0;
    }

    /** Deformação longitudinal (stretch/agachamento) específica da espécie durante preparo e bote. */
    public double getBodyScaleX(boolean lunging) {
        if (roarTimer > 0) return 1.06;
        if (windupRemaining > 0) {
            double p = getWindupProgress();
            if (activeAttack == BossAttackType.PANTHER_SHADOW_POUNCE) return 1.0 - p * 0.18;
            if (activeAttack == BossAttackType.BEAR_SEISMIC_SLAM) return 1.0 + p * 0.14;
        }
        if (lunging && activeAttack == BossAttackType.PANTHER_SHADOW_POUNCE) return 1.24;
        return 1.0;
    }

    /** Deformação transversal do tronco (ex.: urso se erguendo / pantera acumulando impulso). */
    public double getBodyScaleY(boolean lunging) {
        if (roarTimer > 0) return 1.09;
        if (windupRemaining > 0) {
            double p = getWindupProgress();
            if (activeAttack == BossAttackType.PANTHER_SHADOW_POUNCE) return 1.0 + p * 0.14;
            if (activeAttack == BossAttackType.BEAR_SEISMIC_SLAM) return 1.0 + p * 0.20;
        }
        if (lunging && activeAttack == BossAttackType.PANTHER_SHADOW_POUNCE) return 0.86;
        if (lunging && activeAttack == BossAttackType.BEAR_SEISMIC_SLAM) return 1.08;
        return 1.0;
    }

    /** Rotação extra do corpo para o Giro da Morte do Crocodilo. */
    public double getRollAngleOffsetDeg(boolean lunging) {
        if (lunging && activeAttack == BossAttackType.CROC_DEATH_ROLL) {
            return getActiveProgress() * 540.0;
        }
        return 0.0;
    }

    public boolean isNearLeashLimit(double selfX, double selfY) {
        return Math.hypot(selfX - homeX, selfY - homeY) >= archetype.getLeashRadius() * 0.72;
    }

    public BossArchetype getArchetype() { return archetype; }
    public double getHomeX() { return homeX; }
    public double getHomeY() { return homeY; }
    public boolean isRetreating() { return retreating; }
    public boolean isRoaring() { return roarTimer > 0; }
    public boolean isEnraged() { return enraged; }
    public BossAttackType getActiveAttack() { return activeAttack; }
    public boolean isFirstBurstVfxDone() { return firstBurstVfxDone; }
    public void markFirstBurstVfxDone() { this.firstBurstVfxDone = true; }
    public boolean isHitLandedFirst() { return hitLandedFirst; }
    public void markHitLandedFirst() { this.hitLandedFirst = true; }
    public boolean isHitLandedSecond() { return hitLandedSecond; }
    public void markHitLandedSecond() { this.hitLandedSecond = true; }
    public boolean isSecondSlashWindowReady() { return secondSlashTimer <= 0; }

    /** Resultado compacto da avaliação de turno do comportamento do chefe. */
    public static final class BossTickDecision {
        public final AIState state;
        public final boolean stopMovement;
        public final Double lockFaceAngle;
        public final int targetCol;
        public final int targetRow;
        public final Enemy rivalTarget;

        private BossTickDecision(AIState state, boolean stopMovement, Double lockFaceAngle,
                                 int targetCol, int targetRow, Enemy rivalTarget) {
            this.state = state;
            this.stopMovement = stopMovement;
            this.lockFaceAngle = lockFaceAngle;
            this.targetCol = targetCol;
            this.targetRow = targetRow;
            this.rivalTarget = rivalTarget;
        }

        static BossTickDecision roar(double faceAngle) {
            return new BossTickDecision(AIState.ROARING, true, faceAngle, -1, -1, null);
        }

        static BossTickDecision moveTo(AIState state, int col, int row) {
            return new BossTickDecision(state, false, null, col, row, null);
        }

        static BossTickDecision fightingEnemy(Enemy rival, int col, int row) {
            return new BossTickDecision(AIState.FIGHTING_ENEMY, false, null, col, row, rival);
        }

        static BossTickDecision patrol() {
            return new BossTickDecision(AIState.PATROL, false, null, -1, -1, null);
        }
    }
}
