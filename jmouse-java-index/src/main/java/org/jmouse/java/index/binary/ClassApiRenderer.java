package org.jmouse.java.index.binary;

import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Produces a read-only Java API view with real name positions for navigation. */
final class ClassApiRenderer {

    private static final String CONSTRUCTOR_NAME = "<init>";

    String render(BinaryClass type) {
        StringBuilder source =
                new StringBuilder("// Read-only API reconstructed from a class file.\n");
        int separator = type.binaryName().lastIndexOf('/');
        String packageName =
                separator < 0 ? "" : type.binaryName().substring(0, separator).replace('/', '.');
        String[] nesting = type.binaryName().substring(separator + 1).split("\\$");

        if (!packageName.isEmpty()) {
            source.append("package ").append(packageName).append(";\n\n");
        }
        for (int index = 0; index < nesting.length - 1; index++) {
            source.append("class ").append(nesting[index]).append(" {\n");
        }

        String name = nesting[nesting.length - 1];
        appendHeader(source, type, name);
        appendFields(source, type);
        appendMethods(source, type, name);
        source.append("}\n");

        for (int index = 1; index < nesting.length; index++) {
            source.append("}\n");
        }
        return source.toString();
    }

    private void appendHeader(StringBuilder source, BinaryClass type, String name) {
        Set<Modifier> redundant =
                type.kind() == DeclarationKind.CLASS
                        ? Set.of()
                        : Set.of(Modifier.ABSTRACT, Modifier.FINAL);
        appendModifiers(source, type.modifiers(), redundant);
        source.append(
                        switch (type.kind()) {
                            case INTERFACE -> "interface ";
                            case ANNOTATION_TYPE -> "@interface ";
                            case ENUM -> "enum ";
                            case RECORD -> "record ";
                            default -> "class ";
                        })
                .append(name);
        appendVariables(source, type.typeParameters());

        if (type.kind() == DeclarationKind.RECORD) {
            source.append('(')
                    .append(
                            String.join(
                                    ", ",
                                    type.components().stream()
                                            .map(
                                                    component ->
                                                            component.type()
                                                                    + " "
                                                                    + component.name())
                                            .toList()))
                    .append(')');
        }
        if (type.kind() == DeclarationKind.CLASS && type.superclass() != null) {
            source.append(" extends ").append(type.superclass());
        }
        if (!type.interfaces().isEmpty() && type.kind() != DeclarationKind.ANNOTATION_TYPE) {
            source.append(type.kind() == DeclarationKind.INTERFACE ? " extends " : " implements ")
                    .append(String.join(", ", type.interfaces()));
        }
        source.append(" {\n");
    }

    private void appendFields(StringBuilder source, BinaryClass type) {
        if (type.kind() == DeclarationKind.ENUM) {
            source.append("    ")
                    .append(
                            String.join(
                                    ", ",
                                    type.fields().stream()
                                            .filter(BinaryClass.Field::enumConstant)
                                            .map(BinaryClass.Field::name)
                                            .toList()))
                    .append(";\n");
        }
        for (BinaryClass.Field field : type.fields()) {
            boolean component =
                    type.components().stream()
                            .anyMatch(
                                    recordComponent -> recordComponent.name().equals(field.name()));
            if (!field.enumConstant() && !component) {
                source.append("    ");
                appendModifiers(source, field.modifiers(), Set.of());
                source.append(field.type()).append(' ').append(field.name()).append(";\n");
            }
        }
    }

    private void appendMethods(StringBuilder source, BinaryClass type, String name) {
        for (BinaryClass.Method method : type.methods()) {
            source.append("    ");
            appendModifiers(source, method.modifiers(), Set.of());
            appendVariables(source, method.typeParameters());
            if (!method.typeParameters().isEmpty()) {
                source.append(' ');
            }
            boolean constructor = method.name().equals(CONSTRUCTOR_NAME);
            if (!constructor) {
                source.append(method.returnType()).append(' ');
            }
            source.append(constructor ? name : method.name()).append('(');
            List<String> parameters = new ArrayList<>();
            for (int index = 0; index < method.parameters().size(); index++) {
                String parameter = method.parameters().get(index);
                if (method.variableArity()
                        && index == method.parameters().size() - 1
                        && parameter.endsWith("[]")) {
                    parameter = parameter.substring(0, parameter.length() - 2) + "...";
                }
                parameters.add(parameter + " argument" + (index + 1));
            }
            source.append(String.join(", ", parameters)).append(')');
            if (!method.thrownTypes().isEmpty()) {
                source.append(" throws ").append(String.join(", ", method.thrownTypes()));
            }
            source.append(constructor ? " {}\n" : ";\n");
        }
    }

    private void appendVariables(StringBuilder source, List<String> variables) {
        if (!variables.isEmpty()) {
            source.append('<').append(String.join(", ", variables)).append('>');
        }
    }

    private void appendModifiers(
            StringBuilder source, Set<Modifier> modifiers, Set<Modifier> excluded) {
        Arrays.stream(Modifier.values())
                .filter(modifiers::contains)
                .filter(modifier -> !excluded.contains(modifier))
                .forEach(
                        modifier ->
                                source.append(modifier.name().toLowerCase(Locale.ROOT))
                                        .append(' '));
    }
}
