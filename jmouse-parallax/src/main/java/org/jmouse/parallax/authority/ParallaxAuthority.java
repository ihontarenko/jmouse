package org.jmouse.parallax.authority;

import org.jmouse.parallax.problem.ParallaxProblem;

import java.util.Optional;

/**
 * The host's answer to who is acting and what they may do. Parallax asks here from its services, so a request, a
 * worker and a live socket are decided the same way.
 *
 * <p>A refusal is a {@link ParallaxProblem}: {@link ParallaxProblem.Unauthenticated} when nobody is signed in,
 * {@link ParallaxProblem.Refused} when the actor may not, {@link ParallaxProblem.NotFound} when the place is hidden from
 * them, {@link ParallaxProblem.Conflict} when the place no longer allows it, and {@link ParallaxProblem.Invalid} for a
 * question the host cannot decide. A host whose own exceptions differ translates them here, keeping the message and
 * the code, so a service catches one type only.</p>
 */
public interface ParallaxAuthority {

    /** The account (or other actor) the current work runs for; empty when nobody is signed in. */
    Optional<String> actor();

    /** The current actor, or {@link ParallaxProblem.Unauthenticated}. */
    String requireActor();

    boolean permits(ParallaxCapability capability, ParallaxPlace place);

    /** Allowed, or a {@link ParallaxProblem} naming why. */
    void require(ParallaxCapability capability, ParallaxPlace place);

    default boolean permitsGlobally(ParallaxCapability capability) {
        return permits(capability, ParallaxPlace.installation());
    }

    default void requireGlobally(ParallaxCapability capability) {
        require(capability, ParallaxPlace.installation());
    }
}
