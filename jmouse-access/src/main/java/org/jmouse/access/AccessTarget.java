package org.jmouse.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where a request is aimed — an ordered list of <em>places</em>, whose row it is about, and which
 * module it belongs to.
 *
 * <p>A permission string says <em>what</em>; this says <em>where</em>. The two together are the whole
 * question the engine answers, and separating them is what let {@code *:any}, {@code *:listAll} and
 * {@code *:manage} stop being permissions: each of them was a scope spelled into a name.
 *
 * <h2>Why the places are a list and the other two are not</h2>
 *
 * <p>An obvious design gives this record one named field per floor. It is a trap: naming them makes
 * the authorization core know how many floors a product has and what they are called, so a product
 * with departments, or tenants, or nothing at all between an account and a row cannot use it without
 * editing it — and adding a floor costs a field, a getter, a {@code with…}, a branch in the binder
 * and a column.
 *
 * <p>So the places are an ordered list, widest first, and this type names none of them. A product
 * that wants {@code Targets.space(id)} to read better than {@code at(SPACE, id)} writes that one-liner
 * over {@link #at} on its own side.
 *
 * <p><strong>The owner is deliberately not one of them.</strong> It is not a place — it is the rows a
 * subject owns, wherever they happen to live. Folding it into the list would make "which place is this
 * in" and "whose is this" one question, and they have different answers about the same row.
 *
 * <p><strong>The module is not one of them either.</strong> It belongs to the axes that ask about a
 * <em>place</em> rather than about a person, and those are a product's questions rather than the
 * model's.
 *
 * <p><strong>An empty target is a real answer, not a missing one.</strong> Plenty of endpoints are
 * aimed at no place at all, and none of them should be blocked by some place's menu — so a target
 * with nothing in it passes the place-shaped axes untouched. Refusing an unscoped call because it
 * named nowhere would turn every one of them into a question about a place that is not there.
 *
 * @param places    the scopes this request is aimed at, widest first and at most one per kind
 * @param ownerId   who owns the row being touched, where a row is being touched. Filled by an
 *                  {@code AccessTarget} rather than by the call site, so that "is this mine"
 *                  is answered the same way for every kind of resource
 * @param moduleKey the module this endpoint belongs to, where it belongs to one
 */
public record AccessTarget(
        List<ScopeReference> places,
        String               ownerId,
        String               moduleKey
) {

    private static final AccessTarget NOTHING = new AccessTarget(List.of(), null, null);

    public AccessTarget {
        places = places.stream().sorted(ScopeReference.WIDEST_FIRST).toList();
    }

    // ── The generic core ──────────────────────────────────────────────────────

    /** The unscoped request: an installation-wide route with no place and no row. */
    public static AccessTarget installation() {
        return NOTHING;
    }

    /**
     * The same target, additionally aimed at one place — replacing any it already named of that kind.
     *
     * <p>Replacing rather than appending is what keeps "at most one workspace per request" true
     * without anybody checking: a target naming two workspaces would make the covering chain answer
     * about a place the caller did not ask about.
     */
    public AccessTarget at(ScopeKind kind, String instance) {
        if (instance == null || instance.isBlank()) {
            return this;
        }

        List<ScopeReference> narrowed = new ArrayList<>(places.stream()
                .filter(place -> !place.type().equals(kind))
                .toList());
        narrowed.add(ScopeReference.of(kind, instance));

        return new AccessTarget(narrowed, ownerId, moduleKey);
    }

    /** Which instance of this kind of place, where the target names one. */
    public Optional<String> place(ScopeKind kind) {
        return places.stream()
                .filter(scope -> scope.type().equals(kind))
                .map(ScopeReference::id)
                .findFirst();
    }

    public boolean names(ScopeKind kind) {
        return place(kind).isPresent();
    }

    /**
     * The innermost place this target names, where it names any.
     *
     * <p>The places are held widest first, so this is the last of them — and it is the one worth
     * naming when something has to say <em>where</em> in a single breath: a share link is over a row,
     * and the row lives in the narrowest place, not in the account above it.
     */
    public Optional<ScopeReference> narrowestPlace() {
        return places.isEmpty() ? Optional.empty() : Optional.of(places.getLast());
    }

    public AccessTarget withOwner(String ownerId) {
        return new AccessTarget(places, ownerId, moduleKey);
    }

    public AccessTarget withModule(String moduleKey) {
        return new AccessTarget(places, ownerId, moduleKey);
    }

    public boolean hasOwner() {
        return isPresent(ownerId);
    }

    public boolean hasModule() {
        return isPresent(moduleKey);
    }

    /** True where nothing is named — the unscoped request axes 2 to 4 let through. */
    public boolean isUnscoped() {
        return places.isEmpty() && !hasOwner();
    }

    /** What the debug line and the control room print. */
    public String describe() {
        StringBuilder description = new StringBuilder();

        places.forEach(place -> description.append(place.describe()).append(' '));

        if (hasOwner()) {
            description.append("owner=").append(ownerId).append(' ');
        }
        if (hasModule()) {
            description.append("module=").append(moduleKey).append(' ');
        }

        return description.isEmpty() ? "installation" : description.toString().trim();
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
