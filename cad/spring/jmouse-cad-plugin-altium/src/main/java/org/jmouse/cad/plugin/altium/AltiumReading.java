package org.jmouse.cad.plugin.altium;

import org.jmouse.cad.table.ReadTable;
import org.jmouse.altium.AltiumPcbLibrary;
import org.jmouse.altium.AltiumPcbLibraryReader;
import org.jmouse.altium.AltiumReadException;
import org.jmouse.altium.AltiumSchematic;
import org.jmouse.altium.AltiumSchematicReader;
import org.jmouse.cad.altium.AltiumLibraries;
import org.jmouse.cad.altium.AltiumPcbLibraryImport;
import org.jmouse.cad.altium.AltiumSchematicImport;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.FileEncoding;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.exchange.imported.SheetMarks;
import org.jmouse.cad.simulation.CatalogueDocuments;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * An Altium file read as jM documents: a schematic ({@link AltiumSchematicImport}) — the symbols and components its parts
 * are drawn with, and the sheet as a circuit —, or a PCB library ({@link AltiumPcbLibraryImport}) — its footprints and the
 * 3D models it embeds. Both are bytes — a compound file —, so they travel as them ({@link FileEncoding#BINARY}), and each
 * is read alone. A PCB library is somebody's work: whose it is, the reader says ({@link #libraryFileNames()}).
 */
final class AltiumReading implements DocumentImport {

    private static final String SCHEMATIC = ".schdoc";
    private static final String PCB_LIBRARY = ".pcblib";
    private static final List<String> SCHEMATICS = List.of("*.SchDoc");
    private static final List<String> PCB_LIBRARIES = List.of("*.PcbLib");

    /** Its filing(), as the engine's table says it at each use. */
    private final ReadTable<AltiumLibraries> filings;

    AltiumReading(ReadTable<AltiumLibraries> filings) {
        this.filings = filings;
    }

    @Override
    public String id() {
        return AltiumPlugin.FORMAT;
    }

    @Override
    public String title() {
        return AltiumPlugin.TITLE;
    }

    @Override
    public List<String> fileNames() {
        return List.of(SCHEMATICS.getFirst(), PCB_LIBRARIES.getFirst());
    }

    @Override
    public List<String> fileNamesReadAlone() {
        return fileNames();
    }

    @Override
    public List<String> libraryFileNames() {
        return PCB_LIBRARIES;
    }

    @Override
    public FileEncoding encoding() {
        return FileEncoding.BINARY;
    }

    @Override
    public ImportedDocuments read(ExchangeFiles files, CatalogueDocuments catalogue) {
        String main = files.main();
        String normalized = main.replace('\\', '/');
        String file = normalized.substring(normalized.lastIndexOf('/') + 1);
        String lower = file.toLowerCase(Locale.ROOT);
        if (lower.endsWith(PCB_LIBRARY)) {
            AltiumPcbLibrary library = readable(() -> AltiumPcbLibraryReader.read(file, files.mainBytes()));
            // Whose the library is, as the reader says it, before it is filed by the library it comes from.
            return AltiumPcbLibraryImport.read(library, file, filing()).whoseAsStated(files.stated(), file);
        }
        if (!lower.endsWith(SCHEMATIC)) {
            throw new IllegalArgumentException("Altium reads a schematic (*.SchDoc) or a PCB library (*.PcbLib); '" + main + "' is neither.");
        }
        AltiumSchematic sheet = readable(() -> AltiumSchematicReader.read(file, files.mainBytes()));
        String title = files.title() == null || files.title().isBlank() ? file.substring(0, file.length() - SCHEMATIC.length())
                : files.title();
        return AltiumSchematicImport.read(sheet, file, title, filing(), SheetMarks.of(catalogue, filing().markSymbols()), catalogue);
    }

    /** What a file reads as, or why it does not, as the import says a refusal. */
    private static <T> T readable(Supplier<T> reading) {
        try {
            return reading.get();
        } catch (AltiumReadException unreadable) {
            throw new IllegalArgumentException(unreadable.getMessage(), unreadable);
        }
    }

    private AltiumLibraries filing() {
        return filings.current();
    }
}
