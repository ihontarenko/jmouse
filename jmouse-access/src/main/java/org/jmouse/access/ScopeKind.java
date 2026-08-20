package org.jmouse.access;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * A kind of place a permission can be held at.
 *
 * <h2>⚠️ The scopes form a TREE, not a list</h2>
 *
 * <p>They were a list once, and the position in {@code declare scopes} was the width — each scope
 * wrapping the next. That reads well right up to the first product with two kinds of place that do not
 * wrap each other at all. Kiwi has exactly that: sections hold pages, library directories hold files,
 * and one never contains the other. A linear order can only say "the next one is inside the previous",
 * so it described a nesting that does not exist — <em>go into a section, then into a directory</em> —
 * and nothing in the product could ever perform that descent.</p>
 *
 * <p>So a scope now names {@link #inside()}: the place it sits within, or nothing at all, which means it
 * hangs directly off the widest scope. <strong>Siblings are the default and nesting is what has to be
 * stated</strong>, which is the way round that makes the common case short and the load-bearing case
 * explicit.</p>
 *
 * <h2>⚠️ Only a PLACE is in the tree</h2>
 *
 * <p>{@link ScopeNature#EVERYTHING} is the root and {@link ScopeNature#OWN_ROWS} is not a place at all —
 * it answers <em>whose row</em>, which is an axis beside the tree rather than a leaf in it. In the old
 * list they both had to occupy a position, which is why "own rows" ended up declared narrower than a
 * section: an artefact of the notation, never a fact about the model.</p>
 *
 * <h2>⚠️ Covering and existing are two different questions</h2>
 *
 * <p>{@link #inside()} says a grant at the outer place reaches this one. {@link #requiredAncestor()}
 * says a target naming this place must also name that one. They usually coincide and they are not the
 * same: conflating them makes an incompletely addressed target refuse with <em>no permission</em>, which
 * sends whoever reads it looking at grants rather than at the address.</p>
 */
public interface ScopeKind {

    /** What a scope with no instance is addressed as. */
    String NO_INSTANCE = "*";

    /** The scope's name, as the policy file writes it. */
    String name();

    /**
     * Position in the declaration, kept for stable ordering of refusals and reports.
     *
     * <p>⚠️ <strong>No longer the width.</strong> Width is {@link #inside()}. This is only what makes two
     * otherwise equal answers come back in the same order every time — a report that reshuffles between
     * runs is a report nobody trusts.</p>
     *
     * @return the declaration position
     */
    int rank();

    /** Whether this is the widest scope, a place, or the owner axis. */
    ScopeNature nature();

    /** Which request parameter names an instance of this place, where a route can name one. */
    Optional<String> requestParameter();

    /**
     * The place this one sits inside, or empty when it hangs off the widest scope.
     *
     * <p>⚠️ Empty means <strong>sibling</strong>, not <em>narrowest</em>. A product declaring no nesting
     * gets a flat set of independent places, which is what most products actually have.</p>
     *
     * @return the enclosing place
     */
    default Optional<ScopeKind> inside() {
        return Optional.empty();
    }

    /**
     * A place every target naming this one must also name, or empty.
     *
     * <p>⚠️ Deliberately separate from {@link #inside()}. A space is inside an organization <em>and</em>
     * cannot be addressed without one; a directory is inside the installation and there is nothing to
     * name. Stating the second only where it is true keeps a refusal about an incomplete address from
     * reading as a refusal about permissions.</p>
     *
     * @return the ancestor a target must name
     */
    default Optional<ScopeKind> requiredAncestor() {
        return Optional.empty();
    }

    /** Whether naming this kind requires an identifier — a place does, the universal scopes do not. */
    default boolean namesAnInstance() {
        return nature() == ScopeNature.PLACE;
    }

    /**
     * Every place this one sits inside, innermost first.
     *
     * @return the enclosing places
     */
    default Set<ScopeKind> enclosing() {
        Set<ScopeKind> found = new LinkedHashSet<>();

        for (Optional<ScopeKind> above = inside(); above.isPresent(); above = above.get().inside()) {
            // A cycle would loop forever here, so stop on one. ScopeCatalog refuses cycles outright at
            // startup; this guard is what stops a malformed catalogue hanging a request instead.
            if (!found.add(above.get())) {
                break;
            }
        }

        return found;
    }

    /**
     * Whether this kind covers another — the same kind, the widest scope, or an enclosing place.
     *
     * <p>⚠️ Two <strong>siblings</strong> cover each other in neither direction, which is the whole point
     * of the tree: a grant over a section says nothing about a directory, and no ordering of the
     * declaration can accidentally say otherwise.</p>
     *
     * @param other the kind to test
     * @return whether a grant at this kind reaches that one
     */
    default boolean isAtLeastAsWideAs(ScopeKind other) {
        if (equals(other) || nature() == ScopeNature.EVERYTHING) {
            return true;
        }

        // ⚠️ Own-rows is narrower than every place, and it is NOT in the tree — it answers "whose row",
        // which every place still contains. Taking it out of the width chain without saying this made
        // every permission floored at SELF unenforceable at a place, and the whole product refused to
        // start with "means nothing below SELF" on a hundred routes.
        if (other.nature() == ScopeNature.OWN_ROWS) {
            return true;
        }

        // ⚠️ And own-rows is wider than nothing: two siblings cover each other in neither direction,
        // which is the point of the tree.
        if (nature() == ScopeNature.OWN_ROWS) {
            return false;
        }

        return other.enclosing().contains(this);
    }
}
