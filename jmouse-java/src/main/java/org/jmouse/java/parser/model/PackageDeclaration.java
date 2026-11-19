package org.jmouse.java.parser.model;

import java.util.List;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * The {@code package} declaration of a file.
 *
 * @param name        the dotted package name
 * @param annotations package annotations, as written in {@code package-info.java}
 * @param span        from the first annotation or the keyword to the semicolon
 * @param javaDoc     the package documentation comment, or {@code null}
 */
public record PackageDeclaration(String name, List<Annotation> annotations, SourceSpan span, JavaDoc javaDoc) {

    /**
     * Copies the list.
     */
    public PackageDeclaration {
        annotations = List.copyOf(annotations);
    }
}
