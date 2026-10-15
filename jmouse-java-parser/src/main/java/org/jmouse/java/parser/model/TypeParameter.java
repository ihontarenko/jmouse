package org.jmouse.java.parser.model;

import java.util.List;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A declared type variable, for example {@code T extends Comparable<T> & Serializable}.
 *
 * @param name        the variable
 * @param bounds      each bound as written, in order; empty when unbounded
 * @param annotations annotations on the variable
 * @param span        the whole parameter
 */
public record TypeParameter(String name, List<String> bounds, List<Annotation> annotations, SourceSpan span) {

    /**
     * Copies the lists.
     */
    public TypeParameter {
        bounds = List.copyOf(bounds);
        annotations = List.copyOf(annotations);
    }
}
