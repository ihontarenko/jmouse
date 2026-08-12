package org.jmouse.access.enforcement;

import java.util.Collection;
import java.util.List;

/**
 * What is true of <em>every</em> call, attached beside what each route says about itself.
 *
 * <p>{@link AccessContext} reads a method parameter, which is the honest answer for anything the
 * caller passed. Some of what a rule wants to know was never passed: what kind of place the call is
 * in, which tenant a filter resolved, what a header said, which deployment this is. Those are
 * properties of the surrounding request — or of the installation — rather than of the invocation, and
 * this is the seam for them.
 *
 * <h2>What it is for, in one example</h2>
 *
 * <p>A workspace that counts IT assets should list two purposes and no others. Written against a
 * <em>place</em> that is one rule per workspace, because a condition sees which place a grant sits at
 * and not what that place <strong>counts</strong>:
 *
 * <pre>{@code @SPACE:itam-kyiv form:read deny when action == '…' and purpose != 'ASSET'}</pre>
 *
 * <p>Attached as a value, it is one rule for every workspace of that kind, now and later:
 *
 * <pre>{@code @GLOBAL form:read deny when { action == '…' and spaceKind == 'itam' and purpose != 'ASSET' } }</pre>
 *
 * <p>The second is the rule somebody meant. The first is the rule they can write without this, and it
 * is one new workspace away from being quietly wrong.
 *
 * <h2>⚠️ It registers; it does not compute</h2>
 *
 * <pre>{@code
 * @Component
 * class SpaceKindValues implements AmbientAccessValues {
 *
 *     @Override
 *     public void publish(AmbientPublication publication) {
 *         publication.attachLazy("spaceKind", this::activeSpaceKind);
 *     }
 * }
 * }</pre>
 *
 * <p>The body names things and hands over suppliers. That is what lets the same declaration be read
 * twice for two different purposes — <strong>at startup for its names</strong>, so a rule mentioning
 * one can be checked and a rule mentioning a typo can be refused, and <strong>per call for its
 * values</strong>, of which only the ones a rule actually reads are ever worked out.
 *
 * <p>It replaces a version that answered a finished {@code Map}, and the difference is not cosmetic.
 * That shape forced every contributor to compute on every guarded call whether or not anything read
 * it, so each one had to invent its own memoisation, its own null-tolerance and its own way of not
 * putting a query on the security path. And because a map cannot be asked what it would contain, the
 * names had to be written down a second time by hand, beside the bean, where the two could disagree.
 *
 * <h2>⚠️ Three things that still hold</h2>
 *
 * <p><strong>A route's own value wins.</strong> These are merged <em>under</em> whatever
 * {@link AccessContext} declares, so a route that publishes {@code spaceKind} from a parameter means
 * it. Attached is a default, never an override.
 *
 * <p><strong>Absence is ordinary.</strong> Leaving a name out is how a caller with no such context
 * says so — outside a request, in a job, in a test. A rule reading an absent value does not hold,
 * which is the safe direction for a conditional allow and the dangerous one for a conditional deny.
 * An attached value is therefore <em>not</em> subject to the required-by-default rule that governs a
 * declared one: nobody promised it, so nothing is broken when it is not there.
 *
 * <p><strong>Nothing may throw on the decision path.</strong> A supplier that does yields an absent
 * value rather than an exception — see {@link org.jmouse.access.spi.DeferredValue}.
 */
@FunctionalInterface
public interface AmbientAccessValues {


    /** Attaches nothing — the default, and what a caller with no surrounding context has. */
    static AmbientAccessValues none() {
        return publication -> {
            // Nothing is true of every call here.
        };
    }

    /**
     * Every one of these as a single contributor.
     *
     * <p>⚠️ <strong>Contributing is many, consuming is one.</strong> A product attaches its values from
     * wherever they belong — the workspace's kind from the space feature, the deployment name from
     * configuration, whatever a later feature adds — so there are naturally several of these beans.
     * Everything that <em>reads</em> them wants one merged view, and asking a container for "the"
     * contributor when a product has grown a second one fails at startup with a message about bean
     * ambiguity rather than about access control.
     *
     * <p>Registration order is contribution order, so a later contributor attaching a name an earlier
     * one already attached wins — the same last-writer rule a route's own value already has over an
     * attached one.
     */
    static AmbientAccessValues all(Collection<AmbientAccessValues> contributors) {
        List<AmbientAccessValues> attached = List.copyOf(contributors);

        return switch (attached.size()) {
            case 0  -> none();
            case 1  -> attached.getFirst();
            default -> publication -> attached.forEach(contributor -> contributor.publish(publication));
        };
    }

    /**
     * Registers what this contributor attaches.
     *
     * <p>⚠️ Called at startup as well as per call, so the body must read nothing and merely name what
     * it can provide.
     */
    void publish(AmbientPublication publication);
}
