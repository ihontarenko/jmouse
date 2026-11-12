package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.document.source.DocumentLocation;
import org.jmouse.cad.document.source.DocumentResources;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;

import java.util.List;
import java.util.Optional;

/**
 * 🗂️ A place documents are held besides the database, as a plugin brings it: what a folder mounted for a project holds,
 * seen by everything that asks that project's place for a document — a circuit run, a part compiled, a pcb seated, an
 * export written — without being imported. What it holds is found before the store's, as an owner's own copy is found
 * before the catalogue's. See ADR 0001.
 */
public interface PlaceSource extends Identified {

    /** Where a plugin brings a place documents are held. Without one, the database is all there is. */
    Contributions<PlaceSource> POINT = new Contributions<>("cad.place-sources", PlaceSource.class);

    /** What it holds for a place, read for this asking. */
    Holding holding(CadPlace place);

    /** What a source holds for one place, as it stood when it was asked. */
    interface Holding {

        /** Its documents, each read; one that does not read is not among them. */
        List<PlacedDocument> documents();

        /** The files a document it holds keeps beside it; empty for a location that is not its own. */
        Optional<DocumentResources> resourcesOf(DocumentLocation location);
    }

    /**
     * A document a source holds.
     *
     * @param read     the document as the engine read it
     * @param location where this copy is
     */
    record PlacedDocument(ReadDocument read, DocumentLocation location) {
    }
}
