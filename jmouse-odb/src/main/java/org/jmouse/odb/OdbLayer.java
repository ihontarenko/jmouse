package org.jmouse.odb;

/**
 * A layer of a job's matrix: its name, what it is to the board, and — for a drill layer — the layers its holes run from
 * and to.
 *
 * @param start the layer a drill layer's holes start on; null for any other layer
 * @param end   the layer they end on; null for any other layer
 */
public record OdbLayer(String name, Type type, String start, String end) {

    /** What a layer is to the board, as the matrix's {@code TYPE} says. */
    public enum Type {
        SIGNAL,
        POWER_GROUND,
        SOLDER_MASK,
        SOLDER_PASTE,
        SILK_SCREEN,
        COMPONENT,
        DRILL,
        DOCUMENT
    }

    public OdbLayer {
        if (name == null || !name.matches("[a-z0-9_+.-]+")) {
            throw new IllegalArgumentException("An ODB++ layer is named in lowercase letters, digits and _ + . -, not '" + name + "'.");
        }
        if ((type == Type.DRILL) != (start != null && end != null)) {
            throw new IllegalArgumentException("A drill layer, and only a drill layer, names the layers its holes run between.");
        }
    }

    /** A layer that is no drill layer. */
    public static OdbLayer of(String name, Type type) {
        return new OdbLayer(name, type, null, null);
    }

    /** A drill layer, its holes running from one layer to another. */
    public static OdbLayer drill(String name, String start, String end) {
        return new OdbLayer(name, Type.DRILL, start, end);
    }
}
