package org.jmouse.cad.store.authority;

import org.jmouse.cad.store.problem.CadProblem;

import java.util.Optional;

/**
 * The host's answer to who is acting and what they may do. jmCAD asks here from its services, so a request and a live
 * socket are decided the same way.
 *
 * <p>A refusal is a {@link CadProblem}: {@link CadProblem.Unauthenticated} when nobody is signed in,
 * {@link CadProblem.Refused} when the actor may not, {@link CadProblem.NotFound} when the place is hidden from them. A
 * host whose own exceptions differ translates them here, keeping the message, so a service catches one type only.</p>
 */
public interface CadAuthority {

    /** The account (or other actor) the current work runs for; empty when nobody is signed in. */
    Optional<String> actor();

    /** The current actor, or {@link CadProblem.Unauthenticated}. */
    String requireActor();

    boolean permits(Capability capability, CadPlace place);

    /** Allowed, or a {@link CadProblem} naming why. */
    void require(Capability capability, CadPlace place);

    default boolean permitsGlobally(Capability capability) {
        return permits(capability, CadPlace.installation());
    }

    default void requireGlobally(Capability capability) {
        require(capability, CadPlace.installation());
    }
}
