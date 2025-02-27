package org.jmouse.beans.definition;

/**
 * Exception thrown to indicate an error related to a {@link BeanDefinition}.
 * <p>
 * This exception is typically used when there is an issue with defining, creating, or validating
 * structured definitions in a container or application context.
 * </p>
 * <p>Common scenarios for this exception include:</p>
 * <ul>
 *     <li>Attempting to create a structured definition for an unsupported type.</li>
 *     <li>Conflicting or invalid annotations on a structured class or method.</li>
 *     <li>Failure during dependency resolution or structured creation.</li>
 * </ul>
 */
public class BeanDefinitionException extends RuntimeException {

    /**
     * Constructs a new {@code BeanDefinitionException} with no detail message.
     */
    public BeanDefinitionException() {
        super();
    }

    /**
     * Constructs a new {@code BeanDefinitionException} with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception.
     */
    public BeanDefinitionException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code BeanDefinitionException} with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception.
     * @param cause   the cause of the exception, or {@code null} if the cause is not specified.
     */
    public BeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code BeanDefinitionException} with the specified cause.
     *
     * @param cause the cause of the exception, or {@code null} if the cause is not specified.
     */
    public BeanDefinitionException(Throwable cause) {
        super(cause);
    }
}
