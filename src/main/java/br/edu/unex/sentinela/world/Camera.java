package br.edu.unex.sentinela.world;

public class Camera {
    private double x;
    private double y;
    private double viewportWidth;
    private double viewportHeight;
    private double zoom = 1.0; // Zoom factor: 1.0 is default, 1.4 is close-up, 0.75 is overview

    public Camera(double viewportWidth, double viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public void updateViewport(double width, double height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    public void zoomIn() {
        this.zoom = Math.min(1.50, this.zoom + 0.15);
    }

    public void zoomOut() {
        this.zoom = Math.max(0.70, this.zoom - 0.15);
    }

    // Infinite map camera: directly centers on target with smooth tracking and zoom
    public void update(double targetX, double targetY) {
        double visibleWidth = viewportWidth / zoom;
        double visibleHeight = viewportHeight / zoom;
        this.x = targetX - (visibleWidth / 2.0);
        this.y = targetY - (visibleHeight / 2.0);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getViewportWidth() { return viewportWidth; }
    public double getViewportHeight() { return viewportHeight; }
    public double getZoom() { return zoom; }
    public void setZoom(double zoom) { this.zoom = zoom; }
}
