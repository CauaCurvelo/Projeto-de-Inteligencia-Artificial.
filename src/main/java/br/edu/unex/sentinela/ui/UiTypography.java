package br.edu.unex.sentinela.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashSet;
import java.util.Set;

/**
 * Gerenciador de tipografia dinâmica e responsiva.
 * Seleciona famílias modernas do sistema e acopla o tamanho das fontes à dimensão da Scene.
 */
public final class UiTypography {
    private static final String SCALE_PROP_KEY = "sentinela.uiScaleProperty";
    private static final double BASE_W = 960.0;
    private static final double BASE_H = 640.0;

    private static final String HERO_FAMILY;
    private static final String DISPLAY_FAMILY;
    private static final String UI_FAMILY;
    private static final String MONO_FAMILY;

    static {
        Set<String> installed = new HashSet<>(Font.getFamilies());
        HERO_FAMILY = pickFamily(installed, "Bahnschrift SemiBold", "Bahnschrift Condensed", "Bahnschrift", "Trebuchet MS", "System");
        DISPLAY_FAMILY = pickFamily(installed, "Bahnschrift SemiBold", "Bahnschrift", "Trebuchet MS", "Segoe UI", "System");
        UI_FAMILY = pickFamily(installed, "Segoe UI Variable Display", "Segoe UI Variable Text", "Segoe UI", "System");
        MONO_FAMILY = pickFamily(installed, "Consolas", "Cascadia Mono", "Monospaced");
    }

    public enum FamilySlot {
        HERO, DISPLAY, UI, MONO
    }

    public enum TypoRole {
        HERO(54.0, FontWeight.BOLD, FamilySlot.HERO),
        SUBHERO(11.5, FontWeight.BOLD, FamilySlot.DISPLAY),
        TITLE(25.0, FontWeight.BOLD, FamilySlot.DISPLAY),
        SECTION(17.5, FontWeight.BOLD, FamilySlot.DISPLAY),
        ACTION(13.5, FontWeight.BOLD, FamilySlot.DISPLAY),
        BODY(13.0, FontWeight.NORMAL, FamilySlot.UI),
        LABEL(12.5, FontWeight.SEMI_BOLD, FamilySlot.UI),
        NUMERIC(14.5, FontWeight.BOLD, FamilySlot.MONO),
        KEYCAP(11.5, FontWeight.BOLD, FamilySlot.MONO),
        MICRO(11.0, FontWeight.SEMI_BOLD, FamilySlot.UI);

        final double baseSize;
        final FontWeight weight;
        final FamilySlot slot;

        TypoRole(double baseSize, FontWeight weight, FamilySlot slot) {
            this.baseSize = baseSize;
            this.weight = weight;
            this.slot = slot;
        }
    }

    private UiTypography() {}

    public static String getHeroFamily() { return HERO_FAMILY; }
    public static String getDisplayFamily() { return DISPLAY_FAMILY; }
    public static String getUiFamily() { return UI_FAMILY; }
    public static String getMonoFamily() { return MONO_FAMILY; }

    /**
     * Registra listeners de largura/altura na Scene para atualizar a escala tipográfica em tempo real.
     */
    public static void attachToScene(Scene scene) {
        if (scene == null) return;
        scaleProperty(scene);
    }

    /**
     * Retorna a propriedade de escala vinculada à Scene (criando-a caso ainda não exista).
     */
    public static ReadOnlyDoubleProperty scaleProperty(Scene scene) {
        Object existing = scene.getProperties().get(SCALE_PROP_KEY);
        if (existing instanceof DoubleProperty dp) {
            return dp;
        }
        SimpleDoubleProperty scaleProp = new SimpleDoubleProperty(computeScale(scene.getWidth(), scene.getHeight()));
        ChangeListener<Number> resizeListener = (obs, oldVal, newVal) ->
                scaleProp.set(computeScale(scene.getWidth(), scene.getHeight()));
        scene.widthProperty().addListener(resizeListener);
        scene.heightProperty().addListener(resizeListener);
        scene.getProperties().put(SCALE_PROP_KEY, scaleProp);
        return scaleProp;
    }

    public static double computeScale(double sceneW, double sceneH) {
        if (sceneW <= 0 || sceneH <= 0) return 1.0;
        double raw = Math.min(sceneW / BASE_W, sceneH / BASE_H);
        double smooth = 1.0 + (raw - 1.0) * 0.72;
        return Math.max(0.88, Math.min(1.46, smooth));
    }

    public static double getScale(Node node) {
        Scene scene = (node != null) ? node.getScene() : null;
        return (scene != null) ? scaleProperty(scene).get() : 1.0;
    }

