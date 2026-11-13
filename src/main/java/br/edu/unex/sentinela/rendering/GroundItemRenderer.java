package br.edu.unex.sentinela.rendering;

import br.edu.unex.sentinela.ai.EnemySkin;
import br.edu.unex.sentinela.world.Burrow;
import br.edu.unex.sentinela.world.FoodItem;
import br.edu.unex.sentinela.world.FoodVariant;
import br.edu.unex.sentinela.world.MeatItem;
import br.edu.unex.sentinela.world.MeatTier;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Renderizador modular de itens e elementos do solo (Tocas, Arbustos de Frutas e Cortes de Carne).
 * Diferencia visualmente carnes por porte/tier e espécies Alfa (Chefes), mantendo cores pré-cacheadas.
 */
public class GroundItemRenderer {
    // Toca & Sombras
    private static final Color SHADOW_SOFT = Color.color(0.0, 0.0, 0.0, 0.48);
    private static final Color SHADOW_DEEP = Color.color(0.0, 0.0, 0.0, 0.56);
    private static final Color BURROW_SHADOW = Color.color(0.02, 0.04, 0.01, 0.55);
    private static final Color BURROW_RIM_DARK = Color.web("#5C3D22");
    private static final Color BURROW_RIM_LIGHT = Color.web("#785230");
    private static final Color BURROW_INNER = Color.web("#2E1D0E");
    private static final Color BURROW_CORE = Color.web("#0D0804");

    // Arbusto & Frutinhas
    private static final Color BUSH_CAST_SHADOW = Color.color(0.02, 0.06, 0.02, 0.45);
    private static final Color BUSH_AO_SHADOW = Color.color(0.01, 0.04, 0.01, 0.65);
    private static final Color BERRY_SHADOW = Color.color(0.05, 0.12, 0.05, 0.65);

    // Ossos & Marmoreio
    private static final Color BONE_SHADE = Color.web("#CFD8DC");
    private static final Color BONE_PURE = Color.web("#FFFFFF");
    private static final Color MARBLE_CREAM = Color.web("#FFEBEE", 0.85);
    private static final Color MARBLE_GOLD = Color.web("#FFE082", 0.90);

    // Small Cut (Cão de Caça)
    private static final Color SMALL_BASE = Color.web("#7B1B15");
    private static final Color SMALL_BODY = Color.web("#D32F2F");
    private static final Color SMALL_HIGHLIGHT = Color.web("#FF8A80", 0.75);

    // Hunter Ration (Caçador Humano — carne curada/defumada com barbante)
    private static final Color RATION_BASE = Color.web("#3E1F0D");
    private static final Color RATION_BODY = Color.web("#793A1D");
    private static final Color RATION_GLAZE = Color.web("#B06535", 0.80);
    private static final Color RATION_TWINE = Color.web("#EFE0C4");

    // Beast Haunch (Animais Rivais)
    private static final Color BEAST_OUTLINE = Color.web("#560D00");
    private static final Color BEAST_BOAR_BODY = Color.web("#9E1A14");
    private static final Color BEAST_WOLF_BODY = Color.web("#B71C1C");
    private static final Color BEAST_DEFAULT_BODY = Color.web("#C62828");
    private static final Color BEAST_HIGHLIGHT = Color.web("#EF5350", 0.75);

    // Apex Boss Cuts (Urso, Pantera, Crocodilo)
    private static final Color BOSS_HALO_FILL = Color.web("#FFB300", 0.24);
    private static final Color BOSS_HALO_RING = Color.web("#FFD54F", 0.75);
    private static final Color BOSS_BEAR_BASE = Color.web("#4E0808");
    private static final Color BOSS_BEAR_BODY = Color.web("#B71C1C");
    private static final Color BOSS_BEAR_TOP = Color.web("#E53935");

    private static final Color BOSS_PANTHER_HALO = Color.web("#E040FB", 0.22);
    private static final Color BOSS_PANTHER_RING = Color.web("#EA80FC", 0.72);
    private static final Color BOSS_PANTHER_BASE = Color.web("#380A1E");
    private static final Color BOSS_PANTHER_BODY = Color.web("#880E4F");
    private static final Color BOSS_PANTHER_TOP = Color.web("#D81B60");

