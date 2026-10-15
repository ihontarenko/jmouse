package org.jmouse.parallax.jpa.scene;

/**
 * 🪟 One slot of a comparison as its row holds it: a stored object or an inline jMX text, the representation it is
 * shown in, and its own state as JSON documents the caller reads.
 *
 * @param label              what the slot is called, or {@code null}
 * @param objectId           the stored object, or {@code null} for an inline one
 * @param inlineSource       the inline object's jMX text, or {@code null} for a stored one
 * @param representationKind the representation's name
 * @param settings           the representation's settings, as JSON
 * @param parameterValues    the parameter values, as JSON
 * @param camera             the slot's own camera, as JSON, or {@code null}
 */
public record ComparisonSlot(String label, String objectId, String inlineSource, String representationKind,
                             String settings, String parameterValues, String camera) {
}
