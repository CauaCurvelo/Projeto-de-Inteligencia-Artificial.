package br.edu.unex.sentinela.world;

public class WeatherDrop {
    private double x;
    private double y;
    private final double speedX;
    private final double speedY;
    private final double length;

    public WeatherDrop(double x, double y, double speedX, double speedY, double length) {
        this.x = x;
        this.y = y;
        this.speedX = speedX;
        this.speedY = speedY;
        this.length = length;
    }

    /**
     * Atualiza a posição da gota de chuva na tela levando em conta a velocidade relativa da câmera.
     * Na física do mundo real, a chuva cai a uma velocidade constante em relação ao solo.
     * Quando o jogador/câmera se move (camDeltaX, camDeltaY), a tela se desloca em (-camDeltaX, -camDeltaY).
     * Isso impede que a chuva pareça desacelerar ou flutuar ao correr para cima!
     */
    public void update(double deltaTime, double visibleWidth, double visibleHeight, double camDeltaX, double camDeltaY) {
        x += (speedX * deltaTime) - camDeltaX;
        y += (speedY * deltaTime) - camDeltaY;

        double margin = 35.0;
        double w = visibleWidth > 0 ? visibleWidth : 960;
        double h = visibleHeight > 0 ? visibleHeight : 640;

        // Wrap around viewport com margem suave
        if (y > h + margin) {
            y = -margin;
            x = Math.random() * (w + margin * 2) - margin;
        } else if (y < -margin) {
            y = h + margin;
            x = Math.random() * (w + margin * 2) - margin;
        }

        if (x > w + margin) {
            x = -margin;
        } else if (x < -margin) {
            x = w + margin;
        }
    }

    public void update(double deltaTime, double viewportWidth, double viewportHeight) {
        update(deltaTime, viewportWidth, viewportHeight, 0.0, 0.0);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeedX() { return speedX; }
    public double getSpeedY() { return speedY; }
    public double getLength() { return length; }
}

