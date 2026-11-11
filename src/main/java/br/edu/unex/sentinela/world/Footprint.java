package br.edu.unex.sentinela.world;

public class Footprint {
    public enum PrintStyle {
        PAW,    // Almofada + 4 dedos (Raposa, Lobo, Cão, Urso, Pantera, Texugo)
        BOOT,   // Sola de bota + salto (Caçador Humano)
        HOOF    // Casco fendido (Javali)
    }

    private final double x;
    private final double y;
    private final double angle;
    private final boolean leftFoot;
    private final PrintStyle style;
    private final boolean playerOrigin;
    private double lifeTime;
    private final double maxLifeTime;

    public Footprint(double x, double y, double duration) {
        this(x, y, 0.0, true, PrintStyle.PAW, true, duration);
    }

    public Footprint(double x, double y, double angle, boolean leftFoot, PrintStyle style, boolean playerOrigin, double duration) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.leftFoot = leftFoot;
        this.style = style;
        this.playerOrigin = playerOrigin;
        this.lifeTime = duration;
        this.maxLifeTime = duration;
    }

    public void update(double deltaTime) {
        this.lifeTime -= deltaTime;
    }

    public boolean isExpired() {
        return lifeTime <= 0;
    }

    public double getAlpha() {
        return Math.max(0.0, lifeTime / maxLifeTime);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
    public boolean isLeftFoot() { return leftFoot; }
    public PrintStyle getStyle() { return style; }
    public boolean isPlayerOrigin() { return playerOrigin; }
}
