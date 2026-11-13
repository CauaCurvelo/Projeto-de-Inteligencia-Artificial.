package br.edu.unex.sentinela.audio;

import br.edu.unex.sentinela.world.BiomeType;
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
    public AmbienceController() {}

    private static final SoundId[] LOOPS = {
        SoundId.LOOP_WIND, SoundId.LOOP_FOREST, SoundId.LOOP_NIGHT,
        SoundId.LOOP_RIVER, SoundId.LOOP_RAIN, SoundId.LOOP_STORM_WIND
    };
    private final Map<SoundId, Voice> loops = new EnumMap<>(SoundId.class);
    private final Random rand = new Random();
    private double eventTimer = 2.0, thunderTimer = 8.0, scanTimer = 0;
    private double waterRatio, forestRatio;
    private BiomeType currentBiome = BiomeType.MEADOW;

    public void update(World world, SpatialAudio spatial, double dt) {
        AudioEngine engine = AudioEngine.get();
        if (!engine.isReady()) return;
        if (loops.isEmpty()) for (SoundId id : LOOPS) loops.put(id, engine.loop(id));

        scanTimer -= dt;
        if (scanTimer <= 0) { scanSurroundings(world); scanTimer = 0.5; }

        double night = Math.sin(world.getCycleProgressTimer() / world.getCycleDuration() * Math.PI);
        double day = 1 - night;
        double rainIntensity = world.getRainIntensity();
        double fogIntensity = world.getFogIntensity();
        boolean burrowed = world.getPlayer().isBurrowed();
        double muffle = burrowed ? 0.35 : 1.0;

        applyLoopMix(day, night, rainIntensity, fogIntensity, muffle);
        scheduleEvents(world, spatial, dt, night, rainIntensity, muffle);
    }

    /** Atenua suavemente as camadas em pausa/configs da partida e silencia no menu principal. */
    public void updateMenuOrPaused(World world, boolean inMatchMenu) {
        if (loops.isEmpty()) return;
        if (!inMatchMenu || world == null || world.getPlayer() == null) {
            for (SoundId id : LOOPS) setGain(id, 0);
            return;
        }
        double night = Math.sin(world.getCycleProgressTimer() / world.getCycleDuration() * Math.PI);
        double day = 1 - night;
        double rainIntensity = world.getRainIntensity();
        double fogIntensity = world.getFogIntensity();
        double muffle = (world.getPlayer().isBurrowed() ? 0.35 : 1.0) * 0.25;
        applyLoopMix(day, night, rainIntensity, fogIntensity, muffle);
    }

    private void applyLoopMix(double day, double night, double rainIntensity, double fogIntensity, double muffle) {
        double windBias = (currentBiome == BiomeType.HIGHLAND) ? 0.14 : (currentBiome == BiomeType.SNOW ? 0.19 : 0.0);
        double forestBias = (currentBiome == BiomeType.DENSE_FOREST) ? 0.16 : 0.0;
        double swampWaterBias = (currentBiome == BiomeType.SWAMP) ? 0.12 : 0.0;

        setGain(SoundId.LOOP_WIND, (0.18 + windBias + 0.10 * (1 - forestRatio)) * muffle);
        setGain(SoundId.LOOP_FOREST, Math.min(1.0, forestRatio + forestBias) * 0.38 * day * muffle * (1.0 - rainIntensity * 0.55));
        double nightSuppress = (currentBiome == BiomeType.SWAMP ? 0.25 : (currentBiome == BiomeType.SNOW ? 0.72 : 0.35));
        setGain(SoundId.LOOP_NIGHT, Math.max(0, night - nightSuppress) * 0.69 * muffle * (1.0 - rainIntensity * 0.5));
        setGain(SoundId.LOOP_RIVER, Math.min(1, waterRatio * 2.5 + swampWaterBias) * 0.45 * muffle);
        setGain(SoundId.LOOP_RAIN, (currentBiome != BiomeType.SNOW) ? 0.50 * rainIntensity * muffle : 0);
        setGain(SoundId.LOOP_STORM_WIND, (rainIntensity * 0.22 + fogIntensity * 0.16) * muffle);
    }

    private void scheduleEvents(World world, SpatialAudio sp, double dt, double night, double rainIntensity, double muffle) {
        double px = world.getPlayer().getX(), py = world.getPlayer().getY();
        eventTimer -= dt;
        if (eventTimer <= 0) {
            eventTimer = 2.5 + rand.nextDouble() * 5.0;
            double ox = px + (rand.nextDouble() - 0.5) * 500, oy = py + (rand.nextDouble() - 0.5) * 500;
            double pitch = 0.9 + rand.nextDouble() * 0.25;
            if (night > 0.6) {
                sp.playAt(rand.nextInt(3) == 0 ? SoundId.OWL_HOOT : SoundId.CRICKET, ox, oy, 0.42 * muffle, pitch);
            } else if (rainIntensity < 0.35 && night < 0.45) {
                sp.playAt(SoundId.BIRD_CHIRP, ox, oy, (0.22 + 0.28 * forestRatio) * muffle, pitch);
            }
        }
        if (rainIntensity > 0.35) {
            thunderTimer -= dt;
            if (thunderTimer <= 0) {
                thunderTimer = 11 + rand.nextDouble() * 16;
                AudioEngine.get().play(SoundId.THUNDER, 0.8 * muffle * rainIntensity, rand.nextDouble() * 1.2 - 0.6, 0.8 + rand.nextDouble() * 0.3);
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
        currentBiome = map.getBiomeAt(pc, pr);
    }

    private void setGain(SoundId id, double g) {
        Voice v = loops.get(id);
        if (v != null) {
            double chVol = AudioEngine.get().getChannelVolume(id.getChannel());
            v.targetGain = Math.max(0, g * chVol);
        }
    }
}
