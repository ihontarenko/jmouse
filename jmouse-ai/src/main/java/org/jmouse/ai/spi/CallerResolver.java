package org.jmouse.ai.spi;

import org.jmouse.ai.CallerIdentity;

/**
 * Who is calling right now?
 *
 * <p>The first step of the dispatch, and the only one whose answer the library could not possibly
 * guess. A caller arrives from a security context, a bearer token, a session, or a signed-in subject —
 * four mechanisms with nothing in common except that each ends in an identifier.
 *
 * <p><strong>Who is allowed to become a caller lives here too.</strong> A product that refuses a
 * person's session token where an agent credential is required, or refuses a service account its owner
 * has switched off, states that in its resolver — those are identity policy, and the four refusal
 * reasons that went with them in the implementation this library learned from are deliberately absent
 * from {@link org.jmouse.ai.RefusalReason} for that reason. Raise a
 * {@link org.jmouse.ai.ToolRefusedException} carrying {@code NO_CALLER}, with a message that says what
 * kind of credential would have been accepted.
 *
 * <p>Returning {@code null} is permitted and means the same thing; the dispatcher composes the refusal
 * so that a product with nothing to say does not have to write the sentence.
 */
@FunctionalInterface
public interface CallerResolver {

    /** The caller for the call in progress, or null when there is none. */
    CallerIdentity resolve();

    /**
     * A single anonymous caller acting for itself.
     *
     * <p>The default, so that a tool can be dispatched before a product has wired any authentication —
     * a sandbox, a first draft, a test. Everything downstream works: the identity is stable, so the
     * rate limit and the fingerprint have something to key on, and {@code actingSubject()} is equal to
     * it, which is the in-app assistant's shape rather than a special case.
     */
    static CallerResolver anonymous() {
        CallerIdentity anonymous = CallerIdentity.of("anonymous");
        return () -> anonymous;
    }

    /** A fixed caller — what a sandbox or a scheduled job wants. */
    static CallerResolver fixed(CallerIdentity caller) {
        return () -> caller;
    }
}
