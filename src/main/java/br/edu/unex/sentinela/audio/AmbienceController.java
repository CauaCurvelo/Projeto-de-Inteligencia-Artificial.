package br.edu.unex.sentinela.audio;

import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import br.edu.unex.sentinela.world.WeatherType;
import br.edu.unex.sentinela.world.World;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Paisagem sonora: faz crossfade dos loops conforme bioma ao redor, ciclo dia/noite e clima,
 * e dispara eventos pontuais (pássaros, coruja, grilos, trovão).
 */
public final class AmbienceController {
    private static final SoundId[] LOOPS = {
        SoundId.LOOP_WIND, SoundId.LOOP_FOREST, SoundId.LOOP_NIGHT,
        SoundId.LOOP_RIVER, SoundId.LOOP_RAIN, SoundId.LOOP_STORM_WIND
    };
    private final Map<SoundId, Voice> loops = new EnumMap<>(SoundId.class);
    private final Random rand = new Random();
    private double eventTimer = 2.0, thunderTimer = 8.0, scanTimer = 0;
    private double waterRatio, forestRatio;

    public void update(World world, SpatialAudio spatial, double dt) {
        AudioEngine engine = AudioEngine.get();
        if (!engine.isReady()) return;
        if (loops.isEmpty()) for (SoundId id : LOOPS) loops.put(id, engine.loop(id));

        scanTimer -= dt;
        if (scanTimer <= 0) { scanSurroundings(world); scanTimer = 0.5; }

        double night = Math.sin(world.getCycleProgressTimer() / world.getCycleDuration() * Math.PI);
        double day = 1 - night;
        WeatherType w = world.getCurrentWeather();
        boolean rain = w == WeatherType.RAIN, fog = w == WeatherType.FOG;
        boolean burrowed = world.getPlayer().isBurrowed();
        double muffle = burrowed ? 0.35 : 1.0;

        setGain(SoundId.LOOP_WIND, (0.35 + 0.25 * (1 - forestRatio)) * muffle);
        setGain(SoundId.LOOP_FOREST, forestRatio * 0.9 * day * muffle);
        setGain(SoundId.LOOP_NIGHT, Math.max(0, night - 0.35) * 1.3 * muffle);
        setGain(SoundId.LOOP_RIVER, Math.min(1, waterRatio * 2.5) * 0.9 * muffle);
        setGain(SoundId.LOOP_RAIN, rain ? 0.9 * muffle : 0);
        setGain(SoundId.LOOP_STORM_WIND, (rain ? 0.45 : fog ? 0.35 : 0) * muffle);

        scheduleEvents(world, spatial, dt, night, rain, muffle);
    }

    private void scheduleEvents(World world, SpatialAudio sp, double dt, double night, boolean rain, double muffle) {
        double px = world.getPlayer().getX(), py = world.getPlayer().getY();
        eventTimer -= dt;
        if (eventTimer <= 0) {
            eventTimer = 1.5 + rand.nextDouble() * 4.0;
            double ox = px + (rand.nextDouble() - 0.5) * 500, oy = py + (rand.nextDouble() - 0.5) * 500;
            double pitch = 0.9 + rand.nextDouble() * 0.25;
            if (night > 0.6) {
                sp.playAt(rand.nextInt(3) == 0 ? SoundId.OWL_HOOT : SoundId.CRICKET, ox, oy, 0.8 * muffle, pitch);
            } else if (!rain && night < 0.45) {
                sp.playAt(SoundId.BIRD_CHIRP, ox, oy, (0.4 + forestRatio) * muffle, pitch);
            }
        }
        if (rain) {
            thunderTimer -= dt;
            if (thunderTimer <= 0) {
                thunderTimer = 9 + rand.nextDouble() * 16;
                AudioEngine.get().play(SoundId.THUNDER, 0.8 * muffle, rand.nextDouble() * 1.2 - 0.6, 0.8 + rand.nextDouble() * 0.3);
            }
        }
    }

    /** Amostra os tiles num raio de ~7 tiles para medir proporção de água e floresta. */
    private void scanSurroundings(World world) {
        TileMap map = world.getTileMap();
        int pc = (int) Math.floor(world.getPlayer().getX() / TileMap.TILE_SIZE);
        int pr = (int) Math.floor(world.getPlayer().getY() / TileMap.TILE_SIZE);
        int water = 0, forest = 0, total = 0;
        for (int dr = -7; dr <= 7; dr += 2) {
            for (int dc = -7; dc <= 7; dc += 2) {
                TileType t = map.getTileAt(pc + dc, pr + dr);
                if (t == TileType.WATER) water++;
                else if (t == TileType.TREE || t == TileType.TALL_GRASS) forest++;
                total++;
            }
        }
        waterRatio = (double) water / total;
        forestRatio = (double) forest / total;
    }

    private void setGain(SoundId id, double g) {
        Voice v = loops.get(id);
        if (v != null) {
            double chVol = AudioEngine.get().getChannelVolume(id.getChannel());
            v.targetGain = Math.max(0, g * chVol);
        }
    }
}
