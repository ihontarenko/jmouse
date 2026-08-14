package org.jmouse.ai.sandbox;

/**
 * A note belonging to a subject rather than to a place.
 *
 * <p>Here for one reason: it makes {@code notes.delete} a <strong>destructive action with no
 * scope</strong>, which the rehearsal found to be legal and unwritten-about. Every refusal on that
 * path has to have a scopeless half, and this is what exercises them.
 */
public record Note(String id, String subjectId, String text) {
}
