package org.jmouse.ai.administration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Changing which model this application talks to, on whose key — the one thing a management screen
 * does that is not reading.
 *
 * <p><strong>A port rather than a service, and separate from {@link org.jmouse.ai.view.ProviderRegistry}
 * on purpose.</strong> The registry answers <em>what resolved</em>; this changes <em>what is stored</em>,
 * and the two disagree in exactly the situations a screen exists to tell apart — nothing active, two
 * rows active, an active row with no key. Folding them into one interface would have made "read the
 * configuration" and "spend money through a different one" the same capability, and every adopter would
 * have had to separate them again.
 *
 * <p><strong>Why this is here at all.</strong> It was not, and the cost showed up immediately: every
 * product that put its settings in the database wrote the same service — the same one-row-in-force rule,
 * the same blank-key-means-keep rule, the same refusal to delete what is in force — and the same
 * controller over it. Those rules are not a product's taste. They are the conditions
 * {@code JpaProviderSettingsSource} already refuses to resolve without, so they belong beside it.
 *
 * <p>⚠️ <strong>No method here can return a key</strong>, for the reason
 * {@link org.jmouse.ai.view.ProviderRegistry} states at length: {@link Configuration} has no field one
 * would fit in. A key travels <em>in</em>, on a {@link Draft}, and never comes back out.
 *
 * <p>⚠️ <strong>An implementation is optional.</strong> An application whose settings are a property has
 * nothing to administer, and {@link #unavailable()} is what it gets — every write refuses with a
 * sentence saying so, rather than a screen appearing to save something that goes nowhere.
 */
public interface ProviderAdministration {

    /**
     * A stored configuration, with the credential reduced to a yes or a no.
     *
     * @param active        whether this is the one in force. ⚠️ At most one is, and putting one in force
     *                      takes the previous one out of it — the settings source refuses to resolve two
     * @param keyConfigured whether a key is stored. <strong>Never the key</strong>
     */
    record Configuration(
            String  id,
            String  provider,
            String  model,
            String  apiUrl,
            int     maximumTokens,
            boolean active,
            boolean keyConfigured,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    /**
     * What somebody typed, on the way in.
     *
     * @param apiKey ⚠️ blank on a change means <em>leave the stored one alone</em>, never <em>clear
     *               it</em>. A form that round-trips its own fields would otherwise erase the credential
     *               every time somebody corrected a model name — and since no response can hand the old
     *               value back, there would be nothing to resubmit
     */
    record Draft(
            String provider,
            String model,
            String apiKey,
            String apiUrl,
            int    maximumTokens
    ) {

        /** Whether a credential was actually typed, as opposed to the field being left alone. */
        public boolean carriesKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /** Raised where a write cannot be honoured — an unknown identifier, or a rule this port keeps. */
    class RefusedException extends RuntimeException {

        public RefusedException(String message) {
            super(message);
        }
    }

    /**
     * Every provider name a configuration may name, so a screen offers a list rather than a text box.
     *
     * <p>Answered by the implementation rather than listed here: which providers exist is
     * {@code jmouse-ai-provider}'s fact, and this module deliberately cannot see it.
     */
    List<String> supportedProviders();

    /** Every stored configuration, oldest first — the order somebody added them in. */
    List<Configuration> configurations();

    /** One by identifier, or empty where this application has no such row. */
    Optional<Configuration> find(String id);

    /**
     * A new configuration, <strong>idle</strong>.
     *
     * <p>⚠️ Deliberately not put in force on creation. Typing a key is not saying "start spending money
     * through this one" — activating is a second press, and it is the press that changes what the
     * assistant does.
     */
    Configuration add(Draft draft);

    /** Change one. A blank key on the draft keeps the stored one. */
    Configuration change(String id, Draft draft);

    /**
     * Put one in force, taking whatever was in force out of it.
     *
     * <p>⚠️ Refuses a configuration with no key: every call through it would be refused before it was
     * sent, which reads as an outage rather than as a missing credential.
     */
    Configuration putInForce(String id);

    /**
     * Take the one in force out of force, leaving the application with tools and no model.
     *
     * <p>A supported arrangement rather than a broken one — a connected protocol client reaches every
     * action without a provider — so it is its own operation rather than only a side effect of putting
     * something else in force.
     */
    Configuration takeOutOfForce(String id);

    /** ⚠️ Refuses the configuration in force. Take it out of force first, or put another one in force. */
    void discard(String id);

    /**
     * An application with nothing to administer — settings from a property, or no provider at all.
     *
     * <p>Reads answer empty and every write refuses with a sentence that says which arrangement this is.
     * A screen over it shows nothing to change rather than controls that appear to work.
     */
    static ProviderAdministration unavailable() {
        return new ProviderAdministration() {

            private static final String WHY =
                    "This application does not administer its AI provider through rows — its settings "
                    + "come from configuration, so there is nothing here to change. Switch the settings "
                    + "source to the database-backed one to administer them on a screen.";

            @Override
            public List<String> supportedProviders() {
                return List.of();
            }

            @Override
            public List<Configuration> configurations() {
                return List.of();
            }

            @Override
            public Optional<Configuration> find(String id) {
                return Optional.empty();
            }

            @Override
            public Configuration add(Draft draft) {
                throw new RefusedException(WHY);
            }

            @Override
            public Configuration change(String id, Draft draft) {
                throw new RefusedException(WHY);
            }

            @Override
            public Configuration putInForce(String id) {
                throw new RefusedException(WHY);
            }

            @Override
            public Configuration takeOutOfForce(String id) {
                throw new RefusedException(WHY);
            }

            @Override
            public void discard(String id) {
                throw new RefusedException(WHY);
            }
        };
    }
}
