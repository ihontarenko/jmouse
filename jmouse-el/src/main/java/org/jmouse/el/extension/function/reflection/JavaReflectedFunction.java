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
 * A {@link Function} implementation that invokes one or more Java methods reflectively.
 * <p>
 * This class holds a list of {@link Method} instances under a common name and, when executed,
 * selects the appropriate method based on the runtime argument types. It then invokes the
 * matching method reflectively and returns its result.
 * </p>
 */
public class JavaReflectedFunction implements Function {

    /**
     * The name of the function as exposed to the template language.
     */
    private final String name;

    /**
     * The list of Java methods that implement this function under different signatures.
     */
    private final List<Method> methods;

    /**
     * Constructs a new JavaReflectedFunction.
     *
     * @param name    the name by which this function is referenced in templates
     * @param methods the list of {@link Method} instances that can be invoked
     */
    public JavaReflectedFunction(String name, List<Method> methods) {
        this.name = name;
        this.methods = methods;
    }

    /**
     * Executes the function by matching the runtime argument types against the available methods.
     * <p>
     * The runtime classes of the provided {@link Arguments} are used to select a matching method
     * via {@link MethodMatchers#hasSoftParameterTypes(Class[]) MethodMatchers.hasSoftParameterTypes}.
     * If no matching method is found, a {@link FunctionNotFoundException} is thrown.
     * Otherwise, the selected method is invoked reflectively using {@link Reflections#invokeMethod}.
     * </p>
     *
     * @param arguments the arguments passed from the template, in evaluation order
     * @param context   the evaluation context (provides conversion, scope, etc.)
     * @return the result of the reflective method invocation
     * @throws FunctionNotFoundException if no matching method signature is found
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
                    "Function %s%s not found".formatted(name, Arrays.toString(types)
                            .replace('[', '(').replace(']', ')'))
            );
        }

        // Invoke the matched method reflectively (static invocation if target is null)
        return Reflections.invokeMethod(null, method, arguments.toList().toArray(Object[]::new));
    }

    /**
     * Returns the name of this function as used in template expressions.
     *
     * @return the function name
     */
    @Override
    public String getName() {
        return name;
    }
}
