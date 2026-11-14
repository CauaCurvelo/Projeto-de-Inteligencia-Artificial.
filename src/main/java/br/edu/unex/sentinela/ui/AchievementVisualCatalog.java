package br.edu.unex.sentinela.ui;

import br.edu.unex.sentinela.progress.Achievement;

/**
 * Catálogo visual único (DRY) que atribui ícone exclusivo e cor temática
 * por categoria para cada conquista — compartilhado entre toast e galeria.
 */
public final class AchievementVisualCatalog {
    public enum EmblemKind {
        SWORD, BOW, CROWN, MOON, APPLE, MEAT, FOX_MASK, DROPLET, STORM, CLOCK, STAR, TROPHY, HEART, EYE,
        SNOWFLAKE, SHIELD, FEATHER, COMPASS
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
            case FOUR_ALPHAS:
                return new BadgeSpec(EmblemKind.TROPHY, "#E11D48", "#FDA4AF");
            case NEAR_DEATH:
                return new BadgeSpec(EmblemKind.HEART, "#EF4444", "#FCA5A5");
            case PREY_HUNTER:
                return new BadgeSpec(EmblemKind.FOX_MASK, "#F59E0B", "#FDE68A");
            case BIRD_IN_HAND:
                return new BadgeSpec(EmblemKind.FEATHER, "#A855F7", "#E9D5FF");
            case GHOST_FOX:
                return new BadgeSpec(EmblemKind.FOX_MASK, "#A78BFA", "#DDD6FE");
            case TRAP_SURVIVOR:
                return new BadgeSpec(EmblemKind.SHIELD, "#F97316", "#FED7AA");
            case NIGHT_OWL:
                return new BadgeSpec(EmblemKind.MOON, "#60A5FA", "#BAE6FD");
            case RAIN_SURVIVOR:
                return new BadgeSpec(EmblemKind.STORM, "#38BDF8", "#BAE6FD");
            case SURVIVOR_8MIN:
                return new BadgeSpec(EmblemKind.CLOCK, "#818CF8", "#C7D2FE");
            case FORAGER:
                return new BadgeSpec(EmblemKind.APPLE, "#34D399", "#A7F3D0");
            case CARNIVORE:
                return new BadgeSpec(EmblemKind.MEAT, "#F59E0B", "#FDE68A");
            case AQUATIC:
                return new BadgeSpec(EmblemKind.DROPLET, "#2DD4BF", "#99F6E4");
            case ICE_SKATER:
                return new BadgeSpec(EmblemKind.SNOWFLAKE, "#0284C7", "#BAE6FD");
            case BIOME_VOYAGER:
                return new BadgeSpec(EmblemKind.COMPASS, "#10B981", "#A7F3D0");
            case BESTIARY_OBSERVER:
                return new BadgeSpec(EmblemKind.EYE, "#22D3EE", "#A5F3FC");
            case SCORE_1500:
                return new BadgeSpec(EmblemKind.STAR, "#FBBF24", "#FEF08A");
            case SCORE_7500:
            default:
                return new BadgeSpec(EmblemKind.TROPHY, "#E8B948", "#FEF08A");
        }
    }
}
