package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.settings.GameSettings;
import br.edu.unex.sentinela.settings.GraphicsQuality;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Sistema dinâmico e contínuo de vento no mundo.
 * O vento possui intensidade e direção suaves, gerando correntes de ar (WindStreaks)
 * e folhas/partículas voadoras (WindLeaves) que rodopiam e reagem ao bioma e ao clima.
 *
 * O vento afeta a chuva, o faro dos predadores, a física do jogador e camufla os passos acusticamente.
 * NENHUM indicador artificial de HUD é utilizado: a direção e intensidade são 100% percebidas
 * pelas animações diegéticas, som e folhas.
 */
public class WindSystem {

    private static final Random RAND = new Random();

    // Estado físico do vento
    private double currentIntensity = 0.20; // 0.0 (calmo) a ~1.5 (tempestade violenta)
    private double targetIntensity = 0.25;
    private double currentAngle = Math.PI * 0.25; // Radianos [0, 2π)
    private double targetAngle = Math.PI * 0.35;

    // Temporizadores de mudança gradual e rajadas periódicas
    private double changeTimer = 18.0;
    private double gustTimer = 8.0;
    private double gustDuration = 0.0;
    private double gustStrength = 0.0;

    // Partículas diegéticas gerenciadas no espaço da câmera
    private final List<WindStreak> streaks = new ArrayList<>();
    private final List<WindLeaf> leaves = new ArrayList<>();

    public WindSystem() {
        // Inicializa com uma brisa suave
        this.currentIntensity = 0.25;
        this.targetIntensity = 0.25;
        this.currentAngle = Math.PI * 0.20;
        this.targetAngle = Math.PI * 0.30;
    }

    /**
     * Atualiza o estado do vento e as partículas voadoras ao redor da viewport.
     */
    public void update(double deltaTime, double camX, double camY, double viewportW, double viewportH,
                       WeatherType weather, BiomeType biome) {
        if (viewportW <= 0) viewportW = 800;
        if (viewportH <= 0) viewportH = 600;

        // 1. Atualiza objetivos climáticos de base para intensidade
        updateWeatherTargets(deltaTime, weather);

        // 2. Atualiza rajadas periódicas (gusts)
        updateGusts(deltaTime, weather);

        // 3. Interpolação suave contínua de intensidade (nunca há saltos abruptos)
        double totalTargetIntensity = Math.max(0.0, targetIntensity + gustStrength);
        double intensityBlendRate = (weather == WeatherType.STORM) ? 1.2 : 0.65;
        currentIntensity += (totalTargetIntensity - currentIntensity) * Math.min(1.0, deltaTime * intensityBlendRate);

        // 4. Interpolação angular suave pelo menor arco
        double angleDiff = Math.atan2(Math.sin(targetAngle - currentAngle), Math.cos(targetAngle - currentAngle));
        double angleBlendRate = 0.35;
        currentAngle += angleDiff * Math.min(1.0, deltaTime * angleBlendRate);
        currentAngle = (currentAngle % (2.0 * Math.PI) + 2.0 * Math.PI) % (2.0 * Math.PI);

        // 5. Atualiza e cicla as partículas (WindStreaks e WindLeaves)
        updateParticles(deltaTime, camX, camY, viewportW, viewportH, biome);
    }

    private void updateWeatherTargets(double deltaTime, WeatherType weather) {
        changeTimer -= deltaTime;
        if (changeTimer <= 0) {
            changeTimer = 14.0 + RAND.nextDouble() * 20.0;

            // Mudança gradual de ângulo (gira no máximo 45 a 70 graus por mudança)
            double angleDelta = (RAND.nextDouble() - 0.5) * Math.PI * 0.65;
            targetAngle = (currentAngle + angleDelta + 2.0 * Math.PI) % (2.0 * Math.PI);

            // Alvo de intensidade baseado no clima atual
            switch (weather) {
                case FOG:
                    // Ar parado e calmo na neblina
                    targetIntensity = 0.02 + RAND.nextDouble() * 0.12;
                    break;
                case CLEAR:
                    // Brisa leve a moderada agradável
                    targetIntensity = 0.15 + RAND.nextDouble() * 0.28;
                    break;
                case RAIN:
                    // Vento úmido firme
                    targetIntensity = 0.45 + RAND.nextDouble() * 0.35;
                    break;
                case STORM:
                    // Ventania violenta e uivante
                    targetIntensity = 0.95 + RAND.nextDouble() * 0.45;
                    break;
            }
        }
    }

    private void updateGusts(double deltaTime, WeatherType weather) {
        if (gustDuration > 0) {
            gustDuration -= deltaTime;
            if (gustDuration <= 0) {
                gustStrength = 0.0;
                gustTimer = (weather == WeatherType.STORM) ? (3.0 + RAND.nextDouble() * 4.0) : (8.0 + RAND.nextDouble() * 12.0);
            }
        } else {
            gustTimer -= deltaTime;
            if (gustTimer <= 0) {
                gustDuration = 2.0 + RAND.nextDouble() * 3.5;
                if (weather == WeatherType.STORM) {
                    gustStrength = 0.30 + RAND.nextDouble() * 0.45;
                } else if (weather == WeatherType.RAIN) {
                    gustStrength = 0.15 + RAND.nextDouble() * 0.25;
                } else if (weather == WeatherType.CLEAR) {
                    gustStrength = 0.08 + RAND.nextDouble() * 0.16;
                } else {
                    gustStrength = 0.02;
                }
            }
        }
    }

