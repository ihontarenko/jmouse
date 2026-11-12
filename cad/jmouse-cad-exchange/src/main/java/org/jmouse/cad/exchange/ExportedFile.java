package org.jmouse.cad.exchange;

/**
 * One file a document is written as: its name, and its text — or, for a format that is not text (GLB), its bytes, which
 * travel as base64.
 *
 * @param text    the file's text; null for a binary file
 * @param content the file's bytes; null for a text file
 */
public record ExportedFile(String fileName, String text, byte[] content) {

    public ExportedFile {
        if ((text == null) == (content == null)) {
            throw new IllegalArgumentException("A file is either its text or its bytes: " + fileName);
        }
    }

    public ExportedFile(String fileName, String text) {
        this(fileName, text, null);
    }

    public static ExportedFile binary(String fileName, byte[] content) {
        return new ExportedFile(fileName, null, content);
    }
}
