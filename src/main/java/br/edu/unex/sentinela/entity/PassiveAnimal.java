package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.ai.MobSteeringHelper;
import br.edu.unex.sentinela.world.Burrow;
import br.edu.unex.sentinela.world.Firefly;
import br.edu.unex.sentinela.world.FoodItem;
import br.edu.unex.sentinela.world.LightningStrike;
import br.edu.unex.sentinela.world.MeatItem;
import br.edu.unex.sentinela.world.MeatTier;
import br.edu.unex.sentinela.world.Particle;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import br.edu.unex.sentinela.world.WaterRipple;
import br.edu.unex.sentinela.world.WeatherType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Entidade de fauna silvestre passiva e ágil.
 * Possui inteligência de alerta, fuga com desvios em zigue-zague e interação ambiental.
 */
public class PassiveAnimal {
    private static final Random RAND = new Random();
    public static final double BIRD_MAX_CATCHABLE_ALTITUDE = 56.0;

    public enum State {
        FORAGING,
        ALERT,
        FLEEING,
        FLYING_AWAY,
        DRINKING,
        FORAGING_BUSH,
        SHELTERING,
        BURROWED
    }

    private final PassiveAnimalType type;
    private double x;
    private double y;
    private double vx;
    private double vy;
    private double faceDx = 1.0;
    private double faceDy = 0.0;
    private double visualAngle = 0.0;
    private double walkPhase = 0.0;
    private double breathTimer = 0.0;
    private double peckTimer = 0.0;
    private boolean moving = false;
    private int hp;

    private State state = State.FORAGING;
    private double stateTimer = 0.0;
    private double fleeDuration = 0.0;
    private double stuckTimer = 0.0;
    private double evasiveJukeTimer = 0.0;
    private double evasiveHeading = 0.0;
    private double targetHeading = 0.0;
    private double animPhase = 0.0;
    private double hopTimer = 0.0;
    private double hopHeight = 0.0;
    private double flightHeight = 0.0;
    private double birdTakeoffTimer = 0.0;
    private double zigZagTimer = 0.0;
    private double zigZagAngle = 0.0;
    private double rippleCooldown = 0.0;
    private boolean despawned = false;

    // Dinâmica de ecossistema vivo (sede, pastoreio em arbustos, caça de vaga-lumes e abrigo)
    private double thirstTimer = 22.0 + RAND.nextDouble() * 30.0;
    private double drinkingTimer = 0.0;
    private double drinkingWaterX = 0.0;
    private double drinkingWaterY = 0.0;
    private double drinkingRippleTimer = 0.0;

    private double bushForageCooldown = 12.0 + RAND.nextDouble() * 20.0;
    private double bushForageTimer = 0.0;

    private double frogFireflyHuntCooldown = 1.0;
    private double frogTongueTimer = 0.0;
    private double frogTongueTargetX = 0.0;
    private double frogTongueTargetY = 0.0;

    // Dinâmica de tocas de coelho (Burrows)
    private double burrowTimer = 0.0;
    private double burrowCooldown = 0.0;
    private double peekTimer = 0.0;
    private Burrow currentBurrow = null;

    public PassiveAnimal(PassiveAnimalType type, double x, double y) {
        this.type = type != null ? type : PassiveAnimalType.RABBIT;
        this.x = x;
        this.y = y;
        this.hp = this.type.getMaxHp();
        this.stateTimer = 1.0 + RAND.nextDouble() * 2.0;
        this.visualAngle = RAND.nextDouble() * Math.PI * 2.0 - Math.PI;
        this.targetHeading = this.visualAngle;
        this.faceDx = Math.cos(visualAngle);
        this.faceDy = Math.sin(visualAngle);
        this.breathTimer = RAND.nextDouble() * 5.0;
    }

    public void update(double dt, Player player, List<Enemy> predators, TileMap tileMap, List<WaterRipple> ripples) {
        update(dt, player, predators, null, null, null, null, null, tileMap, ripples, WeatherType.CLEAR, 0.0);
    }

    public void update(double dt, Player player, List<Enemy> predators, List<PassiveAnimal> allPassives, TileMap tileMap, List<WaterRipple> ripples) {
        update(dt, player, predators, allPassives, null, null, null, null, tileMap, ripples, WeatherType.CLEAR, 0.0);
    }

    public void update(double dt, Player player, List<Enemy> predators, List<PassiveAnimal> allPassives,
                       List<FoodItem> foodList, List<Burrow> burrows, List<Firefly> fireflies,
                       List<Particle> particles, TileMap tileMap, List<WaterRipple> ripples,
                       WeatherType weather, double timeProgress) {
        update(dt, player, predators, allPassives, foodList, burrows, fireflies, particles, tileMap, ripples, weather, timeProgress, null);
    }

