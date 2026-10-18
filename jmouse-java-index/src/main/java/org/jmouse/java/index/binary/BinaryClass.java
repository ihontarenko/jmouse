package org.jmouse.java.index.binary;

import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.Modifier;

import java.util.List;
import java.util.Set;

/**
 * A class file's declaration surface. Code attributes and implementation details are never read.
 */
record BinaryClass(
        String binaryName,
        DeclarationKind kind,
        Set<Modifier> modifiers,
        List<String> typeParameters,
        String superclass,
        List<String> interfaces,
        List<Field> fields,
        List<Method> methods,
        List<Component> components) {

    BinaryClass {
        modifiers = Set.copyOf(modifiers);
        typeParameters = List.copyOf(typeParameters);
        interfaces = List.copyOf(interfaces);
        fields = List.copyOf(fields);
        methods = List.copyOf(methods);
        components = List.copyOf(components);
    }

    record Field(String name, String type, Set<Modifier> modifiers, boolean enumConstant) {
        Field {
            modifiers = Set.copyOf(modifiers);
        }
    }

    record Method(
            String name,
            String returnType,
            List<String> parameters,
            List<String> typeParameters,
            List<String> thrownTypes,
            Set<Modifier> modifiers,
            boolean variableArity) {
        Method {
            parameters = List.copyOf(parameters);
            typeParameters = List.copyOf(typeParameters);
            thrownTypes = List.copyOf(thrownTypes);
            modifiers = Set.copyOf(modifiers);
        }
    }

    record Component(String name, String type) {}
}
