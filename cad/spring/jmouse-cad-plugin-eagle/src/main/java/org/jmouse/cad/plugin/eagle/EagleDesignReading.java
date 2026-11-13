package org.jmouse.cad.plugin.eagle;

import org.jmouse.cad.table.ReadTable;
import org.jmouse.cad.eagle.EagleDesignImport;
import org.jmouse.cad.exchange.imported.SheetMarks;
import org.jmouse.cad.eagle.EagleLibraries;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.eagle.EagleBoard;
import org.jmouse.eagle.EagleDesign;
import org.jmouse.eagle.EagleReadException;
import org.jmouse.eagle.EagleReader;
import org.jmouse.eagle.EagleSchematic;

import java.util.List;
import java.util.Optional;

/**
 * An Eagle design read as jM documents ({@link EagleDesignImport}). A schematic carries its libraries, so it is read
 * alone; a pcb is read with the schematic of its name beside it, as Eagle opens the two together — the pcb's parts
 * and nets are its circuit's.
 */
final class EagleDesignReading implements DocumentImport {

    private static final String SCHEMATIC = ".sch";
    private static final String PCB = ".brd";

    /** Its filing(), as the engine's table says it at each use. */
    private final ReadTable<EagleLibraries> filings;

    EagleDesignReading(ReadTable<EagleLibraries> filings) {
        this.filings = filings;
    }

    @Override
    public String id() {
        return EaglePlugin.FORMAT;
    }

    @Override
    public String title() {
        return EaglePlugin.TITLE;
    }

    @Override
    public List<String> fileNames() {
        return List.of("*" + SCHEMATIC, "*" + PCB);
    }

    @Override
    public List<String> fileNamesReadAlone() {
        return List.of("*" + SCHEMATIC);
    }

    @Override
    public ImportedDocuments read(ExchangeFiles files, CatalogueDocuments catalogue) {
        String main = files.main();
        String file = nameOf(main);
        String lowered = file.toLowerCase();
        if (!lowered.endsWith(SCHEMATIC) && !lowered.endsWith(PCB)) {
            throw new IllegalArgumentException("Eagle reads a schematic (*.sch) or a PCB (*.brd); '" + main + "' is neither.");
        }
        String stem = file.substring(0, file.length() - SCHEMATIC.length());
        String title = files.title() == null || files.title().isBlank() ? stem : files.title();
        if (lowered.endsWith(SCHEMATIC)) {
            return EagleDesignImport.read(schematicOf(main, files.mainText()), file, Optional.empty(), title, filing(),
                    SheetMarks.of(catalogue, filing().markSymbols()));
        }
        String schematicFile = files.files().keySet().stream().filter(name -> nameOf(name).equalsIgnoreCase(stem + SCHEMATIC))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("The PCB " + file + " is read with its schematic "
                        + stem + SCHEMATIC + ", which is not beside it: choose the design's folder, both of its files."));
        EagleBoard pcb = read(main, files.mainText()).board().orElseThrow(() -> new IllegalArgumentException(
                "'" + main + "' is an Eagle file with no PCB in it."));
        return EagleDesignImport.read(schematicOf(schematicFile, files.files().get(schematicFile)), nameOf(schematicFile),
                Optional.of(new EagleDesignImport.Read(pcb, file)), title, filing(), SheetMarks.of(catalogue, filing().markSymbols()));
    }

    private static EagleSchematic schematicOf(String name, String text) {
        return read(name, text).schematic().orElseThrow(() -> new IllegalArgumentException(
                "'" + name + "' is an Eagle file with no schematic in it."));
    }

    private static EagleDesign read(String name, String text) {
        try {
            return EagleReader.read(name, text);
        } catch (EagleReadException unreadable) {
            throw new IllegalArgumentException(unreadable.getMessage(), unreadable);
        }
    }

    private static String nameOf(String path) {
        String normalized = path.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private EagleLibraries filing() {
        return filings.current();
    }
}
