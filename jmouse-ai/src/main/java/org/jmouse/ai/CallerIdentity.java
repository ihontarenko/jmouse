package org.jmouse.ai;

import java.util.Map;
import java.util.Objects;

/**
 * Who is calling, and whom they are calling for.
 *
 * <p><strong>Authorization rests on the caller; ownership rests on the acting subject.</strong> That
 * one sentence is the whole reason this type exists rather than a bare identifier. An assistant
 * running under a service credential may do only what that credential is allowed to do, while
 * anything it creates belongs to the person it works for — and conflating the two breaks the feature
 * in both directions at once: records created by the caller become invisible to their owner, and
 * "what do I have" comes back empty.
 *
 * <p><strong>Both accessors are plain, and there is no third that picks between them.</strong> An
 * earlier design offered a single {@code actingUser()} returning the subject, with a comment naming
 * the one action that wanted the caller instead — a comment doing a type's job, and the kind that
 * survives exactly until somebody writes the second exception. A handler that asks what the
 * <em>caller</em> can reach reads {@link #callerId()}; a handler acting on somebody's records reads
 * {@link #actingSubject()}. Neither is the default, because there is no default.
 *
 * <p>For a person using an in-app assistant the two identifiers are equal, and that must not require
 * a second type — {@link #of(String)} is that case.
 *
 * <p>Who is <em>allowed</em> to become a caller is a product's identity policy and is decided behind
 * {@link org.jmouse.ai.spi.CallerResolver}. Nothing here refuses anything.
 *
 * @param callerId         the identity the authorization decision is made against; never blank
 * @param actsOnBehalfOfId whose records are in view; equal to {@code callerId} when the caller acts
 *                         for itself
 * @param attributes       whatever else a product's resolver knows and its handlers may want — a
 *                         tenant, a locale, a credential's scope. Opaque here: the mechanism carries
 *                         them and never reads one
 */
public record CallerIdentity(String callerId, String actsOnBehalfOfId, Map<String, String> attributes) {

    public CallerIdentity {
        Objects.requireNonNull(callerId, "callerId");
        Objects.requireNonNull(actsOnBehalfOfId, "actsOnBehalfOfId");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /** A caller acting for itself — a person using an assistant, or a single-tenant service. */
    public static CallerIdentity of(String callerId) {
        return new CallerIdentity(callerId, callerId, Map.of());
    }

    /** A caller acting for somebody else — a service credential issued under an account. */
    public static CallerIdentity actingFor(String callerId, String subjectId) {
        return new CallerIdentity(callerId, subjectId, Map.of());
    }

    /** The same identity, carrying what the product's resolver knows about it. */
    public CallerIdentity with(Map<String, String> additional) {
        Map<String, String> merged = new java.util.LinkedHashMap<>(attributes);
        merged.putAll(additional);
        return new CallerIdentity(callerId, actsOnBehalfOfId, merged);
    }

    /**
     * Whose records are in view.
     *
     * <p>Named for what it is rather than for what it usually holds, so that reading it is a decision
     * a handler makes on purpose.
     */
    public String actingSubject() {
        return actsOnBehalfOfId;
    }

    /** Whether the caller and the subject are the same identity — the in-app assistant's case. */
    public boolean actsForItself() {
        return callerId.equals(actsOnBehalfOfId);
    }

    /** How the pair reads in a log line, without pretending they are one thing when they are two. */
    public String describe() {
        return actsForItself() ? callerId : callerId + " for " + actsOnBehalfOfId;
    }
}
