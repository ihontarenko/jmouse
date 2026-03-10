package org.jmouse.el.extension.function;

import org.jmouse.core.Verify;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;

/**
 * EL function that resolves a Java {@link Class} by its fully qualified name. 🧩
 *
 * <p>
 * This function attempts to load a class using {@link Class#forName(String)}.
 * If the class cannot be found, {@code null} is returned.
 * </p>
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * class('java.lang.String')
 * class('java.util.ArrayList')
 * }</pre>
 *
 * <p>
 * This is typically used in EL expressions where a {@link Class} reference
 * is required for reflection-based operations.
 * </p>
 */
public class ClassFunction implements Function {

    /**
     * Resolves a {@link Class} instance from the provided class name.
     *
     * @param arguments function arguments (first argument must be class name)
     * @param context   evaluation context
     * @return resolved {@link Class} or {@code null} if not found
     */
    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        try {
            String classType = Verify.notBlank(String.valueOf(arguments.getFirst()), "class value");
            return Class.forName(classType, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException ignored) { }
        return null;
    }

    /**
     * Returns the EL function name.
     *
     * @return function name {@code "class"}
     */
    @Override
    public String getName() {
        return "class";
    }
}