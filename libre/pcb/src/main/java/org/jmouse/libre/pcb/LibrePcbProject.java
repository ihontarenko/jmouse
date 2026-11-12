package org.jmouse.libre.pcb;

import org.jmouse.sexpression.SExpressionParser;
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

    private static final SExpressionParser PARSER = new SExpressionParser();

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

    private static LibrePcbLibrary library(Map<String, String> files) {
        Map<String, LibrePcbLibrary.Symbol> symbols = new LinkedHashMap<>();
        Map<String, LibrePcbLibrary.Component> components = new LinkedHashMap<>();
        Map<String, LibrePcbLibrary.Device> devices = new LinkedHashMap<>();
        Map<String, LibrePcbLibrary.Package> packages = new LinkedHashMap<>();
        files.forEach((path, text) -> LibrePcbFile.Element.of(path).ifPresent(element -> {
            SExpressionList read = parsed(path, text);
            switch (element) {
                case SYMBOL -> symbols.put(read.word(1), LibrePcbLibrary.Symbol.of(read));
                case COMPONENT -> components.put(read.word(1), LibrePcbLibrary.Component.of(read));
                case DEVICE -> devices.put(read.word(1), LibrePcbLibrary.Device.of(read));
                case PACKAGE -> packages.put(read.word(1), LibrePcbLibrary.Package.of(read));
            }
        }));
        return new LibrePcbLibrary(symbols, components, devices, packages);
    }

    private static SExpressionList required(Map<String, String> files, String path) {
        String text = files.get(path);
        if (text == null) {
            throw new LibrePcbReadException("The project's " + path + " is not among its files: choose the project's whole folder.");
        }
        return parsed(path, text);
    }

    private static SExpressionList parsed(String path, String text) {
        try {
            return PARSER.parse(path, text).root();
        } catch (IllegalArgumentException unread) {
            throw new LibrePcbReadException(path + " does not read: " + unread.getMessage(), unread);
        }
    }
}
