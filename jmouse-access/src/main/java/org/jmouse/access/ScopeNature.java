package org.jmouse.access;

/**
 * What a scope kind <em>is</em> to the model, as opposed to what the product calls it.
 *
 * <p>The engine has to reason about two scopes it can never be told the name of. The covering chain
 * always begins with "everything", or an unscoped grant would stop working the moment a product
 * renamed its widest scope; and it ends with "the rows I own" whenever the target names an owner, or
 * ownership would go back to being a mechanism of its own. Everything between those two is a
 * <em>place</em>, and the engine's whole ambition about places is to count them and order them.
 *
 * <p>So this is the one question a product must answer about each of its scopes, and answering it is
 * not optional: {@link ScopeCatalog} refuses to start on a vocabulary with no {@link #EVERYTHING}, on
 * two of them, or on either universal scope declared somewhere other than the end it belongs at. A
 * product cannot forget, because there is nowhere to leave the answer out.
 *
 * @see ScopeKind
 */
public enum ScopeNature {

    /**
     * The widest scope there is — every place, every row, always in the covering chain.
     *
     * <p>One product calls it {@code INSTALLATION}; a hosted product might call it {@code GLOBAL} and a
     * single-tenant one might have nothing else at all. Exactly one scope is this, and it is first.
     */
    EVERYTHING,

    /**
     * A place: one of the product's nestings, which a target names an instance of.
     *
     * <p>These are the floors — one product's {@code ORGANIZATION} and {@code SPACE}, another's
     * {@code TENANT · DIVISION · DEPARTMENT}, or none. Their <em>order</em> is their containment,
     * widest first, and which one sits inside which is {@link org.jmouse.access.spi.ScopeHierarchy}'s
     * question rather than this enum's.
     */
    PLACE,

    /**
     * The rows the subject owns, wherever they live.
     *
     * <p>The narrowest scope and never a place, which is why it names no instance: "mine" is answered
     * by comparing the row's owner against the subject, not by naming somewhere. At most one scope is
     * this, and a product without a notion of ownership simply declares none.
     */
    OWN_ROWS
}
