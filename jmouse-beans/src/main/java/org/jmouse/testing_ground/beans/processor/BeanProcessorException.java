package org.jmouse.testing_ground.beans.processor;

/**
 * Exception class for handling errors related to {@link BeanPostProcessor}.
 * <p>
 * This exception is thrown when there is an issue during the processing
 * of beans before or after initialization.
 * </p>
 *
 * @see BeanPostProcessor
 */
public class BeanProcessorException extends RuntimeException {

    /**
     * Constructs a new {@code BeanProcessorException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public BeanProcessorException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code BeanProcessorException} with the specified detail message
     * and cause.
     *
     * @param message the detail message.
     * @param cause   the cause of the exception.
     */
    public BeanProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}