package br.edu.unex.sentinela.audio;

import br.edu.unex.sentinela.ai.AIState;
import br.edu.unex.sentinela.entity.Enemy;
import br.edu.unex.sentinela.entity.Player;
import br.edu.unex.sentinela.world.BiomeType;
import br.edu.unex.sentinela.world.Projectile;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import br.edu.unex.sentinela.world.WeatherType;
import br.edu.unex.sentinela.world.World;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Fachada de gameplay do áudio. Observa o estado do World a cada frame (diff de snapshots)
 * e dispara os sons correspondentes, sem acoplar áudio às classes de entidade.
 */
public final class GameAudioDirector {
    private static final double VOICE_COOLDOWN = 1.6;

    private final SpatialAudio spatial = new SpatialAudio();
    private final AmbienceController ambience = new AmbienceController();
    private final MusicController music = new MusicController();
    private final Random rand = new Random();
    private final Map<Enemy, EnemySnapshot> enemySnaps = new IdentityHashMap<>();
    private final Map<Projectile, Boolean> knownProjectiles = new IdentityHashMap<>();

    private Player lastPlayer;
    private int lastLives, lastApples, lastMeats, lastDefeated, lastDay;
    private double lastHunger, heartbeatTimer, hungerTimer;
    private long lastStepIndex;
    private boolean lastAttacking, lastBurrowed, lastGameOver, lastSwimming;
    private WeatherType lastWeather;

    private static final class EnemySnapshot {
        AIState state; boolean windup; long stepIndex; double voiceCooldown; int hp;
    }

    public void update(World world, double dt) {
        if (!AudioEngine.get().isReady()) return;
        Player p = world.getPlayer();
        if (p != lastPlayer) resetSnapshots(world);
        spatial.setListener(p.getX(), p.getY());

        ambience.update(world, spatial, dt);
        music.update(world, dt);
        handleGameOver(world);
        if (world.isGameOver()) return;

        handlePlayer(world, p, dt);
        handleEnemies(world, dt);
        handleProjectiles(world);
        handleWorldEvents(world);
    }

    private void handlePlayer(World world, Player p, double dt) {
        TileType tile = tileAt(world, p.getX(), p.getY());
        BiomeType biome = world.getTileMap().getBiomeAtWorld(p.getX(), p.getY());
        long step = (long) Math.floor(p.getWalkPhase() / Math.PI);
        if (p.isMoving() && step != lastStepIndex && !p.isBurrowed()) {
            double gain = (p.isSprinting() ? 0.68 : 0.42) * (0.94 + rand.nextDouble() * 0.12);
            AudioEngine.get().play(CreatureSoundMap.playerFootstep(tile, biome), gain, 0, 0.96 + rand.nextDouble() * 0.08);
            if (tile == TileType.TALL_GRASS && biome != BiomeType.SNOW && rand.nextInt(3) == 0) {
                AudioEngine.get().play(SoundId.LEAVES_RUSTLE, 0.34, 0, 0.96 + rand.nextDouble() * 0.08);
            }
        }
        lastStepIndex = step;

        if (p.isAttacking() && !lastAttacking) AudioEngine.get().play(SoundId.BITE_WHOOSH, 0.8, 0, 0.95 + rand.nextDouble() * 0.1);
        lastAttacking = p.isAttacking();

        if (p.isBurrowed() != lastBurrowed) AudioEngine.get().play(p.isBurrowed() ? SoundId.BURROW_ENTER : SoundId.BURROW_EXIT, 0.7, 0, 1);
        lastBurrowed = p.isBurrowed();

        if (p.isSwimming() && !lastSwimming) AudioEngine.get().play(SoundId.SPLASH, 0.7, 0, 1);
        lastSwimming = p.isSwimming();

        if (p.getLives() < lastLives) AudioEngine.get().play(SoundId.PLAYER_HURT, 0.9, 0, 1);
        else if (p.getLives() > lastLives) AudioEngine.get().play(SoundId.HEAL, 0.6, 0, 1);
        lastLives = p.getLives();

        if (world.getApplesEaten() > lastApples) {
            AudioEngine.get().play(SoundId.EAT_APPLE, 0.75, 0, 1);
        }
        if (world.getMeatsEaten() > lastMeats) {
            AudioEngine.get().play(SoundId.EAT_MEAT, 0.75, 0, 1);
        }
        lastHunger = p.getHunger();
        lastApples = world.getApplesEaten();
        lastMeats = world.getMeatsEaten();

        updateVitalWarnings(p, dt);
    }

    /** Batimento cardíaco com vida baixa e ronco de estômago com fome baixa. */
    private void updateVitalWarnings(Player p, double dt) {
        heartbeatTimer -= dt;
        if (p.getLives() <= 1 && heartbeatTimer <= 0) {
            AudioEngine.get().play(SoundId.HEARTBEAT, 0.8, 0, 1);
            heartbeatTimer = 0.85;
        }
        hungerTimer -= dt;
        if (p.getHunger() < p.getMaxHunger() * 0.2 && hungerTimer <= 0) {
            AudioEngine.get().play(SoundId.HUNGER_GROWL, 0.6, 0, 0.9 + rand.nextDouble() * 0.2);
            hungerTimer = 7.0;
        }
    }

