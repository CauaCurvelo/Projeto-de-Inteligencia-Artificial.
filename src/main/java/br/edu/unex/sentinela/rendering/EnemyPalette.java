package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.ai.AnimalVariant;
import br.edu.unex.sentinela.ai.EnemySkin;
import javafx.scene.paint.Color;

/**
 * Paleta anatômica para renderização de animais e suas variações regionais
 * sem duplicar rotinas de desenho vetorial em CharacterSpriteRenderer.
 */
public final class EnemyPalette {
    public final Color primary;
    public final Color secondary;
    public final Color dark;
    public final Color accent;
    public final Color trim;
    public final Color eye;

    private EnemyPalette(Color primary, Color secondary, Color dark, Color accent, Color trim, Color eye) {
        this.primary = primary;
        this.secondary = secondary;
        this.dark = dark;
        this.accent = accent;
        this.trim = trim;
        this.eye = eye;
    }

    public static EnemyPalette of(EnemySkin skin, AnimalVariant variant) {
        AnimalVariant v = variant != null ? variant : AnimalVariant.STANDARD;
        if (skin == null) return wolf(v);
        switch (skin) {
            case HUNTER_SNIPER: return hunter(v);
            case HOUND_TRACKER: return hound(v);
            case BOAR: return boar(v);
            case BADGER: return badger(v);
            case WOLF:
            default: return wolf(v);
        }
    }

    private static EnemyPalette hunter(AnimalVariant v) {
        switch (v) {
            case MARSH:
                return new EnemyPalette(Color.web("#435E2B"), Color.web("#2B3D1B"), Color.web("#332318"),
                    Color.web("#3B4D2B"), Color.web("#81C784"), Color.web("#D7CCC8"));
            case JUNGLE:
                return new EnemyPalette(Color.web("#1D6B3C"), Color.web("#114024"), Color.web("#263829"),
                    Color.web("#264D2C"), Color.web("#FFB74D"), Color.web("#D7CCC8"));
            case ALPINE:
                return new EnemyPalette(Color.web("#3949AB"), Color.web("#242D6B"), Color.web("#263238"),
                    Color.web("#455A64"), Color.web("#81D4FA"), Color.web("#ECEFF1"));
            case ARCTIC:
                return new EnemyPalette(Color.web("#CBD5E1"), Color.web("#64748B"), Color.web("#1E293B"),
                    Color.web("#94A3B8"), Color.web("#38BDF8"), Color.web("#F8FAFC"));
            case STANDARD:
            default:
                return new EnemyPalette(Color.web("#8E2424"), Color.web("#5A1818"), Color.web("#4E342E"),
                    Color.web("#795548"), Color.web("#C0392B"), Color.web("#D7CCC8"));
        }
    }

    private static EnemyPalette hound(AnimalVariant v) {
        switch (v) {
            case MARSH:
                return new EnemyPalette(Color.web("#79553A"), Color.web("#543A26"), Color.web("#332215"),
                    Color.web("#3E2723"), Color.web("#26A69A"), Color.web("#FFF176"));
            case JUNGLE:
                return new EnemyPalette(Color.web("#423034"), Color.web("#291C1F"), Color.web("#181012"),
                    Color.web("#211618"), Color.web("#FFB300"), Color.web("#76FF03"));
            case ALPINE:
                return new EnemyPalette(Color.web("#78909C"), Color.web("#546E7A"), Color.web("#2E3E47"),
                    Color.web("#37474F"), Color.web("#40C4FF"), Color.web("#FFF59D"));
            case ARCTIC:
                return new EnemyPalette(Color.web("#E2E8F0"), Color.web("#475569"), Color.web("#1E293B"),
                    Color.web("#334155"), Color.web("#38BDF8"), Color.web("#7DD3FC"));
            case STANDARD:
            default:
                return new EnemyPalette(Color.web("#6C3483"), Color.web("#4A235A"), Color.web("#23112B"),
                    Color.web("#2E1236"), Color.web("#E74C3C"), Color.web("#FFEB3B"));
        }
    }

