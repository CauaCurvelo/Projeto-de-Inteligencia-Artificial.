package br.edu.unex.sentinela.settings;

/**
 * Níveis de fidelidade gráfica para otimização de performance.
 */
public enum GraphicsQuality {
    LOW("Baixa (Máxima Fluidez)"),
    MEDIUM("Média (Equilibrada)"),
    HIGH("Alta (Máximos Detalhes 2.5D)");

    private final String label;

    GraphicsQuality(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
