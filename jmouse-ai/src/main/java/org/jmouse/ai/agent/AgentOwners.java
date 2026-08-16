package org.jmouse.ai.agent;

import java.util.Optional;

/**
 * Whose agents these are — the signed-in person, as an {@link Agent#ownerReference()}.
 *
 * <h2>Why a port and not {@code Principal.getName()}</h2>
 *
 * <p>Because the two products do not agree, and neither is wrong. One puts its account entity into the
 * authentication and its {@code getName()} answers an email address; the other puts a {@code Jwt} in and
 * its {@code getName()} answers whatever the token's subject happens to be — an identity-server user
 * identifier, not this installation's account. An owner reference is neither: it is the opaque string a
 * product decided to key an agent on, which only that product can produce.
 *
 * <p>⚠️ <strong>This is what makes a self-scoped screen possible at all.</strong> Every other route in
 * this module is an administrator's, gated on one permission and free to name any owner it likes. A
 * person editing <em>their own</em> agents must not be able to name one — so the owner cannot come from
 * the request, and something has to answer it from the security context instead.
 *
 * <p>⚠️ <strong>Empty means "nobody is signed in", never "this person owns nothing".</strong> The
 * self-scoped controller refuses on empty rather than answering an empty list, because an empty list
 * would tell somebody whose session had quietly expired that their agents had been deleted.
 */
@FunctionalInterface
public interface AgentOwners {

    /**
     * The owner reference of whoever is calling, or empty if that is nobody.
     *
     * <p>Read from the security context rather than taken as an argument, deliberately: a signature that
     * accepted one would be a signature somebody could pass somebody else's.
     */
    Optional<String> current();

    /**
     * For a product with no notion of a person owning an agent.
     *
     * <p>The self-scoped routes then refuse everything, which is a screen that explains itself rather
     * than one that silently shows nothing.
     */
    static AgentOwners nobody() {
        return Optional::empty;
    }
}
