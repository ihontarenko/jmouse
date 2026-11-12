package org.jmouse.cad.exchange;

/**
 * How a format's files travel to the server: as the text they are, or — a format whose files are bytes, a compound file
 * — as those bytes in Base64, the one form a JSON request carries them in unharmed.
 */
public enum FileEncoding {
    /** The file's text. */
    TEXT,
    /** The file's bytes, in Base64. */
    BINARY
}
