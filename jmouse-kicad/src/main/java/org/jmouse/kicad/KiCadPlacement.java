package org.jmouse.kicad;

/**
 * How a symbol stands on its sheet: where its origin is, how far it is turned, and whether it is mirrored — and so where
 * each of its pins lands. KiCad turns a symbol by a multiple of 90° counter-clockwise as the sheet is seen, then
 * mirrors it: {@code (mirror x)} upside down, {@code (mirror y)} left for right. A library draws Y upwards and a sheet
 * downwards, so a pin's point is turned over first.
 *
 * @param at     the symbol's origin on the sheet
 * @param angle  degrees, 0, 90, 180 or 270
 * @param mirror how it is mirrored
 */
public record KiCadPlacement(KiCadPoint at, int angle, Mirror mirror) {

    /** How a symbol is mirrored: not, upside down ({@code x}), or left for right ({@code y}). */
    public enum Mirror {
        NONE(""), X("x"), Y("y");

        private final String word;

        Mirror(String word) {
            this.word = word;
        }

        public String word() {
            return word;
        }

        /** The mirror a {@code (mirror …)} word writes; none for no word. */
        public static Mirror of(String word) {
            for (Mirror mirror : values()) {
                if (mirror.word.equals(word)) {
                    return mirror;
                }
            }
            throw new IllegalArgumentException("(mirror " + word + ") is no mirror: x or y");
        }
    }

    public KiCadPlacement {
        angle = Math.floorMod(angle, 360);
        if (angle % 90 != 0) {
            throw new IllegalArgumentException("A symbol stands at a multiple of 90°, not " + angle + "°.");
        }
        mirror = mirror == null ? Mirror.NONE : mirror;
    }

    /** Where a point of the symbol's own drawing — Y upwards — lands on the sheet. */
    public KiCadPoint place(KiCadPoint inSymbol) {
        double x = inSymbol.x();
        double y = -inSymbol.y();
        double turnedX;
        double turnedY;
        switch (angle) {
            case 90 -> {
                turnedX = y;
                turnedY = -x;
            }
            case 180 -> {
                turnedX = -x;
                turnedY = -y;
            }
            case 270 -> {
                turnedX = -y;
                turnedY = x;
            }
            default -> {
                turnedX = x;
                turnedY = y;
            }
        }
        if (mirror == Mirror.Y) {
            turnedX = -turnedX;
        } else if (mirror == Mirror.X) {
            turnedY = -turnedY;
        }
        return at.plus(new KiCadPoint(turnedX, turnedY));
    }
}