    private static final Color BOSS_CROC_HALO = Color.web("#00E676", 0.20);
    private static final Color BOSS_CROC_RING = Color.web("#69F0AE", 0.72);
    private static final Color BOSS_CROC_RIND = Color.web("#1B5E20");
    private static final Color BOSS_CROC_SCALES = Color.web("#2E7D32");
    private static final Color BOSS_CROC_SCALE_TIP = Color.web("#66BB6A", 0.85);
    private static final Color BOSS_CROC_BODY = Color.web("#C62828");
    private static final Color BOSS_CROC_TOP = Color.web("#EF9A9A", 0.85);

    private final GraphicsContext gc;

    public GroundItemRenderer(GraphicsContext gc) {
        this.gc = gc;
    }

    public void renderBurrow(Burrow b, double bx, double by) {
        gc.setFill(BURROW_SHADOW);
        gc.fillOval(bx - 16, by - 12, 34, 30);

        gc.setFill(BURROW_RIM_DARK);
        gc.fillOval(bx - 15, by - 14, 30, 28);
        gc.setFill(BURROW_RIM_LIGHT);
        gc.fillOval(bx - 14, by - 15, 26, 23);

        gc.setFill(BURROW_INNER);
        gc.fillOval(bx - 10, by - 10, 20, 20);

        gc.setFill(BURROW_CORE);
        gc.fillOval(bx - 8, by - 8, 16, 16);
    }

    public void drawBerryBush(double fx, double fy, double sunOffsetX, double sunOffsetY) {
        drawBerryBush(null, fx, fy, sunOffsetX, sunOffsetY);
    }

    public void drawBerryBush(FoodItem food, double fx, double fy, double sunOffsetX, double sunOffsetY) {
        FoodVariant variant = (food != null) ? food.getVariant() : FoodVariant.WILD_APPLE;
        Color leafDark = Color.web(variant.getBushDarkHex());
        Color leafMid = leafDark.deriveColor(4, 0.95, 1.42, 1.0);
        Color leafLight = Color.web(variant.getBushLightHex());
        Color leafTop = leafLight.deriveColor(-2, 0.85, 1.25, 0.88);
        Color berryMid = Color.web(variant.getBerryColorHex());
        Color berryDark = berryMid.deriveColor(0, 1.15, 0.62, 1.0);
        Color berrySpec = Color.web(variant.getHighlightColorHex());

        gc.setFill(BUSH_CAST_SHADOW);
        gc.fillOval(fx - 14 + sunOffsetX * 0.45, fy - 6 + sunOffsetY * 0.45, 30, 20);

        gc.setFill(BUSH_AO_SHADOW);
        gc.fillOval(fx - 12, fy, 24, 12);

        if (variant == FoodVariant.GLOW_MYRTLE) {
            gc.setFill(berrySpec.deriveColor(0, 1.0, 1.0, 0.18));
            gc.fillOval(fx - 17, fy - 16, 34, 30);
        }

        gc.setFill(leafDark);
        gc.fillOval(fx - 14, fy - 13, 28, 25);

        gc.setFill(leafMid);
        gc.fillOval(fx - 12, fy - 14, 23, 21);
        gc.fillOval(fx - 7, fy - 11, 20, 19);

        gc.setFill(leafLight);
        gc.fillOval(fx - 10, fy - 13, 16, 15);
        gc.setFill(leafTop);
        gc.fillOval(fx - 8, fy - 12, 10, 9);

        if (variant == FoodVariant.WILD_APPLE) {
            drawSingleBerry(fx - 6, fy - 5, 6.0, berryDark, berryMid, berrySpec);
            drawSingleBerry(fx + 3, fy - 4, 6.0, berryDark, berryMid, berrySpec);
            drawSingleBerry(fx - 1, fy + 2, 6.0, berryDark, berryMid, berrySpec);
        } else {
            // Cachos menores e numerosos para frutinhas silvestres (amora, oxicoco, mirtilo, sorveira)
            drawSingleBerry(fx - 7, fy - 5, 4.8, berryDark, berryMid, berrySpec);
            drawSingleBerry(fx - 3, fy - 8, 4.6, berryDark, berryMid, berrySpec);
            drawSingleBerry(fx + 3, fy - 5, 4.8, berryDark, berryMid, berrySpec);
            drawSingleBerry(fx + 5, fy + 0, 4.5, berryDark, berryMid, berrySpec);
            drawSingleBerry(fx - 2, fy + 2, 4.8, berryDark, berryMid, berrySpec);
        }
    }

