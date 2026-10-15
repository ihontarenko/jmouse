package org.jmouse.parallax.authority;

import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.problem.ParallaxProblem;

import java.time.Instant;

/**
 * The host's owners as places: Parallax stores an owner as a pair and asks here whether it still exists, what it
 * sits in, and whether it takes new work.
 *
 * <p>A refusal is a {@link ParallaxProblem}: {@link ParallaxProblem.NotFound} for an owner that does not exist,
 * {@link ParallaxProblem.Conflict} for one that takes nothing new, and {@link ParallaxProblem.Unauthenticated} or
 * {@link ParallaxProblem.Refused} when the host checks access on the way. A host whose own exceptions differ
 * translates them here, keeping the message and the code.</p>
 */
public interface ParallaxPlaces {

    /**
     * The owner's whole chain, outermost first and ending with the owner itself — a project gives its workspace and
     * then the project. Refuses an owner that does not exist.
     */
    ParallaxPlace requireOwner(ParallaxOwner owner);

    /** Refuses an owner that takes nothing new — an archived project. Asked after access. */
    void requireAcceptsNew(ParallaxOwner owner);

    /** Something of the owner's changed at this moment; a host that keeps an owner's last change records it. */
    void recordChange(ParallaxOwner owner, Instant when);
}
