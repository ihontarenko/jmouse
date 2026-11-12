package org.jmouse.eagle;

/**
 * How Eagle turns a thing, as its {@code rot} says: {@code R90} is a quarter turn counter-clockwise, {@code MR180} the
 * same mirrored about the vertical — on a board, a part on the bottom —, {@code SR45} spun, its text not kept upright.
 *
 * @param angle    degrees counter-clockwise, 0 … 360
 * @param mirrored whether it is mirrored about the vertical
 * @param spin     whether its text turns with it
 */
public record EagleRotation(double angle, boolean mirrored, boolean spin) {

    public static final EagleRotation NONE = new EagleRotation(0, false, false);

    /** A {@code rot} as Eagle writes it; none for an empty one. */
    public static EagleRotation of(String written) {
        if (written == null || written.isBlank()) {
            return NONE;
        }
        String rest = written.strip();
        boolean mirrored = false;
        boolean spin = false;
        while (!rest.isEmpty() && (rest.charAt(0) == 'M' || rest.charAt(0) == 'S')) {
            mirrored |= rest.charAt(0) == 'M';
            spin |= rest.charAt(0) == 'S';
            rest = rest.substring(1);
        }
        if (!rest.startsWith("R")) {
            throw new EagleReadException("'" + written + "' is no rotation: Eagle writes R90, MR180, SR45");
        }
        double angle = Double.parseDouble(rest.substring(1));
        return new EagleRotation(((angle % 360) + 360) % 360, mirrored, spin);
    }
}
