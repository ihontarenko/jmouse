package org.jmouse.cad.exchange;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.plugin.Contributions;

import java.util.List;

/**
 * A kind of document that is written as another kind: a panel of pcbs is written as the one pcb it makes, so every format of a
 * pcb writes a panel with no word of its own about panels.
 */
public interface DocumentFlattening {

    Contributions<DocumentFlattening> POINT = new Contributions<>("cad.exchange.flattenings", DocumentFlattening.class);

    /** The kind it reads. */
    DocumentKind from();

    /** The kind it makes. */
    DocumentKind to();

    /**
     * @param document  a document of the kind it reads
     * @param catalogue what the document names, as the reader may see it
     * @throws IllegalArgumentException when the document cannot be made into one, with why
     */
    Flattened flatten(DocumentNode document, CatalogueDocuments catalogue);

    /**
     * What a document becomes.
     *
     * @param document  the document made; it keeps the slug of the one it came from
     * @param catalogue the catalogue it is read against, with whatever it was made together with
     * @param notes     what a reader should know of the making
     */
    record Flattened(DocumentNode document, CatalogueDocuments catalogue, List<String> notes) {

        public Flattened {
            notes = List.copyOf(notes);
        }
    }
}
