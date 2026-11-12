package org.jmouse.pdf;

import java.util.Locale;

/**
 * What is drawn on one page of a {@link PdfDocument}: paths, stroked or filled, in points from the page's lower left corner.
 * A path is begun by {@link #moveTo}, grown by {@link #lineTo} and ended by {@link #stroke}, {@link #fill} or
 * {@link #fillEvenOdd}. Words are set by {@link #text}, in a font the document embeds, so they read the same anywhere and can
 * be searched and copied.
 */
public final class PdfCanvas {

    /** How many decimals a coordinate keeps: a hundred-thousandth of a point is far below what any device shows. */
    private static final String NUMBER = "%.5f";

    private final double        width;
    private final double        height;
    private final StringBuilder content = new StringBuilder();
    private final PdfDocument   document;

    PdfCanvas(PdfDocument document, double width, double height) {
        this.document = document;
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("A page is wider and higher than nothing: " + width + " by " + height);
        }
        this.width = width;
        this.height = height;
        // Round ends and joins: a line of any width closes on itself, and lines meeting at a point meet cleanly.
        content.append("1 J 1 j\n");
    }

    /** How wide the page is, in points. */
    public double width() {
        return width;
    }

    /** How high the page is, in points. */
    public double height() {
        return height;
    }

    /** How wide the lines stroked from here on are, in points. */
    public PdfCanvas lineWidth(double points) {
        if (points < 0) {
            throw new IllegalArgumentException("A line is not narrower than nothing: " + points);
        }
        content.append(number(points)).append(" w\n");
        return this;
    }

    /** The colour lines are stroked and areas filled with from here on. */
    public PdfCanvas colour(PdfColour colour) {
        String components = number(colour.red()) + " " + number(colour.green()) + " " + number(colour.blue());
        content.append(components).append(" RG ").append(components).append(" rg\n");
        return this;
    }

    public PdfCanvas moveTo(double x, double y) {
        content.append(number(x)).append(' ').append(number(y)).append(" m\n");
        return this;
    }

    public PdfCanvas lineTo(double x, double y) {
        content.append(number(x)).append(' ').append(number(y)).append(" l\n");
        return this;
    }

    /** The path's piece closed back to where it began. */
    public PdfCanvas close() {
        content.append("h\n");
        return this;
    }

    /** The path drawn as lines, and ended. */
    public PdfCanvas stroke() {
        content.append("S\n");
        return this;
    }

    /** The path filled by the nonzero rule, and ended. */
    public PdfCanvas fill() {
        content.append("f\n");
        return this;
    }

    /** The path filled by the even-odd rule, and ended: a piece inside another is a hole. */
    public PdfCanvas fillEvenOdd() {
        content.append("f*\n");
        return this;
    }

    /**
     * Words on one line, in the colour last set.
     *
     * @param font    a font of this document, from {@link PdfDocument#embed}
     * @param size    its size, in points
     * @param x       where the line starts, on its baseline
     * @param y       its baseline
     * @param degrees how far it is turned about that point, anticlockwise
     */
    public PdfCanvas text(PdfFont font, double size, double x, double y, double degrees, String words) {
        String resource = document.resourceOf(font);
        double radians = Math.toRadians(degrees);
        StringBuilder glyphs = new StringBuilder();
        words.codePoints().forEach(character -> glyphs.append(String.format(Locale.ROOT, "%04X", document.glyphUsed(font, character))));
        content.append("BT /").append(resource).append(' ').append(number(size)).append(" Tf ")
                .append(number(Math.cos(radians))).append(' ').append(number(Math.sin(radians))).append(' ')
                .append(number(-Math.sin(radians))).append(' ').append(number(Math.cos(radians))).append(' ')
                .append(number(x)).append(' ').append(number(y)).append(" Tm <").append(glyphs).append("> Tj ET\n");
        return this;
    }

    String content() {
        return content.toString();
    }

    private static String number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("A page holds finite numbers, not " + value);
        }
        String written = String.format(Locale.ROOT, NUMBER, value);
        int end = written.length();
        while (written.charAt(end - 1) == '0') {
            end--;
        }
        if (written.charAt(end - 1) == '.') {
            end--;
        }
        String trimmed = written.substring(0, end);
        return trimmed.equals("-0") ? "0" : trimmed;
    }
}
