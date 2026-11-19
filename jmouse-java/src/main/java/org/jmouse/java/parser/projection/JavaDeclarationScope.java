package org.jmouse.java.parser.projection;

import org.jmouse.java.parser.node.declaration.JavaInitializerKind;
import org.jmouse.java.parser.node.declaration.JavaRecordComponentNode;
import org.jmouse.java.parser.node.parameter.JavaParameterKind;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class JavaDeclarationScope {
    private final String qualifiedName;
    private final List<JavaRecordComponentNode> components;
    private final Map<JavaInitializerKind, Integer> initializers =
            new EnumMap<>(JavaInitializerKind.class);

    public JavaDeclarationScope(String qualifiedName, List<JavaRecordComponentNode> components) {
        this.qualifiedName = qualifiedName;
        this.components = List.copyOf(components);
    }

    public String qualifiedName() {
        return qualifiedName;
    }

    public List<String> canonicalParameters() {
        return components.stream()
                .map(
                        component ->
                                JavaSourceSpelling.identity(component.type())
                                        + (component.kind() == JavaParameterKind.VARIABLE_ARITY
                                                ? "..."
                                                : ""))
                .toList();
    }

    public int nextInitializer(JavaInitializerKind kind) {
        return initializers.merge(kind, 1, Integer::sum);
    }
}
