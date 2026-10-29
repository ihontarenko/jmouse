package org.jmouse.cad.exchange;

/** One file a document is written as: its name, and its text. */
public record ExportedFile(String fileName, String text) {
}
