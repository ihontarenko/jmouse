package org.jmouse.java.parser.model;

import java.util.List;
import java.util.Set;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A formal parameter of a method or constructor.
 *
 * @param name          the parameter name; {@code this} for a receiver parameter
 * @param type          the type as written, without the {@code ...} of a variable-arity parameter
 * @param annotations   annotations written before the type
 * @param modifiers     {@code final}, when written
 * @param variableArity whether the parameter is declared with {@code ...}
 * @param receiver      whether this is an explicit receiver parameter ({@code Type this})
 * @param span          the whole parameter
 */
public record Parameter(String name, String type, List<Annotation> annotations, Set<Modifier> modifiers,
                        boolean variableArity, boolean receiver, SourceSpan span) {

    /**
     * Copies the collections.
     */
    public Parameter {
        annotations = List.copyOf(annotations);
        modifiers = Set.copyOf(modifiers);
    }
}
