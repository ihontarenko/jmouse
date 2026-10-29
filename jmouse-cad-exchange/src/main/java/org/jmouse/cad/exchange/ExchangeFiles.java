package org.jmouse.cad.exchange;

import java.util.Map;

/**
 * The files an import is handed, as the reader chose them.
 *
 * @param files every file by the path it is named with: the one read first among them, and whatever it names — the
 *              sheets a design places, the libraries a netlist includes
 * @param main  the file read first
 * @param title what the made document is called, or null for the file's own word; a format may read it as which of
 *              several things in the file to take
 */
public record ExchangeFiles(Map<String, String> files, String main, String title) {

    public ExchangeFiles {
        files = Map.copyOf(files == null ? Map.of() : files);
        if (main == null || !files.containsKey(main)) {
            throw new IllegalArgumentException("An import reads its main file first: send it among the files.");
        }
    }

    public String mainText() {
        return files.get(main);
    }
}
