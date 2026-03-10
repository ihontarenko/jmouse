package org.jmouse.el.extension.filter;

import org.jmouse.core.Verify;
import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

/**
 * EL filter that resolves a Java {@link Class} by its name. 🧩
 *
 * <p>
 * The filter expects the first argument to contain a fully qualified
 * class name and attempts to load it using {@link Class#forName(String, boolean, ClassLoader)}.
 * </p>
 *
 * <p>
 * If the class cannot be resolved, {@code null} is returned.
 * </p>
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * 'java.lang.String' | class
 * 'java.util.ArrayList' | class
 * }</pre>
 *
 * <p>
 * This filter is typically used when expressions need to obtain
 * a {@link Class} reference for reflection-based operations.
 * </p>
 */
public class ClassFilter extends AbstractFilter {

    /**
     * Resolves a class from the provided argument.
     *
     * @param input     input value (unused)
     * @param arguments filter arguments (first argument must be class name)
     * @param context   evaluation context
     * @param type      input type classifier
     * @return resolved {@link Class} or {@code null} if resolution fails
     */
    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        try {
            String classType = Verify.notBlank(String.valueOf(input), "class value");
            return Class.forName(classType, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException ignored) { }
        return null;
    }

    /**
     * Returns the filter name used in EL expressions.
     *
     * @return filter name {@code "class"}
     */
    @Override
    public String getName() {
        return "class";
    }
}