    public void update(double dt, Player player, List<Enemy> predators, List<PassiveAnimal> allPassives,
                       List<FoodItem> foodList, List<Burrow> burrows, List<Firefly> fireflies,
                       List<Particle> particles, TileMap tileMap, List<WaterRipple> ripples,
                       WeatherType weather, double timeProgress, List<LightningStrike> lightningStrikes) {
        if (despawned) return;

        stateTimer -= dt;
        animPhase += dt * 6.0;
        breathTimer += dt;
        rippleCooldown -= dt;
        if (frogTongueTimer > 0) frogTongueTimer -= dt;
        burrowCooldown = Math.max(0.0, burrowCooldown - dt);

        // 1. Identificar a ameaça mais próxima (Jogador, Predador ou Telégrafo de Raio)
        double nearestThreatDist = 99999.0;
        double threatX = 0;
        double threatY = 0;

        if (player != null && !player.isBurrowed()) {
            double dp = Math.hypot(player.getX() - x, player.getY() - y);
            if (dp < nearestThreatDist) {
                nearestThreatDist = dp;
                threatX = player.getX();
                threatY = player.getY();
            }
        }

        if (predators != null) {
            for (Enemy enemy : predators) {
                if (enemy.getHp() <= 0) continue;
                double de = Math.hypot(enemy.getX() - x, enemy.getY() - y);
                if (de < nearestThreatDist) {
                    nearestThreatDist = de;
                    threatX = enemy.getX();
                    threatY = enemy.getY();
                }
            }
        }

        if (lightningStrikes != null) {
            for (LightningStrike ls : lightningStrikes) {
                if (ls.isWarning()) {
                    double dls = Math.hypot(ls.getX() - x, ls.getY() - y);
                    if (dls < 90.0 && dls < nearestThreatDist) {
                        nearestThreatDist = dls;
                        threatX = ls.getX();
                        threatY = ls.getY();
                        if (state != State.FLEEING && state != State.BURROWED) {
                            state = State.FLEEING;
                            fleeDuration = 3.5;
                        }
                    }
                }
            }
        }

        // 2. Máquina de Estados da Presa
        double effectivePerception = type.getPerceptionRadius();
        if (state == State.DRINKING) {
            // Bebendo com cabeça baixa: percepção reduzida em 25% (foco de emboscadas naturais)
            effectivePerception *= 0.75;
        } else if (state == State.SHELTERING) {
            effectivePerception *= 0.85;
        }

        if (type == PassiveAnimalType.BIRD) {
            // Se o pássaro está no solo debicando sementes (cabeça rente à terra)
            if (state == State.FORAGING || peckTimer > 0) {
                effectivePerception *= 0.60;
            }
            // Furtividade da raposa predadora
            if (player != null && nearestThreatDist == Math.hypot(player.getX() - x, player.getY() - y)) {
                // Caminhada silenciosa sem sprint
                if (!player.isSprinting()) {
                    effectivePerception *= 0.68;
                }
                // Camuflagem na grama alta
                int pCol = (int) Math.floor(player.getX() / TileMap.TILE_SIZE);
                int pRow = (int) Math.floor(player.getY() / TileMap.TILE_SIZE);
                if (tileMap != null && tileMap.getTileAt(pCol, pRow) == TileType.TALL_GRASS) {
                    effectivePerception *= 0.62;
                }
                // Ataque pelas costas (fora do cone de visão de 160° da ave)
                if (state != State.FLYING_AWAY) {
                    double angleToPlayer = Math.atan2(player.getY() - y, player.getX() - x);
                    double angleDiff = Math.abs(normalizeAngle(angleToPlayer - visualAngle));
                    if (angleDiff > Math.PI * 0.45) {
                        effectivePerception *= 0.55;
                    }
                }
            }
            // Clima e Chuva mascaram ruído
            if (weather == WeatherType.RAIN || weather == WeatherType.STORM) {
                effectivePerception *= 0.75;
            }
        }

        boolean sensedThreat = nearestThreatDist <= effectivePerception;

        if (state == State.FLYING_AWAY) {
            if (birdTakeoffTimer > 0) {
                birdTakeoffTimer -= dt;
                // Arrancada de sustentação no solo: sobe lentamente (18 px/s)
                flightHeight += dt * 18.0;
                double flySpeed = type.getBaseSpeed() * 0.80;
                x += faceDx * flySpeed * dt;
                y += faceDy * flySpeed * dt;
            } else {
                // Voo de cruzeiro ascendente
                flightHeight += dt * 38.0;
                double flySpeed = type.getBaseSpeed() * 1.30;
                x += faceDx * flySpeed * dt;
                y += faceDy * flySpeed * dt;
            }
            moving = true;
            walkPhase += dt * 26.0;
            double awayAngle = Math.atan2(faceDy, faceDx);
            visualAngle = lerpAngle(visualAngle, awayAngle, Math.min(1.0, dt * 10.0));
            if (flightHeight >= 125.0) {
                despawned = true;
            }
            return;
        }

        if (sensedThreat && state != State.BURROWED) {
            if (type == PassiveAnimalType.BIRD) {
                // Se notou ameaça mas ainda está a mais de 50px de distância:
                // Primeiro entra em ALERT (head-bob desconfiado) por 0.45s antes de decolar
                if (state != State.ALERT && state != State.FLYING_AWAY && nearestThreatDist > 50.0) {
                    state = State.ALERT;
                    stateTimer = 0.45;
                    vx *= 0.20;
                    vy *= 0.20;
                } else if (state != State.FLYING_AWAY && (nearestThreatDist <= 50.0 || (state == State.ALERT && stateTimer <= 0))) {
                    state = State.FLYING_AWAY;
                    birdTakeoffTimer = 0.35;
                    double awayAngle = Math.atan2(y - threatY, x - threatX);
                    faceDx = Math.cos(awayAngle);
                    faceDy = Math.sin(awayAngle);
                    visualAngle = awayAngle;
                    return;
                }
            } else {
                if (state != State.FLEEING) {
                    state = State.FLEEING;
                    fleeDuration = 3.2 + RAND.nextDouble() * 1.6;
                } else {
                    fleeDuration = Math.max(fleeDuration, 2.0);
                }
                // Cervo emite sinal de alerta e assusta outros animais passivos no raio de 200px
                if (type == PassiveAnimalType.DEER && allPassives != null) {
                    for (PassiveAnimal other : allPassives) {
                        if (other == this || other.isDespawned()) continue;
                        double dOther = Math.hypot(other.getX() - x, other.getY() - y);
                        if (dOther < 200.0 && other.getState() != State.FLEEING) {
                            other.triggerAlertFlee(threatX, threatY);
                        }
                    }
                }
            }
        } else if (state == State.FLEEING) {
            fleeDuration -= dt;
            if (fleeDuration <= 0) {
                state = State.ALERT;
                stateTimer = 1.3 + RAND.nextDouble() * 1.2;
                vx *= 0.40;
                vy *= 0.40;
            }
        } else if (state == State.ALERT && stateTimer <= 0) {
            if (type == PassiveAnimalType.BIRD && sensedThreat) {
                state = State.FLYING_AWAY;
                birdTakeoffTimer = 0.35;
                double awayAngle = Math.atan2(y - threatY, x - threatX);
                faceDx = Math.cos(awayAngle);
                faceDy = Math.sin(awayAngle);
                visualAngle = awayAngle;
                return;
            }
            state = State.FORAGING;
            stateTimer = 2.0 + RAND.nextDouble() * 3.0;
        }

        // 3. Comportamento Específico por Estado
        switch (state) {
            case BURROWED:
                updateBurrowed(dt, player, predators, particles);
                break;
            case DRINKING:
                updateDrinking(dt, ripples);
                break;
            case FORAGING_BUSH:
                updateForagingBush(dt, particles);
                break;
            case SHELTERING:
                updateSheltering(dt, weather);
                break;
            case FORAGING:
                updateForaging(dt, player, allPassives, foodList, burrows, fireflies, particles, tileMap, ripples, weather, timeProgress);
                break;
            case ALERT:
                vx *= 0.82;
                vy *= 0.82;
                if (sensedThreat) {
                    double targetAngle = Math.atan2(threatY - y, threatX - x);
                    visualAngle = lerpAngle(visualAngle, targetAngle, Math.min(1.0, dt * 12.0));
                    faceDx = Math.cos(visualAngle);
                    faceDy = Math.sin(visualAngle);
                }
                break;
            case FLEEING:
                updateFleeing(dt, threatX, threatY, allPassives, burrows, particles, player, tileMap);
                break;
            default:
                break;
        }

        if (state == State.BURROWED) {
            moving = false;
            vx = 0;
            vy = 0;
            hopTimer = 0;
            hopHeight = 0;
            return;
        }

        // 4. Mecânica de Locomoção e Saltos (Coelhos e Sapos)
        double curSpeed = Math.hypot(vx, vy);
        moving = curSpeed > 2.0;

        if (moving) {
            if (type == PassiveAnimalType.FROG) {
                int col = (int) Math.floor(x / TileMap.TILE_SIZE);
                int row = (int) Math.floor(y / TileMap.TILE_SIZE);
                boolean inWater = (tileMap.getTileAt(col, row) == TileType.WATER);
                if (inWater) {
                    hopHeight = 0.0; // Na água desliza e nada sem pausas no solo
                } else {
                    hopTimer += dt * (state == State.FLEEING ? 8.2 : 4.5);
                    double cycle = hopTimer % (Math.PI * 2.0);
                    if (cycle < Math.PI) {
                        hopHeight = Math.sin(cycle) * (state == State.FLEEING ? 8.5 : 5.0);
                    } else {
                        hopHeight = 0.0;
                        vx *= 0.45;
                        vy *= 0.45;
                    }
                }
            } else if (type == PassiveAnimalType.RABBIT) {
                hopTimer += dt * (state == State.FLEEING ? 9.6 : 5.0);
                double sinH = Math.sin(hopTimer);
                hopHeight = sinH > 0.0 ? sinH * (state == State.FLEEING ? 7.2 : 4.0) : 0.0;
            } else {
                hopHeight = 0.0;
            }
        } else {
            hopTimer = 0.0;
            hopHeight = 0.0;
        }

        // 5. Aplicação Física com Detecção de Obstáculos, Deslizamento em Quinas e Anti-Travamento
        moveWithCollisionSlide(dt, tileMap);

        // 6. Atualização de Rotação Visual e Fases de Animação
        double postSpeed = Math.hypot(vx, vy);
        moving = postSpeed > 2.0;
        if (moving) {
            walkPhase += postSpeed * dt * (type == PassiveAnimalType.DEER ? 0.11 : 0.15);
        } else if (type == PassiveAnimalType.BIRD && state == State.FORAGING) {
            peckTimer += dt * 3.8;
        }

        // 7. Geração de Ondulações na Água
        if (ripples != null && rippleCooldown <= 0) {
            int col = (int) Math.floor(x / TileMap.TILE_SIZE);
            int row = (int) Math.floor(y / TileMap.TILE_SIZE);
            TileType curTile = tileMap.getTileAt(col, row);
            if (curTile == TileType.WATER) {
                if (postSpeed > 15.0 || hopHeight > 2.0) {
                    ripples.add(new WaterRipple(x, y, type == PassiveAnimalType.DEER ? 18.0 : 12.0, 0.70));
                    rippleCooldown = type == PassiveAnimalType.FROG ? 0.35 : 0.22;
                }
            }
        }
    }

