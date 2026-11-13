package br.edu.unex.sentinela.world;

import br.edu.unex.sentinela.ai.EnemySkin;

/**
 * Representa um item de carne derrubado no solo, diferenciando tier nutricional e espécie de origem.
 */
public class MeatItem {
    private final double x;
    private final double y;
    private final MeatTier tier;
    private final EnemySkin sourceSkin;
    private boolean eaten = false;
    private double lifeTime;

    public MeatItem(double x, double y) {
        this(x, y, MeatTier.SMALL_CUT, null);
    }

    public MeatItem(double x, double y, MeatTier tier, EnemySkin sourceSkin) {
        this.x = x;
        this.y = y;
        this.tier = tier != null ? tier : MeatTier.SMALL_CUT;
        this.sourceSkin = sourceSkin;
        this.lifeTime = this.tier.getLifeTimeSeconds();
    }

    public void update(double deltaTime) {
        lifeTime -= deltaTime;
        if (lifeTime <= 0) {
            eaten = true;
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public MeatTier getTier() { return tier; }
    public EnemySkin getSourceSkin() { return sourceSkin; }
    public double getLifeTime() { return lifeTime; }
    public boolean isEaten() { return eaten; }
    public void setEaten(boolean eaten) { this.eaten = eaten; }
}
