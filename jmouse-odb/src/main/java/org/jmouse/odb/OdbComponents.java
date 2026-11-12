package org.jmouse.odb;

import java.util.ArrayList;
import java.util.List;

/**
 * A side's {@code components} file: where each component stands, and each of its toeprints with the net and subnet it joins
 * in {@code eda/data}. Rotations are clockwise as the board is seen; a mirrored component is on the bottom.
 */
public final class OdbComponents {

    /** The side of the board. */
    public enum Side {
        TOP("comp_+_top"),
        BOTTOM("comp_+_bot");

        private final String layer;

        Side(String layer) {
            this.layer = layer;
        }

        /** The matrix layer its components are kept on. */
        public String layer() {
            return layer;
        }
    }

    /**
     * A pin of a component joined to the board.
     *
     * @param pin    the pin's number in its package
     * @param x      where it is on the board
     * @param y      where it is on the board
     * @param net    its net's number in {@code eda/data}
     * @param subnet its subnet's number in that net
     */
    public record Toeprint(int pin, double x, double y, int net, int subnet, String name) {
    }

    /**
     * @param packageNumber the package's number in {@code eda/data}
     * @param rotation      degrees clockwise
     */
    public record Component(int packageNumber, double x, double y, double rotation, String name, String part, List<Toeprint> toeprints) {

        public Component {
            toeprints = List.copyOf(toeprints);
        }
    }

    private final Side            side;
    private final List<Component> components = new ArrayList<>();

    public OdbComponents(Side side) {
        this.side = side;
    }

    public Side side() {
        return side;
    }

    /** The next component. Its number is how many were given before it, the one a {@link OdbEda.Toeprint} names. */
    public OdbComponents add(Component component) {
        components.add(component);
        return this;
    }

    String text() {
        String mirror = side == Side.BOTTOM ? "M" : "N";
        StringBuilder text = new StringBuilder("UNITS=MM\n#\n");
        for (int index = 0; index < components.size(); index++) {
            Component component = components.get(index);
            String rotation = OdbNumbers.millimetres(component.rotation());
            text.append("# CMP ").append(index).append("\nCMP ").append(component.packageNumber()).append(' ')
                    .append(OdbNumbers.millimetres(component.x())).append(' ').append(OdbNumbers.millimetres(component.y())).append(' ')
                    .append(rotation).append(' ').append(mirror).append(' ')
                    .append(OdbNames.word(component.name())).append(' ').append(OdbNames.word(component.part())).append('\n');
            for (Toeprint toeprint : component.toeprints()) {
                text.append("TOP ").append(toeprint.pin()).append(' ')
                        .append(OdbNumbers.millimetres(toeprint.x())).append(' ').append(OdbNumbers.millimetres(toeprint.y())).append(' ')
                        .append(rotation).append(' ').append(mirror).append(' ')
                        .append(toeprint.net()).append(' ').append(toeprint.subnet()).append(' ').append(OdbNames.word(toeprint.name())).append('\n');
            }
            text.append("#\n");
        }
        return text.toString();
    }
}
