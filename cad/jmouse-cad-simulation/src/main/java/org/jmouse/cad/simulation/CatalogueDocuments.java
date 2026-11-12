package org.jmouse.cad.simulation;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.TechnicalClass;

import java.util.List;
import java.util.Optional;

/**
 * Where the engine finds the documents a document names — a base it extends, a family or a library it uses, a component
 * a part is —, by slug. The product says where they live: its catalogue, its store, a project's drafts.
 */
@FunctionalInterface
public interface CatalogueDocuments {

    Optional<DocumentNode> find(String slug);

    /**
     * The document whose {@code meta} writes this technical code — {@code FP.CHIP:R0805}, the identity a document keeps when its slug is
     * renamed —, the nearest copy where several do; empty where nothing here indexes codes.
     */
    default Optional<DocumentNode> findCoded(String code) {
        return Optional.empty();
    }

    /**
     * The codes of the documents of one class whose code ends in this name, in whatever library — what another product names
     * {@code R0805} without its library may be {@code FP.CHIP:R0805} —, the nearest first; empty where nothing here indexes
     * codes.
     */
    default List<String> codesNamed(TechnicalClass kind, String name) {
        return List.of();
    }

    /**
     * The revision a document is, as the place it is read for keeps it: its label, marked when the document differs from what
     * is stored; empty when it was never stored, or nothing here keeps revisions.
     */
    default Optional<String> revisionOf(DocumentNode document) {
        return Optional.empty();
    }

    /** A catalogue that holds nothing: for a document that names nothing. */
    static CatalogueDocuments empty() {
        return slug -> Optional.empty();
    }
}
