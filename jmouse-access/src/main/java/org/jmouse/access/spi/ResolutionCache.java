package org.jmouse.access.spi;

import org.jmouse.access.AccessTarget;
import org.jmouse.access.EffectivePermissions;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.VisibilityScope;

import java.util.List;
import java.util.function.Supplier;

/**
 * Somewhere to keep an answer for as long as one unit of work lasts.
 *
 * <p>Resolution is expensive and repetitive in a way that only shows up under load. A menu render asks
 * about a dozen permissions; a paginated listing asks about a page of rows; both are one question
 * asked many times, and both get the same answer every time. So the resolvers memoise — and
 * <em>where</em> they memoise is the caller's business, because only the caller knows what a unit of
 * work is. In a web application it is the request; in a queue consumer it is the message; in a batch
 * job it might be the whole run.
 *
 * <p>This exists rather than a {@code Map} field because a cache with the wrong lifetime is worse than
 * none: a singleton holding effective permissions would keep serving a revoked grant until restart.
 * Making the caller supply the scope makes that impossible to do by accident.
 *
 * <p><strong>{@link #none()} is a legitimate implementation</strong>, and the resolvers work with it —
 * every question is simply asked again. Use it outside any unit of work, where remembering an answer
 * would be remembering it for the wrong length of time.
 */
public interface ResolutionCache {

    /** A cache that keeps nothing: correct, and slower. */
    static ResolutionCache none() {
        return new ResolutionCache() {

            @Override
            public EffectivePermissions permissions(
                    String subjectId, List<ScopeReference> chain, Supplier<EffectivePermissions> loader) {
                return loader.get();
            }

            @Override
            public VisibilityScope visibility(
                    String subjectId, String permission, Supplier<VisibilityScope> loader) {
                return loader.get();
            }
        };
    }

    /**
     * The effective set for one subject at one scope chain.
     *
     * <p>Keyed on the whole chain rather than on its narrowest link: the same person legitimately has
     * different answers in two places, and two targets sharing one place but not the one above it are
     * two different questions.
     */
    EffectivePermissions permissions(
            String subjectId, List<ScopeReference> chain, Supplier<EffectivePermissions> loader);

    /**
     * Which rows one subject may see under one permission.
     *
     * <p>The N+1 this exists to stop is the one a listing invites: visibility is a property of the
     * <em>reader</em> rather than of the row, so resolving it inside the loop asks the same question
     * once per row and gets the same answer every time.
     */
    VisibilityScope visibility(String subjectId, String permission, Supplier<VisibilityScope> loader);

    /**
     * The covering chain for one target — the places it names, and everything containing them.
     *
     * <h3>⚠️ The chain is built BEFORE the other two can be consulted, so it needs its own line</h3>
     *
     * <p>{@link #permissions} is keyed ON the finished chain, which means the walk that builds the key
     * can never be saved by it — and that walk asks a {@link ScopeHierarchy} per place the target
     * names, which for a tree is an indexed query each. A listing deciding per row therefore paid for
     * the chain again on every decision, including the decisions the permissions cache then answered
     * from memory: measured at 156 ancestor queries for 63 folders in one request.</p>
     *
     * <p>⚠️ <strong>Keyed on the subject as well as the target</strong>, because the chain depends on
     * both: {@code covering} is passed which rows this subject owns, and an owned row names a
     * {@code SELF} place that somebody else's does not.</p>
     *
     * <p>⚠️ Default: no memory at all, which is correct and merely slower — the same contract as
     * {@link #none()}. An implementation that already keeps the other two answers should keep this
     * one; it is the cheapest of the three and the most often repeated.</p>
     *
     * @param subjectId who is asking
     * @param target    what is being asked about
     * @param loader    how to build it when nothing is remembered
     * @return the chain, widest first
     */
    default List<ScopeReference> covering(String subjectId, AccessTarget target,
                                          Supplier<List<ScopeReference>> loader) {
        return loader.get();
    }
}
