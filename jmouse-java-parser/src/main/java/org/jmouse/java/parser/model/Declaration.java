package org.jmouse.java.parser.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A named element declared in a type or at the top of a file.
 *
 * <p>The hierarchy is closed so a consumer can switch over it exhaustively. Every declaration has:
 * <ul>
 *     <li>a {@link SymbolId stable id};</li>
 *     <li>a {@link #span() span} from its first annotation or modifier (or its first keyword when it has
 *     neither) to its closing brace or semicolon — it never includes the documentation comment, whose
 *     span is separate;</li>
 *     <li>a {@link #signature() signature}: the header text as written, from the span start to just
 *     before the body, the {@code ;}, the initializer or the {@code default} value — with each run of
 *     whitespace collapsed to one space. A field's signature is its declaration header followed by the
 *     variable name.</li>
 * </ul>
 */
public sealed interface Declaration
        permits TypeDeclaration, FieldDeclaration, MethodDeclaration, ConstructorDeclaration,
                EnumConstantDeclaration, InitializerDeclaration, RecordComponentDeclaration {

    /**
     * Returns the stable id.
     *
     * @return the id
     */
    SymbolId symbolId();

    /**
     * Returns what is declared.
     *
     * @return the kind
     */
    DeclarationKind kind();

    /**
     * Returns the simple name.
     *
     * @return the name; for initializers {@code <initializer>} or {@code <static-initializer>}
     */
    String name();

    /**
     * Returns the dotted qualified name, for example {@code org.example.Outer.Inner.method}.
     *
     * @return the qualified name
     */
    String qualifiedName();

    /**
     * Returns the modifiers written on the declaration.
     *
     * @return the modifiers; empty when none
     */
    Set<Modifier> modifiers();

    /**
     * Returns the annotations written on the declaration, in order.
     *
     * @return the annotations
     */
    List<Annotation> annotations();

    /**
     * Returns the header as written, whitespace collapsed.
     *
     * @return the signature
     */
    String signature();

    /**
     * Returns the exact span of the header the signature was taken from.
     *
     * @return the signature span
     */
    SourceSpan signatureSpan();

    /**
     * Returns the whole declaration, documentation comment excluded.
     *
     * @return the span
     */
    SourceSpan span();

    /**
     * Returns the documentation comment.
     *
     * @return the comment, or {@code null} when the declaration has none
     */
    JavaDoc javaDoc();

    /**
     * Returns the documentation comment, if any.
     *
     * @return the comment
     */
    default Optional<JavaDoc> findJavaDoc() {
        return Optional.ofNullable(javaDoc());
    }

    /**
     * Returns where the declaration begins including its documentation comment.
     *
     * @return the comment's start when there is one, otherwise the span's start offset
     */
    default int documentedStartOffset() {
        JavaDoc documentation = javaDoc();
        if (documentation != null && documentation.span().start().offset() < span().start().offset()) {
            return documentation.span().start().offset();
        }
        return span().start().offset();
    }
}
