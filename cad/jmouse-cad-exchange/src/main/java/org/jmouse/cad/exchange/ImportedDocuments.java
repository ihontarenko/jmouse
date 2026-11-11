package org.jmouse.cad.exchange;

import org.jmouse.cad.document.DocumentNode;

import java.util.List;

/**
 * What an import made, in the order it is stored.
 *
 * @param library what a design is built from — footprints, symbols, components —, each before what names it; one already
 *                held elsewhere is reused rather than written over
 * @param designs the circuits, each placed sheet before the sheet that places it, the root last; empty for an import
 *                that made a component alone
 * @param notes   what the import could not carry, part by part
 */
public record ImportedDocuments(List<DocumentNode> library, List<DocumentNode> designs, List<String> notes) {

    public ImportedDocuments {
        library = List.copyOf(library);
        designs = List.copyOf(designs);
        notes = List.copyOf(notes == null ? List.of() : notes);
        if (library.isEmpty() && designs.isEmpty()) {
            throw new IllegalArgumentException("An import made nothing.");
        }
    }

    /** The document the import stands for: the root of its design, or the one thing it made. */
    public DocumentNode root() {
        return designs.isEmpty() ? library.getLast() : designs.getLast();
    }
}
