package org.jmouse.el.extension.calculator.operation;

public class CalculationOperationException extends RuntimeException {

    public CalculationOperationException(String message) {
        super(message);
    }

    public CalculationOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
