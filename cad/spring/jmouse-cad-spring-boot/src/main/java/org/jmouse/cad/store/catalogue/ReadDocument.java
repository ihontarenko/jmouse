package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.library.DocumentIndex;

import java.util.Optional;

/**
 * A document's text as the engine read it: its tree, what a catalogue keeps of it, and what it is to the engine.
 *
 * @param source   the text
 * @param checksum the text's checksum, the key it is cached under
 * @param document its tree
 * @param index    where it is filed and what it names
 * @param model    the engine's model of it — a {@code Component}, a {@code Circuit}, a drawing —, or empty for a kind
 *                 the engine keeps none of
 */
public record ReadDocument(String source, String checksum, DocumentNode document, DocumentIndex index,
                           Optional<Object> model) {
}