    private void updateDrinking(double dt, List<WaterRipple> ripples) {
        drinkingTimer -= dt;
        drinkingRippleTimer -= dt;
        vx = 0;
        vy = 0;
        moving = false;

        if (drinkingRippleTimer <= 0 && ripples != null) {
            drinkingRippleTimer = 0.55;
            ripples.add(new WaterRipple(drinkingWaterX, drinkingWaterY, type == PassiveAnimalType.DEER ? 15.0 : 10.0, 0.70));
        }

        if (drinkingTimer <= 0) {
            thirstTimer = 38.0 + RAND.nextDouble() * 32.0;
            state = State.FORAGING;
            stateTimer = 1.6;
        }
    }

    private void updateForagingBush(double dt, List<Particle> particles) {
        bushForageTimer -= dt;
        vx = 0;
        vy = 0;
        moving = false;
        peckTimer += dt * 3.5;

        if (particles != null && RAND.nextDouble() < 0.10) {
            particles.add(new Particle(x + faceDx * 6.0, y + faceDy * 6.0,
                (RAND.nextDouble() - 0.5) * 12.0, -6.0 - RAND.nextDouble() * 8.0,
                0.35, "#81C784", 2.0));
        }

        if (bushForageTimer <= 0) {
            if (hp < type.getMaxHp()) hp++;
            bushForageCooldown = 26.0 + RAND.nextDouble() * 24.0;
            state = State.FORAGING;
            stateTimer = 1.6;
        }
    }

    private void updateSheltering(double dt, WeatherType weather) {
        vx = 0;
        vy = 0;
        moving = false;
        if (weather == null || !weather.isRaining()) {
            state = State.FORAGING;
            stateTimer = 1.4;
        }
    }

