package org.jmouse.access;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The permissions this installation has — the third registration, beside {@link ScopeCatalog} and
 * {@link AxisCatalog}.
 *
 * <p>A permission is a bare {@code String} everywhere it is <em>asked about</em>, and deliberately so:
 * the decision path compares strings and a map lookup per request would buy nothing. This catalogue is
 * for the other two readers — whatever <em>writes</em> a grant, and whatever <em>checks</em> one that
 * was written.
 *
 * <p>That second reader is the point. While grants are rows inserted by code holding a constant, a
 * typo cannot happen. The moment a human writes {@code form:wrtie} in a policy file it loads, matches
 * nothing, and says nothing — the permission simply never appears in anybody's effective set. A
 * catalogue turns that into a failure at load, which is the whole argument for writing rules down
 * rather than inserting them.
 *
 * <p>⚠️ <strong>Not consulted by the engine.</strong> Nothing on the decision path reads this, and
 * nothing should start: an engine that validated every permission on every request would have made a
 * load-time concern into a per-call cost.
 */
public final class PermissionCatalog {

    private final Set<String> declared;

    public PermissionCatalog(List<String> permissions) {
        this.declared = Set.copyOf(new LinkedHashSet<>(permissions));
    }

    /** Whether anybody declared this permission. */
    public boolean contains(String permission) {
        return declared.contains(permission);
    }

    /** Every declared permission. */
    public Set<String> all() {
        return declared;
    }

    /**
     * Every permission in one namespace — what {@code form:*} expands to.
     *
     * <p>Expanded at load rather than matched per request, so the grant set stays concrete and the
     * control room can list what somebody actually holds. A wildcard evaluated per call would make
     * "what does this person hold" unanswerable, which is the question the control room exists for.
     *
     * @param namespace the part before the colon, without it
     */
    public Set<String> inNamespace(String namespace) {
        String prefix = namespace + ":";

        return declared.stream()
                .filter(permission -> permission.startsWith(prefix))
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    /** The permission by that exact name, where it is declared. */
    public Optional<String> byName(String permission) {
        return declared.contains(permission) ? Optional.of(permission) : Optional.empty();
    }
}