    private void drawSingleBerry(double bx, double by, double size, Color dark, Color mid, Color spec) {
        double r = size * 0.5;
        gc.setFill(BERRY_SHADOW);
        gc.fillOval(bx - r + 0.4, by - r + 1.4, size + 0.5, size + 0.5);
        gc.setFill(dark);
        gc.fillOval(bx - r, by - r, size, size);
        gc.setFill(mid);
        gc.fillOval(bx - r + 0.4, by - r + 0.2, size * 0.80, size * 0.80);
        gc.setFill(spec);
        gc.fillOval(bx - r * 0.5, by - r * 0.65, size * 0.32, size * 0.32);
    }

    public void drawBearTrap(br.edu.unex.sentinela.world.BearTrap trap, double tx, double ty) {
        if (trap == null) return;
        boolean armed = trap.isArmed();

        gc.setFill(Color.color(0.01, 0.03, 0.01, 0.50));
        gc.fillOval(tx - 11, ty - 5, 22, 10);

        if (armed) {
            gc.setStroke(Color.web("#37474F"));
            gc.setLineWidth(2.4);
            gc.strokeOval(tx - 9, ty - 4.5, 18, 9);

            gc.setFill(Color.web("#263238"));
            gc.fillOval(tx - 3.5, ty - 2, 7, 4);
            gc.setFill(Color.web("#78909C"));
            gc.fillOval(tx - 2, ty - 1.2, 4, 2.4);

            gc.setFill(Color.web("#CFD8DC"));
            for (int i = 0; i < 6; i++) {
                double ang = i * Math.PI / 3.0;
                double dx = Math.cos(ang) * 8.5;
                double dy = Math.sin(ang) * 4.2;
                gc.fillPolygon(new double[]{tx + dx - 1.2, tx + dx + 1.2, tx + dx},
                               new double[]{ty + dy, ty + dy, ty + dy - 3.5}, 3);
            }
        } else {
            gc.setStroke(Color.web("#455A64"));
            gc.setLineWidth(2.8);
            gc.strokeArc(tx - 7, ty - 9, 14, 13, 30, 120, javafx.scene.shape.ArcType.OPEN);
            gc.strokeArc(tx - 7, ty - 9, 14, 13, 210, 120, javafx.scene.shape.ArcType.OPEN);

            gc.setFill(Color.web("#263238"));
            gc.fillOval(tx - 4, ty - 3, 8, 5);
            gc.setFill(Color.web("#B0BEC5"));
            gc.fillRect(tx - 1, ty - 7, 2, 8);
        }
    }

    public void drawMeatDrop(MeatItem meat, double mx, double my, double animTime) {
        MeatTier tier = (meat != null) ? meat.getTier() : MeatTier.SMALL_CUT;
        EnemySkin skin = (meat != null) ? meat.getSourceSkin() : null;
        boolean expiringSoon = meat != null && meat.getLifeTime() < 6.0;

        if (expiringSoon) {
            gc.save();
            gc.setGlobalAlpha(0.45 + 0.55 * (0.5 + 0.5 * Math.sin(animTime * 12.0)));
        }

        switch (tier) {
            case GAME_MORSEL:
                drawGameMorsel(mx, my);
                break;
            case APEX_PRIME_CUT:
                drawApexPrimeCut(mx, my, skin, animTime);
                break;
            case BEAST_HAUNCH:
                drawBeastHaunch(mx, my, skin);
                break;
            case HUNTER_RATION:
                drawHunterRation(mx, my);
                break;
            case SMALL_CUT:
            default:
                drawSmallCut(mx, my);
                break;
        }

        if (expiringSoon) {
            gc.restore();
        }
    }

