package org.jmouse.ai.preferences;

import java.util.List;
import java.util.Optional;

/**
 * One setting a product declares, together with the ready-made values it ships for it.
 *
 * <p><strong>The variants are in code and the rows are in a table, and that order is the whole
 * design.</strong> A system prompt is the clearest case: it decides how a model behaves, it wants
 * editing without a deploy, and it must still exist — in a good version — on an installation whose
 * database was created five minutes ago. A table alone gives the second and takes the third.
 *
 * <p>So the declaration is what a fresh installation is <em>seeded from</em>, and it stays the answer
 * when nothing is in force. Everything after that is rows: several of them, one in force, each editable
 * and each able to be put back to the text this build shipped.
 *
 * <p>⚠️ <strong>Declared by the product, never by this library.</strong> A library shipping a prompt
 * would be describing a domain it does not have, and every adopter would begin by deleting the
 * paragraph. What is here is the mechanism.
 *
 * @param name           how it is addressed, in a row and in a request — lower case, dotted, stable
 *                       forever. ⚠️ Renaming one abandons the rows stored under the old name
 * @param title          what a screen calls it
 * @param description    one or two sentences on what changing it does, read at the moment of changing
 * @param multiline      whether the value is a paragraph rather than a word, so a screen offers the
 *                       right control. Presentation only; nothing here treats the two differently
 * @param variants       what the product ships, in the order they are meant to be read. ⚠️ Never empty
 * @param defaultVariant the key of the one that is in force on a fresh installation
 */
public record PreferenceDefinition(
        String                  name,
        String                  title,
        String                  description,
        boolean                 multiline,
        List<PreferenceVariant> variants,
        String                  defaultVariant
) {

    public PreferenceDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "A preference must be declared with a name — it is how a row, a request and a "
                    + "reader all address the same setting.");
        }

        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException(
                    "Preference '" + name + "' ships no variants. At least one is what makes a fresh "
                    + "installation behave like a configured one, and what a row is seeded from.");
        }

        variants = List.copyOf(variants);

        List<String> keys = variants.stream().map(PreferenceVariant::key).toList();

        if (keys.stream().distinct().count() != keys.size()) {
            throw new IllegalArgumentException(
                    "Preference '" + name + "' ships two variants under one key: " + keys + ". A row "
                    + "points back at a key, so which text it would be restored to would be a coin toss.");
        }

        if (!keys.contains(defaultVariant)) {
            throw new IllegalArgumentException(
                    "Preference '" + name + "' names '" + defaultVariant + "' as the variant in force "
                    + "on a fresh installation, and ships no such variant. Shipped: " + keys + ".");
        }
    }

    /** A setting with one shipped wording and no choice to make about it. */
    public static PreferenceDefinition text(
            String name, String title, String description, String value) {

        PreferenceVariant only = new PreferenceVariant(
                "standard", "Standard", "The wording this build ships.", value);

        return new PreferenceDefinition(name, title, description, true, List.of(only), only.key());
    }

    /** A paragraph with several shipped wordings — a prompt, most obviously. */
    public static PreferenceDefinition text(
            String                  name,
            String                  title,
            String                  description,
            List<PreferenceVariant> variants,
            String                  defaultVariant) {

        return new PreferenceDefinition(name, title, description, true, variants, defaultVariant);
    }

    /** A word or a number, on one line. */
    public static PreferenceDefinition line(
            String name, String title, String description, String value) {

        PreferenceVariant only = new PreferenceVariant(
                "standard", "Standard", "The value this build ships.", value);

        return new PreferenceDefinition(name, title, description, false, List.of(only), only.key());
    }

    public Optional<PreferenceVariant> variant(String key) {
        return variants.stream().filter(shipped -> shipped.key().equals(key)).findFirst();
    }

    /**
     * What this setting says where nothing is stored at all.
     *
     * <p>⚠️ The reason {@link AiPreferences#value} can promise never to answer null: an empty table is a
     * correctly working installation, not an unconfigured one.
     */
    public String defaultValue() {
        return variant(defaultVariant).orElseThrow().value();
    }
}