    private static EnemyPalette wolf(AnimalVariant v) {
        switch (v) {
            case MARSH:
                return new EnemyPalette(Color.web("#8D4E32"), Color.web("#6B3721"), Color.web("#3E2115"),
                    Color.web("#C77C54"), Color.web("#522818"), Color.web("#FFCC80"));
            case JUNGLE:
                return new EnemyPalette(Color.web("#2D4238"), Color.web("#1D2E26"), Color.web("#121D18"),
                    Color.web("#4E7360"), Color.web("#385245"), Color.web("#69F0AE"));
            case ALPINE:
                return new EnemyPalette(Color.web("#90A4AE"), Color.web("#607D8B"), Color.web("#37474F"),
                    Color.web("#ECEFF1"), Color.web("#78909C"), Color.web("#80D8FF"));
            case ARCTIC:
                return new EnemyPalette(Color.web("#F1F5F9"), Color.web("#94A3B8"), Color.web("#475569"),
                    Color.web("#FFFFFF"), Color.web("#CBD5E1"), Color.web("#38BDF8"));
            case STANDARD:
            default:
                return new EnemyPalette(Color.web("#546E7A"), Color.web("#37474F"), Color.web("#263238"),
                    Color.web("#78909C"), Color.web("#455A64"), Color.web("#FFC107"));
        }
    }

    private static EnemyPalette boar(AnimalVariant v) {
        switch (v) {
            case MARSH:
                return new EnemyPalette(Color.web("#4B4736"), Color.web("#312F23"), Color.web("#1B1A13"),
                    Color.web("#657042"), Color.web("#DCE775"), Color.web("#7A735A"));
            case JUNGLE:
                return new EnemyPalette(Color.web("#844329"), Color.web("#572917"), Color.web("#2A130A"),
                    Color.web("#1E100B"), Color.web("#FFF8E1"), Color.web("#A85B3B"));
            case ALPINE:
                return new EnemyPalette(Color.web("#6D5954"), Color.web("#4A3C38"), Color.web("#29211E"),
                    Color.web("#CFD8DC"), Color.web("#FFF8E1"), Color.web("#9E8881"));
            case ARCTIC:
                return new EnemyPalette(Color.web("#A8A29E"), Color.web("#57534E"), Color.web("#292524"),
                    Color.web("#E7E5E4"), Color.web("#F8FAFC"), Color.web("#D6D3D1"));
            case STANDARD:
            default:
                return new EnemyPalette(Color.web("#6D4C41"), Color.web("#4E342E"), Color.web("#271914"),
                    Color.web("#21120E"), Color.web("#FFF8E1"), Color.web("#8D6E63"));
        }
    }

    private static EnemyPalette badger(AnimalVariant v) {
        switch (v) {
            case MARSH:
                return new EnemyPalette(Color.web("#495740"), Color.web("#2F3828"), Color.web("#1A2116"),
                    Color.web("#1F261A"), Color.web("#E6EE9C"), Color.web("#CFD8DC"));
            case JUNGLE:
                return new EnemyPalette(Color.web("#2C2C2C"), Color.web("#1A1A1A"), Color.web("#0D0D0D"),
                    Color.web("#111111"), Color.web("#FFE082"), Color.web("#FFD54F"));
            case ALPINE:
                return new EnemyPalette(Color.web("#78909C"), Color.web("#455A64"), Color.web("#263238"),
                    Color.web("#1A242B"), Color.web("#FFFFFF"), Color.web("#B3E5FC"));
            case ARCTIC:
                return new EnemyPalette(Color.web("#E2E8F0"), Color.web("#334155"), Color.web("#0F172A"),
                    Color.web("#1E293B"), Color.web("#FFFFFF"), Color.web("#38BDF8"));
            case STANDARD:
            default:
                return new EnemyPalette(Color.web("#455A64"), Color.web("#263238"), Color.web("#1C262B"),
                    Color.web("#11171A"), Color.web("#FFFFFF"), Color.web("#CFD8DC"));
        }
    }
}
