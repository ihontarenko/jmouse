package org.jmouse.access.policy;

import org.jmouse.access.ScopeReference;
import org.jmouse.access.spi.DirectGrant;
import org.jmouse.access.spi.GrantStore;
import org.jmouse.access.spi.RoleGrant;

import java.util.ArrayList;
import java.util.List;

/**
 * Several grant stores read as one — the shape a real installation actually has.
 *
 * <p>Policy files fit <strong>roles and their bundles</strong>: structural, versioned, changed with a
 * release. They do not fit <strong>individual assignments</strong> — "give Petro access to this
 * workspace" must not require a deploy. So the honest arrangement is both, in that order: a policy for
 * the structure and a database for who holds what.
 *
 * <p><strong>Order is presentation, not precedence.</strong> Every store's grants are concatenated and
 * the engine resolves the union; a store listed first does not win. It cannot, because deny already
 * wins over every allow regardless of where either came from — and a composite that introduced
 * precedence would put a rule in front of that one.
 *
 * <p>The reason to care about order at all is reading: provenance appears in the order the stores are
 * listed, so putting the policy first makes the structural half of an answer appear first in the
 * control room.
 */
public final class CompositeGrantStore implements GrantStore {

    private final List<GrantStore> stores;

    public CompositeGrantStore(List<GrantStore> stores) {
        this.stores = List.copyOf(stores);
    }

    public static CompositeGrantStore of(GrantStore... stores) {
        return new CompositeGrantStore(List.of(stores));
    }

    @Override
    public List<RoleGrant> rolesCovering(String subjectId, List<ScopeReference> chain) {
        return collect(store -> store.rolesCovering(subjectId, chain));
    }

    @Override
    public List<DirectGrant> directCovering(String subjectId, List<ScopeReference> chain) {
        return collect(store -> store.directCovering(subjectId, chain));
    }

    @Override
    public List<RoleGrant> rolesHeldBy(String subjectId) {
        return collect(store -> store.rolesHeldBy(subjectId));
    }

    @Override
    public List<DirectGrant> directHeldBy(String subjectId) {
        return collect(store -> store.directHeldBy(subjectId));
    }

    private <T> List<T> collect(java.util.function.Function<GrantStore, List<T>> question) {
        List<T> answers = new ArrayList<>();

        for (GrantStore store : stores) {
            answers.addAll(question.apply(store));
        }

        return List.copyOf(answers);
    }
}
