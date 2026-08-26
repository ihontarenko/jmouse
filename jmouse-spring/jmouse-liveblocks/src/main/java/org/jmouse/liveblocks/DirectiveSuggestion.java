package org.jmouse.liveblocks;

/**
 * One thing a document could refer to — an answer to <em>"what is there?"</em> rather than
 * <em>"what is this?"</em>.
 *
 * <h2>⚠️ Why this could not be built on {@link ResolvedDirective}</h2>
 *
 * <p>Resolving answers about a thing somebody has already named. A picker runs <em>before</em> anything
 * is named, so it needs the one field a resolved answer has no reason to carry: the text to write.
 *
 * <p>{@link #reference} is that text, and it is the whole point of this record. A consumer inserts it
 * <strong>verbatim</strong> — it does not assemble a reference from a name, a key or a URL, because the
 * producer is the only side that knows which of its identifiers is the permanent one. A picker that
 * built the string itself would be a second place deciding the format, and the day the two disagreed
 * every link written that week would resolve to nothing.
 *
 * @param reference what to write into the document — {@code issue:9f3a21}. ⚠️ The **permanent** form,
 *                  never whatever the thing is currently called
 * @param label     the short name it is known by, for the list and for the badge it becomes
 * @param title     the line somebody picks by — a summary, a name
 * @param subtitle  the state around it, in one line. Optional
 * @param url       where it lives, for a picker that previews. Absolute, and pointing at this product
 */
public record DirectiveSuggestion(
        String reference,
        String label,
        String title,
        String subtitle,
        String url
) {
}
