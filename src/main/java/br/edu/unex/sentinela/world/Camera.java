package br.edu.unex.sentinela.world;

public class Camera {
    private double x;
    private double y;
    private double viewportWidth;
    private double viewportHeight;
    private double zoom = 1.0; // Zoom factor: 1.0 is default, 1.4 is close-up, 0.75 is overview
    private double shakeTimer = 0.0;
    private double shakeDuration = 0.0;
    private double shakeIntensity = 0.0;
    private double shakePhase = 0.0;

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

    /** Inicia um tremor momentâneo na câmera (usado por golpes sísmicos e rugidos de chefe). */
    public void triggerShake(double intensity, double duration) {
        if (duration <= 0 || intensity <= 0) return;
        this.shakeIntensity = Math.max(this.shakeIntensity, intensity);
        this.shakeDuration = Math.max(this.shakeDuration, duration);
        this.shakeTimer = Math.max(this.shakeTimer, duration);
    }

    public void updateShake(double deltaTime) {
        if (shakeTimer > 0) {
            shakeTimer = Math.max(0.0, shakeTimer - deltaTime);
            shakePhase += deltaTime * 52.0;
            if (shakeTimer <= 0.0) {
                shakeIntensity = 0.0;
                shakeDuration = 0.0;
            }
        }
    }

    // Infinite map camera: directly centers on target with optional shake offset and zoom
    public void update(double targetX, double targetY) {
        double visibleWidth = viewportWidth / zoom;
        double visibleHeight = viewportHeight / zoom;
        double offsetX = 0.0;
        double offsetY = 0.0;
        if (shakeTimer > 0 && shakeDuration > 0) {
            double damp = shakeTimer / shakeDuration;
            double mag = shakeIntensity * damp;
            offsetX = Math.sin(shakePhase) * mag;
            offsetY = Math.cos(shakePhase * 1.37) * mag * 0.85;
        }
        this.x = targetX - (visibleWidth / 2.0) + offsetX;
        this.y = targetY - (visibleHeight / 2.0) + offsetY;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getViewportWidth() { return viewportWidth; }
    public double getViewportHeight() { return viewportHeight; }
    public double getZoom() { return zoom; }
    public void setZoom(double zoom) { this.zoom = zoom; }
}
