package org.jmouse.core.context.result;

/**
 * 🛠 Mutable execution result context.
 *
 * <p>
 * Allows controlled mutation of execution results,
 * including return value assignment and error collection.
 * </p>
 *
 * <p>
 * Intended for use during execution,
 * typically exposed as read-only {@link ResultContext}
 * after completion.
 * </p>
 */
public interface MutableResultContext extends ResultContext {

    /**
     * 📦 Set execution return value.
     *
     * @param value return value
     */
    void setReturnValue(Object value);

    /**
     * ❌ Add error details.
     *
     * @param errorDetails error descriptor
     */
    void addError(ErrorDetails errorDetails);

    /**
     * ❌ Add error by code and message.
     *
     * @param code    error code
     * @param message error message
     */
    default void addError(String code, String message) {
        addError(new ErrorDetails(code, message));
    }

    /**
     * 🧹 Clear return value and errors.
     */
    void clear();
}
