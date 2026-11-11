package org.jmouse.cad.exchange;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;

import java.util.Set;

/** A format a document is written in: a circuit as a SPICE deck or a KiCad schematic, a component as a {@code .subckt}. */
public interface DocumentExport extends Identified {

    Contributions<DocumentExport> POINT = new Contributions<>("cad.exchange.exports", DocumentExport.class);

    String title();

    /** The kinds of document it writes. */
    Set<DocumentKind> kinds();

    /**
     * @param catalogue the components the document's parts name, as the reader may see them
     * @throws IllegalArgumentException when the document cannot be said in the format, with what
     */
    ExportedFiles write(DocumentNode document, CatalogueDocuments catalogue);
}
