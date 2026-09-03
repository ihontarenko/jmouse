package org.jmouse.ai.provider;

/**
 * Where the settings come from, asked once per call.
 *
 * <p>⚠️ <strong>Per call is the whole point, and binding it at startup is the mistake it exists to
 * prevent.</strong> An administrator who rotates a key, raises a token ceiling or switches provider
 * expects the next request to use it; settings resolved into a bean at startup mean a restart instead,
 * and a restart is exactly what nobody does at the moment a key is discovered to be leaking.
 *
 * <p>An implementation reading a database row every call is not the problem it looks like — one query
 * against a single row, beside an HTTP call to a language model that will take a thousand times longer.
 * An implementation that wants a cache is free to be one; a caller cannot tell the difference, which is
 * why the caching decision belongs behind this interface rather than in front of it.
 */
@FunctionalInterface
public interface ProviderSettingsSource {

    /**
     * The purpose everything is configured for when nothing says otherwise.
     *
     * <p>⚠️ A NAME rather than {@code null}, so a row can be found by it and a screen can label it.
     * Null would make "the general one" invisible in a list and unaddressable in a query.
     */
    String GENERAL = "general";

    /**
     * @return the settings in force right now, for the general purpose
     * @throws ProviderException when there are none, with a message saying what to configure and where
     */
    ProviderSettings settings();

    /**
     * The settings in force for one PURPOSE.
     *
     * <h2>⚠️ WHY A PURPOSE AT ALL: one active provider per application is not enough</h2>
     *
     * <p>An installation reasonably wants its assistant on one vendor and something else on another —
     * a cheap fast model for classifying, a strong one for the assistant, a local one for anything
     * that must not leave the machine. Keyed only by application, that is inexpressible: there is one
     * active row, and every caller gets it.
     *
     * <p>It surfaced concretely in Norro, which matches films against a model and also wants an
     * assistant. It had to keep its own configuration table rather than use this one — a second store
     * of the same thing, in one product, because the shared one could not say "this vendor, for this".
     *
     * <h2>⚠️ THE PURPOSE IS THE PRODUCT'S WORD, and this library must never enumerate them</h2>
     *
     * <p>"Movie matching" means nothing here and never will. A free string keyed by the product is
     * what lets a product name its own purposes without this module learning its domain — and an enum
     * would mean every new purpose in any product is a release of this one.
     *
     * <h2>⚠️ IT FALLS BACK TO {@link #GENERAL}, and that is the whole migration story</h2>
     *
     * <p>An installation configured before purposes existed has one row and no purpose on it. It goes
     * on answering every purpose, so nothing anywhere has to be reconfigured — and a product that
     * asks for a purpose nobody has set up gets the general provider rather than a refusal.
     *
     * <p>⚠️ Defaulted rather than abstract, so every existing implementation compiles and behaves
     * exactly as it did. Adding a method to a published interface is otherwise a break in every
     * product at once.
     */
    default ProviderSettings settings(String purpose) {
        return settings();
    }

    /** One fixed set, for a test and for a product that configures its model in a properties file. */
    static ProviderSettingsSource fixed(ProviderSettings settings) {
        return () -> settings;
    }
}
