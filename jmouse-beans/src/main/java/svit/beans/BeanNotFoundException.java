package svit.beans;

/**
 * An exception indicating error that bean can be found in any contained or bean is unknown at all
 */
public class BeanNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code BeanNotFoundException} with no detail message or cause.
     */
    public BeanNotFoundException() {
        super();
    }

    /**
     * Constructs a new {@code BeanNotFoundException} with the specified detail message.
     *
     * @param message the detail message
     */
    public BeanNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code BeanNotFoundException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public BeanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code BeanContextException} with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public BeanNotFoundException(Throwable cause) {
        super(cause);
    }
}
