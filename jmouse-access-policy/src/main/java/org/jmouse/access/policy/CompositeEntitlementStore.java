package org.jmouse.access.policy;

import org.jmouse.access.ScopeReference;
import org.jmouse.access.spi.CapabilityGrant;
import org.jmouse.access.spi.EntitlementStore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Several entitlement stores read as one — {@link CompositeGrantStore} for the capability axis, and
 * for the identical reason.
 *
 * <p>A policy document fits what a tier <strong>contains</strong> and the handful of grants an
 * installation must never lose — structural, versioned, reviewed before it takes effect. It does not
 * fit <em>"this account started a trial on Tuesday"</em>, which a machine writes and which must not
 * produce a diff in a document people read. So the honest arrangement is both.
 *
 * <p><strong>Order is presentation, not precedence.</strong> Every store's grants are concatenated and
 * the engine resolves the union; a store listed first does not win. It cannot, because deny already
 * beats every allow regardless of which store either came from — and a composite that introduced
 * precedence would put a rule in front of that one.
 *
 * <p>⚠️ <strong>Which is why a file can never delete a row.</strong> A document that wants to take
 * something away writes a {@code deny}, and that denial wins wherever the row it is arguing with came
 * from. Subtraction is a grant here, not an absence — an absence would be a store quietly disagreeing
 * with another store, and nobody could see it happen.
 *
 * <p>The reason to care about order at all is reading: provenance appears in the order the stores are
 * listed, so putting the document first makes the structural half of an answer appear first on the
 * screen that explains one.
 */
public final class CompositeEntitlementStore implements EntitlementStore {

    private final List<EntitlementStore> stores;

    public CompositeEntitlementStore(List<EntitlementStore> stores) {
        this.stores = List.copyOf(stores);
    }

    public static CompositeEntitlementStore of(EntitlementStore... stores) {
        return new CompositeEntitlementStore(List.of(stores));
    }

    @Override
    public List<CapabilityGrant> covering(List<ScopeReference> chain) {
        return collect(store -> store.covering(chain));
    }

    @Override
    public List<CapabilityGrant> heldAt(ScopeReference place) {
        return collect(store -> store.heldAt(place));
    }

    private List<CapabilityGrant> collect(Function<EntitlementStore, List<CapabilityGrant>> question) {
        List<CapabilityGrant> answers = new ArrayList<>();

        for (EntitlementStore store : stores) {
            answers.addAll(question.apply(store));
        }

        return List.copyOf(answers);
    }
}
