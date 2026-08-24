package org.jmouse.query.compose;

/**
 * A row a query cannot be composed from.
 *
 * <p>⚠️ Separate from {@code QueryCheckException} on purpose. A check refusal says <em>this query does
 * not make sense against this schema</em> and is somebody's query being wrong. This says <em>this is not
 * a row</em> — an operator nobody offers, a value where none belongs — and is a caller being wrong. The
 * two deserve different answers, and a screen shows only the first.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ComposeException extends RuntimeException {

    public ComposeException(String message) {
        super(message);
    }
}
