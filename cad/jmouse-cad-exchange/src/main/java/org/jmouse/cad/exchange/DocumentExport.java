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

    /** Whether it can write now; one that stands on a program of the server cannot while no program is set. */
    default boolean available() {
        return true;
    }

    /** What its files are for: what a factory makes from is written only from a document its checks pass. */
    default ExportPurpose purpose() {
        return ExportPurpose.EXCHANGE;
    }

    /** The kinds of document it writes. */
    Set<DocumentKind> kinds();

    /**
     * @param catalogue the components the document's parts name, as the reader may see them
     * @throws IllegalArgumentException when the document cannot be said in the format, with what
     */
    ExportedFiles write(DocumentNode document, CatalogueDocuments catalogue);
}
