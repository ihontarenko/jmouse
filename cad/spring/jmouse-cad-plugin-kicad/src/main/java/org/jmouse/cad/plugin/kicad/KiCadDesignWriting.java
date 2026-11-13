package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.table.ReadTable;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.ExportedFile;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.KiCadLibraryMap;
import org.jmouse.cad.kicad.KiCadProjectExport;
import org.jmouse.cad.kicad.KiCadSchematicExport;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;

import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.List;
import java.util.Set;

/**
 * A design written for KiCad: a circuit as a KiCad schematic, each part on the KiCad symbol the library map names for its
 * component; a pcb as a whole KiCad project — its board, the schematic of its circuit and the project file —, each
 * footprint standing for its symbol.
 */
final class KiCadDesignWriting implements DocumentExport {

    /** The map of KiCad's symbols to ours and KiCad's filing, as the engine's tables say them at each use. */
    private final ReadTable<KiCadLibraryMap> maps;
    private final ReadTable<KiCadLibraries>  filings;
    /** The ways a pcb is made, read through the engine's tables. */
    private final CadStandards               standards;
    private final Supplier<PcbArtwork.Settings> artwork;

    /** @param artwork how every format draws what no document says: curves, a designator nothing places, the mask's opening */
    KiCadDesignWriting(Supplier<PcbArtwork.Settings> artwork, ReadTable<KiCadLibraryMap> maps, ReadTable<KiCadLibraries> filings,
                       CadStandards standards) {
        this.maps = maps;
        this.filings = filings;
        this.standards = standards;
        this.artwork = artwork;
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
    public Set<DocumentKind> kinds() {
        return Set.of(SimulationModule.CIRCUIT, PcbModule.PCB);
    }

    @Override
    public ExportedFiles write(DocumentNode document, CatalogueDocuments catalogue) {
        if (document.kind().equals(PcbModule.PCB)) {
            return KiCadProjectExport.of(document, catalogue, maps.current(), filings.current(),
                    standards.pcbRules().valuesOf(PcbDrawing.of(document).rules()), artwork.get());
        }
        KiCadSchematicExport.Exported exported = KiCadSchematicExport.of(document.slug(), document.title(),
                SimulationReading.circuit(document), catalogue, maps.current(), filings.current());
        if (exported.sheets().isEmpty()) {
            return ExportedFiles.single(exported.fileName(), exported.text(), exported.notes());
        }
        // A design of sheets is a file for each, beside the root's.
        List<ExportedFile> files = new ArrayList<>();
        files.add(new ExportedFile(exported.fileName(), exported.text()));
        exported.sheets().forEach(sheet -> files.add(new ExportedFile(sheet.fileName(), sheet.text())));
        return new ExportedFiles(document.slug() + KiCadProjectExport.SET_ENDING, files, exported.notes());
    }
}
