package org.jmouse.ai.preferences;

import java.time.Instant;
import java.util.List;

/**
 * What this application's AI behaviour is set to, and the one place it is changed.
 *
 * <p>Read on the path that uses it — a system prompt is fetched when a conversation starts, not held
 * from startup — so an edit takes effect on the next question rather than on the next deploy. That is
 * the same promise {@code ProviderAdministration} makes about a key, for the same reason: the moment
 * somebody needs to change how the assistant speaks is the moment they least want to redeploy.
 *
 * <p><strong>Several stored values, one in force.</strong> Deliberately the shape the provider
 * configuration already has, because it is the same situation: a person wants to keep the long prompt
 * while trying the short one, and switching back has to be a press rather than a paste. What the
 * assistant reads is the one in force, and nothing else about the others matters at runtime.
 *
 * <p><strong>Application-scoped, and the application is not a parameter.</strong> One installation's
 * table can serve several applications; which one this instance is, is fixed where it is constructed. A
 * request that could name an application is a request that can read and rewrite somebody else's
 * settings, which is exactly the mistake {@code JpaProviderAdministration} already refuses to make.
 *
 * <p>⚠️ <strong>An empty table is a working installation.</strong> A setting with no rows is seeded from
 * what the product declares, and a setting with rows but none in force still answers with the shipped
 * default rather than with nothing. {@link #value} never returns null for a declared name.
 */
public interface AiPreferences {

    /**
     * One stored value — a whole prompt, with a name somebody gave it.
     *
     * @param shippedKey which shipped variant this row started as, or null for one somebody wrote here.
     *                   ⚠️ It is a provenance note and nothing reads it at runtime; what it buys is
     *                   {@link #restore}, which is the difference between experimenting and losing the
     *                   original
     * @param asShipped  whether the text still equals what the build ships for that variant. Computed
     *                   here so a screen can say <em>edited</em> without being sent every shipped text
     *                   to compare against
     */
    record StoredValue(
            String  id,
            String  label,
            String  value,
            boolean inForce,
            String  shippedKey,
            boolean asShipped,
            Instant createdAt,
            Instant changedAt
    ) {
    }

    /**
     * One declared setting with everything stored for it.
     *
     * @param values ⚠️ Never empty in practice — a setting with nothing stored is seeded from the
     *               declaration on first read, so a screen showing this list is showing rows
     */
    record Setting(
            String            name,
            String            title,
            String            description,
            boolean           multiline,
            List<StoredValue> values
    ) {
    }

    /** What somebody typed, on the way in. */
    record Draft(String label, String value) {
    }

    /** Raised where a name or an identifier is not this application's, or a rule this port keeps. */
    class RefusedException extends RuntimeException {

        public RefusedException(String message) {
            super(message);
        }
    }

    /**
     * What the setting says right now — the value in force, or the shipped default where none is.
     *
     * @throws RefusedException for a name nothing declared, naming what was declared instead. Silently
     *                          answering a default for an unknown name would make a typo in a reader
     *                          indistinguishable from a setting nobody has changed
     */
    String value(String name);

    /** Every declared setting with everything stored for it — what an administration screen lists. */
    List<Setting> all();

    /** One of them, for a screen editing a single setting. */
    Setting find(String name);

    /**
     * A new stored value for a declared setting, <strong>idle</strong>.
     *
     * <p>⚠️ Deliberately not put in force on creation, exactly as a provider configuration is not.
     * Writing a prompt is not the same as deciding the assistant should start using it, and separating
     * the two is what lets somebody draft one over several sittings.
     */
    StoredValue add(String name, Draft draft);

    /** Change one — its text, its name, or both. Does not change what is in force. */
    StoredValue change(String id, Draft draft);

    /** Put one in force, taking whatever was in force out of it. The next conversation reads it. */
    StoredValue putInForce(String id);

    /**
     * Put a row back to the text this build ships for the variant it started as.
     *
     * @throws RefusedException for a row nobody seeded — there is no shipped text to go back to, and
     *                          inventing one would overwrite somebody's own writing with a guess
     */
    StoredValue restore(String id);

    /** ⚠️ Refuses the one in force. Put another one in force first — see {@link #putInForce}. */
    void discard(String id);

    /**
     * An application that stores nothing — every setting is the shipped default and every write refuses.
     *
     * <p>The counterpart of {@code ProviderAdministration.unavailable()}, with one difference: reads
     * still work, because a shipped default is a real value and the assistant has to run. Writes say
     * plainly that there is nowhere to put one, rather than a screen appearing to save something that
     * goes nowhere.
     */
    static AiPreferences shipped(PreferenceCatalog catalog) {
        return new AiPreferences() {

            private static final String WHY =
                    "This application keeps no AI preferences of its own, so there is nothing here to "
                    + "change — every setting is the wording the product ships. Add the persistence "
                    + "module to administer them.";

            @Override
            public String value(String name) {
                return catalog.require(name).defaultValue();
            }

            @Override
            public List<Setting> all() {
                return catalog.declared().stream().map(this::describe).toList();
            }

            @Override
            public Setting find(String name) {
                return describe(catalog.require(name));
            }

            @Override
            public StoredValue add(String name, Draft draft) {
                throw new RefusedException(WHY);
            }

            @Override
            public StoredValue change(String id, Draft draft) {
                throw new RefusedException(WHY);
            }

            @Override
            public StoredValue putInForce(String id) {
                throw new RefusedException(WHY);
            }

            @Override
            public StoredValue restore(String id) {
                throw new RefusedException(WHY);
            }

            @Override
            public void discard(String id) {
                throw new RefusedException(WHY);
            }

            /**
             * The shipped variants, drawn as though they were rows — with no identifiers, because
             * nothing here can be addressed.
             */
            private Setting describe(PreferenceDefinition declared) {
                List<StoredValue> shipped = declared.variants().stream()
                        .map(variant -> new StoredValue(
                                null,
                                variant.label(),
                                variant.value(),
                                variant.key().equals(declared.defaultVariant()),
                                variant.key(),
                                true,
                                null,
                                null))
                        .toList();

                return new Setting(
                        declared.name(),
                        declared.title(),
                        declared.description(),
                        declared.multiline(),
                        shipped);
            }
        };
    }
}
