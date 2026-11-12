package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;

import java.util.Optional;

/**
 * Where the documents a part is built from are found — its component, the bases it extends, the symbols and footprints it
 * names —, each read: the store's documents seen from a place ({@link StoredDocuments}), or the documents of a file read
 * in place before them ({@link ForeignDocuments}).
 */
public interface PartDocuments extends CatalogueDocuments {

    /** The document a reference names — {@code slug}, {@code slug@revision} or a technical code —, read. */
    Optional<ReadDocument> resolve(String reference);

    /** The slug of the document a reference names, its revision kept; empty for none. */
    Optional<String> slugOf(String reference);

    @Override
    default Optional<DocumentNode> find(String reference) {
        return resolve(reference).map(ReadDocument::document);
    }
}
