package org.jmouse.java.index;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Java invocation conversions and built-in types, including a conservative fallback without a
 * classpath.
 */
final class JavaTypeRules {
    static final Set<String> PRIMITIVES =
            Set.of("boolean", "byte", "short", "char", "int", "long", "float", "double", "void");
    static final Set<String> JAVA_LANG =
            Set.of(
                    "String",
                    "Object",
                    "Number",
                    "Boolean",
                    "Byte",
                    "Short",
                    "Character",
                    "Integer",
                    "Long",
                    "Float",
                    "Double",
                    "Void",
                    "Class",
                    "Throwable",
                    "Exception",
                    "RuntimeException");
    static final Map<String, String> BOXES =
            Map.of(
                    "boolean",
                    "java.lang.Boolean",
                    "byte",
                    "java.lang.Byte",
                    "short",
                    "java.lang.Short",
                    "char",
                    "java.lang.Character",
                    "int",
                    "java.lang.Integer",
                    "long",
                    "java.lang.Long",
                    "float",
                    "java.lang.Float",
                    "double",
                    "java.lang.Double");

    private static final int MAXIMUM_HIERARCHY_TYPES = 128;
    private final SymbolTable symbols;

    JavaTypeRules(SymbolTable symbols) {
        this.symbols = symbols;
    }

    int conversion(String argument, String parameter) {
        if (argument == null || parameter == null) {
            return 3;
        }
        if (argument.equals(parameter)) {
            return 0;
        }
        if (argument.equals("null")) {
            return PRIMITIVES.contains(parameter) ? -1 : 1;
        }
        if (PRIMITIVES.contains(argument) && PRIMITIVES.contains(parameter)) {
            return primitiveWidening(argument, parameter) ? 1 : -1;
        }
        if (PRIMITIVES.contains(argument)) {
            String boxed = BOXES.get(argument);
            return boxed != null
                            && (boxed.equals(parameter)
                                    || parameter.equals("java.lang.Object")
                                    || (!argument.equals("boolean")
                                            && !argument.equals("char")
                                            && parameter.equals("java.lang.Number")))
                    ? 2
                    : -1;
        }
        if (PRIMITIVES.contains(parameter)) {
            String unboxed =
                    BOXES.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(argument))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null);
            return unboxed != null
                            && (unboxed.equals(parameter) || primitiveWidening(unboxed, parameter))
                    ? 2
                    : -1;
        }
        if (parameter.equals("java.lang.Object") || subtype(argument, parameter, new HashSet<>())) {
            return 1;
        }
        if (parameter.equals("java.lang.Number")
                && BOXES.containsValue(argument)
                && !Set.of("java.lang.Boolean", "java.lang.Character").contains(argument)) {
            return 1;
        }
        if (Set.of(
                        "java.lang.String",
                        "java.lang.Boolean",
                        "java.lang.Character",
                        "java.lang.Byte",
                        "java.lang.Short",
                        "java.lang.Integer",
                        "java.lang.Long",
                        "java.lang.Float",
                        "java.lang.Double")
                .contains(parameter)) {
            return -1;
        }
        if (argument.endsWith("[]") && parameter.endsWith("[]")) {
            String component = argument.substring(0, argument.length() - 2);
            String target = parameter.substring(0, parameter.length() - 2);
            return !PRIMITIVES.contains(component) && conversion(component, target) == 1 ? 1 : -1;
        }
        // An absent classpath may hide a legal supertype conversion. Preserve uncertainty.
        return symbols.findTypes(argument).isEmpty() || symbols.findTypes(parameter).isEmpty()
                ? 3
                : -1;
    }

    private static boolean primitiveWidening(String from, String to) {
        return switch (from) {
            case "byte" -> Set.of("short", "int", "long", "float", "double").contains(to);
            case "short", "char" -> Set.of("int", "long", "float", "double").contains(to);
            case "int" -> Set.of("long", "float", "double").contains(to);
            case "long" -> Set.of("float", "double").contains(to);
            case "float" -> to.equals("double");
            default -> false;
        };
    }

    boolean subtype(String from, String to, Set<TypeSymbol> visited) {
        if (from.equals(to)) {
            return true;
        }
        for (TypeSymbol type : symbols.findTypes(from)) {
            if (!visited.add(type) || visited.size() > MAXIMUM_HIERARCHY_TYPES) {
                continue;
            }
            for (TypeSymbol parent : symbols.supertypes(type)) {
                if (subtype(parent.qualifiedName(), to, visited)) {
                    return true;
                }
            }
        }
        return false;
    }
}
