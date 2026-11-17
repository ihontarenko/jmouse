package org.jmouse.libre.pcb;

import org.jmouse.sexpression.node.SExpressionList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A LibrePCB project as its files say it: its metadata, its circuit, its sheets and boards, and the library it carries.
 * Millimetres with Y upwards, angles counter-clockwise.
 */
public record LibrePcbProject(String name, String author, String version, LibrePcbCircuit circuit, List<LibrePcbSchematic> schematics,
                              List<LibrePcbBoard> boards, LibrePcbLibrary library) {

    public LibrePcbProject {
        schematics = List.copyOf(schematics);
        boards = List.copyOf(boards);
    }

    /**
     * @param files the project's files by their path inside its folder: {@code circuit/circuit.lp}
     * @throws LibrePcbReadException naming what is missing or what does not read
     */
    public static LibrePcbProject read(Map<String, String> files) {
        SExpressionList metadata = required(files, LibrePcbFile.METADATA.path());
        return new LibrePcbProject(LibrePcbValues.text(metadata, LibrePcbKeyword.NAME), LibrePcbValues.text(metadata, LibrePcbKeyword.AUTHOR),
                LibrePcbValues.text(metadata, LibrePcbKeyword.VERSION), LibrePcbCircuit.of(required(files, LibrePcbFile.CIRCUIT.path())),
                listed(files, LibrePcbFile.SCHEMATICS, LibrePcbKeyword.SCHEMATIC).stream().map(LibrePcbSchematic::of).toList(),
                listed(files, LibrePcbFile.BOARDS, LibrePcbKeyword.BOARD).stream().map(LibrePcbBoard::of).toList(), library(files));
    }

    /** The files an index names, in its order: {@code (board "boards/default/board.lp")}. */
    private static List<SExpressionList> listed(Map<String, String> files, LibrePcbFile index, LibrePcbKeyword item) {
        return required(files, index.path()).lists(item).stream().map(entry -> required(files, entry.word(1))).toList();
    }

    /** The library the project carries: its files under {@link LibrePcbFile#PROJECT_LIBRARY}, read as any library is. */
    private static LibrePcbLibrary library(Map<String, String> files) {
        Map<String, String> carried = new LinkedHashMap<>();
        files.forEach((path, text) -> {
            if (path.startsWith(LibrePcbFile.PROJECT_LIBRARY)) {
                carried.put(path.substring(LibrePcbFile.PROJECT_LIBRARY.length()), text);
            }
        });
        return LibrePcbLibrary.read(carried);
    }

    private static SExpressionList required(Map<String, String> files, String path) {
        String text = files.get(path);
        if (text == null) {
            throw new LibrePcbReadException("The project's " + path + " is not among its files: choose the project's whole folder.");
        }
        return LibrePcbParsing.parsed(path, text);
    }
}
