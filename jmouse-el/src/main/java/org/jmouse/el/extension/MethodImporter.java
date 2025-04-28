package org.jmouse.el.extension;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.MethodFinder;
import org.jmouse.core.reflection.MethodMatchers;
import org.jmouse.el.extension.function.reflection.JavaReflectedFunction;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * Utility for importing public static methods from a given class as template functions.
 * <p>
 * Scans all public static methods of the specified source class, groups them by method name,
 * and registers each group as a {@link JavaReflectedFunction} in the provided
 * {@link ExtensionContainer}.
 * </p>
 */
public class MethodImporter {

    public static final Matcher<Executable> MATCHER = MethodMatchers.isPublic()
            .and(MethodMatchers.isStatic());

    /**
     * Imports all public static methods of the given source class into the specified extension container.
     * <p>
     * Only methods matching {@link MethodMatchers#isPublic()} and {@link MethodMatchers#isStatic()}
     * are considered. Methods are grouped by name so that overloaded methods become one function
     * with multiple signatures.
     * </p>
     *
     * @param source    the class whose public static methods should be imported as functions
     * @param container the extension container to receive the imported functions
     */
    public static void importMethod(Class<?> source, ExtensionContainer container) {
        // Discover methods, filter by matcher, then group by method name
        Map<String, List<Method>> grouped = new MethodFinder()
                .getMembers(source, false)
                .stream()
                .filter(MATCHER::matches)
                .collect(groupingBy(Method::getName, toList()));

        // Register each group of overloaded methods as a single reflected function
        grouped.forEach((name, methods)
                -> container.addFunction(new JavaReflectedFunction(name, methods)));
    }
}
