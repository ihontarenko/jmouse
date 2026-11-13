package org.jmouse.cad.kicad;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.KiCadProjectSettings;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.sexpression.SExpressionParseException;
import org.jmouse.sexpression.SExpressionParser;
import org.jmouse.kicad.pcb.KiCadPcb;
import org.jmouse.kicad.schematic.KiCadSchematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A KiCad design as jM documents: its footprints, symbols and components, then a circuit for each sheet, then its pcb where
 * its board was sent. Which file is which is its caller's to say.
 */
public final class KiCadDesign {

    /**
     * What a design is read from.
     *
     * @param files       every file sent, by its name
     * @param root        the root schematic's name
     * @param title       what the design is called
     * @param boardFile   the name its board would have
     * @param projectFile the name its project file would have
     * @param opened      what opening it had to say
     */
    public record Sources(Map<String, String> files, String root, String title, String boardFile, String projectFile, List<String> opened) {

        public Sources {
            files = Map.copyOf(files);
            opened = List.copyOf(opened);
        }
    }

    private KiCadDesign() {
    }

    /**
     * @param installed KiCad's own libraries: its footprints, looked in after the design's, and its symbols, which say whether a
     *                  symbol the design keeps a copy of is KiCad's
     * @throws IllegalArgumentException when a file does not read, or a sheet the design places was not sent
     */
    public static ImportedDocuments read(Sources sources, CatalogueDocuments catalogue, InstalledKiCadLibraries installed,
                                         KiCadLibraryMap map, KiCadLibraries filing) {
        Map<String, String> files = sources.files();
        Optional<KiCadPcb> board = boardOf(sources.boardFile(), files);
        KiCadProjectImport imported = KiCadProjectImport.of(project(sources.root(), files), sources.title(), map,
                KiCadCatalogue.of(catalogue), KiCadFootprintLibraries.among(files).then(installed.footprints()), installed, board, filing);
        // What a design is built from, each before what names it: a footprint before the component landing on it.
        List<DocumentNode> library = new ArrayList<>(imported.footprints());
        library.addAll(imported.symbols());
        library.addAll(imported.components());
        // The import answers the root first; a sheet is stored before the sheet that places it.
        List<DocumentNode> designs = new ArrayList<>(imported.circuits().reversed());
        List<String> notes = new ArrayList<>(sources.opened());
        notes.addAll(imported.notes());
        board.ifPresent(read -> {
            String circuit = imported.circuits().getFirst().slug();
            KiCadProjectSettings settings = Optional.ofNullable(files.get(sources.projectFile()))
                    .map(KiCadProjectSettings::of).orElse(KiCadProjectSettings.NONE);
            KiCadPcbImport.Pcb laid = KiCadPcbImport.read(read, settings, sources.boardFile(), PcbDrawing.slugOf(circuit), circuit,
                    sources.title(), imported.identifiers(), imported.ground(), documentsOf(library, designs, catalogue), filing);
            // The pcb names its circuit, so the circuit is stored first; the pcb is what the import stands for.
            designs.add(laid.document());
            notes.addAll(laid.notes());
        });
        return new ImportedDocuments(library, designs, notes);
    }

    private static Optional<KiCadPcb> boardOf(String file, Map<String, String> files) {
        String text = files.get(file);
        if (text == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(KiCadPcb.of(new SExpressionParser().parse(file, text)));
        } catch (SExpressionParseException unreadable) {
            throw new IllegalArgumentException(unreadable.getMessage(), unreadable);
        }
    }

    /** The design as it was just made, before anything else the reader may see: none of it is stored yet. */
    private static CatalogueDocuments documentsOf(List<DocumentNode> library, List<DocumentNode> designs, CatalogueDocuments catalogue) {
        Map<String, DocumentNode> made = new HashMap<>();
        library.forEach(document -> made.put(document.slug(), document));
        designs.forEach(document -> made.put(document.slug(), document));
        return slug -> Optional.ofNullable(made.get(slug)).or(() -> catalogue.find(slug));
    }

    private static KiCadProject project(String root, Map<String, String> files) {
        SExpressionParser parser = new SExpressionParser();
        Map<String, KiCadSchematic> read = new HashMap<>();
        try {
            return KiCadProject.of(root, file -> read.computeIfAbsent(file, named -> {
                String text = files.get(named);
                if (text == null) {
                    throw new IllegalArgumentException("The design places the sheet " + named + ", which was not sent: "
                                                       + "choose the project's folder, every sheet of it.");
                }
                return KiCadSchematic.of(parser.parse(named, text));
            }));
        } catch (SExpressionParseException unreadable) {
            throw new IllegalArgumentException(unreadable.getMessage(), unreadable);
        }
    }
}
