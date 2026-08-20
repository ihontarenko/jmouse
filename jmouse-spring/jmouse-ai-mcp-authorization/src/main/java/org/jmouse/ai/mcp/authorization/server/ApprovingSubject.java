package org.jmouse.ai.mcp.authorization.server;

import java.util.List;
import java.util.Optional;

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
     * How a choice says <em>"there is nothing yet — make one"</em>.
     *
     * <h2>⚠️ A shared spelling, and still not something this module reads</h2>
     *
     * <p>The reference remains opaque here: nothing between {@link #current()} and {@link CredentialIssuer}
     * inspects it, and the flow matches it the same way it matches any other — by exact string, against the
     * list this caller was offered. What this prefix buys is that <strong>two products encode the same
     * intention identically</strong>, which is the difference between one screen and two that merely look
     * alike.
     *
     * <p>It exists because a person with nothing to pick from yet must still be able to connect a client.
     * The alternative was an empty list, and an empty list is a dead end — the screen showed a client, an
     * approver and an address, offered an Approve button, and answered it with <em>"there is nothing here
     * for a client to act as"</em>.
     *
     * <p>⚠️ <strong>The owner travels in the reference, and that is what makes it usable at redemption.</strong>
     * An approval and its redemption are two requests and only the first has a signed-in person behind it,
     * so a bare sentinel would arrive at {@link CredentialIssuer} with no way to say whose it is. Carrying
     * the owner is safe for the same reason every other reference is: the flow re-reads
     * {@link Approver#choices()} when the approval arrives and refuses anything that is not in it, so a
     * forged owner is a reference nobody was offered.
     */
    String NEW_SUBJECT_PREFIX = "new-subject:";

    /**
     * Whoever is signed in on this request, and everything they may authorize a client to act as.
     *
     * @throws org.jmouse.ai.mcp.authorization.McpAuthorizationException when nobody is
     */
    Approver current();

    /**
     * A choice that does not exist yet, offered so a first-time connection is one step rather than three.
     *
     * @param ownerReference whose it will be, carried through to redemption — see {@link #NEW_SUBJECT_PREFIX}
     */
    static Choice newSubject(String ownerReference, String name, String detail) {
        return Choice.of(NEW_SUBJECT_PREFIX + ownerReference, name, detail);
    }

    /** Whose it would be, where this reference asks for something new — and empty where it does not. */
    static Optional<String> ownerOfNewSubject(String reference) {
        if (reference == null || !reference.startsWith(NEW_SUBJECT_PREFIX)) {
            return Optional.empty();
        }

        String ownerReference = reference.substring(NEW_SUBJECT_PREFIX.length());

        return ownerReference.isBlank() ? Optional.empty() : Optional.of(ownerReference);
    }

    /**
     * The person deciding, as the screen needs to show them.
     *
     * @param displayName how to name them
     * @param detail      something that tells one signed-in person from another — an address, a handle,
     *                    or null where there is nothing worth saying
     * @param choices     ⚠️ <strong>everything they may authorize, and nothing they may not.</strong>
     *                    This module re-reads the list when an approval arrives and refuses a reference
     *                    that is not in it, so it is a permission boundary rather than a convenience —
     *                    but only as narrow as the product makes it. Empty is a screen nobody can act on
     *                    and is worth avoiding rather than handling: a product whose person owns nothing
     *                    yet should offer {@link #newSubject} instead of nothing.
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
