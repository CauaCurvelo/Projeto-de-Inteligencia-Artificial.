package br.edu.unex.sentinela.entity;

import br.edu.unex.sentinela.world.MeatItem;
import br.edu.unex.sentinela.world.MeatTier;
import br.edu.unex.sentinela.world.TileMap;
import br.edu.unex.sentinela.world.TileType;
import br.edu.unex.sentinela.world.WaterRipple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Entidade de fauna silvestre passiva e ágil.
 * Possui inteligência de alerta, fuga com desvios em zigue-zague e interação ambiental.
 */
public class PassiveAnimal {
    private static final Random RAND = new Random();
    public static final double BIRD_MAX_CATCHABLE_ALTITUDE = 24.0;

    public enum State {
        FORAGING,
        ALERT,
        FLEEING,
        FLYING_AWAY
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
    private double zigZagTimer = 0.0;
    private double zigZagAngle = 0.0;
    private double rippleCooldown = 0.0;
    private boolean despawned = false;

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
        update(dt, player, predators, null, tileMap, ripples);
    }

    public void update(double dt, Player player, List<Enemy> predators, List<PassiveAnimal> allPassives, TileMap tileMap, List<WaterRipple> ripples) {
        if (despawned) return;

        stateTimer -= dt;
        animPhase += dt * 6.0;
        breathTimer += dt;
        rippleCooldown -= dt;

        // 1. Identificar a ameaça mais próxima (Jogador ou Predador)
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

        // 2. Máquina de Estados da Presa
        boolean sensedThreat = nearestThreatDist <= type.getPerceptionRadius();

        if (state == State.FLYING_AWAY) {
            flightHeight += dt * 45.0;
            double flySpeed = type.getBaseSpeed() * 1.35;
            x += faceDx * flySpeed * dt;
            y += faceDy * flySpeed * dt;
            moving = true;
            walkPhase += dt * 24.0;
            double awayAngle = Math.atan2(faceDy, faceDx);
            visualAngle = lerpAngle(visualAngle, awayAngle, Math.min(1.0, dt * 10.0));
            if (flightHeight >= 90.0) {
                despawned = true;
            }
            return;
        }

        if (sensedThreat) {
            if (type == PassiveAnimalType.BIRD) {
                // Pássaro bate asas e voa para longe
                state = State.FLYING_AWAY;
                double awayAngle = Math.atan2(y - threatY, x - threatX);
                faceDx = Math.cos(awayAngle);
                faceDy = Math.sin(awayAngle);
                visualAngle = awayAngle;
                return;
            } else {
                if (state != State.FLEEING) {
                    state = State.FLEEING;
                    fleeDuration = 3.2 + RAND.nextDouble() * 1.6;
                } else {
                    fleeDuration = Math.max(fleeDuration, 2.0);
                }
                // Cervo emite sinal de alerta e assusta outros animais passivos no raio de 160px
                if (type == PassiveAnimalType.DEER && allPassives != null) {
                    for (PassiveAnimal other : allPassives) {
                        if (other == this || other.isDespawned()) continue;
                        double dOther = Math.hypot(other.getX() - x, other.getY() - y);
                        if (dOther < 160.0 && other.getState() != State.FLEEING) {
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
            state = State.FORAGING;
            stateTimer = 2.0 + RAND.nextDouble() * 3.0;
        }

        // 3. Comportamento Específico por Estado
        switch (state) {
            case FORAGING:
                updateForaging(dt, tileMap);
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
                updateFleeing(dt, threatX, threatY, tileMap);
                break;
            default:
                break;
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

    public void triggerAlertFlee(double threatX, double threatY) {
        if (state != State.FLEEING && state != State.FLYING_AWAY) {
            if (type == PassiveAnimalType.BIRD) {
                state = State.FLYING_AWAY;
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

    private void updateFleeing(double dt, double threatX, double threatY, TileMap tileMap) {
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

        // Sapo já na água: nada em alta velocidade na direção de maior fuga
        if (type == PassiveAnimalType.FROG) {
            int curC = (int) Math.floor(x / TileMap.TILE_SIZE);
            int curR = (int) Math.floor(y / TileMap.TILE_SIZE);
            if (tileMap.getTileAt(curC, curR) == TileType.WATER) {
                double bestWaterAngle = computeBestFleeAngle(threatX, threatY, tileMap);
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
        targetHeading = computeBestFleeAngle(threatX, threatY, tileMap);

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

    private double computeBestFleeAngle(double threatX, double threatY, TileMap tileMap) {
        double baseAwayAngle = Math.atan2(y - threatY, x - threatX);
        boolean canWalkWater = (type == PassiveAnimalType.FROG);
        double r = getBodyRadius();
        double probeDist = (type == PassiveAnimalType.DEER) ? 62.0 : (type == PassiveAnimalType.RABBIT ? 46.0 : 36.0);

        // Componente de zigue-zague do coelho
        double desiredAngle = baseAwayAngle;
        if (type == PassiveAnimalType.RABBIT) {
            desiredAngle += zigZagAngle;
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
    public boolean isDespawned() { return despawned; }
}
