package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.KiCadLibraryMap;
import org.jmouse.cad.kicad.KiCadSchematicExport;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;

import java.util.Set;

/** A circuit written as a KiCad schematic, each part on the KiCad symbol the library map names for its component. */
final class KiCadSchematicWriting implements DocumentExport {

    private final KiCadLibraryMap map    = KiCadLibraryMap.standard();
    private final KiCadLibraries  filing = KiCadLibraries.standard();

    @Override
    public String id() {
        return KiCadPlugin.FORMAT;
    }

    @Override
    public String title() {
        return KiCadPlugin.TITLE;
    }

    @Override
    public Set<DocumentKind> kinds() {
        return Set.of(SimulationModule.CIRCUIT);
    }

    @Override
    public ExportedFiles write(DocumentNode document, CatalogueDocuments catalogue) {
        KiCadSchematicExport.Exported exported = KiCadSchematicExport.of(document.slug(), document.title(),
                SimulationReading.circuit(document), catalogue, map, filing);
        return ExportedFiles.single(exported.fileName(), exported.text(), exported.notes());
    }
}
