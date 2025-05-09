package org.jmouse.el.extension.calculator.operation;

public interface OperationHandler<X, Y> {

    boolean supports(OperationType type, Class<?> x, Class<?> y);

    Object execute(OperationType type, X x, Y y);

}
