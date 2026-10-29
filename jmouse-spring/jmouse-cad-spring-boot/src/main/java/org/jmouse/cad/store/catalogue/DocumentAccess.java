package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.jpa.catalogue.StoredDocument;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.problem.CadProblem;

/**
 * Who may see and change a document, decided in one place for every caller: a published catalogue document is open to
 * anybody, a catalogue draft to whoever writes the catalogue, an owned document to whoever may read where its owner
 * is. A refused read answers "not found", so a hidden document is not told apart from a missing one.
 */
public class DocumentAccess {

    private final DocumentStore documents;
    private final CadAuthority  authority;
    private final CadPlaces     places;

    public DocumentAccess(DocumentStore documents, CadAuthority authority, CadPlaces places) {
        this.documents = documents;
        this.authority = authority;
        this.places = places;
    }

    /** A document by its address — its identifier or its slug —, when the reader may see it. */
    public StoredDocument requireReadable(String address) {
        StoredDocument document = documents.findByAddress(address)
                .orElseThrow(() -> CadProblem.notFound("The document"));
        if (!canRead(document)) {
            throw CadProblem.notFound("The document");
        }
        return document;
    }

    public boolean canRead(StoredDocument document) {
        if (document.isCatalogue()) {
            return document.getStatus() == StoredDocument.Status.PUBLISHED
                    || authority.permitsGlobally(CadCapability.CATALOGUE_WRITE);
        }
        return authority.permits(CadCapability.OWNER_READ, placeOf(document));
    }

    /** Whether the reader may write the document's next revision — the rule {@link #requireWritable} enforces. */
    public boolean canWrite(StoredDocument document) {
        if (document.isCatalogue()) {
            return authority.permitsGlobally(CadCapability.CATALOGUE_WRITE);
        }
        return authority.permits(CadCapability.OWNER_WRITE, placeOf(document));
    }

    public void requireWritable(StoredDocument document) {
        if (document.isCatalogue()) {
            authority.requireGlobally(CadCapability.CATALOGUE_WRITE);
        } else {
            authority.require(CadCapability.OWNER_WRITE, placeOf(document));
        }
    }

    /** Where an owned document is: its owner's whole chain, as the host gives it. */
    public CadPlace placeOf(StoredDocument document) {
        return places.requireOwner(document.owner().orElseThrow());
    }
}
