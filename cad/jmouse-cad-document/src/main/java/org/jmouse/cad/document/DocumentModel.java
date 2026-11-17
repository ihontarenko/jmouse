package org.jmouse.cad.document;

import java.util.function.Function;

/**
 * What a document of a kind is to the engine — a component, a symbol's drawing, a pcb —: the type it is, which its schema is
 * written from, and how it is read from the document's tree, where a text that reads is refused for what it says.
 *
 * @param type    the type a document of the kind is read into
 * @param reading how one is read from its document's tree
 * @param <T>     the type
 */
public record DocumentModel<T>(Class<T> type, Function<DocumentNode, ? extends T> reading) {

    public static <T> DocumentModel<T> of(Class<T> type, Function<DocumentNode, ? extends T> reading) {
        return new DocumentModel<>(type, reading);
    }

    /** A document's model, read from its tree. */
    public T read(DocumentNode document) {
        return reading.apply(document);
    }
}
