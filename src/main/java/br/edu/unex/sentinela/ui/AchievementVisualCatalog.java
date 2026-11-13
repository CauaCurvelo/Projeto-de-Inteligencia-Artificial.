package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.Achievement;

/**
 * Catálogo visual único (DRY) que atribui ícone exclusivo e cor temática
 * por categoria para cada conquista — compartilhado entre toast e galeria.
 */
public final class AchievementVisualCatalog {
    public enum EmblemKind {
        SWORD, BOW, CROWN, MOON, APPLE, MEAT, FOX_MASK, DROPLET, STORM, CLOCK, STAR, TROPHY, HEART, EYE
    }

    public static final class BadgeSpec {
        private final EmblemKind emblem;
        private final String accentHex;
        private final String glowHex;

        BadgeSpec(EmblemKind emblem, String accentHex, String glowHex) {
            this.emblem = emblem;
            this.accentHex = accentHex;
            this.glowHex = glowHex;
        }

        public EmblemKind getEmblem() { return emblem; }
        public String getAccentHex() { return accentHex; }
        public String getGlowHex() { return glowHex; }
    }

    private AchievementVisualCatalog() {}

    public static BadgeSpec resolve(Achievement achievement) {
        if (achievement == null) {
            return new BadgeSpec(EmblemKind.TROPHY, UiTheme.ACCENT_AMBER, "#FDE68A");
        }
        switch (achievement) {
            case FIRST_BITE:
                return new BadgeSpec(EmblemKind.SWORD, "#F87171", "#FCA5A5");
            case HUNTER_OF_HUNTERS:
                return new BadgeSpec(EmblemKind.BOW, "#FB923C", "#FDBA74");
            case APEX_SLAYER:
                return new BadgeSpec(EmblemKind.CROWN, "#FB7185", "#FDA4AF");
            case NEAR_DEATH:
                return new BadgeSpec(EmblemKind.HEART, "#EF4444", "#FCA5A5");
            case NIGHT_OWL:
                return new BadgeSpec(EmblemKind.MOON, "#60A5FA", "#BAE6FD");
            case RAIN_SURVIVOR:
                return new BadgeSpec(EmblemKind.STORM, "#38BDF8", "#BAE6FD");
            case SURVIVOR_5MIN:
                return new BadgeSpec(EmblemKind.CLOCK, "#818CF8", "#C7D2FE");
            case FORAGER:
                return new BadgeSpec(EmblemKind.APPLE, "#34D399", "#A7F3D0");
            case CARNIVORE:
                return new BadgeSpec(EmblemKind.MEAT, "#F59E0B", "#FDE68A");
            case GHOST_FOX:
                return new BadgeSpec(EmblemKind.FOX_MASK, "#A78BFA", "#DDD6FE");
            case AQUATIC:
                return new BadgeSpec(EmblemKind.DROPLET, "#2DD4BF", "#99F6E4");
            case BESTIARY_OBSERVER:
                return new BadgeSpec(EmblemKind.EYE, "#22D3EE", "#A5F3FC");
            case SCORE_1000:
                return new BadgeSpec(EmblemKind.STAR, "#FBBF24", "#FEF08A");
            case SCORE_5000:
            default:
                return new BadgeSpec(EmblemKind.TROPHY, "#E8B948", "#FEF08A");
        }
    }
}
