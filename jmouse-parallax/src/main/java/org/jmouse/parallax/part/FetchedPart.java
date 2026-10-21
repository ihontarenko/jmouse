package org.jmouse.parallax.part;

import org.jmouse.parallax.script.ExperienceDocument;

import java.util.List;

/**
 * A candidate made into documents: what to store, in the order to store it — a footprint before the component that
 * lands on it —, and which of them a reader opens.
 *
 * @param documents the documents, each storable as it is
 * @param open      the slug of the one to open: the component, or the footprint fetched alone
 * @param notes     what the source said that the documents could not carry
 */
public record FetchedPart(List<ExperienceDocument> documents, String open, List<String> notes) {

    public FetchedPart {
        documents = List.copyOf(documents);
        notes = List.copyOf(notes);
    }
}
