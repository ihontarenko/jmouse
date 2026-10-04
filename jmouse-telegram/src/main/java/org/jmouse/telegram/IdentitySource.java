package org.jmouse.telegram;

/**
 * Where identities come from, asked once per call.
 *
 * <p>⚠️ <strong>Per call is the whole point, and binding one at startup is the mistake this exists to
 * prevent.</strong> An administrator who rotates a bot token, disables an account or points it at a
 * self-hosted API server expects the next send to use that; an identity resolved into a bean at
 * startup means a restart instead — and a restart is exactly what nobody performs at the moment a
 * token is discovered to be leaking.
 *
 * <p>An implementation reading a database row per call is not the problem it looks like: one query
 * against one row, beside an HTTP round trip to Telegram that takes a thousand times longer. An
 * implementation that wants a cache is free to be one, and a caller cannot tell the difference —
 * which is why the caching decision belongs behind this interface rather than in front of it.
 *
 * @see TelegramIdentity
 */
@FunctionalInterface
public interface IdentitySource {

    /**
     * The purpose everything is configured for when nothing says otherwise.
     *
     * <p>⚠️ A NAME rather than {@code null}, so a row can be found by it and a screen can label it.
     * Null would make "the general one" invisible in a listing and unaddressable in a query.
     */
    String GENERAL = "general";

    /**
     * @return the identity in force right now, for the general purpose
     * @throws TelegramException when there is none, carrying a sentence saying what to configure
     */
    TelegramIdentity identity();

    /**
     * The identity in force for one PURPOSE.
     *
     * <h2>⚠️ One bot per application is not enough</h2>
     *
     * <p>An installation reasonably wants a loud bot for operational alerts and a quiet one for
     * routine notices, or a separate bot per product sharing one deployment. Keyed only by
     * application, that is inexpressible: there is one active identity and every caller gets it.
     *
     * <h2>⚠️ THE PURPOSE IS THE PRODUCT'S WORD, and this library must never enumerate them</h2>
     *
     * <p>{@code "kitsu-notifications"} means nothing here and never will. A free string keyed by the
     * product is what lets a product name its own purposes without this module learning its domain —
     * an enum would make every new purpose in any product a release of this one.
     *
     * <h2>⚠️ It falls back to {@link #GENERAL}, and that is the whole migration story</h2>
     *
     * <p>An installation configured before purposes existed has one identity and no purpose on it. It
     * goes on answering every purpose, so nothing anywhere has to be reconfigured — and a product
     * asking for a purpose nobody set up gets the general identity rather than a refusal.
     *
     * <p>⚠️ Defaulted rather than abstract, so every existing implementation compiles and behaves
     * exactly as it did. Adding a method to a published interface is otherwise a break in every
     * consumer at once.
     */
    default TelegramIdentity identity(String purpose) {
        return identity();
    }

    /** One fixed identity, for a smoke class and for a product that configures its bot in properties. */
    static IdentitySource fixed(TelegramIdentity identity) {
        return () -> identity;
    }
}