    private void drawGameMorsel(double mx, double my) {
        gc.setFill(SHADOW_SOFT);
        gc.fillOval(mx - 5.5, my - 1.5, 11.0, 6.5);

        drawBoneKnob(mx + 3.0, my - 1.0, 2.8);

        gc.setFill(Color.web("#5C1D18"));
        gc.fillOval(mx - 5.0, my - 3.5, 9.5, 7.0);
        gc.setFill(Color.web("#B71C1C"));
        gc.fillOval(mx - 4.5, my - 3.0, 8.0, 5.5);
        gc.setFill(Color.web("#E57373", 0.85));
        gc.fillOval(mx - 3.0, my - 2.5, 4.0, 2.5);
    }

    private void drawSmallCut(double mx, double my) {
        gc.setFill(SHADOW_SOFT);
        gc.fillOval(mx - 7.5, my - 2.0, 16.0, 9.0);

        drawBoneKnob(mx + 4.5, my - 1.5, 4.0);

        gc.setFill(SMALL_BASE);
        gc.fillOval(mx - 6.5, my - 4.5, 12.5, 9.0);
        gc.setFill(SMALL_BODY);
        gc.fillOval(mx - 5.8, my - 4.5, 10.8, 7.6);
        gc.setFill(SMALL_HIGHLIGHT);
        gc.fillOval(mx - 4.2, my - 3.6, 6.5, 3.6);
    }

    private void drawHunterRation(double mx, double my) {
        gc.setFill(SHADOW_SOFT);
        gc.fillOval(mx - 10.0, my - 2.0, 21.0, 11.0);

        drawBoneKnob(mx - 9.0, my - 2.2, 4.2);

        // Peça de carne curada/defumada em formato alongado com amarrações de barbante
        gc.setFill(RATION_BASE);
        gc.fillRoundRect(mx - 8.0, my - 6.0, 16.5, 11.5, 7.0, 7.0);
        gc.setFill(RATION_BODY);
        gc.fillRoundRect(mx - 7.0, my - 5.8, 14.5, 9.5, 6.0, 6.0);
        gc.setFill(RATION_GLAZE);
        gc.fillRoundRect(mx - 5.0, my - 4.8, 10.5, 4.2, 3.5, 3.5);

        gc.setStroke(RATION_TWINE);
        gc.setLineWidth(1.4);
        gc.strokeLine(mx - 2.8, my - 5.5, mx - 2.8, my + 4.5);
        gc.strokeLine(mx + 2.8, my - 5.5, mx + 2.8, my + 4.5);
    }

    private void drawBeastHaunch(double mx, double my, EnemySkin skin) {
        gc.setFill(SHADOW_SOFT);
        gc.fillOval(mx - 11.5, my - 2.5, 24.0, 13.5);

        drawBoneKnob(mx - 11.5, my - 3.0, 5.2);
        drawBoneKnob(mx + 7.2, my - 3.0, 5.2);

        Color bodyColor = BEAST_DEFAULT_BODY;
        if (skin == EnemySkin.BOAR) {
            bodyColor = BEAST_BOAR_BODY;
        } else if (skin == EnemySkin.WOLF) {
            bodyColor = BEAST_WOLF_BODY;
        }

        gc.setFill(BEAST_OUTLINE);
        gc.fillOval(mx - 9.5, my - 7.0, 19.0, 13.5);
        gc.setFill(bodyColor);
        gc.fillOval(mx - 8.5, my - 7.0, 17.0, 11.5);
        gc.setFill(BEAST_HIGHLIGHT);
        gc.fillOval(mx - 6.0, my - 5.8, 10.5, 5.2);

        // Veio sutil de gordura
        gc.setFill(MARBLE_CREAM);
        gc.fillOval(mx - 3.0, my - 3.8, 5.5, 2.0);
    }

