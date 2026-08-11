package org.jmouse.access.policy;

import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.policy.PolicyEntitlementStore.SubjectHandleResolver;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.spi.CapabilityGrant;
import org.jmouse.access.spi.EntitlementStore;

import java.util.List;

/**
 * The capability grants of whatever policy is <strong>in force right now</strong>.
 *
 * <h2>⚠️ Why this exists rather than one store built at startup</h2>
 *
 * <p>{@link LivePolicy} is live: a policy editor adopts a new document without a deploy, and the
 * reference moves. A {@link PolicyEntitlementStore} constructed once from the startup document would
 * therefore serve yesterday's entitlements for the rest of the process — <em>silently</em>, and on the
 * one axis nobody thinks to check, because the permission half would have moved correctly and only
 * what people are paying for would be stale.
 *
 * <p>So the document is read on every question and the derived store is memoised against
 * <strong>the document it was derived from</strong>, by identity. Adopting replaces the reference, the
 * comparison fails, and the next question rebuilds. Nothing has to remember to invalidate anything,
 * which is the property worth having: an invalidation somebody must remember is an invalidation that
 * is eventually forgotten.
 *
 * <h2>⚠️ Rebuilding is a real cost, and that is why it is memoised at all</h2>
 *
 * <p>Deriving a store resolves every written handle — {@code @ORGANIZATION:acme} into an identifier —
 * and a product's resolver may well query for it. That must not happen per decision. It happens once
 * per document, inside the memoised build, and never again until the document changes.
 */
public final class LivePolicyEntitlements implements EntitlementStore {

    private final LivePolicy            live;
    private final ScopeCatalog          scopes;
    private final SubjectHandleResolver handles;
    private final QuantityScale         scale;

    /**
     * ⚠️ Neither field is {@code volatile}, and neither is guarded. A race here costs one duplicated
     * build of a small structure and can never produce a wrong answer: both threads derive the same
     * store from the same document, and whichever reference wins is equivalent to the one it replaced.
     * A lock on the hot path of every authorization decision would be a real cost paid to avoid an
     * imaginary one.
     */
    private PolicyDocument         derivedFrom;
    private PolicyEntitlementStore derived;

    public LivePolicyEntitlements(
            LivePolicy live, ScopeCatalog scopes, SubjectHandleResolver handles, QuantityScale scale) {

        this.live    = live;
        this.scopes  = scopes;
        this.handles = handles;
        this.scale   = scale;
    }

    @Override
    public List<CapabilityGrant> covering(List<ScopeReference> chain) {
        return current().covering(chain);
    }

    @Override
    public List<CapabilityGrant> heldAt(ScopeReference place) {
        return current().heldAt(place);
    }

    private PolicyEntitlementStore current() {
        PolicyDocument document = live.document();

        if (document != derivedFrom) {
            derived     = PolicyEntitlementStore.of(document, scopes, handles, scale);
            derivedFrom = document;
        }

        return derived;
    }
}
