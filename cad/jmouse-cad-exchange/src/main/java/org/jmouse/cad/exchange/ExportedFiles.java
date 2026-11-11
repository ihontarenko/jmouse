package org.jmouse.cad.exchange;

import java.util.List;

/**
 * A document written in a format: one file — a SPICE deck, a KiCad schematic — or a set — the files a board house makes
 * a board from.
 *
 * @param name  what the whole is called: the file's own name when it is one, the set's otherwise ({@code amplifier-gerber})
 * @param notes what the format could not be told, part by part
 */
public record ExportedFiles(String name, List<ExportedFile> files, List<String> notes) {

    public ExportedFiles {
        if (files.isEmpty()) {
            throw new IllegalArgumentException("A document is written as one file at least.");
        }
        files = List.copyOf(files);
        notes = List.copyOf(notes == null ? List.of() : notes);
    }

    /** A document written as one file, named as the file is. */
    public static ExportedFiles single(String fileName, String text, List<String> notes) {
        return new ExportedFiles(fileName, List.of(new ExportedFile(fileName, text)), notes);
    }
}
