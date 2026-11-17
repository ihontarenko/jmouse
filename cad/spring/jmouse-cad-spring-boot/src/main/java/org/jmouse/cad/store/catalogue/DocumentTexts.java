package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.document.source.DocumentLocation;
import org.jmouse.cad.folder.DocumentRevision;
import org.jmouse.cad.folder.RevisionOrigin;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.store.library.ShippedDocuments;

import java.util.List;
import java.util.Optional;

/**
 * 📜 The one way a catalogued document's text is read, wherever it is held (ADR 0001): a document written here from
 * its revisions in the database; one a library ships from its file, in place — it has one version, its file's, and the
 * database keeps no copy of it. Nothing else reads a document's text, so no reader needs to know where it is held.
 */
public class DocumentTexts {

    private final DocumentStore    documents;
    private final ShippedDocuments shipped;

    public DocumentTexts(DocumentStore documents, ShippedDocuments shipped) {
        this.documents = documents;
        this.shipped = shipped;
    }

    /** Where a catalogued document's copy is held, at a revision of it. */
    public static DocumentLocation locationOf(StoredDocument document, int revision) {
        return document.shipped().<DocumentLocation>map(shipped -> shipped)
                .orElseGet(() -> new DocumentLocation.Stored(document.owner().orElse(null), document.getId(), revision));
    }

    /** Its head text. */
    public String head(StoredDocument document) {
        return at(document, document.getRevision()).orElseThrow(() -> new IllegalStateException(
                "Document '" + document.getSlug() + "' has no text at its head revision " + document.getRevision()));
    }

    /** Its text at a revision; empty for a revision it does not have. */
    public Optional<String> at(StoredDocument document, int revision) {
        if (!document.isShipped()) {
            return documents.sourceAt(document.getId(), revision);
        }
        return revision == document.getRevision()
                ? shipped.find(document.getSlug()).map(ShippedDocuments.ShippedDocument::source) : Optional.empty();
    }

    /**
     * The revision what follows an {@code @} names — its number, its label, a day or {@code latest} —; a shipped
     * document answers only for its one version.
     */
    public Optional<Integer> revisionWritten(StoredDocument document, String written) {
        if (!document.isShipped()) {
            return documents.revisionWritten(document.getId(), written);
        }
        return names(document, written == null ? "" : written.strip()) ? Optional.of(document.getRevision()) : Optional.empty();
    }

    /** Every revision, newest first; a shipped document's one. */
    public List<DocumentRevision> revisions(StoredDocument document) {
        if (!document.isShipped()) {
            return documents.revisions(document.getId());
        }
        return List.of(new DocumentRevision(document.getRevision(), document.getSourceChecksum(),
                                            document.getUpdatedAt(), null, RevisionOrigin.UNRECORDED));
    }

    private static boolean names(StoredDocument document, String written) {
        return written.equalsIgnoreCase(DocumentStore.LATEST) || written.equals(String.valueOf(document.getRevision()));
    }
}
