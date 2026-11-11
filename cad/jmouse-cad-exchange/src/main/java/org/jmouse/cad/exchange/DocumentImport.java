package org.jmouse.cad.exchange;

import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;

import java.util.List;

/**
 * A format whose files read as jM documents: a SPICE netlist, a KiCad design. It only converts — the files it is handed
 * are what the reader chose, it opens nothing itself, and storing what it made is the store's.
 */
public interface DocumentImport extends Identified {

    Contributions<DocumentImport> POINT = new Contributions<>("cad.exchange.imports", DocumentImport.class);

    String title();

    /** The files it reads, as a file chooser filters them: {@code *.cir}, {@code *.kicad_sch}. */
    List<String> fileNames();

    /**
     * The files as documents.
     *
     * @param catalogue the components a converted part may name, as the reader may see them
     * @throws ExchangeRefusal          when a file does not read, with where
     * @throws IllegalArgumentException when what it reads cannot be carried into jM, with what
     */
    ImportedDocuments read(ExchangeFiles files, CatalogueDocuments catalogue);
}
