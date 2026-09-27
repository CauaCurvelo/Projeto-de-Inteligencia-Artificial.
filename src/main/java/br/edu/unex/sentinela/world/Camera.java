package br.edu.unex.sentinela.world;

public class Camera {
    private double x;
    private double y;
    private double viewportWidth;
    private double viewportHeight;

    public Camera(double viewportWidth, double viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public void updateViewport(double width, double height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    // Infinite map camera: directly centers on target with smooth tracking, no borders
    public void update(double targetX, double targetY) {
        this.x = targetX - (viewportWidth / 2.0);
        this.y = targetY - (viewportHeight / 2.0);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getViewportWidth() { return viewportWidth; }
    public double getViewportHeight() { return viewportHeight; }
}
