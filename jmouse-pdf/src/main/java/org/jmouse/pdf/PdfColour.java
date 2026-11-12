package org.jmouse.pdf;

/**
 * A device colour.
 *
 * @param red   from 0 to 1
 * @param green from 0 to 1
 * @param blue  from 0 to 1
 */
public record PdfColour(double red, double green, double blue) {

    public PdfColour {
        for (double component : new double[]{red, green, blue}) {
            if (component < 0 || component > 1) {
                throw new IllegalArgumentException("A colour's component is from 0 to 1, not " + component);
            }
        }
    }

    /** The colour of a {@code #rrggbb} word. */
    public static PdfColour ofHex(String hex) {
        if (hex == null || !hex.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("A colour is written #rrggbb, not " + hex);
        }
        int value = Integer.parseInt(hex.substring(1), 16);
        return new PdfColour(((value >> 16) & 0xFF) / 255.0, ((value >> 8) & 0xFF) / 255.0, (value & 0xFF) / 255.0);
    }
}
