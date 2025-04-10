package org.jmouse.el.extension;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;

/**
 * Defines a test extension used to evaluate conditions or expressions within the
 * expression language framework.
 */
public interface Test {

    /**
     * Evaluates a condition or test on the given object instance.
     *
     * @param value     the object instance to test
     * @param arguments the arguments to use during the test evaluation
     * @param context   the evaluation context providing additional evaluation services
     * @param type
     * @return {@code true} if the test passes; {@code false} otherwise
     */
    boolean test(Object value, Arguments arguments, EvaluationContext context, ClassTypeInspector type);

    /**
     * Returns the name of this test extension.
     *
     * @return the name of the test extension
     */
    String getName();
}
