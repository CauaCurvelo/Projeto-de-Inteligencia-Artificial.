package br.edu.unex.sentinela.world;

public class WeatherDrop {
    private double x;
    private double y;
    private final double speedY;
    private final double speedX;
    private final double length;

    public WeatherDrop(double x, double y, double speedX, double speedY, double length) {
        this.x = x;
        this.y = y;
        this.speedX = speedX;
        this.speedY = speedY;
        this.length = length;
    }

    public void update(double deltaTime, double viewportWidth, double viewportHeight) {
        x += speedX * deltaTime;
        y += speedY * deltaTime;

        // Wrap around viewport
        if (y > viewportHeight) {
            y = -10;
            x = Math.random() * viewportWidth;
        }
        if (x > viewportWidth) {
            x = 0;
        } else if (x < 0) {
            x = viewportWidth;
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeedX() { return speedX; }
    public double getSpeedY() { return speedY; }
    public double getLength() { return length; }
}