    private void updateBurrowed(double dt, Player player, List<Enemy> predators, List<Particle> particles) {
        burrowTimer -= dt;
        peekTimer += dt;
        vx = 0;
        vy = 0;
        moving = false;

        // 1. Invasão / Despejo: Se o jogador raposa invadir a mesma toca
        if (player != null && player.isBurrowed()) {
            double distToPlayer = Math.hypot(player.getX() - x, player.getY() - y);
            if (distToPlayer < 24.0) {
                flushOutFromBurrow(player.getX(), player.getY(), particles);
                return;
            }
        }

        // 2. Passos pesados do jogador correndo/andando acima da toca atrasam a saída do coelho
        if (player != null && !player.isBurrowed()) {
            double distToPlayer = Math.hypot(player.getX() - x, player.getY() - y);
            if (distToPlayer < 50.0 && (player.isSprinting() || player.isMoving())) {
                burrowTimer = Math.max(burrowTimer, 2.5);
            }
        }

        // 3. Feras e predadores rondando a boca da toca
        if (predators != null) {
            for (Enemy enemy : predators) {
                if (enemy.getHp() <= 0) continue;
                double de = Math.hypot(enemy.getX() - x, enemy.getY() - y);
                if (de < 55.0) {
                    burrowTimer = Math.max(burrowTimer, 3.0);
                    break;
                }
            }
        }

        // 4. Emerge com segurança quando o tempo terminar e não houver perigo imediato
        if (burrowTimer <= 0) {
            double nearestDanger = 9999.0;
            if (player != null && !player.isBurrowed()) {
                nearestDanger = Math.min(nearestDanger, Math.hypot(player.getX() - x, player.getY() - y));
            }
            if (predators != null) {
                for (Enemy enemy : predators) {
                    if (enemy.getHp() <= 0) continue;
                    nearestDanger = Math.min(nearestDanger, Math.hypot(enemy.getX() - x, enemy.getY() - y));
                }
            }

            if (nearestDanger < 55.0) {
                burrowTimer = 2.0;
                return;
            }

            emergeFromBurrow(particles);
        }
    }

