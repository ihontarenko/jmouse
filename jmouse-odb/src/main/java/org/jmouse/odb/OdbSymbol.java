package org.jmouse.odb;

/**
 * A standard symbol of ODB++ — the shape a pad is flashed with or a line is drawn with —, sized in millimetres and named
 * as a job in millimetres names it: its sizes in microns ({@code r250} is a round symbol 0.25 mm across).
 */
public sealed interface OdbSymbol {

    /** The symbol's name in a features file's table: {@code r250}, {@code rect1600x1200}. */
    String name();

    record Round(double diameter) implements OdbSymbol {
        @Override
        public String name() {
            return "r" + OdbNumbers.microns(diameter);
        }
    }

    record Rectangle(double width, double height) implements OdbSymbol {
        @Override
        public String name() {
            return "rect" + OdbNumbers.microns(width) + "x" + OdbNumbers.microns(height);
        }
    }

    /** A rectangle with every corner rounded by {@code radius}. */
    record RoundedRectangle(double width, double height, double radius) implements OdbSymbol {
        @Override
        public String name() {
            return "rect" + OdbNumbers.microns(width) + "x" + OdbNumbers.microns(height) + "xr" + OdbNumbers.microns(radius);
        }
    }

    record Oval(double width, double height) implements OdbSymbol {
        @Override
        public String name() {
            return "oval" + OdbNumbers.microns(width) + "x" + OdbNumbers.microns(height);
        }
    }
}
