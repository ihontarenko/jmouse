package org.jmouse.el.extension.function.reflection;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.MethodMatchers;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;
import org.jmouse.el.node.expression.FunctionNotFoundException;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 🔭 Reflectively-backed EL function (overload-aware).
 *
 * <p>Holds multiple Java {@link Method}s under a single function name and picks the best
 * match at runtime using soft parameter-type matching (boxing, assignability, etc.).</p>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>🧩 Overloads are resolved via {@link MethodMatchers#hasSoftParameterTypes(Class[])}.</li>
 *   <li>⚙️ Invokes instance or static methods depending on whether {@code instance} is provided.</li>
 *   <li>🚫 {@code null} arguments cannot be typed and will not match without additional typing hints.</li>
 * </ul>
 */
public class JavaReflectedFunction implements Function {

    /**
     * 🏷️ Function name as exposed to the expression language.
     */
    private final String name;

    /**
     * 🔁 Candidate Java methods implementing this function (possibly overloaded).
     */
    private final List<Method> methods;

    /**
     * 🧱 Target instance for invocation (nullable).
     * <p>If {@code null}, matched methods are invoked as static.</p>
     */
    private final Object instance;

    /**
     * 🏗️ Creates a new reflective function wrapper.
     *
     * @param instance target instance for invocation; {@code null} for static methods
     * @param name     the function name referenced in expressions
     * @param methods  candidate Java methods (overloads allowed)
     */
    public JavaReflectedFunction(Object instance, String name, List<Method> methods) {
        this.instance = instance;
        this.name = name;
        this.methods = methods;
    }

    /**
     * 🚀 Executes by selecting the first method whose parameters softly match runtime argument types.
     *
     * <p>Runtime classes of {@link Arguments} are collected and checked against candidates using
     * {@link MethodMatchers#hasSoftParameterTypes(Class[])}. On success, the method is invoked via
     * {@link Reflections#invokeMethod(Object, Method, Object...)} and its result is returned.</p>
     *
     * @param arguments arguments from the expression (evaluation order)
     * @param context   evaluation context (conversion, scopes, etc.)
     * @return the reflective invocation result (may be {@code null})
     * @throws FunctionNotFoundException if no overload matches the runtime types
     */
    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        // Determine runtime types of the provided arguments
        Class<?>[] types = arguments.map(Object::getClass)
                .stream().toArray(Class[]::new);

        // Find a matching method by soft parameter-type matching
        Matcher<Executable> matcher = MethodMatchers.hasSoftParameterTypes(types);
        Method              method  = null;

        for (Method candidate : methods) {
            if (matcher.matches(candidate)) {
                method = candidate;
                break;
            }
        }

        if (method == null) {
            throw new FunctionNotFoundException(
                    "Function %s%s not found".formatted(
                            name, Arrays.toString(types).replace('[', '(').replace(']', ')')
                    )
            );
        }

        // Invoke the matched method reflectively (static invocation if target is null)
        return Reflections.invokeMethod(instance, method, arguments.toList().toArray(Object[]::new));
    }

    /**
     * 🏷️ Returns the function's public name (as used in expressions).
     *
     * @return function name
     */
    @Override
    public String getName() {
        return name;
    }
}
