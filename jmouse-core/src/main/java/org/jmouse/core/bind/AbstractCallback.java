package org.jmouse.core.bind;

/**
 * Abstract base class for binding callbacks.
 * <p>
 * This class provides a base implementation for {@link BindCallback} with support for delegation
 * to a parent callback. It allows chaining of binding operations where multiple callbacks
 * can be executed in sequence.
 * </p>
 *
 * @author JMouse - Team
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public abstract class AbstractCallback implements BindCallback {

    /** The parent callback to delegate to, if applicable. */
    protected BindCallback parent;

    /**
     * Constructs an {@code AbstractCallback} with a specified parent callback.
     *
     * @param parent the parent {@link BindCallback} to delegate to
     */
    protected AbstractCallback(BindCallback parent) {
        this.parent = parent;
    }

    /**
     * Constructs an {@code AbstractCallback} with a default parent callback (NOOP).
     */
    protected AbstractCallback() {
        this(NOOP);
    }
}
