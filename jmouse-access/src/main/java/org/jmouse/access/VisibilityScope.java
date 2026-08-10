package org.jmouse.access;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Which rows a subject may see — the read half of the same answer {@code @RequiresAccess} gives
 * about one row.
 *
 * <pre>
 * visible(row) =  row.owner = me
 *              ∪  row lives in one of the places I hold this permission at
 *              ∪  everything, if I hold it at INSTALLATION
 * </pre>
 *
 * <p>Three implementations of that sentence used to exist — {@code VisibleForms.PREDICATE},
 * {@code FormVisibility} and {@code FormEntryService}'s {@code EntryVisibility} — and they disagreed.
 * The form picker filtered by status and by not-already-here and by nothing else, while opening a
 * form asked for ownership or {@code form:listAll}; so the picker offered colleagues' forms nobody
 * could then open, and in an installation with two customers it offered the other customer's forms
 * outright. Entries were decided from a workspace <em>rank</em> — {@code membership.canEdit()} —
 * which answered a question about editing to decide something about reading.
 *
 * <p><strong>The narrow permission gates, the scope filters.</strong> That is the lesson of ADR-0016
 * kept rather than repeated: a listing route asks for {@code form:read} and this decides how much of
 * the library that reaches. Gating the route on the wide answer is what once refused a reader the
 * list of their own forms, and after this cluster it is not expressible — there is no second
 * permission left to gate on.
 *
 * <p><strong>The places are a set of scope references, not two named fields.</strong> Same reason as
 * {@link AccessTarget}: a third floor is then one more registered {@link ScopeKind} and nothing here.
 *
 * @param breadth how far the answer reaches
 * @param ownerId whose rows count as the subject's own — its master's, for a service sub-account.
 *                Part of every answer except where nothing is visible
 * @param places  the scopes the permission is held at, where {@link Breadth#PLACES}. Already
 *                flattened: a grant at a wider place arrives as the narrower ones it contains, since
 *                a filter has no row to ask upward from
 */
public record VisibilityScope(
        Breadth              breadth,
        String               ownerId,
        Set<ScopeReference>  places
) {

    public enum Breadth {

        /** Not held anywhere, or taken away by a deny. No row matches. */
        NOTHING,

        /** Held only at {@code SELF}: the rows this subject owns, wherever they live. */
        OWN_ONLY,

        /** Held at one or more places, plus whatever the subject owns. */
        PLACES,

        /** Held at {@code INSTALLATION}. Every row, which is what {@code form:listAll} meant. */
        EVERYTHING
    }

    public VisibilityScope {
        places = Set.copyOf(places);
    }

    public static VisibilityScope nothing() {
        return new VisibilityScope(Breadth.NOTHING, null, Set.of());
    }

    public static VisibilityScope everything(String ownerId) {
        return new VisibilityScope(Breadth.EVERYTHING, ownerId, Set.of());
    }

    public static VisibilityScope ownOnly(String ownerId) {
        return new VisibilityScope(Breadth.OWN_ONLY, ownerId, Set.of());
    }

    /**
     * Held at these places, or — where there are none — over the subject's own rows only.
     *
     * <p>No places is not a narrower list of places; it is a different answer, and collapsing it
     * here is what keeps an empty {@code IN} clause out of every query downstream.
     */
    public static VisibilityScope places(String ownerId, Set<ScopeReference> places) {
        return places.isEmpty() ? ownOnly(ownerId) : new VisibilityScope(Breadth.PLACES, ownerId, places);
    }

    public boolean seesNothing() {
        return breadth == Breadth.NOTHING;
    }

    public boolean seesEverything() {
        return breadth == Breadth.EVERYTHING;
    }

    /**
     * Whether this reaches past the subject's own rows.
     *
     * <p>The question {@code form:listAll}, {@code form:write:any} and {@code entry:manage} were
     * separate permissions for — one boolean, derived, rather than three catalogue entries each with
     * a paragraph explaining how it differs from the first.
     */
    public boolean seesBeyondOwnRows() {
        return breadth == Breadth.PLACES || breadth == Breadth.EVERYTHING;
    }

    /** Whether one place is in view — everything is, where the permission is held installation-wide. */
    public boolean includes(ScopeReference place) {
        return seesEverything() || places.contains(place);
    }

    /** The instances of one kind of place this reaches, for a query to bind. */
    public Set<String> instancesOf(ScopeKind kind) {
        return places.stream()
                .filter(place -> place.type().equals(kind))
                .map(ScopeReference::id)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    /** What the control room and a debug line print. */
    public String describe() {
        return switch (breadth) {
            case NOTHING    -> "nothing";
            case OWN_ONLY   -> "own rows";
            case EVERYTHING -> "everything";
            case PLACES     -> "own rows + " + new TreeSet<>(places.stream().map(ScopeReference::describe).toList());
        };
    }

}
