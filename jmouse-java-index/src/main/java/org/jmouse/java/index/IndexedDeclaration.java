package org.jmouse.java.index;

import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.Modifier;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.List;
import java.util.Set;

/**
 * One declaration of a file, flattened out of the parser's tree: a type at any nesting depth, or a
 * member.
 *
 * <p>The tree is kept as a link — {@link #container()} — rather than as nesting, so a file's
 * declarations are one list that can be stored, scanned and searched without walking anything.
 *
 * @param symbolId the parser's stable id
 * @param kind what is declared
 * @param name the simple name
 * @param qualifiedName the dotted qualified name; for a type its canonical name, for a member its
 *     type's plus its own
 * @param container the type this is declared in, or {@code null} for a top-level type
 * @param typeParameters the names of the type variables it declares, in order
 * @param modifiers the modifiers written on it
 * @param signature its header as written, whitespace collapsed
 * @param nameSpan where its name is written
 * @param bodySpan a type's body from brace to brace; {@code null} for a member
 * @param span the whole declaration
 */
public record IndexedDeclaration(
        SymbolId symbolId,
        DeclarationKind kind,
        String name,
        String qualifiedName,
        SymbolId container,
        List<String> typeParameters,
        Set<Modifier> modifiers,
        String signature,
        SourceSpan nameSpan,
        SourceSpan bodySpan,
        SourceSpan span) {

    /** Copies the collections. */
    public IndexedDeclaration {
        typeParameters = List.copyOf(typeParameters);
        modifiers = Set.copyOf(modifiers);
    }

    /**
     * Returns whether this declares a type.
     *
     * @return {@code true} for a class, interface, enum, record or annotation type
     */
    public boolean isType() {
        return kind.isType();
    }
}
