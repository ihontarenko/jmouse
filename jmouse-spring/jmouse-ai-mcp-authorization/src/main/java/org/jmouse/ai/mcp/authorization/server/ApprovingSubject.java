package org.jmouse.ai.mcp.authorization.server;

import java.util.List;

/**
 * Who is signed in right now, and what a client could be authorized to act as.
 *
 * <p>⚠️ <strong>{@link #current()} is the whole of the identity model this module is willing to
 * know.</strong> One product's person is themselves — a credential acts as them, and there is nothing to
 * choose, so the list has one entry. Another product's person owns <em>agent accounts</em>: sub-accounts
 * with their own permissions, where picking between them is the most important thing on the screen. Both
 * are expressed the same way here, and neither word — person, member, agent, account — appears anywhere
 * in this module.
 *
 * <p>The chosen {@link Choice#reference()} is what travels through the flow as the opaque subject
 * reference: stored against a one-time code, handed back on redemption, and passed to
 * {@link CredentialIssuer}. Nothing between here and there reads it.
 *
 * <p>Resolved once per request rather than injected, because the answer is a property of whoever is
 * calling and not of the application. A product implements this over its own security context.
 */
public interface ApprovingSubject {

    /**
     * Whoever is signed in on this request, and everything they may authorize a client to act as.
     *
     * @throws org.jmouse.ai.mcp.authorization.McpAuthorizationException when nobody is
     */
    Approver current();

    /**
     * The person deciding, as the screen needs to show them.
     *
     * @param displayName how to name them
     * @param detail      something that tells one signed-in person from another — an address, a handle,
     *                    or null where there is nothing worth saying
     * @param choices     ⚠️ <strong>everything they may authorize, and nothing they may not.</strong>
     *                    This module re-reads the list when an approval arrives and refuses a reference
     *                    that is not in it, so it is a permission boundary rather than a convenience —
     *                    but only as narrow as the product makes it. Empty means there is nothing to
     *                    approve, and the screen says so rather than offering a button that would fail.
     */
    record Approver(String displayName, String detail, List<Choice> choices) {

        public Approver {
            choices = List.copyOf(choices);
        }
    }

    /**
     * One thing a client could be authorized to act as.
     *
     * @param reference         the product's own opaque identifier for it
     * @param name              what to call it on the screen
     * @param detail            a second line, where there is something worth saying
     * @param available         false when it exists but cannot be chosen — a switched-off agent, say
     * @param unavailableReason why not, in a sentence somebody can act on; ignored when available
     */
    record Choice(String reference, String name, String detail, boolean available, String unavailableReason) {

        /** The ordinary case: something that can be chosen right now. */
        public static Choice of(String reference, String name, String detail) {
            return new Choice(reference, name, detail, true, null);
        }
    }
}
