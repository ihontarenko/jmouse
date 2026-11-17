package org.jmouse.libre.pcb;

import org.jmouse.sexpression.node.SExpressionList;

import java.util.List;

/**
 * What an installed library says of itself in its {@code library.lp}: who it is, its version, and the libraries its elements name
 * elements of — a component of one, a device of another.
 *
 * @param id           its UUID: the same wherever it is installed, whatever its folder is called
 * @param name         its name to a reader
 * @param version      its version, as it writes it
 * @param dependencies the UUIDs of the libraries it depends on
 */
public record LibrePcbLibraryHeader(String id, String name, String version, List<String> dependencies) {

    /** The file an installed library says itself in, inside its folder. */
    public static final String FILE = "library.lp";

    public LibrePcbLibraryHeader {
        dependencies = List.copyOf(dependencies);
    }

    /**
     * @param path where the file was read, which a refusal names
     * @throws LibrePcbReadException when the file does not read
     */
    public static LibrePcbLibraryHeader read(String path, String text) {
        SExpressionList header = LibrePcbParsing.parsed(path, text);
        return new LibrePcbLibraryHeader(header.word(1), LibrePcbValues.text(header, LibrePcbKeyword.NAME),
                LibrePcbValues.text(header, LibrePcbKeyword.VERSION),
                header.lists(LibrePcbKeyword.DEPENDENCY).stream().map(dependency -> dependency.word(1)).toList());
    }
}
