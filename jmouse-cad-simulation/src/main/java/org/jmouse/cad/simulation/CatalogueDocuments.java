package org.jmouse.cad.simulation;

import org.jmouse.cad.document.DocumentNode;

import java.util.Optional;

/**
 * Where the engine finds the documents a document names — a base it extends, a family or a library it uses, a component
 * a part is —, by slug. The product says where they live: its catalogue, its store, a project's drafts.
 */
@FunctionalInterface
public interface CatalogueDocuments {

    Optional<DocumentNode> find(String slug);

    /** A catalogue that holds nothing: for a document that names nothing. */
    static CatalogueDocuments empty() {
        return slug -> Optional.empty();
    }
}
