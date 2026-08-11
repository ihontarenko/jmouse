package org.jmouse.access.spi;

import org.jmouse.access.ScopeReference;

import java.util.List;

/**
 * Where capability grants are kept — the seam {@link GrantStore} already is for permissions.
 *
 * <p>It exists because the entitlement axis was the one axis of the engine whose answer came from a
 * mechanism the engine knew nothing about: a resolver inside a product, holding a repository, with its
 * own spelling of <em>deny wins</em>. Two things called a grant, resolved two ways. Above this
 * interface there are now capabilities and scopes for both axes, and a schema on the other side of it
 * for both.
 *
 * <h2>⚠️ A capability is granted to a place, never to a person</h2>
 *
 * <p>The one place this interface deliberately does <strong>not</strong> mirror {@link GrantStore}, and
 * it took an adapter to notice. These methods used to take a {@code subjectId} beside the scopes,
 * copied across from the permission half — and no implementation could use it coherently: the covering
 * query ignored it outright while the other read it as a <em>place</em> identifier. Two readings of one
 * parameter in one class.
 *
 * <p>The model already said so. A capability is held by an account, a workspace, a tenant — and if
 * per-person metering is ever wanted, it arrives as a <em>scope</em> rather than as a second addressing
 * scheme, which is the same decision that let {@code MEMBERSHIP} be dropped. So a place is the whole
 * address, and there is nothing else to pass.
 *
 * <h2>Two questions, and why they are not one</h2>
 *
 * <p>The split {@link GrantStore} makes, for the same reason. <strong>Covering</strong> is what a
 * <em>check</em> asks: it knows the chain of scopes that could matter and wants only the grants sitting
 * on it. <strong>Held at</strong> is what an <em>administration screen</em> asks: it is showing one
 * place's own grants, including the ones a check would never reach because they sit below it. Serving
 * the second from the first is impossible; serving the first from the second is an N+1 on the hot path.
 *
 * <p><strong>An implementation must return only exact matches.</strong> A covering query typically
 * matches scope types and identifiers as two independent lists so a chain of four costs one round trip
 * — and the widened result that produces has to be narrowed back inside the adapter. An engine that
 * knew a store's queries were approximate would be an engine that knew the store.
 *
 * <h2>⚠️ Expired grants are returned, not filtered</h2>
 *
 * <p>A store must <strong>not</strong> add {@code AND valid_until > now} to its queries. Resolution
 * drops an expired grant; the control room prints it. Filtering here destroys the difference between
 * <em>"your trial ended on the 12th"</em> and <em>"you never had this"</em>, which is the difference
 * between a sentence somebody can act on and one they cannot.
 *
 * <h2>⚠️ Deliberately read-only, and deliberately not enumerable</h2>
 *
 * <p>Issuing an entitlement is a product's own business — its screens, its audit trail, its rules
 * about who may — and an interface that could also write would invite the engine to. Nor can it be
 * asked to list subjects: a store that could enumerate every account would be a store the engine could
 * walk. Whoever opens an administration screen already knows which accounts they are looking at.
 *
 * <h2>What a second product has to do to adopt this</h2>
 *
 * <p>Declare its own capabilities and implement these two methods over whatever table it keeps them in.
 * Nothing above this interface knows what is being counted, so a product whose layers are issues,
 * boards and sprints rather than workspaces and seats changes nothing in the engine. A product with no
 * layers at all passes a single place and gets the same answers.
 */
public interface EntitlementStore {

    /** Every capability grant sitting at one of these places, and nothing at any other. */
    List<CapabilityGrant> covering(List<ScopeReference> chain);

    /** Every capability grant this one place holds — what an administration screen lists. */
    List<CapabilityGrant> heldAt(ScopeReference place);

    /** A store holding nothing, for a product that has not adopted the axis. */
    static EntitlementStore empty() {
        return new EntitlementStore() {

            @Override
            public List<CapabilityGrant> covering(List<ScopeReference> chain) {
                return List.of();
            }

            @Override
            public List<CapabilityGrant> heldAt(ScopeReference place) {
                return List.of();
            }
        };
    }
}
