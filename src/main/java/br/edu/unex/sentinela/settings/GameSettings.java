package br.edu.unex.sentinela.settings;

import br.edu.unex.sentinela.audio.AudioChannel;
import br.edu.unex.sentinela.audio.AudioEngine;

import java.io.*;
import java.util.Properties;

/**
 * Singleton responsável pelas preferências do jogador (gráficos, controles e áudio)
 * com persistência automática em arquivo de propriedades.
 */
public final class GameSettings {
    private static final String SETTINGS_FILE = "settings.properties";
    private static final GameSettings INSTANCE = new GameSettings();

    private final KeyBindings keyBindings = new KeyBindings();

    // Gráficos
    private GraphicsQuality graphicsQuality = GraphicsQuality.HIGH;
    private int targetFps = 60;
    private boolean showVisionCones = true;
    private boolean showPathTrails = true;
    private boolean showNameplates = true;
    private boolean screenShake = true;
    private boolean fullscreen = false;

    // Áudio
    private double masterVolume = 0.8;
    private double sfxVolume = 1.0;
    private double stepsVolume = 0.8;
    private double creaturesVolume = 0.9;
    private double ambienceVolume = 0.7;
    private double weatherVolume = 0.8;
    private double uiVolume = 0.9;

    private GameSettings() {
        load();
        applyAudioSettings();
    }

    public static GameSettings get() {
        return INSTANCE;
    }

    public KeyBindings getKeyBindings() {
        return keyBindings;
    }

    public GraphicsQuality getGraphicsQuality() { return graphicsQuality; }
    public void setGraphicsQuality(GraphicsQuality q) { this.graphicsQuality = q; save(); }

    public int getTargetFps() { return targetFps; }
    public void setTargetFps(int fps) { this.targetFps = fps; save(); }

    public boolean isShowVisionCones() { return showVisionCones; }
    public void setShowVisionCones(boolean show) { this.showVisionCones = show; save(); }

    public boolean isShowPathTrails() { return showPathTrails; }
    public void setShowPathTrails(boolean show) { this.showPathTrails = show; save(); }

    public boolean isShowNameplates() { return showNameplates; }
    public void setShowNameplates(boolean show) { this.showNameplates = show; save(); }

    public boolean isScreenShake() { return screenShake; }
    public void setScreenShake(boolean shake) { this.screenShake = shake; save(); }

    public boolean isFullscreen() { return fullscreen; }
    public void setFullscreen(boolean fs) { this.fullscreen = fs; save(); }

    public double getMasterVolume() { return masterVolume; }
    public void setMasterVolume(double v) {
        this.masterVolume = Math.max(0, Math.min(1.0, v));
        AudioEngine.get().setMasterVolume(this.masterVolume);
        save();
    }

    public double getChannelVolume(AudioChannel ch) {
        switch (ch) {
            case MASTER: return masterVolume;
            case SFX: return sfxVolume;
            case STEPS: return stepsVolume;
            case CREATURES: return creaturesVolume;
            case AMBIENCE: return ambienceVolume;
            case WEATHER: return weatherVolume;
            case UI: return uiVolume;
            default: return 1.0;
        }
    }

    public void setChannelVolume(AudioChannel ch, double volume) {
        double v = Math.max(0, Math.min(1.0, volume));
        switch (ch) {
            case MASTER: setMasterVolume(v); return;
            case SFX: sfxVolume = v; break;
            case STEPS: stepsVolume = v; break;
            case CREATURES: creaturesVolume = v; break;
            case AMBIENCE: ambienceVolume = v; break;
            case WEATHER: weatherVolume = v; break;
            case UI: uiVolume = v; break;
        }
        AudioEngine.get().setChannelVolume(ch, v);
        save();
    }

    public void applyAudioSettings() {
        AudioEngine engine = AudioEngine.get();
        engine.setMasterVolume(masterVolume);
        engine.setChannelVolume(AudioChannel.SFX, sfxVolume);
        engine.setChannelVolume(AudioChannel.STEPS, stepsVolume);
        engine.setChannelVolume(AudioChannel.CREATURES, creaturesVolume);
        engine.setChannelVolume(AudioChannel.AMBIENCE, ambienceVolume);
        engine.setChannelVolume(AudioChannel.WEATHER, weatherVolume);
        engine.setChannelVolume(AudioChannel.UI, uiVolume);
    }

    public void load() {
        File f = new File(SETTINGS_FILE);
        if (!f.exists()) return;
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(f)) {
            props.load(in);
            keyBindings.loadFromProperties(props);
            graphicsQuality = GraphicsQuality.valueOf(props.getProperty("gfx.quality", GraphicsQuality.HIGH.name()));
            targetFps = Integer.parseInt(props.getProperty("gfx.fps", "60"));
            showVisionCones = Boolean.parseBoolean(props.getProperty("gfx.visionCones", "true"));
            showPathTrails = Boolean.parseBoolean(props.getProperty("gfx.pathTrails", "true"));
            showNameplates = Boolean.parseBoolean(props.getProperty("gfx.nameplates", "true"));
            screenShake = Boolean.parseBoolean(props.getProperty("gfx.screenShake", "true"));
            fullscreen = Boolean.parseBoolean(props.getProperty("gfx.fullscreen", "false"));

            masterVolume = Double.parseDouble(props.getProperty("audio.master", "0.8"));
            sfxVolume = Double.parseDouble(props.getProperty("audio.sfx", "1.0"));
            stepsVolume = Double.parseDouble(props.getProperty("audio.steps", "0.8"));
            creaturesVolume = Double.parseDouble(props.getProperty("audio.creatures", "0.9"));
            ambienceVolume = Double.parseDouble(props.getProperty("audio.ambience", "0.7"));
            weatherVolume = Double.parseDouble(props.getProperty("audio.weather", "0.8"));
            uiVolume = Double.parseDouble(props.getProperty("audio.ui", "0.9"));
        } catch (Exception ignored) {}
    }

    public void save() {
        Properties props = new Properties();
        keyBindings.saveToProperties(props);
        props.setProperty("gfx.quality", graphicsQuality.name());
        props.setProperty("gfx.fps", String.valueOf(targetFps));
        props.setProperty("gfx.visionCones", String.valueOf(showVisionCones));
        props.setProperty("gfx.pathTrails", String.valueOf(showPathTrails));
        props.setProperty("gfx.nameplates", String.valueOf(showNameplates));
        props.setProperty("gfx.screenShake", String.valueOf(screenShake));
        props.setProperty("gfx.fullscreen", String.valueOf(fullscreen));

        props.setProperty("audio.master", String.valueOf(masterVolume));
        props.setProperty("audio.sfx", String.valueOf(sfxVolume));
        props.setProperty("audio.steps", String.valueOf(stepsVolume));
        props.setProperty("audio.creatures", String.valueOf(creaturesVolume));
        props.setProperty("audio.ambience", String.valueOf(ambienceVolume));
        props.setProperty("audio.weather", String.valueOf(weatherVolume));
        props.setProperty("audio.ui", String.valueOf(uiVolume));

        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            props.store(out, "Instinto Selvagem - Preferencias do Jogador");
        } catch (IOException ignored) {}
    }
}
