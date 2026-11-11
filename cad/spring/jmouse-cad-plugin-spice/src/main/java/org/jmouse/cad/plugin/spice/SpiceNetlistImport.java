package org.jmouse.cad.plugin.spice;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ExchangeRefusal;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.spice.SpiceConverter;
import org.jmouse.cad.spice.SpiceIncludes;
import org.jmouse.spice.SpiceParseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A SPICE netlist read as a jM document: a circuit for a deck, a component for a {@code .subckt} alone. The files beside
 * the main one are what its {@code .include} and {@code .lib} lines name.
 */
final class SpiceNetlistImport implements DocumentImport {

    @Override
    public String id() {
        return SpicePlugin.FORMAT;
    }

    @Override
    public String title() {
        return SpicePlugin.TITLE;
    }

    @Override
    public List<String> fileNames() {
        return SpicePlugin.FILE_NAMES;
    }

    @Override
    public ImportedDocuments read(ExchangeFiles files, CatalogueDocuments catalogue) {
        Map<String, String> includes = new HashMap<>(files.files());
        includes.remove(files.main());
        SpiceConverter.SpiceImport imported;
        try {
            imported = new SpiceConverter(catalogue, CadLanguage.standard().language())
                    .importNetlist(files.mainText(), files.title(), SpiceIncludes.of(includes));
        } catch (SpiceParseException refused) {
            throw new ExchangeRefusal(refused.getDetail(), refused.getPosition().line(), refused.getPosition().column());
        }
        DocumentNode document = imported.document();
        boolean circuit = document.kind().equals(SimulationModule.CIRCUIT);
        return new ImportedDocuments(circuit ? List.of() : List.of(document), circuit ? List.of(document) : List.of(),
                                     imported.notes());
    }
}
