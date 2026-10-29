package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.jpa.catalogue.StoredDocument;
import org.jmouse.cad.library.DocumentIndex;
import org.jmouse.cad.simulation.CatalogueDocuments;

import java.util.Objects;
import java.util.Optional;

/**
 * The catalogue as the engine asks it — by slug, or by {@code slug@revision} — for a document of one owner, or of the
 * shared catalogue: what a circuit's parts, a component's base and family are found in. It sees the shared catalogue
 * and that owner's own documents, never another owner's, whatever their slugs say.
 *
 * <p>Access is decided before the engine is asked: the document being run or written was checked, and what it names
 * is whatever its owner may place.</p>
 */
public class StoredDocuments implements CatalogueDocuments {

    private final DocumentStore   documents;
    private final DocumentReading reading;
    private final DocumentOwner   owner;

    /**
     * @param owner whose documents are seen besides the catalogue's; null for the catalogue's alone
     */
    public StoredDocuments(DocumentStore documents, DocumentReading reading, DocumentOwner owner) {
        this.documents = documents;
        this.reading = reading;
        this.owner = owner;
    }

    @Override
    public Optional<DocumentNode> find(String reference) {
        return resolve(reference).map(ReadDocument::document);
    }

    /** The document a reference names, read at the revision it pins — its head when it pins none. */
    public Optional<ReadDocument> resolve(String reference) {
        String slug = DocumentIndex.slugOf(reference);
        Optional<StoredDocument> found = documents.findBySlug(slug).filter(this::visible);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        StoredDocument document = found.get();
        if (slug.length() == reference.length()) {
            return Optional.of(head(document));
        }
        String written = reference.substring(slug.length() + DocumentIndex.REVISION_MARK.length());
        return documents.revisionWritten(document.getId(), written)
                .flatMap(revision -> documents.sourceAt(document.getId(), revision))
                .map(source -> reading.read(source, reference));
    }

    /** A stored document's head revision, read once per revision. */
    public ReadDocument head(StoredDocument document) {
        return reading.stored(document.getSourceChecksum(), () -> headSource(document), document.getSlug());
    }

    /** A stored document's head text: the document itself. */
    public String headSource(StoredDocument document) {
        return documents.sourceAt(document.getId(), document.getRevision())
                .orElseThrow(() -> new IllegalStateException("Document '" + document.getSlug() + "' has no revision "
                        + document.getRevision() + " of its text"));
    }

    /** Whether the engine may find this document from here. */
    public boolean visible(StoredDocument document) {
        return document.isCatalogue() || document.owner().filter(found -> Objects.equals(found, owner)).isPresent();
    }
}
