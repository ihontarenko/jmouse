package org.jmouse.cad.document.source;

import java.util.Optional;

/**
 * 🗂️ A place documents are held — the database, the jars that ship them, a mounted folder, another product's files —,
 * the one way the catalogue reads them. Only this layer knows where a document is: the engine is handed what it finds as
 * documents by slug, and a document's files as {@link DocumentResources}.
 *
 * <p>See ADR 0001 (Agents {@code jmCAD/_shared/decisions/0001-document-sources.md}).</p>
 */
public interface DocumentSource {

    /**
     * The document a reference names — a slug, or {@code slug@revision} where the source keeps revisions —, as this
     * source holds it; empty where it holds none, or none the place it is asked for may see.
     */
    Optional<HeldDocument> locate(String reference);

    /** The files a document this source holds keeps beside its text. */
    DocumentResources resources(HeldDocument document);
}