    private void updateParticles(double deltaTime, double camX, double camY, double viewportW, double viewportH, BiomeType biome) {
        GraphicsQuality quality = GameSettings.get().getGraphicsQuality();
        if (quality == GraphicsQuality.LOW && streaks.size() > 25) {
            streaks.subList(25, streaks.size()).clear();
        }

        double vx = getWindVx();
        double vy = getWindVy();

        // Dimensões do envelope ao redor da câmera
        double margin = 60.0;
        double minX = camX - margin;
        double maxX = camX + viewportW + margin;
        double minY = camY - margin;
        double maxY = camY + viewportH + margin;

        // Limites de quantidade de partículas conforme intensidade
        int targetStreakCount;
        int targetLeafCount;

        if (currentIntensity < 0.10) {
            targetStreakCount = 2;
            targetLeafCount = 4;
        } else if (currentIntensity < 0.40) {
            targetStreakCount = 10;
            targetLeafCount = 18;
        } else if (currentIntensity < 0.85) {
            targetStreakCount = 24;
            targetLeafCount = 38;
        } else {
            // Tempestade / Vendaval
            targetStreakCount = 48;
            targetLeafCount = 65;
        }

        if (quality == GraphicsQuality.LOW) {
            targetStreakCount /= 2;
            targetLeafCount /= 2;
        } else if (quality == GraphicsQuality.MEDIUM) {
            targetStreakCount = (int) (targetStreakCount * 0.75);
            targetLeafCount = (int) (targetLeafCount * 0.75);
        }

        // 1. Atualiza WindStreaks existentes
        Iterator<WindStreak> sIt = streaks.iterator();
        while (sIt.hasNext()) {
            WindStreak s = sIt.next();
            s.update(deltaTime, vx, vy);
            if (s.isDead() || s.x < minX - 100 || s.x > maxX + 100 || s.y < minY - 100 || s.y > maxY + 100) {
                sIt.remove();
            }
        }

        // Gera novos WindStreaks na borda a montante (upwind)
        while (streaks.size() < targetStreakCount) {
            double[] spawn = pickUpwindSpawnPos(camX, camY, viewportW, viewportH, vx, vy, margin);
            double length = 28.0 + RAND.nextDouble() * (25.0 + currentIntensity * 45.0);
            double life = 1.0 + RAND.nextDouble() * 1.5;
            streaks.add(new WindStreak(spawn[0], spawn[1], currentAngle, length, life, 0.12 + currentIntensity * 0.35));
        }

        // 2. Atualiza WindLeaves existentes
        Iterator<WindLeaf> lIt = leaves.iterator();
        while (lIt.hasNext()) {
            WindLeaf l = lIt.next();
            l.update(deltaTime, vx, vy, currentIntensity);
            if (l.isDead() || l.x < minX - 80 || l.x > maxX + 80 || l.y < minY - 80 || l.y > maxY + 80) {
                lIt.remove();
            }
        }

        // Gera novas WindLeaves na borda a montante
        while (leaves.size() < targetLeafCount) {
            double[] spawn = pickUpwindSpawnPos(camX, camY, viewportW, viewportH, vx, vy, margin);
            Color leafColor = pickLeafColor(biome);
            double size = (biome == BiomeType.SNOW) ? (2.0 + RAND.nextDouble() * 2.5) : (3.5 + RAND.nextDouble() * 4.0);
            double life = 2.0 + RAND.nextDouble() * 3.5;
            leaves.add(new WindLeaf(spawn[0], spawn[1], leafColor, size, life));
        }
    }

    private double[] pickUpwindSpawnPos(double camX, double camY, double w, double h, double vx, double vy, double margin) {
        // Gera ao longo das bordas de onde o vento sopra para dentro da tela
        double sx, sy;
        boolean fromHorizontalEdge = RAND.nextDouble() < (w / (w + h));

        if (fromHorizontalEdge) {
            sx = camX - margin + RAND.nextDouble() * (w + margin * 2.0);
            sy = (vy >= 0) ? (camY - margin) : (camY + h + margin);
        } else {
            sx = (vx >= 0) ? (camX - margin) : (camX + w + margin);
            sy = camY - margin + RAND.nextDouble() * (h + margin * 2.0);
        }

        return new double[]{sx, sy};
    }

