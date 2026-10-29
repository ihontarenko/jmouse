package org.jmouse.cad.plugin.spice;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.spice.SpiceConverter;

import java.util.Set;

/** A circuit written as a SPICE deck, a component as its {@code .model} or {@code .subckt}. */
final class SpiceDeckExport implements DocumentExport {

    /** What a deck's file ends with, and a library's of models and subcircuits. */
    private static final String DECK_SUFFIX    = ".cir";
    private static final String LIBRARY_SUFFIX = ".lib";

    @Override
    public String id() {
        return SpicePlugin.FORMAT;
    }

    @Override
    public String title() {
        return SpicePlugin.TITLE;
    }

    @Override
    public Set<DocumentKind> kinds() {
        return Set.of(SimulationModule.CIRCUIT, SimulationModule.COMPONENT);
    }

    @Override
    public ExportedFiles write(DocumentNode document, CatalogueDocuments catalogue) {
        SpiceConverter.SpiceExport exported = new SpiceConverter(catalogue, CadLanguage.standard().language())
                .exportDocument(document);
        String suffix = document.kind().equals(SimulationModule.CIRCUIT) ? DECK_SUFFIX : LIBRARY_SUFFIX;
        return ExportedFiles.single(document.slug() + suffix, exported.text(), exported.notes());
    }
}
