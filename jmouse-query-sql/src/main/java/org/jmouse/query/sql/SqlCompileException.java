package org.jmouse.query.sql;

/**
 * A query that parsed and checked, and that this backend still cannot express.
 *
 * <p>⚠️ <strong>Refusing is the contract; ignoring is the bug.</strong> A backend that quietly dropped a
 * clause it could not honour would return rows that satisfy part of what was asked and nothing would say
 * so — which is the failure this whole design exists to prevent. If a compiler cannot translate
 * something, it says which thing and stops.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SqlCompileException extends RuntimeException {

    public SqlCompileException(String message) {
        super(message);
    }
}