    private void drawApexPrimeCut(double mx, double my, EnemySkin skin, double animTime) {
        double pulse = 0.5 + 0.5 * Math.sin(animTime * 4.5);
        Color haloFill = BOSS_HALO_FILL;
        Color haloRing = BOSS_HALO_RING;
        if (skin == EnemySkin.PANTHER) {
            haloFill = BOSS_PANTHER_HALO;
            haloRing = BOSS_PANTHER_RING;
        } else if (skin == EnemySkin.CROCODILE) {
            haloFill = BOSS_CROC_HALO;
            haloRing = BOSS_CROC_RING;
        } else if (skin == EnemySkin.POLAR_BEAR) {
            haloFill = Color.web("#38BDF8", 0.24);
            haloRing = Color.web("#BAE6FD", 0.60);
        }

        // Halo pulsante indicativo de presa Alfa / Chefe
        double auraW = 34.0 + pulse * 4.5;
        double auraH = 22.0 + pulse * 3.0;
        gc.save();
        gc.setGlobalAlpha(0.60 + pulse * 0.40);
        gc.setFill(haloFill);
        gc.fillOval(mx - auraW * 0.5, my - auraH * 0.45, auraW, auraH);
        gc.setStroke(haloRing);
        gc.setLineWidth(1.5);
        gc.strokeOval(mx - auraW * 0.46, my - auraH * 0.42, auraW * 0.92, auraH * 0.84);
        gc.restore();

        // Sombra pesada no solo
        gc.setFill(SHADOW_DEEP);
        gc.fillOval(mx - 15.0, my - 3.0, 31.0, 18.0);

        if (skin == EnemySkin.CROCODILE) {
            drawCrocodilePrimeCut(mx, my);
            return;
        }

        // Ossos espessos cruzados/duplos para Urso e Pantera
        drawBoneKnob(mx - 15.0, my - 4.2, 6.8);
        drawBoneKnob(mx + 9.0, my - 4.2, 6.8);
        drawBoneKnob(mx - 3.2, my - 11.0, 5.5);

        Color base = (skin == EnemySkin.PANTHER) ? BOSS_PANTHER_BASE : BOSS_BEAR_BASE;
        Color body = (skin == EnemySkin.PANTHER) ? BOSS_PANTHER_BODY : BOSS_BEAR_BODY;
        Color top = (skin == EnemySkin.PANTHER) ? BOSS_PANTHER_TOP : BOSS_BEAR_TOP;

        gc.setFill(base);
        gc.fillOval(mx - 13.0, my - 9.2, 26.0, 18.5);
        gc.setFill(body);
        gc.fillOval(mx - 11.8, my - 9.0, 23.6, 16.0);
        gc.setFill(top);
        gc.fillOval(mx - 8.5, my - 7.5, 16.5, 9.0);

        // Marmoreio nobre duplo
        gc.setFill(skin == EnemySkin.PANTHER ? MARBLE_CREAM : MARBLE_GOLD);
        gc.fillOval(mx - 6.0, my - 4.8, 9.0, 2.6);
        gc.fillOval(mx - 1.5, my - 1.2, 7.0, 2.2);
    }

    private void drawCrocodilePrimeCut(double mx, double my) {
        drawBoneKnob(mx - 14.0, my - 3.0, 6.2);
        drawBoneKnob(mx + 8.5, my - 3.0, 6.2);

        // Crosta externa escamada característica da cauda do Crocodilo do Rio
        gc.setFill(BOSS_CROC_RIND);
        gc.fillRoundRect(mx - 12.5, my - 9.0, 25.0, 17.5, 9.0, 9.0);
        gc.setFill(BOSS_CROC_SCALES);
        gc.fillRoundRect(mx - 11.5, my - 8.6, 23.0, 15.5, 8.0, 8.0);

        // Escamas dorsais sutis
        gc.setFill(BOSS_CROC_SCALE_TIP);
        gc.fillOval(mx - 6.0, my - 7.6, 3.6, 2.6);
        gc.fillOval(mx - 0.5, my - 7.8, 3.6, 2.6);
        gc.fillOval(mx + 4.8, my - 7.2, 3.4, 2.4);

        // Posta interna suculenta
        gc.setFill(BOSS_CROC_BODY);
        gc.fillOval(mx - 9.2, my - 5.2, 18.4, 11.5);
        gc.setFill(BOSS_CROC_TOP);
        gc.fillOval(mx - 6.5, my - 3.8, 12.5, 5.8);
        gc.setFill(MARBLE_CREAM);
        gc.fillOval(mx - 3.0, my - 2.0, 6.5, 2.0);
    }

    private void drawBoneKnob(double x, double y, double size) {
        double radius = size;
        gc.setFill(BONE_SHADE);
        gc.fillOval(x, y, radius, radius * 1.15);
        gc.setFill(BONE_PURE);
        gc.fillOval(x + 0.6, y + 0.5, radius * 0.72, radius * 0.80);
    }
}
