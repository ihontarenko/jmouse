package org.jmouse.cad.document.part;

import org.jmouse.cad.document.DocumentNode;

import java.util.List;

/**
 * A candidate made into documents: what to store, in the order to store it — a footprint before the component that
 * lands on it —, and which of them a reader opens.
 *
 * @param documents the documents, each storable as it is
 * @param files     the files those documents hold besides their text — a model's geometry —, each filed against its document
 * @param open      the slug of the one to open: the component, or the footprint fetched alone
 * @param notes     what the source said that the documents could not carry
 */
public record FetchedPart(List<DocumentNode> documents, List<FetchedFile> files, String open, List<String> notes) {

    public FetchedPart {
        documents = List.copyOf(documents);
        files = List.copyOf(files);
        notes = List.copyOf(notes);
    }

    /** Documents that hold nothing besides their text. */
    public FetchedPart(List<DocumentNode> documents, String open, List<String> notes) {
        this(documents, List.of(), open, notes);
    }
}
