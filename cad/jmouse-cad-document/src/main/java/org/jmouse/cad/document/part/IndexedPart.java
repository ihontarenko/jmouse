package org.jmouse.cad.document.part;

/**
 * One part of an {@linkplain InstalledLibrary installed library}, as it is offered and as it is found: the candidate shown, and the
 * words a query is scored against besides its title.
 *
 * @param candidate   what a reader is offered
 * @param keywords    what the library says it is looked for by; empty where it says nothing
 * @param description what it says of itself; empty where it says nothing
 */
public record IndexedPart(PartCandidate candidate, String keywords, String description) {

    public IndexedPart {
        keywords = keywords == null ? "" : keywords;
        description = description == null ? "" : description;
    }

    /** How well it answers a query: none when a word is missing. */
    int score(PartQuery query) {
        return query.score(candidate.title(), keywords, description);
    }
}
