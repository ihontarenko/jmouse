package org.jmouse.liveblocks;

/**
 * One {@code :::} line, as a document wrote it.
 *
 * <p>⚠️ <strong>A name and an argument, and that is the whole of it.</strong> The library could not tell
 * you what an issue is, what fields one has, or whether {@code TSSR-4} is a plausible identifier — which
 * is the point rather than a limitation. A contract that knew would be a contract released whenever any
 * product's vocabulary changed.
 *
 * @param name     the directive's first word — {@code issue}, {@code sprint}, {@code part}. Lowercase
 * @param argument what followed it, exactly as written. ⚠️ <strong>Never normalised in transit:</strong>
 *                 a client that tidied it would be asking about a line the document does not contain
 */
public record Directive(String name, String argument) {

    /** Whether this is worth passing to a resolver at all — a blank half is a line, not a question. */
    public boolean isAskable() {
        return name != null && !name.isBlank() && argument != null && !argument.isBlank();
    }

    /** The name a resolver is matched on: trimmed and lowercase, because a document is written by hand. */
    public String normalisedName() {
        return name == null ? "" : name.trim().toLowerCase();
    }

}