    public void enterBurrow(Burrow b, List<Particle> particles, double duration) {
        state = State.BURROWED;
        burrowTimer = duration;
        peekTimer = 0.0;
        currentBurrow = b;
        x = b.getX();
        y = b.getY();
        vx = 0;
        vy = 0;
        moving = false;

        br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.BURROW_ENTER, 0.70, 0, 1.25);
        if (particles != null) {
            for (int k = 0; k < 6; k++) {
                particles.add(new Particle(x, y,
                    (RAND.nextDouble() - 0.5) * 35.0,
                    -10.0 - RAND.nextDouble() * 15.0,
                    0.35, "#8D6E63", 2.6));
            }
        }
    }

    public void flushOutFromBurrow(double threatX, double threatY, List<Particle> particles) {
        state = State.FLEEING;
        burrowCooldown = 12.0;
        fleeDuration = 4.0;
        stateTimer = fleeDuration;

        double away = Math.atan2(y - threatY, x - threatX);
        if (Math.hypot(y - threatY, x - threatX) < 0.01) {
            away = visualAngle + Math.PI;
        }
        visualAngle = away;
        faceDx = Math.cos(away);
        faceDy = Math.sin(away);
        vx = faceDx * type.getBaseSpeed() * 1.35;
        vy = faceDy * type.getBaseSpeed() * 1.35;
        moving = true;
        currentBurrow = null;

        br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.BURROW_EXIT, 0.85, 0, 1.30);
        br.edu.unex.sentinela.progress.GameEventBus.get().post(
            new br.edu.unex.sentinela.progress.GameEvent(br.edu.unex.sentinela.progress.GameEvent.Type.RABBIT_FLUSHED)
        );
        if (particles != null) {
            for (int k = 0; k < 8; k++) {
                particles.add(new Particle(x, y,
                    faceDx * 30.0 + (RAND.nextDouble() - 0.5) * 35.0,
                    faceDy * 30.0 + (RAND.nextDouble() - 0.5) * 35.0,
                    0.40, "#D7CCC8", 3.0));
            }
        }
    }

    public void emergeFromBurrow(List<Particle> particles) {
        state = State.ALERT;
        burrowCooldown = 14.0 + RAND.nextDouble() * 10.0;
        stateTimer = 1.8;
        currentBurrow = null;

        br.edu.unex.sentinela.audio.AudioEngine.get().play(br.edu.unex.sentinela.audio.SoundId.BURROW_EXIT, 0.65, 0, 1.15);
        if (particles != null) {
            for (int k = 0; k < 5; k++) {
                particles.add(new Particle(x, y,
                    (RAND.nextDouble() - 0.5) * 25.0,
                    -8.0 - RAND.nextDouble() * 15.0,
                    0.30, "#8D6E63", 2.2));
            }
        }
    }

    private void updateForaging(double dt, Player player, List<PassiveAnimal> allPassives, List<FoodItem> foodList,
                                List<Burrow> burrows, List<Firefly> fireflies, List<Particle> particles,
                                TileMap tileMap, List<WaterRipple> ripples, WeatherType weather, double timeProgress) {
        // 1. Abrigo em tocas durante chuva para coelhos
        if (weather != null && weather.isRaining() && type == PassiveAnimalType.RABBIT && burrows != null && burrowCooldown <= 0) {
            Burrow nearestBurrow = null;
            double minBurrowDist = 180.0;
            for (Burrow b : burrows) {
                if (player != null && player.isBurrowed() && Math.hypot(b.getX() - player.getX(), b.getY() - player.getY()) < 18.0) {
                    continue;
                }
                double db = Math.hypot(b.getX() - x, b.getY() - y);
                if (db < minBurrowDist) {
                    minBurrowDist = db;
                    nearestBurrow = b;
                }
            }
            if (nearestBurrow != null) {
                double db = Math.hypot(nearestBurrow.getX() - x, nearestBurrow.getY() - y);
                if (db <= 14.0) {
                    enterBurrow(nearestBurrow, particles, 14.0 + RAND.nextDouble() * 10.0);
                    return;
                } else {
                    double ang = Math.atan2(nearestBurrow.getY() - y, nearestBurrow.getX() - x);
                    vx = Math.cos(ang) * type.getBaseSpeed() * 0.85;
                    vy = Math.sin(ang) * type.getBaseSpeed() * 0.85;
                    visualAngle = ang;
                    faceDx = Math.cos(ang);
                    faceDy = Math.sin(ang);
                    return;
                }
            }
        }

        // 2. Caça de vaga-lumes bioluminescentes por sapos anfíbios
        if (type == PassiveAnimalType.FROG && fireflies != null) {
            frogFireflyHuntCooldown -= dt;
            if (frogFireflyHuntCooldown <= 0) {
                Firefly nearestF = null;
                double minFDist = 95.0;
                for (Firefly ff : fireflies) {
                    if (ff.isEaten()) continue;
                    double df = Math.hypot(ff.getX() - x, ff.getY() - y);
                    if (df < minFDist) {
                        minFDist = df;
                        nearestF = ff;
                    }
                }
                if (nearestF != null) {
                    if (minFDist <= 28.0) {
                        frogTongueTimer = 0.22;
                        frogTongueTargetX = nearestF.getX();
                        frogTongueTargetY = nearestF.getY();
                        nearestF.setEaten(true);
                        frogFireflyHuntCooldown = 4.0 + RAND.nextDouble() * 3.0;
                        visualAngle = Math.atan2(nearestF.getY() - y, nearestF.getX() - x);
                        faceDx = Math.cos(visualAngle);
                        faceDy = Math.sin(visualAngle);
                        if (particles != null) {
                            for (int p = 0; p < 4; p++) {
                                particles.add(new Particle(nearestF.getX(), nearestF.getY(),
                                    (RAND.nextDouble() - 0.5) * 25.0, -10.0 - RAND.nextDouble() * 15.0,
                                    0.45, "#E0F7FA", 2.2));
                            }
                        }
                        vx = 0;
                        vy = 0;
                        return;
                    } else {
                        double ang = Math.atan2(nearestF.getY() - y, nearestF.getX() - x);
                        visualAngle = ang;
                        faceDx = Math.cos(ang);
                        faceDy = Math.sin(ang);
                        vx = Math.cos(ang) * type.getBaseSpeed() * 0.65;
                        vy = Math.sin(ang) * type.getBaseSpeed() * 0.65;
                        return;
                    }
                }
            }
        }

        // 3. Forrageamento em arbustos de frutas naturais (Coelhos e Cervos)
        if ((type == PassiveAnimalType.RABBIT || type == PassiveAnimalType.DEER) && foodList != null) {
            bushForageCooldown -= dt;
            if (bushForageCooldown <= 0) {
                FoodItem nearestBush = null;
                double minBushDist = 140.0;
                for (FoodItem fi : foodList) {
                    if (fi.isEaten()) continue;
                    double df = Math.hypot(fi.getX() - x, fi.getY() - y);
                    if (df < minBushDist) {
                        minBushDist = df;
                        nearestBush = fi;
                    }
                }
                if (nearestBush != null) {
                    if (minBushDist <= 18.0) {
                        state = State.FORAGING_BUSH;
                        bushForageTimer = 2.8 + RAND.nextDouble() * 1.4;
                        vx = 0;
                        vy = 0;
                        moving = false;
                        visualAngle = Math.atan2(nearestBush.getY() - y, nearestBush.getX() - x);
                        faceDx = Math.cos(visualAngle);
                        faceDy = Math.sin(visualAngle);
                        return;
                    } else {
                        double ang = Math.atan2(nearestBush.getY() - y, nearestBush.getX() - x);
                        vx = Math.cos(ang) * type.getBaseSpeed() * 0.40;
                        vy = Math.sin(ang) * type.getBaseSpeed() * 0.40;
                        visualAngle = ang;
                        faceDx = Math.cos(ang);
                        faceDy = Math.sin(ang);
                        return;
                    }
                }
            }
        }

        // 4. Ciclo de sede e busca de água na margem de rios e lagos
        thirstTimer -= dt;
        if (thirstTimer <= 0) {
            int curCol = (int) Math.floor(x / TileMap.TILE_SIZE);
            int curRow = (int) Math.floor(y / TileMap.TILE_SIZE);
            int[] shore = MobSteeringHelper.findNearestWaterShoreTile(tileMap, curCol, curRow, 9);
            if (shore != null) {
                double shoreX = shore[0] * TileMap.TILE_SIZE + 16.0;
                double shoreY = shore[1] * TileMap.TILE_SIZE + 16.0;
                double dShore = Math.hypot(shoreX - x, shoreY - y);
                if (dShore <= 18.0) {
                    state = State.DRINKING;
                    drinkingTimer = 3.2 + RAND.nextDouble() * 1.8;
                    drinkingWaterX = shore[2] * TileMap.TILE_SIZE + 16.0;
                    drinkingWaterY = shore[3] * TileMap.TILE_SIZE + 16.0;
                    vx = 0;
                    vy = 0;
                    moving = false;
                    visualAngle = Math.atan2(drinkingWaterY - y, drinkingWaterX - x);
                    faceDx = Math.cos(visualAngle);
                    faceDy = Math.sin(visualAngle);
                    return;
                } else {
                    double ang = Math.atan2(shoreY - y, shoreX - x);
                    vx = Math.cos(ang) * type.getBaseSpeed() * 0.45;
                    vy = Math.sin(ang) * type.getBaseSpeed() * 0.45;
                    visualAngle = ang;
                    faceDx = Math.cos(ang);
                    faceDy = Math.sin(ang);
                    return;
                }
            }
        }

        // 5. Coesão de bando / rebanho de cervos
        if (type == PassiveAnimalType.DEER && allPassives != null) {
            double herdCenterX = 0;
            double herdCenterY = 0;
            int deerCount = 0;
            double pushX = 0;
            double pushY = 0;

            for (PassiveAnimal other : allPassives) {
                if (other == this || other.isDespawned() || other.getType() != PassiveAnimalType.DEER) continue;
                double dd = Math.hypot(other.x - x, other.y - y);
                if (dd < 180.0) {
                    herdCenterX += other.x;
                    herdCenterY += other.y;
                    deerCount++;
                    if (dd < 42.0 && dd > 0.001) {
                        pushX += (x - other.x) / dd;
                        pushY += (y - other.y) / dd;
                    }
                }
            }

            if (deerCount > 0) {
                herdCenterX /= deerCount;
                herdCenterY /= deerCount;
                double distCentroid = Math.hypot(herdCenterX - x, herdCenterY - y);
                if (distCentroid > 70.0) {
                    vx += ((herdCenterX - x) / distCentroid) * type.getBaseSpeed() * 0.18;
                    vy += ((herdCenterY - y) / distCentroid) * type.getBaseSpeed() * 0.18;
                }
                if (Math.hypot(pushX, pushY) > 0.01) {
                    vx += pushX * 14.0;
                    vy += pushY * 14.0;
                }
            }
        }

        // Pastoreio padrão
        updateForaging(dt, tileMap);
    }

    private void updateForaging(double dt, TileMap tileMap) {
        if (stateTimer <= 0) {
            stateTimer = 1.6 + RAND.nextDouble() * 2.4;
            if (RAND.nextDouble() < 0.42) {
                // Parada para debicar/pastar
                vx = 0;
                vy = 0;
            } else {
                // Caminhada lenta inteligente: procura direção desobstruída
                double speed = type.getBaseSpeed() * 0.28;
                boolean canWalkWater = (type == PassiveAnimalType.FROG);
                double r = getBodyRadius();
                double chosenAngle = RAND.nextDouble() * Math.PI * 2.0;

                for (int t = 0; t < 8; t++) {
                    double ang = RAND.nextDouble() * Math.PI * 2.0;
                    if (evaluatePathClearance(x, y, ang, 36.0, r, tileMap, canWalkWater) >= 0.70) {
                        chosenAngle = ang;
                        break;
                    }
                }

                vx = Math.cos(chosenAngle) * speed;
                vy = Math.sin(chosenAngle) * speed;
                visualAngle = chosenAngle;
                faceDx = Math.cos(chosenAngle);
                faceDy = Math.sin(chosenAngle);
            }
        } else if (stuckTimer > 0.08 && moving) {
            // Se encostou em um obstáculo durante o pastoreio ocioso, para imediatamente sem forçar a parede
            vx = 0;
            vy = 0;
            stateTimer = 1.2 + RAND.nextDouble() * 1.5;
            stuckTimer = 0.0;
        }
    }

    public void triggerFlightBurst(double originX, double originY) {
        if (despawned) return;
        if (type == PassiveAnimalType.BIRD) {
            if (state != State.FLYING_AWAY) {
                state = State.FLYING_AWAY;
                birdTakeoffTimer = 0.35;
                double away = Math.atan2(y - originY, x - originX);
                faceDx = Math.cos(away);
                faceDy = Math.sin(away);
                visualAngle = away;
            }
        } else if (state != State.FLEEING) {
            triggerAlertFlee(originX, originY);
        }
    }

    public void triggerAlertFlee(double threatX, double threatY) {
        if (state != State.FLEEING && state != State.FLYING_AWAY) {
            if (type == PassiveAnimalType.BIRD) {
                state = State.FLYING_AWAY;
                birdTakeoffTimer = 0.35;
                double awayAngle = Math.atan2(y - threatY, x - threatX);
                faceDx = Math.cos(awayAngle);
                faceDy = Math.sin(awayAngle);
                visualAngle = awayAngle;
            } else {
                state = State.FLEEING;
                fleeDuration = 3.2 + RAND.nextDouble() * 1.5;
                stateTimer = fleeDuration;
            }
        }
    }

    private void updateFleeing(double dt, double threatX, double threatY, List<PassiveAnimal> allPassives,
                              List<Burrow> burrows, List<Particle> particles, Player player, TileMap tileMap) {
        // Se estiver em arrancada de desvio evasivo de quina, segue o vetor de juke
        if (evasiveJukeTimer > 0) {
            evasiveJukeTimer -= dt;
            double burstSpeed = type.getBaseSpeed() * (type == PassiveAnimalType.DEER ? 1.35 : 1.25);
            vx = Math.cos(evasiveHeading) * burstSpeed;
            vy = Math.sin(evasiveHeading) * burstSpeed;
            faceDx = Math.cos(evasiveHeading);
            faceDy = Math.sin(evasiveHeading);
            return;
        }

        // Se estiver bloqueado contra paredes/quinas por mais de 0.16s, executa juke reflexo imediato
        if (stuckTimer >= 0.16) {
            triggerCornerEscape(tileMap);
            stuckTimer = 0.0;
            return;
        }

        // Toca de fuga para coelhos
        Burrow targetBurrow = null;
        if (type == PassiveAnimalType.RABBIT && burrows != null && burrowCooldown <= 0) {
            double minBurrowDist = 180.0;
            for (Burrow b : burrows) {
                if (player != null && player.isBurrowed() && Math.hypot(b.getX() - player.getX(), b.getY() - player.getY()) < 18.0) {
                    continue;
                }
                double db = Math.hypot(b.getX() - x, b.getY() - y);
                if (db < minBurrowDist) {
                    minBurrowDist = db;
                    targetBurrow = b;
                }
            }

            if (targetBurrow != null) {
                if (minBurrowDist <= 14.0) {
                    enterBurrow(targetBurrow, particles, 6.5 + RAND.nextDouble() * 3.5);
                    return;
                }
            }
        }

        // Sapo já na água: nada em alta velocidade na direção de maior fuga
        if (type == PassiveAnimalType.FROG) {
            int curC = (int) Math.floor(x / TileMap.TILE_SIZE);
            int curR = (int) Math.floor(y / TileMap.TILE_SIZE);
            if (tileMap.getTileAt(curC, curR) == TileType.WATER) {
                double bestWaterAngle = computeBestFleeAngle(threatX, threatY, null, tileMap);
                double swimSpeed = type.getBaseSpeed() * 1.45;
                targetHeading = bestWaterAngle;
                visualAngle = lerpAngle(visualAngle, targetHeading, Math.min(1.0, dt * 14.0));
                vx = Math.cos(visualAngle) * swimSpeed;
                vy = Math.sin(visualAngle) * swimSpeed;
                faceDx = Math.cos(visualAngle);
                faceDy = Math.sin(visualAngle);
                return;
            }
        }

        // Zigue-zague arisco orgânico do coelho em campo aberto
        if (type == PassiveAnimalType.RABBIT) {
            zigZagTimer -= dt;
            if (zigZagTimer <= 0) {
                zigZagTimer = 0.22 + RAND.nextDouble() * 0.18;
                zigZagAngle = (RAND.nextDouble() - 0.5) * 0.90;
            }
        }

        // Análise de feelers/whiskers para encontrar o melhor corredor livre de fuga
        targetHeading = computeBestFleeAngle(threatX, threatY, targetBurrow, tileMap);

        double turnRate = (type == PassiveAnimalType.RABBIT ? 20.0 : (type == PassiveAnimalType.DEER ? 13.0 : 16.0));
        visualAngle = lerpAngle(visualAngle, targetHeading, Math.min(1.0, dt * turnRate));

        double fleeSpeed = type.getBaseSpeed() * (type == PassiveAnimalType.DEER ? 1.25 : 1.12);
        double targetVx = Math.cos(visualAngle) * fleeSpeed;
        double targetVy = Math.sin(visualAngle) * fleeSpeed;

        double accel = 16.0;
        vx += (targetVx - vx) * Math.min(1.0, dt * accel);
        vy += (targetVy - vy) * Math.min(1.0, dt * accel);

        faceDx = Math.cos(visualAngle);
        faceDy = Math.sin(visualAngle);
    }

    private double computeBestFleeAngle(double threatX, double threatY, Burrow targetBurrow, TileMap tileMap) {
        double baseAwayAngle = Math.atan2(y - threatY, x - threatX);
        boolean canWalkWater = (type == PassiveAnimalType.FROG);
        double r = getBodyRadius();
        double probeDist = (type == PassiveAnimalType.DEER) ? 62.0 : (type == PassiveAnimalType.RABBIT ? 46.0 : 36.0);

        // Componente de navegação do coelho (atração para a toca com zigue-zague evasivo)
        double desiredAngle = baseAwayAngle;
        if (type == PassiveAnimalType.RABBIT) {
            if (targetBurrow != null) {
                double toBurrow = Math.atan2(targetBurrow.getY() - y, targetBurrow.getX() - x);
                desiredAngle = lerpAngle(desiredAngle, toBurrow, 0.45);
            } else {
                desiredAngle += zigZagAngle;
            }
        }

        // Whiskers em leque de 13 ângulos cobrindo frente, flancos e rotas tangenciais
        double[] angleOffsets = {
            0.0,
            0.35, -0.35,
            0.70, -0.70,
            1.05, -1.05,
            1.40, -1.40,
            1.75, -1.75,
            2.10, -2.10
        };

        double bestAngle = desiredAngle;
        double bestScore = -1e9;

        for (double offset : angleOffsets) {
            double candAngle = desiredAngle + offset;
            double clearance = evaluatePathClearance(x, y, candAngle, probeDist, r, tileMap, canWalkWater);
            if (clearance <= 0.0) continue; // Obstáculo sólido imediato

            // Pontuação por distância desobstruída
            double score = clearance * 5.0;

            // Alinhamento vetorial com o afastamento do predador
            double awayAlignment = Math.cos(candAngle - baseAwayAngle);
            score += awayAlignment * 3.5;

            // Continuidade de momento (evita oscilação brusca esquerda/direita)
            double headingAlignment = Math.cos(candAngle - visualAngle);
            score += headingAlignment * 1.6;

            // Análise do terreno no destino do feeler
            double farX = x + Math.cos(candAngle) * probeDist;
            double farY = y + Math.sin(candAngle) * probeDist;
            int col = (int) Math.floor(farX / TileMap.TILE_SIZE);
            int row = (int) Math.floor(farY / TileMap.TILE_SIZE);
            TileType endTile = tileMap.getTileAt(col, row);

            if (type == PassiveAnimalType.FROG) {
                if (endTile == TileType.WATER) {
                    score += 6.5; // Sapos buscam vorazmente o refúgio na água
                }
            } else {
                if (endTile == TileType.WATER) {
                    score -= 5.5; // Presas terrestres evitam lagos e rios fundos
                }
                if (type == PassiveAnimalType.RABBIT && endTile == TileType.TALL_GRASS) {
                    score += 2.0; // Coelhos preferem camuflagem na vegetação densa
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestAngle = candAngle;
            }
        }

        if (bestScore <= -1e8) {
            return findEmergencyClearanceAngle(tileMap, canWalkWater, r);
        }

        return bestAngle;
    }

    private double findEmergencyClearanceAngle(TileMap tileMap, boolean canWalkWater, double r) {
        double maxDist = -1.0;
        double bestAng = visualAngle + Math.PI; // Rotação de 180° por padrão
        for (int i = 0; i < 16; i++) {
            double ang = i * (Math.PI * 2.0 / 16.0);
            double cl = evaluatePathClearance(x, y, ang, 36.0, r, tileMap, canWalkWater);
            if (cl > maxDist) {
                maxDist = cl;
                bestAng = ang;
            }
        }
        return bestAng;
    }

    private void triggerCornerEscape(TileMap tileMap) {
        boolean canWalkWater = (type == PassiveAnimalType.FROG);
        double r = getBodyRadius();
        double bestAng = findEmergencyClearanceAngle(tileMap, canWalkWater, r);
        evasiveHeading = bestAng;
        evasiveJukeTimer = 0.38; // Trava a rota evasiva por 380ms para sair da quina
        visualAngle = bestAng;
        faceDx = Math.cos(bestAng);
        faceDy = Math.sin(bestAng);
        double burstSpeed = type.getBaseSpeed() * (type == PassiveAnimalType.DEER ? 1.35 : 1.25);
        vx = Math.cos(bestAng) * burstSpeed;
        vy = Math.sin(bestAng) * burstSpeed;
    }

    private double evaluatePathClearance(double startX, double startY, double angle, double dist, double radius, TileMap tileMap, boolean canWalkWater) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double rNear = radius + 6.0;
        double rMid = radius + dist * 0.52;
        double rFar = radius + dist;

        if (!isPositionWalkable(startX + cos * rNear, startY + sin * rNear, radius, tileMap, canWalkWater)) {
            return 0.0; // Bloqueado imediatamente em frente
        }
        if (!isPositionWalkable(startX + cos * rMid, startY + sin * rMid, radius, tileMap, canWalkWater)) {
            return 0.35; // Bloqueado a meio caminho
        }
        if (!isPositionWalkable(startX + cos * rFar, startY + sin * rFar, radius, tileMap, canWalkWater)) {
            return 0.70; // Bloqueado no final do raio
        }
        return 1.0; // Caminho 100% livre
    }

    private void moveWithCollisionSlide(double dt, TileMap tileMap) {
        double prevX = x;
        double prevY = y;
        double stepX = vx * dt;
        double stepY = vy * dt;
        double nextX = x + stepX;
        double nextY = y + stepY;

        boolean canWalkWater = (type == PassiveAnimalType.FROG);
        double r = getBodyRadius();

        // 1. Passada completa desobstruída
        if (isPositionWalkable(nextX, nextY, r, tileMap, canWalkWater)) {
            x = nextX;
            y = nextY;
            stuckTimer = Math.max(0.0, stuckTimer - dt * 2.0);
            return;
        }

        // 2. Deslizamento em eixos individuais (Wall Sliding)
        boolean moved = false;
        if (isPositionWalkable(nextX, y, r, tileMap, canWalkWater)) {
            x = nextX;
            moved = true;
        } else {
            vx *= 0.15; // Amortece componente perpendicular contra a parede
        }

        if (isPositionWalkable(x, nextY, r, tileMap, canWalkWater)) {
            y = nextY;
            moved = true;
        } else {
            vy *= 0.15;
        }

        // 3. Resolução de quinas e micro-passos diagonais
        if (!moved) {
            if (isPositionWalkable(x + stepX * 0.5, y, r, tileMap, canWalkWater)) {
                x += stepX * 0.5;
                moved = true;
            } else if (isPositionWalkable(x, y + stepY * 0.5, r, tileMap, canWalkWater)) {
                y += stepY * 0.5;
                moved = true;
            }
        }

        // 4. Detecção de travamento (Stuck Detection)
        double actualDist = Math.hypot(x - prevX, y - prevY);
        double expectedDist = Math.hypot(stepX, stepY);
        if (state == State.FLEEING && (actualDist < expectedDist * 0.25 || actualDist < 0.8)) {
            stuckTimer += dt;
        } else {
            stuckTimer = Math.max(0.0, stuckTimer - dt * 1.5);
        }
    }

    public double getBodyRadius() {
        switch (type) {
            case DEER: return 10.0;
            case RABBIT: return 6.0;
            case FROG: return 5.0;
            case BIRD: return 4.5;
            default: return 6.0;
        }
    }

    private boolean isSinglePointWalkable(double wx, double wy, TileMap tileMap, boolean canWalkWater) {
        int col = (int) Math.floor(wx / TileMap.TILE_SIZE);
        int row = (int) Math.floor(wy / TileMap.TILE_SIZE);
        TileType t = tileMap.getTileAt(col, row);
        if (canWalkWater && t == TileType.WATER) return true;
        return t.isWalkable();
    }

    private boolean isPositionWalkable(double cx, double cy, double r, TileMap tileMap, boolean canWalkWater) {
        if (!isSinglePointWalkable(cx, cy, tileMap, canWalkWater)) return false;
        if (!isSinglePointWalkable(cx - r, cy, tileMap, canWalkWater)) return false;
        if (!isSinglePointWalkable(cx + r, cy, tileMap, canWalkWater)) return false;
        if (!isSinglePointWalkable(cx, cy - r, tileMap, canWalkWater)) return false;
        if (!isSinglePointWalkable(cx, cy + r, tileMap, canWalkWater)) return false;
        return true;
    }

    public boolean isCatchable() {
        if (despawned) return false;
        if (state == State.BURROWED) return false;
        if (type == PassiveAnimalType.BIRD && state == State.FLYING_AWAY) {
            return flightHeight <= BIRD_MAX_CATCHABLE_ALTITUDE;
        }
        return true;
    }

    public boolean takeDamage(int amount) {
        if (!isCatchable()) {
            return false;
        }
        hp -= amount;
        if (hp <= 0) {
            despawned = true;
            return true;
        }
        state = State.FLEEING;
        fleeDuration = 4.0;
        stateTimer = fleeDuration;
        return false;
    }

    public List<MeatItem> createMorselDrops() {
        List<MeatItem> list = new ArrayList<>();
        int count = type.getMorselDropCount();
        for (int i = 0; i < count; i++) {
            double ox = (RAND.nextDouble() - 0.5) * 12.0;
            double oy = (RAND.nextDouble() - 0.5) * 12.0;
            list.add(new MeatItem(x + ox, y + oy, MeatTier.GAME_MORSEL, null));
        }
        return list;
    }

    public static double lerpAngle(double from, double to, double alpha) {
        double diff = normalizeAngle(to - from);
        return from + diff * alpha;
    }

    public static double normalizeAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    public PassiveAnimalType getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getFaceDx() { return faceDx; }
    public double getFaceDy() { return faceDy; }
    public double getVisualAngle() { return visualAngle; }
    public void setVisualAngle(double visualAngle) {
        this.visualAngle = visualAngle;
        this.faceDx = Math.cos(visualAngle);
        this.faceDy = Math.sin(visualAngle);
    }
    public double getWalkPhase() { return walkPhase; }
    public void setWalkPhase(double walkPhase) { this.walkPhase = walkPhase; }
    public double getBreathTimer() { return breathTimer; }
    public void setBreathTimer(double breathTimer) { this.breathTimer = breathTimer; }
    public double getPeckTimer() { return peckTimer; }
    public boolean isMoving() { return moving; }
    public void setMoving(boolean moving) { this.moving = moving; }
    public double getAnimPhase() { return animPhase; }
    public double getHopHeight() { return hopHeight; }
    public double getFlightHeight() { return flightHeight; }
    public State getState() { return state; }
    public int getHp() { return hp; }
    public boolean isDead() { return hp <= 0 || despawned; }
    public boolean isDespawned() { return despawned; }
    public double getFrogTongueTimer() { return frogTongueTimer; }
    public double getFrogTongueTargetX() { return frogTongueTargetX; }
    public double getFrogTongueTargetY() { return frogTongueTargetY; }
    public double getDrinkingWaterX() { return drinkingWaterX; }
    public double getDrinkingWaterY() { return drinkingWaterY; }
    public boolean isBurrowed() { return state == State.BURROWED; }
    public double getPeekTimer() { return peekTimer; }
    public Burrow getCurrentBurrow() { return currentBurrow; }
}