    private Color pickLeafColor(BiomeType biome) {
        if (biome == null) biome = BiomeType.MEADOW;
        double r = RAND.nextDouble();

        switch (biome) {
            case MEADOW:
                // Prado/Clareira: pétalas coloridas de flores e folhinhas verdes
                if (r < 0.28) return Color.web("#FDD835", 0.88); // Botão de ouro (amarelo)
                if (r < 0.50) return Color.web("#EF5350", 0.88); // Papoula silvestre (vermelho)
                if (r < 0.70) return Color.web("#42A5F5", 0.88); // Miosótis (azul)
                if (r < 0.85) return Color.web("#FFFFFF", 0.90); // Margarida (branco)
                return Color.web("#66BB6A", 0.85); // Folha verde
            case DENSE_FOREST:
                // Floresta Densa: folhas de carvalho verdes e secas
                if (r < 0.55) return Color.web("#2E7D32", 0.85);
                if (r < 0.80) return Color.web("#43A047", 0.85);
                return Color.web("#8D6E63", 0.85); // Folha seca outonal
            case HIGHLAND:
                // Serra/Montanhas: agulhas de pinheiro douradas e castanhas
                if (r < 0.45) return Color.web("#FFB300", 0.85);
                if (r < 0.75) return Color.web("#FB8C00", 0.85);
                return Color.web("#6D4C41", 0.85);
            case SWAMP:
                // Pântano: esporos luminescentes e fragmentos de musgo
                if (r < 0.50) return Color.web("#9CCC65", 0.85);
                if (r < 0.80) return Color.web("#C0CA33", 0.85);
                return Color.web("#33691E", 0.85);
            case SNOW:
                // Tundra: cristais de gelo e flocos de neve rodopiantes
                if (r < 0.60) return Color.web("#FFFFFF", 0.90);
                return Color.web("#B3E5FC", 0.85);
            default:
                return Color.web("#4CAF50", 0.85);
        }
    }

    // Vetores componentes de velocidade do vento (normalizados pela intensidade)
    public double getWindVx() {
        return Math.cos(currentAngle) * currentIntensity;
    }

    public double getWindVy() {
        return Math.sin(currentAngle) * currentIntensity;
    }

    public double getIntensity() {
        return currentIntensity;
    }

    public double getAngle() {
        return currentAngle;
    }

    public List<WindStreak> getStreaks() {
        return streaks;
    }

    public List<WindLeaf> getLeaves() {
        return leaves;
    }

    // ==========================================
    // CLASSES DE PARTÍCULAS DIEGÉTICAS DE VENTO
    // ==========================================

    /**
     * Linha de corrente de ar (aerodinâmica e translúcida) que desliza pelo céu.
     */
    public static class WindStreak {
        public double x;
        public double y;
        public final double angle;
        public final double length;
        public final double maxLife;
        public double life;
        public final double baseAlpha;

        public WindStreak(double x, double y, double angle, double length, double life, double baseAlpha) {
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.length = length;
            this.life = life;
            this.maxLife = life;
            this.baseAlpha = baseAlpha;
        }

        public void update(double dt, double windVx, double windVy) {
            double speed = 140.0 + Math.hypot(windVx, windVy) * 260.0;
            this.x += Math.cos(angle) * speed * dt;
            this.y += Math.sin(angle) * speed * dt;
            this.life -= dt;
        }

        public boolean isDead() {
            return life <= 0;
        }

        public double getAlpha() {
            double progress = Math.max(0.0, life / maxLife);
            // Fade-in e fade-out suaves em curva seno
            return baseAlpha * Math.sin(progress * Math.PI);
        }
    }

    /**
     * Folha ou pétala voadora que cai, sobe e rodopia em 3D ao sabor do vento.
     */
    public static class WindLeaf {
        public double x;
        public double y;
        public double z; // Altura no ar
        public double rot;
        public double rotSpeed;
        public double flutterPhase;
        public double flutterSpeed;
        public final Color color;
        public final double size;
        public final double maxLife;
        public double life;

        public WindLeaf(double x, double y, Color color, double size, double life) {
            this.x = x;
            this.y = y;
            this.z = 12.0 + RAND.nextDouble() * 32.0;
            this.rot = RAND.nextDouble() * 360.0;
            this.rotSpeed = (RAND.nextDouble() - 0.5) * 180.0;
            this.flutterPhase = RAND.nextDouble() * Math.PI * 2.0;
            this.flutterSpeed = 3.0 + RAND.nextDouble() * 5.0;
            this.color = color;
            this.size = size;
            this.life = life;
            this.maxLife = life;
        }

        public void update(double dt, double windVx, double windVy, double intensity) {
            double speedX = windVx * (90.0 + intensity * 150.0);
            double speedY = windVy * (90.0 + intensity * 150.0);

            // Oscilação senoidal perpendicular ao vento
            double perpX = -windVy;
            double perpY = windVx;
            double sway = Math.sin(flutterPhase) * (18.0 + intensity * 35.0);

            this.x += (speedX + perpX * sway) * dt;
            this.y += (speedY + perpY * sway) * dt;

            // Queda no eixo Z com sustentação do vento
            double sinkRate = 8.0 - intensity * 6.0;
            this.z = Math.max(0.0, z - sinkRate * dt);

            this.rot += (rotSpeed + intensity * 120.0) * dt;
            this.flutterPhase += flutterSpeed * dt;
            this.life -= dt;
        }

        public boolean isDead() {
            return life <= 0;
        }

        public double getAlpha() {
            double p = Math.max(0.0, life / maxLife);
            return Math.sin(p * Math.PI);
        }
    }
}