    public static Font font(TypoRole role, double scale) {
        double quantized = Math.round(role.baseSize * scale * 2.0) / 2.0;
        String family = switch (role.slot) {
            case HERO -> HERO_FAMILY;
            case DISPLAY -> DISPLAY_FAMILY;
            case UI -> UI_FAMILY;
            case MONO -> MONO_FAMILY;
        };
        return Font.font(family, role.weight, quantized);
    }

    /**
     * Vincula dinamicamente a fonte de qualquer Labeled ao tamanho da Scene atual e futuras trocas de Scene.
     */
    public static <T extends Labeled> T bind(T control, TypoRole role) {
        if (control == null) return null;
        control.setFont(font(role, getScale(control)));
        applySceneBinding(control, control.getScene(), role);
        control.sceneProperty().addListener((obs, oldScene, newScene) ->
                applySceneBinding(control, newScene, role));
        return control;
    }

    /**
     * Configura ComboBox com buttonCell e cellFactory tipograficamente dinâmicas.
     */
    public static <T> void bindComboBox(ComboBox<T> combo, TypoRole role, java.util.function.Function<T, String> labelFn) {
        combo.setButtonCell(createBoundCell(role, labelFn));
        combo.setCellFactory(listView -> createBoundCell(role, labelFn));
    }

    /**
     * Escala dinamicamente dimensões máximas de painéis acompanhando a Scene (com margem de segurança).
     */
    public static void bindScaledPanel(Region region, double baseWidth, double baseHeight) {
        if (region == null) return;
        applyPanelBounds(region, region.getScene(), baseWidth, baseHeight);
        region.sceneProperty().addListener((obs, oldScene, newScene) ->
                applyPanelBounds(region, newScene, baseWidth, baseHeight));
    }

    /**
     * Mantém larguras laterais (ex.: lista do Bestiário) proporcionais ao tamanho dinâmico das fontes.
     */
    public static void bindScaledWidth(Region region, double baseWidth) {
        if (region == null) return;
        applyScaledWidth(region, region.getScene(), baseWidth);
        region.sceneProperty().addListener((obs, oldScene, newScene) ->
                applyScaledWidth(region, newScene, baseWidth));
    }

    private static void applyPanelBounds(Region region, Scene scene, double baseWidth, double baseHeight) {
        region.maxWidthProperty().unbind();
        region.maxHeightProperty().unbind();
        if (scene == null) {
            region.setMaxWidth(baseWidth);
            if (baseHeight > 0) region.setMaxHeight(baseHeight);
            return;
        }
        ReadOnlyDoubleProperty scaleProp = scaleProperty(scene);
        region.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.min(Math.max(280.0, scene.getWidth() - 40.0), baseWidth * scaleProp.get()),
                scaleProp, scene.widthProperty()));
        if (baseHeight > 0) {
            region.maxHeightProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.min(Math.max(260.0, scene.getHeight() - 40.0), baseHeight * scaleProp.get()),
                    scaleProp, scene.heightProperty()));
        } else {
            region.setMaxHeight(Region.USE_PREF_SIZE);
        }
    }

    private static void applyScaledWidth(Region region, Scene scene, double baseWidth) {
        region.minWidthProperty().unbind();
        region.prefWidthProperty().unbind();
        region.maxWidthProperty().unbind();
        if (scene == null) {
            region.setMinWidth(baseWidth);
            region.setPrefWidth(baseWidth);
            region.setMaxWidth(baseWidth);
            return;
        }
        ReadOnlyDoubleProperty scaleProp = scaleProperty(scene);
        var widthBinding = Bindings.createDoubleBinding(() -> (double) Math.round(baseWidth * scaleProp.get()), scaleProp);
        region.minWidthProperty().bind(widthBinding);
        region.prefWidthProperty().bind(widthBinding);
        region.maxWidthProperty().bind(widthBinding);
    }

    private static <T> ListCell<T> createBoundCell(TypoRole role, java.util.function.Function<T, String> labelFn) {
        ListCell<T> cell = new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : labelFn.apply(item));
            }
        };
        bind(cell, role);
        return cell;
    }

    private static void applySceneBinding(Labeled control, Scene scene, TypoRole role) {
        control.fontProperty().unbind();
        if (scene == null) {
            control.setFont(font(role, 1.0));
            return;
        }
        ReadOnlyDoubleProperty scaleProp = scaleProperty(scene);
        control.fontProperty().bind(Bindings.createObjectBinding(
                () -> font(role, scaleProp.get()), scaleProp));
    }

    private static String pickFamily(Set<String> installed, String... candidates) {
        for (String c : candidates) {
            if ("System".equals(c) || "Monospaced".equals(c) || installed.contains(c)) {
                return c;
            }
        }
        return "System";
    }
}
