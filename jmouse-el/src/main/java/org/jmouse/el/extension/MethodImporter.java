package org.jmouse.el.extension;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.MethodFinder;
import org.jmouse.core.reflection.MethodMatchers;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.function.reflection.JavaReflectedFunction;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * 🧩 Utility for importing public methods as EL view functions.
 *
 * <p>Scans a source class, groups candidate methods by name, and registers each group as a single
 * {@link JavaReflectedFunction} in the {@link ExtensionContainer} obtained from the given
 * {@link EvaluationContext}.</p>
 *
 * <h3>Selection rules</h3>
 * <ul>
 *   <li>🔒 If {@code instance == null} → import <b>public static</b> methods only.</li>
 *   <li>🔓 If {@code instance != null} → import <b>public</b> methods (instance &amp; static).</li>
 * </ul>
 *
 * <p>Overload resolution is deferred to runtime by {@link JavaReflectedFunction}.</p>
 *
 * @apiNote Namespacing is applied via {@link #getNamespaceFunction(String, String)} when registering functions.
 */
public class MethodImporter {

    /**
     * 🎯 Matcher for public methods (instance or static).
     */
    public static final Matcher<Executable> PUBLIC_METHODS = MethodMatchers.isPublic();

    /**
     * 🧱 Matcher for public static methods.
     */
    public static final Matcher<Executable> STATIC_METHODS = PUBLIC_METHODS
            .and(MethodMatchers.isStatic());

    /**
     * 🚀 Import all public static methods from {@code source} into the container of {@code context}
     * under a given namespace.
     *
     * <p>Equivalent to calling
     * {@link #importMethod(Object, Class, String, EvaluationContext)} with a {@code null} instance.</p>
     *
     * @param source    class to scan for public static methods
     * @param namespace optional namespace to qualify function names (may be {@code null} or blank)
     * @param context   evaluation context providing the target {@link ExtensionContainer}
     */
    public static void importMethod(Class<?> source, String namespace, EvaluationContext context) {
        importMethod(null, source, namespace, context);
    }

    /**
     * 🚀 Import all public static methods from {@code source} without a namespace.
     *
     * <p>Shorthand for {@link #importMethod(Class, String, EvaluationContext)} with
     * {@code namespace == null}.</p>
     *
     * @param source  class to scan for public static methods
     * @param context evaluation context providing the target {@link ExtensionContainer}
     */
    public static void importMethod(Class<?> source, EvaluationContext context) {
        importMethod(null, source, null, context);
    }

    /**
     * 🚀 Import public methods from {@code source} using the supplied instance (if any), without a namespace.
     *
     * <p>Shorthand for {@link #importMethod(Object, Class, String, EvaluationContext)} with
     * {@code namespace == null}.</p>
     *
     * @param instance optional target instance used for invoking instance methods; {@code null} for static-only import
     * @param source   class to scan for eligible methods
     * @param context  evaluation context providing the target {@link ExtensionContainer}
     * @param <T>      the source/instance type
     */
    public static <T> void importMethod(T instance, Class<T> source, EvaluationContext context) {
        importMethod(instance, source, null, context);
    }

    /**
     * 🚀 Import public methods from {@code source} into the container of {@code context},
     * optionally qualifying names with a {@code namespace}.
     *
     * <p>Behavior depends on {@code instance}:</p>
     * <ul>
     *   <li>If {@code instance == null} → import public static methods.</li>
     *   <li>If {@code instance != null} → import public methods (instance &amp; static); the instance
     *       will be passed to {@link JavaReflectedFunction} for invocation.</li>
     * </ul>
     *
     * <p>Each group of overloaded methods is registered as a single function; overload selection happens
     * at call time.</p>
     *
     * @param instance  optional target instance used for invoking instance methods; {@code null} for static-only import
     * @param source    class to scan for eligible methods
     * @param namespace optional namespace to qualify function names (may be {@code null} or blank)
     * @param context   evaluation context providing the target {@link ExtensionContainer}
     * @param <T>       the source/instance type
     */
    public static <T> void importMethod(T instance, Class<T> source, String namespace, EvaluationContext context) {
        // Discover methods, filter by matcher, then group by method name
        Matcher<Executable>       matcher = (instance == null) ? STATIC_METHODS : PUBLIC_METHODS;
        Map<String, List<Method>> grouped = new MethodFinder()
                .getMembers(source, false).stream()
                .filter(matcher::matches)
                .collect(groupingBy(Method::getName, toList()));

        ExtensionContainer container = context.getExtensions();

        // Register each group of overloaded methods as a single reflected function
        grouped.forEach((name, methods) ->
                container.addFunction(new JavaReflectedFunction(instance, getNamespaceFunction(name, namespace), methods)));
    }

    /**
     * 🏷️ Compose a namespaced function name.
     *
     * <p>Returns either the original {@code name} (when {@code ns} is {@code null}/blank),
     * or a namespaced variant using a colon separator.</p>
     *
     * <p><b>Current format:</b> {@code ns:name} (prefix namespace).</p>
     *
     * @param name original function name (must not be {@code null})
     * @param ns   optional namespace (may be {@code null} or blank)
     * @return composed function identifier
     */
    public static String getNamespaceFunction(String name, String ns) {
        String function = name;

        if (ns != null && !ns.isBlank()) {
            function = ns + ":" + function;
        }

        return function;
    }
}
