package org.jmouse.java.index;

import org.jmouse.java.parser.model.ImportDeclaration;
import org.jmouse.java.parser.model.SymbolId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Decides what a type name written in a file denotes, by the Java scoping rules, against a {@link
 * SymbolTable}.
 *
 * <p>A simple name is looked up innermost first, and the first scope that knows it wins:
 *
 * <ol>
 *   <li>type variables of the declaring method or constructor;
 *   <li>for each enclosing type, outward: the types nested in it, then its own type variables;
 *   <li>single-type imports;
 *   <li>the file's own package;
 *   <li>on-demand imports, then {@code java.lang} — every match counts, and two are ambiguous.
 * </ol>
 *
 * A qualified name {@code A.B.C} first tries {@code A} as a simple name and looks {@code B.C} up
 * inside what it found; failing that, the whole name is taken as canonical.
 *
 * <p>Not yet followed: member types inherited from supertypes, and the shadowing a local or
 * anonymous class would introduce inside a body — bodies are not read at all at this level.
 */
public final class TypeResolver {

    private static final String JAVA_LANG = "java.lang";

    private final SymbolTable symbols;

    /**
     * Creates a resolver.
     *
     * @param symbols where types are looked up
     */
    public TypeResolver(SymbolTable symbols) {
        this.symbols = symbols;
    }

    /**
     * Resolves one reference the file writes.
     *
     * @param file the file the reference is written in
     * @param reference one of that file's references
     * @return what the name denotes
     */
    public Resolution resolve(FileIndex file, IndexedReference reference) {
        String name = reference.name();
        String first = reference.firstSegment();
        Resolution head = resolveSimpleName(file, reference, first);
        if (first.equals(name)) {
            return head;
        }
        String remainder = name.substring(first.length());
        if (head instanceof Resolution.Resolved resolved) {
            return byCanonicalName(resolved.target().qualifiedName() + remainder, name);
        }
        return byCanonicalName(name, name);
    }

    private Resolution resolveSimpleName(FileIndex file, IndexedReference reference, String name) {
        Optional<Resolution> scoped = resolveInScope(file, reference, name);
        if (scoped.isPresent()) {
            return scoped.get();
        }
        for (ImportDeclaration declaration : file.imports()) {
            if (!declaration.onDemand() && lastSegment(declaration.name()).equals(name)) {
                return byCanonicalName(declaration.name(), name);
            }
        }
        List<TypeSymbol> inPackage = symbols.findTypes(qualify(file.packageName(), name));
        if (!inPackage.isEmpty()) {
            return of(inPackage, name, null);
        }
        Map<TypeSymbol, TypeSymbol> onDemand = new LinkedHashMap<>();
        for (ImportDeclaration declaration : file.imports()) {
            if (declaration.onDemand()) {
                symbols.findTypes(declaration.name() + "." + name)
                        .forEach(type -> onDemand.putIfAbsent(type, type));
            }
        }
        symbols.findTypes(JAVA_LANG + "." + name).forEach(type -> onDemand.putIfAbsent(type, type));
        return of(new ArrayList<>(onDemand.values()), name, null);
    }

    /**
     * Type variables and nested types, from the reference outward; empty when no enclosing scope
     * declares the name.
     */
    private Optional<Resolution> resolveInScope(
            FileIndex file, IndexedReference reference, String name) {
        Optional<IndexedDeclaration> owner =
                Optional.ofNullable(reference.owner()).flatMap(file::declaration);
        // A method's variables, or — for a reference in a type's own header — that type's
        // variables.
        if (owner.isPresent() && owner.get().typeParameters().contains(name)) {
            return Optional.of(new Resolution.TypeVariable(name, owner.get().symbolId()));
        }
        SymbolId scope = reference.scopeType();
        while (scope != null) {
            IndexedDeclaration type = file.declaration(scope).orElse(null);
            if (type == null) {
                break;
            }
            List<TypeSymbol> nestedTypes = symbols.findTypes(type.qualifiedName() + "." + name);
            if (!nestedTypes.isEmpty()) {
                return Optional.of(of(nestedTypes, name, null));
            }
            for (IndexedDeclaration nested : file.declarations()) {
                if (nested.isType()
                        && type.symbolId().equals(nested.container())
                        && nested.name().equals(name)) {
                    return Optional.of(byCanonicalName(nested.qualifiedName(), name));
                }
            }
            if (type.typeParameters().contains(name)) {
                return Optional.of(new Resolution.TypeVariable(name, type.symbolId()));
            }
            scope = type.container();
        }
        return Optional.empty();
    }

    private Resolution byCanonicalName(String qualifiedName, String writtenName) {
        return of(symbols.findTypes(qualifiedName), writtenName, qualifiedName);
    }

    private static Resolution of(
            List<TypeSymbol> found, String writtenName, String presumedQualifiedName) {
        if (found.isEmpty()) {
            return new Resolution.Unresolved(writtenName, presumedQualifiedName);
        }
        if (found.size() == 1) {
            return new Resolution.Resolved(found.getFirst());
        }
        return new Resolution.Ambiguous(found);
    }

    private static String qualify(String packageName, String name) {
        return packageName.isEmpty() ? name : packageName + "." + name;
    }

    private static String lastSegment(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