    private void handleEnemies(World world, double dt) {
        enemySnaps.keySet().retainAll(world.getEnemies());
        for (Enemy e : world.getEnemies()) {
            EnemySnapshot s = enemySnaps.computeIfAbsent(e, k -> snapshotOf(e));
            s.voiceCooldown -= dt;
            double x = e.getX(), y = e.getY();
            double pitch = 0.96 + rand.nextDouble() * 0.08;

            long step = (long) Math.floor(e.getWalkPhase() / Math.PI);
            if (e.isMoving() && step != s.stepIndex) {
                TileType t = tileAt(world, x, y);
                double stepGain = CreatureSoundMap.footstepGain(e.getSkin()) * (0.92 + rand.nextDouble() * 0.16);
                spatial.playAt(CreatureSoundMap.footstep(e.getSkin(), t), x, y, stepGain, pitch);
            }
            s.stepIndex = step;

            if (e.getState() != s.state) onEnemyStateChange(e, s, x, y, pitch);
            if (e.isWindup() && !s.windup && s.voiceCooldown <= 0) {
                spatial.playAt(CreatureSoundMap.attackVoice(e.getSkin()), x, y, 1.0, pitch);
                s.voiceCooldown = VOICE_COOLDOWN;
            }
            s.windup = e.isWindup();

            if (e.getHp() < s.hp) spatial.playAt(SoundId.HIT_IMPACT, x, y, 1.0, pitch);
            s.hp = e.getHp();
        }
    }

    private void onEnemyStateChange(Enemy e, EnemySnapshot s, double x, double y, double pitch) {
        AIState now = e.getState();
        boolean wasHostile = s.state == AIState.CHASE || s.state == AIState.ALERT || s.state == AIState.AIMING || s.state == AIState.ROARING;
        if ((now == AIState.CHASE || now == AIState.ALERT || now == AIState.AIMING || now == AIState.ROARING) && !wasHostile) {
            spatial.playAt(SoundId.ALERT_STINGER, x, y, 0.9, 1);
            if (s.voiceCooldown <= 0 || now == AIState.ROARING) {
                spatial.playAt(CreatureSoundMap.alertVoice(e.getSkin()), x, y, 1.0, pitch);
                s.voiceCooldown = VOICE_COOLDOWN;
            }
        } else if (now == AIState.SNIFFING_TRACK || now == AIState.SUSPICIOUS) {
            spatial.playAt(SoundId.SNIFF, x, y, 0.8, pitch);
        } else if (now == AIState.EATING) {
            spatial.playAt(SoundId.EAT_MEAT, x, y, 0.6, 0.90 + rand.nextDouble() * 0.08);
        } else if (wasHostile && (now == AIState.PATROL || now == AIState.RETREATING)) {
            spatial.playAt(SoundId.LOST_TARGET, x, y, 0.75, 1);
        }
        s.state = now;
    }

    private void handleProjectiles(World world) {
        Map<Projectile, Boolean> current = new IdentityHashMap<>();
        for (Projectile pr : world.getProjectiles()) {
            if (!knownProjectiles.containsKey(pr)) spatial.playAt(SoundId.BOW_SHOT, pr.getX(), pr.getY(), 1.0, 0.95 + rand.nextDouble() * 0.1);
            current.put(pr, Boolean.TRUE);
        }
        knownProjectiles.clear();
        knownProjectiles.putAll(current);
    }

    private void handleWorldEvents(World world) {
        if (world.getEnemiesDefeated() > lastDefeated) AudioEngine.get().play(SoundId.ENEMY_DEATH, 0.8, 0, 1);
        lastDefeated = world.getEnemiesDefeated();
        if (world.getDayCycleNumber() > lastDay) AudioEngine.get().play(SoundId.DAY_CHIME, 0.6, 0, 1);
        lastDay = world.getDayCycleNumber();
        if (world.getCurrentWeather() != lastWeather) AudioEngine.get().play(SoundId.WEATHER_SHIFT, 0.6, 0, 1);
        lastWeather = world.getCurrentWeather();
    }

    private void handleGameOver(World world) {
        if (world.isGameOver() && !lastGameOver) AudioEngine.get().play(SoundId.GAME_OVER, 0.8, 0, 1);
        lastGameOver = world.isGameOver();
    }

    private void resetSnapshots(World world) {
        Player p = world.getPlayer();
        lastPlayer = p;
        lastLives = p.getLives();
        lastHunger = p.getHunger();
        lastApples = world.getApplesEaten();
        lastMeats = world.getMeatsEaten();
        lastDefeated = world.getEnemiesDefeated();
        lastDay = world.getDayCycleNumber();
        lastWeather = world.getCurrentWeather();
        lastGameOver = false;
        music.resetForNewMatch(world);
        enemySnaps.clear();
        knownProjectiles.clear();
        for (Projectile pr : world.getProjectiles()) knownProjectiles.put(pr, Boolean.TRUE);
    }

    private EnemySnapshot snapshotOf(Enemy e) {
        EnemySnapshot s = new EnemySnapshot();
        s.state = e.getState(); s.windup = e.isWindup(); s.hp = e.getHp();
        s.stepIndex = (long) Math.floor(e.getWalkPhase() / Math.PI);
        return s;
    }

    /** Atenua áudio ambiente e música em pausa/configs da partida e silencia no menu principal. */
    public void updateMenuOrPaused(World world, boolean inMatchMenu) {
        if (world != null) {
            ambience.updateMenuOrPaused(world, inMatchMenu);
            music.updateMenuOrPaused(world, inMatchMenu);
        }
    }

    private static TileType tileAt(World w, double x, double y) {
        return w.getTileMap().getTileAt((int) Math.floor(x / TileMap.TILE_SIZE), (int) Math.floor(y / TileMap.TILE_SIZE));
    }
}
