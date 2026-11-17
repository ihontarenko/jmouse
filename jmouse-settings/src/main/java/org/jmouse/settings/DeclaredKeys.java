package org.jmouse.settings;

import org.jmouse.core.parameters.RequestParametersPathToken;
import org.jmouse.core.parameters.RequestParametersPathToken.ArrayIndexToken;
import org.jmouse.core.parameters.RequestParametersPathToken.PropertyNameToken;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Whether a section still declares a key a row is kept under. A later release may take a parameter away; the rows kept of it then
 * name nothing, and a section read with one would be refused whole — so a row a section no longer declares is forgotten, never read.
 *
 * <p>A key is declared where its path runs through the section's shape: a fixed section's by one of its parameters; a structured
 * section's by its record — a component by name, a list or an array by any place in it, a map by any key —; an open section's,
 * whose keys the library does not know, always.</p>
 */
final class DeclaredKeys {

    private DeclaredKeys() {
    }

    static boolean declares(Section section, String key) {
        return switch (section.shape()) {
            case Section.Fixed fixed -> fixed.parameters().stream().anyMatch(parameter -> parameter.key().equals(key));
            case Section.Structured structured -> runsThrough(structured.type(), ParameterTree.pathOf(key), 0);
            case Section.Open ignored -> true;
        };
    }

    /** Whether the rest of a path, from a place in it, runs through a type. */
    private static boolean runsThrough(Type type, List<RequestParametersPathToken> path, int at) {
        if (at == path.size()) {
            return true;
        }
        RequestParametersPathToken token = path.get(at);
        Class<?> raw = rawOf(type);
        if (raw == Object.class) {
            // A type nothing is told of: what runs below it is not the library's to refuse.
            return true;
        }
        if (raw == Optional.class) {
            return runsThrough(argumentOf(type, 0), path, at);
        }
        if (raw.isArray() || type instanceof GenericArrayType) {
            Type element = type instanceof GenericArrayType generic ? generic.getGenericComponentType() : raw.getComponentType();
            return token instanceof ArrayIndexToken && runsThrough(element, path, at + 1);
        }
        if (Collection.class.isAssignableFrom(raw)) {
            return token instanceof ArrayIndexToken && runsThrough(argumentOf(type, 0), path, at + 1);
        }
        if (Map.class.isAssignableFrom(raw)) {
            return token instanceof PropertyNameToken && runsThrough(argumentOf(type, 1), path, at + 1);
        }
        if (raw.isRecord()) {
            if (!(token instanceof PropertyNameToken name)) {
                return false;
            }
            for (RecordComponent component : raw.getRecordComponents()) {
                if (component.getName().equals(name.propertyName())) {
                    return runsThrough(component.getGenericType(), path, at + 1);
                }
            }
            return false;
        }
        // A value — a number, a word, an enum's constant —: nothing runs on below it.
        return false;
    }

    private static Class<?> rawOf(Type type) {
        if (type instanceof Class<?> raw) {
            return raw;
        }
        if (type instanceof ParameterizedType parameterized) {
            return (Class<?>) parameterized.getRawType();
        }
        if (type instanceof GenericArrayType generic) {
            return java.lang.reflect.Array.newInstance(rawOf(generic.getGenericComponentType()), 0).getClass();
        }
        return Object.class;
    }

    /** A type's argument at a place — a list's element, a map's value —; anything where it is not told. */
    private static Type argumentOf(Type type, int place) {
        if (type instanceof ParameterizedType parameterized && parameterized.getActualTypeArguments().length > place) {
            return parameterized.getActualTypeArguments()[place];
        }
        return Object.class;
    }
}
