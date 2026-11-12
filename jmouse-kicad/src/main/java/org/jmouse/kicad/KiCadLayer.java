package org.jmouse.kicad;

/** The layers a board's lists name that mean something beyond their own name, as KiCad writes them. */
public final class KiCadLayer {

    /** Where a board's edge is drawn. */
    public static final String EDGE = "Edge.Cuts";
    /** What a copper layer's name ends with. */
    public static final String COPPER_SUFFIX = ".Cu";
    /** What a solder mask layer's name ends with. */
    public static final String MASK_SUFFIX = ".Mask";
    /** What a paste layer's name ends with. */
    public static final String PASTE_SUFFIX = ".Paste";
    /** The mask on both sides, in one word. */
    public static final String ALL_MASK = "*.Mask";
    /** Every copper layer, in one word. */
    public static final String ALL_COPPER = "*.Cu";
    /** Both outer copper layers, in one word. */
    public static final String BOTH_SIDES_COPPER = "F&B.Cu";
    public static final String FRONT_SILKSCREEN = "F.SilkS";
    public static final String BACK_SILKSCREEN = "B.SilkS";

    /** Whether a layer is on the board's front: {@code F.Mask}. */
    public static boolean isFront(String layer) {
        return layer.startsWith("F.");
    }

    /** Whether a layer is on the board's back: {@code B.Mask}. */
    public static boolean isBack(String layer) {
        return layer.startsWith("B.");
    }

    private KiCadLayer() {
    }
}
