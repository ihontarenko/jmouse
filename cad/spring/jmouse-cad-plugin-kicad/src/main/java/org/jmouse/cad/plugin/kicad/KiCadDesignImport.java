package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.kicad.KiCadCatalogue;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.KiCadLibraryMap;
import org.jmouse.cad.kicad.KiCadProjectImport;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.kicad.parser.KiCadParseException;
import org.jmouse.kicad.parser.KiCadParser;
import org.jmouse.kicad.schematic.KiCadSchematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A KiCad design read as jM documents: its footprints, symbols and components, then a circuit for each sheet. The main
 * file is the root schematic; the others are the sheets it places and the footprints of the project's own libraries
 * ({@code LIBRARY.pretty/NAME.kicad_mod}). A footprint only KiCad holds is looked up in its installed libraries.
 */
final class KiCadDesignImport implements DocumentImport {

    private static final String SCHEMATIC_SUFFIX = ".kicad_sch";

    private final KiCadFootprintLibraries installedFootprints;
    private final KiCadLibraryMap         map    = KiCadLibraryMap.standard();
    private final KiCadLibraries          filing = KiCadLibraries.standard();

    KiCadDesignImport(KiCadFootprintLibraries installedFootprints) {
        this.installedFootprints = installedFootprints;
    }

    @Override
    public String id() {
        return KiCadPlugin.FORMAT;
    }

    @Override
    public String title() {
        return KiCadPlugin.TITLE;
    }

    @Override
    public List<String> fileNames() {
        return List.of("*" + SCHEMATIC_SUFFIX, "*.kicad_mod", "*.kicad_pro");
    }

    @Override
    public ImportedDocuments read(ExchangeFiles files, CatalogueDocuments catalogue) {
        String root = files.main();
        if (!root.endsWith(SCHEMATIC_SUFFIX)) {
            throw new IllegalArgumentException("A KiCad design is read from its root schematic, a " + SCHEMATIC_SUFFIX
                                               + " file; '" + root + "' is not one.");
        }
        String name = files.title() == null || files.title().isBlank() ? root.substring(0, root.length() - SCHEMATIC_SUFFIX.length())
                : files.title();
        KiCadProjectImport imported = KiCadProjectImport.of(project(root, files.files()), name, map,
                KiCadCatalogue.of(catalogue), KiCadFootprintLibraries.among(files.files()).then(installedFootprints), filing);
        // What a design is built from, each before what names it: a footprint before the component landing on it.
        List<DocumentNode> library = new ArrayList<>(imported.footprints());
        library.addAll(imported.symbols());
        library.addAll(imported.components());
        // The import answers the root first; a sheet is stored before the sheet that places it.
        return new ImportedDocuments(library, imported.circuits().reversed(), imported.notes());
    }

    private static KiCadProject project(String root, Map<String, String> files) {
        KiCadParser parser = new KiCadParser();
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
        } catch (KiCadParseException unreadable) {
            throw new IllegalArgumentException(unreadable.getMessage(), unreadable);
        }
    }
}
