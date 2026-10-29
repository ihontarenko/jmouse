package org.jmouse.cad.spice;

/**
 * Something a conversion did not carry across, or carried in a way the reader should know of — named, never dropped.
 *
 * @param line the line it is about in the text that was converted; 0 when it is about the whole text
 * @param text what happened, in a sentence
 */
public record SpiceNote(int line, String text) {

    /** {@code line 12: E1 (a voltage-controlled source) has no jMX counterpart; left out}. */
    @Override
    public String toString() {
        return line > 0 ? "line " + line + ": " + text : text;
    }
}
