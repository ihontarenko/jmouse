package org.jmouse.java.parser.model;

import java.util.List;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * The module declaration of a {@code module-info.java}. Its directives are not parsed: the body is
 * kept as an opaque span.
 *
 * @param name        the dotted module name
 * @param open        whether it is declared {@code open}
 * @param annotations annotations, in order
 * @param body        the directives block from its opening to its closing brace
 * @param span        the whole declaration
 * @param javaDoc     the documentation comment, or {@code null}
 */
public record ModuleDeclaration(String name, boolean open, List<Annotation> annotations, SourceSpan body, SourceSpan span,
                                JavaDoc javaDoc) {

    /**
     * Copies the list.
     */
    public ModuleDeclaration {
        annotations = List.copyOf(annotations);
    }
}
