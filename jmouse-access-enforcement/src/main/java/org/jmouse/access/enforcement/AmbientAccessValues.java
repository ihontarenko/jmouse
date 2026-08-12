package org.jmouse.access.enforcement;

import java.util.Map;

/**
 * What is true of <em>every</em> call, published beside what each route says about itself.
 *
 * <p>{@link AccessValue} reads a method parameter, which is the honest answer for anything the caller
 * passed. Some of what a rule wants to know was never passed: what kind of place the call is in, which
 * tenant a filter resolved, what a header said. Those are properties of the surrounding request rather
 * than of the invocation, and this is the seam for them.
 *
 * <h2>What it is for, in one example</h2>
 *
 * <p>A workspace that counts IT assets should list two purposes and no others. Written against a
 * <em>place</em> that is one rule per workspace, because a condition sees which place a grant sits at
 * and not what that place <strong>counts</strong>:
 *
 * <pre>{@code @SPACE:itam-kyiv form:read deny when action == '…' and purpose != 'ASSET'}</pre>
 *
 * <p>Published as a value, it is one rule for every workspace of that kind, now and later:
 *
 * <pre>{@code @GLOBAL form:read deny when { action == '…' and spaceKind == 'itam' and purpose != 'ASSET' } }</pre>
 *
 * <p>The second is the rule somebody meant. The first is the rule they can write today, and it is one
 * new workspace away from being quietly wrong.
 *
 * <h2>⚠️ Three things to know before implementing one</h2>
 *
 * <p><strong>It is asked once per guarded call.</strong> Not once per request, and not once per
 * decision — so an implementation that reaches a database has put a query on the security path.
 * Memoise it wherever the surrounding unit of work lives; for a web application that is a
 * request-scoped bean, which is the same place the value came from.
 *
 * <p><strong>A route's own value wins.</strong> These are merged <em>under</em> whatever
 * {@link AccessContext} declares, so a route that publishes {@code spaceKind} from a parameter means
 * it. Ambient is a default, never an override.
 *
 * <p><strong>Absence is ordinary.</strong> Leaving a name out is how a caller with no such context
 * says so — outside a request, in a job, in a test. ⚠️ A rule reading a value that is absent does not
 * hold, which is the safe direction for a conditional allow and the dangerous one for a conditional
 * deny. An ambient value is therefore <em>not</em> subject to the required-by-default rule that
 * governs {@link AccessValue}: nobody promised it, so nothing is broken when it is not there. Write
 * denials that read one with that in mind.
 */
@FunctionalInterface
public interface AmbientAccessValues {

    /** Publishes nothing — the default, and what a caller with no surrounding context has. */
    static AmbientAccessValues none() {
        return Map::of;
    }

    /**
     * The values true of this call, or an empty map where none are.
     *
     * <p>⚠️ Must not throw for ordinary data. This runs while a decision is being made, and an
     * exception here is an outage wearing an authorization failure's clothes.
     */
    Map<String, Object> published();
}
