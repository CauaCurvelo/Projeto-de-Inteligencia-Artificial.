package br.edu.unex.sentinela.audio;

/**
 * Fachada de síntese dos efeitos sonoros curtos (SFX).
 * Delega para especialistas de passos, vozes de criaturas e gameplay orgânico.
 */
final class SfxSynthesizer {
    private SfxSynthesizer() {}

    static float[] create(SoundId id) {
        return createVariants(id)[0];
    }

    static float[][] createVariants(SoundId id) {
        if (FootstepSynthesizer.supports(id)) {
            return FootstepSynthesizer.createVariants(id);
        }
        if (CreatureVoiceSynthesizer.supports(id)) {
            return CreatureVoiceSynthesizer.createVariants(id);
        }
        return GameplaySfxSynthesizer.createVariants(id);
    }
}
