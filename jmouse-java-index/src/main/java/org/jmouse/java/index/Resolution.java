package org.jmouse.java.index;

import org.jmouse.java.parser.model.SymbolId;

import java.util.List;

/**
 * What a type name turned out to denote. Closed: a consumer handles every outcome, and none of them
 * is an exception — a name that cannot be found is as ordinary a result as one that can.
 */
public sealed interface Resolution
        permits Resolution.Resolved,
                Resolution.Ambiguous,
                Resolution.TypeVariable,
                Resolution.Unresolved {

    /**
     * Exactly one type is meant.
     *
     * @param target the type
     */
    record Resolved(TypeSymbol target) implements Resolution {}

    /**
     * Several types are equally meant — two on-demand imports both supplying the name, or one
     * canonical name declared twice. None is preferred; every candidate is listed.
     *
     * @param candidates the types, at least two
     */
    record Ambiguous(List<TypeSymbol> candidates) implements Resolution {

        /** Copies the candidates. */
        public Ambiguous {
            candidates = List.copyOf(candidates);
        }
    }

    /**
     * The name is a type variable in scope, not a type.
     *
     * @param name the variable
     * @param declaredBy the type, method or constructor that declares it
     */
    record TypeVariable(String name, SymbolId declaredBy) implements Resolution {}

    /**
     * Nothing known declares the name — a library type, a missing dependency, a typing mistake. The
     * name never becomes a pointer to an invented type.
     *
     * @param name the name as written
     * @param presumedQualifiedName the canonical name the source itself commits to, when it does —
     *     through a single-type import or by writing the name qualified; {@code null} when the
     *     source leaves it open
     */
    record Unresolved(String name, String presumedQualifiedName) implements Resolution {}
}
