package org.jmouse.access;

import java.util.Comparator;

/**
 * One scope, named: a {@link ScopeKind} and — where the kind is a place — which instance of it.
 *
 * <p>The unit a grant carries and the unit resolution matches on. Coverage runs widest to narrowest:
 * the {@linkplain ScopeNature#EVERYTHING widest scope} covers everything, a {@linkplain
 * ScopeNature#PLACE place} covers that place and everything nested inside it, and {@linkplain
 * ScopeNature#OWN_ROWS own rows} covers the rows the subject owns. In this product that reads
 * {@code INSTALLATION} · {@code ORGANIZATION:{id}} · {@code SPACE:{id}} · {@code SELF}, but the type
 * itself names none of them.
 *
 * <p>The chain of references covering a target is built by {@link ScopeCatalog#covering}, which is
 * where the vocabulary lives; which places nest inside which is
 * {@link org.jmouse.access.spi.ScopeHierarchy}'s question.
 *
 * @param type the kind of scope
 * @param id   which one, or {@link ScopeKind#NO_INSTANCE} where the kind names no instance
 */
public record ScopeReference(ScopeKind type, String id) {

    /**
     * Widest scope first — the order every list of these is read in.
     *
     * <p>It lives here rather than beside its one caller in {@link AccessTarget} on purpose. It is an
     * ordering <em>of this type</em>, and a constant in another class is initialised by that class's
     * own initialiser: {@code AccessTarget}'s constant empty target is built while its class is still
     * initialising, so a comparator declared there would have to be declared above it or be
     * {@code null} at the moment it is used. That ordering constraint is invisible and one reorder
     * away from an {@code ExceptionInInitializerError}; from here there is nothing to get wrong.
     */
    public static final Comparator<ScopeReference> WIDEST_FIRST =
            Comparator.comparingInt(place -> place.type().rank());

    public ScopeReference {
        if (type == null) {
            throw new IllegalArgumentException("A scope reference needs a scope type.");
        }
        if (type.namesAnInstance() && (id == null || id.isBlank() || ScopeKind.NO_INSTANCE.equals(id))) {
            throw new IllegalArgumentException(type.name() + " names one instance, so it needs an identifier.");
        }
        if (!type.namesAnInstance()) {
            id = ScopeKind.NO_INSTANCE;
        }
    }

    public static ScopeReference of(ScopeKind type, String id) {
        return new ScopeReference(type, id);
    }

    /** How the control room and the debug line print it. */
    public String describe() {
        return type.namesAnInstance() ? type.name() + ":" + id : type.name();
    }

}
