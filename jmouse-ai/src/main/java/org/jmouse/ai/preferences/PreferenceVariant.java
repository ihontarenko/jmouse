package org.jmouse.ai.preferences;

/**
 * One ready-made value a product ships for a setting — a whole prompt, not a fragment of one.
 *
 * <p><strong>Several rather than one, because the right length is a decision and not a fact.</strong> A
 * long prompt buys care: it says what to do when a call is refused, what a preview means, how to name
 * the place it acted. It also costs those tokens on <em>every round of every conversation</em>, which
 * against a small per-minute allowance is the difference between an assistant and a refusal. Neither
 * answer is right for both installations, so the product ships the choice rather than picking.
 *
 * <p>⚠️ <strong>A variant is a starting point, not a mode.</strong> Nothing reads the key at runtime:
 * once a variant is a row it is text like any other text, editable to whatever somebody wants, and the
 * only thing the key still does is let that row be put back to what the build shipped.
 *
 * @param key         stable, lower case, and never renamed — it is what a stored row points back at
 * @param label       what a screen calls it
 * @param description one line on the trade this variant makes, read at the moment of choosing
 * @param value       the whole text
 */
public record PreferenceVariant(String key, String label, String description, String value) {

    public PreferenceVariant {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "A shipped variant must have a key — it is what a stored row points back at when "
                    + "somebody asks for the wording this build ships.");
        }

        if (value == null) {
            throw new IllegalArgumentException("Shipped variant '" + key + "' carries no value.");
        }
    }
}
