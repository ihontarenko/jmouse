package org.jmouse.el.core.extension;

/**
 * 🎛️ Functional interface for evaluating expressions with operators.
 *
 * @param <R> 🔢 The return type of the calculation
 * @author Ivan Hontarenko
 */
@FunctionalInterface
public interface Calculator<R> {

    /**
     * 🏗️ Performs a calculation based on the given operator.
     *
     * @param operands 📦 The operands for the operation
     * @return 🔢 The result of the calculation
     */
    R calculate(Object... operands);

}
