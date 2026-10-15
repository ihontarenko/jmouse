package org.jmouse.script.el;

/**
 * A document asked for a facade it was not given, or asked by type. Thrown at run time by {@link Facades}; a
 * runtime that audits a document at load refuses the same name earlier, with a line.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FacadeAccessException extends RuntimeException {

    public FacadeAccessException(String message) {
        super(message);
    }
}
