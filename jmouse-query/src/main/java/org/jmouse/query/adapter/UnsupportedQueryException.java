package org.jmouse.query.adapter;

/**
 * A query a backend cannot honour.
 *
 * <p>⚠️ <strong>Refusing is the contract; ignoring is the bug.</strong> A backend that quietly dropped a
 * clause would return rows satisfying part of what was asked, and nothing anywhere would say so —
 * which is precisely the class of failure this cluster was built to prevent.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class UnsupportedQueryException extends RuntimeException {

    public UnsupportedQueryException(String message) {
        super(message);
    }
}
