package org.jmouse.cad.document.source;

import org.jmouse.cad.document.DocumentNode;

/**
 * A document as a source holds it: read, with where this copy is and what changes whenever it does.
 *
 * @param version what changes whenever the copy does — a stored revision, a file's checksum —: what a reading of it is
 *                kept by
 */
public record HeldDocument(DocumentNode document, DocumentLocation location, String version) {
}
