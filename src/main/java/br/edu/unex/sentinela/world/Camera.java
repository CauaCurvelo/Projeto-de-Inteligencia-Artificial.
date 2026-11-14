package br.edu.unex.sentinela.world;

public class Camera {
    private double x;
    private double y;
    private double deltaX = 0.0;
    private double deltaY = 0.0;
    private boolean initialized = false;
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

    public void reset() {
        this.initialized = false;
        this.deltaX = 0.0;
        this.deltaY = 0.0;
        this.shakeTimer = 0.0;
        this.shakeDuration = 0.0;
        this.shakeIntensity = 0.0;
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
        double nextX = targetX - (visibleWidth / 2.0) + offsetX;
        double nextY = targetY - (visibleHeight / 2.0) + offsetY;

        if (!initialized) {
            this.x = nextX;
            this.y = nextY;
            this.deltaX = 0.0;
            this.deltaY = 0.0;
            this.initialized = true;
        } else {
            this.deltaX = nextX - this.x;
            this.deltaY = nextY - this.y;
            this.x = nextX;
            this.y = nextY;
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getCenterX() { return x + (getVisibleWidth() / 2.0); }
    public double getCenterY() { return y + (getVisibleHeight() / 2.0); }
    public double getDeltaX() { return deltaX; }
    public double getDeltaY() { return deltaY; }
    public double getVisibleWidth() { return viewportWidth / zoom; }
    public double getVisibleHeight() { return viewportHeight / zoom; }
    public double getViewportWidth() { return viewportWidth; }
    public double getViewportHeight() { return viewportHeight; }
    public double getZoom() { return zoom; }
    public void setZoom(double zoom) { this.zoom = zoom; }

    /**
     * Verifica se uma coordenada do mundo (wx, wy) está dentro da área visível da câmera,
     * considerando uma margem de segurança ao redor das bordas da tela.
     */
    public boolean isPointInView(double wx, double wy, double margin) {
        double left = x - margin;
        double right = x + getVisibleWidth() + margin;
        double top = y - margin;
        double bottom = y + getVisibleHeight() + margin;
        return wx >= left && wx <= right && wy >= top && wy <= bottom;
    }

    /**
     * Calcula a distância mínima necessária a partir do centro da câmera ao longo de um ângulo
     * para que um ponto fique completamente fora da tela (além da margem de segurança).
     */
    public double getMinOffscreenDistance(double angle, double margin) {
        double halfW = (getVisibleWidth() / 2.0) + margin;
        double halfH = (getVisibleHeight() / 2.0) + margin;
        double cosA = Math.abs(Math.cos(angle));
        double sinA = Math.abs(Math.sin(angle));
        double distW = cosA > 0.0001 ? halfW / cosA : Double.MAX_VALUE;
        double distH = sinA > 0.0001 ? halfH / sinA : Double.MAX_VALUE;
        return Math.min(distW, distH);
    }
}
