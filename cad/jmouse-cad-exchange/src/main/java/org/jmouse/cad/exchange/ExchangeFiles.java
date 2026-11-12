package org.jmouse.cad.exchange;

import org.jmouse.cad.document.Provenance;

import java.util.Base64;
import java.util.Map;

/**
 * The files an import is handed, as the reader chose them.
 *
 * @param files every file by the path it is named with: the one read first among them, and whatever it names — the
 *              sheets a design places, the libraries a netlist includes —, as the format's {@link FileEncoding} sends it
 * @param main  the file read first
 * @param title  what the made document is called, or null for the file's own word; a format may read it as which of
 *               several things in the file to take
 * @param stated whose the files are and under what terms, as the reader says them — asked for a library's files
 *               ({@link DocumentImport#libraryFileNames()}), whose text does not say it; {@link Provenance#OURS} where nothing is said
 */
public record ExchangeFiles(Map<String, String> files, String main, String title, Provenance stated) {

    /** Files of which nothing is said of whose they are: a design's, which carries its own. */
    public ExchangeFiles(Map<String, String> files, String main, String title) {
        this(files, main, title, Provenance.OURS);
    }

    public ExchangeFiles {
        files = Map.copyOf(files == null ? Map.of() : files);
        stated = stated == null ? Provenance.OURS : stated;
        if (main == null || !files.containsKey(main)) {
            throw new IllegalArgumentException("An import reads its main file first: send it among the files.");
        }
    }

    public String mainText() {
        return files.get(main);
    }

    /**
     * The main file's bytes, for a format that reads bytes ({@link FileEncoding#BINARY}): sent in Base64.
     *
     * @throws IllegalArgumentException when what was sent is no Base64
     */
    public byte[] bytesOf(String name) {
        try {
            return Base64.getDecoder().decode(files.get(name));
        } catch (IllegalArgumentException notBase64) {
            throw new IllegalArgumentException(name + " came as text, and its format reads it as bytes, sent in Base64.");
        }
    }

    /**
     * The main file's bytes, for a format that reads bytes ({@link FileEncoding#BINARY}): sent in Base64.
     *
     * @throws IllegalArgumentException when what was sent is no Base64
     */
    public byte[] mainBytes() {
        try {
            return Base64.getDecoder().decode(files.get(main));
        } catch (IllegalArgumentException notBase64) {
            throw new IllegalArgumentException(main + " came as text, and its format reads it as bytes, sent in Base64.");
        }
    }
}
