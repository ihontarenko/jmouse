package org.jmouse.java.index;

import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeReferenceRole;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * One type name a file writes, with the two declarations that decide what it can mean.
 *
 * <p>They are different on purpose. In {@code class Binder extends Base} the reference belongs to
 * {@code Binder} — it is part of what {@code Binder} says about itself — but it stands
 * <em>outside</em> {@code Binder}'s body, so a type nested in {@code Binder} is not in scope there
 * while {@code Binder}'s type parameters are. A parameter type of a method belongs to the method
 * and stands inside the type's body.
 *
 * @param name the name as written, type arguments and annotations removed
 * @param role the clause it is written in
 * @param span where it is written
 * @param owner the innermost declaration whose header contains it; {@code null} only for an
 *     annotation on the package declaration
 * @param scopeType the innermost type whose body contains it, or {@code null} when it is written
 *     outside every body — in a top-level type's header
 */
public record IndexedReference(
        String name, TypeReferenceRole role, SourceSpan span, SymbolId owner, SymbolId scopeType) {

    /**
     * Returns the first segment: the part of the name that has to be found in scope before anything
     * else.
     *
     * @return {@code Map} for {@code Map.Entry}
     */
    public String firstSegment() {
        int dot = name.indexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    /**
     * Returns the last segment: what the name calls the type.
     *
     * @return {@code Entry} for {@code Map.Entry}
     */
    public String simpleName() {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }
}
