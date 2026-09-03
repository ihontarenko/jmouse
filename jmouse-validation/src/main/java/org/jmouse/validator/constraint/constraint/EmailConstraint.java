package org.jmouse.validator.constraint.constraint;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

import java.util.regex.Pattern;

/**
 * A value that has to look like an email address. ✉️
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * @Email('message':'That does not look like an email address')
 * @Email('domain':'innoventa.net','message':'Use your work address')
 * }</pre>
 *
 * <h3>Behaviour</h3>
 * <ul>
 *     <li>{@code null} is valid — compose with {@code required} when presence is the question.</li>
 *     <li>{@code domain} restricts the part after the {@code @}, compared without regard to case.</li>
 * </ul>
 *
 * <p>⚠️ <strong>The pattern is deliberately permissive, and that is not laziness.</strong> RFC 5322
 * admits addresses almost nobody believes are addresses, and every stricter regex ever written rejects
 * somebody's real mailbox — a false refusal a person cannot argue with, in the one field that is their
 * only way to make contact. What is checked here is the shape that catches a typo: something, an
 * {@code @}, a domain with a dot in it, no spaces. Whether the mailbox exists is answered by sending to
 * it, which is the only thing that ever answers it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class EmailConstraint implements Constraint {

    private static final Pattern SHAPE = Pattern.compile("[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+");

    private String domain;
    private String message;

    /**
     * Returns a stable constraint code.
     *
     * @return {@code "email"}
     */
    @Override
    public String code() {
        return "email";
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Object[] arguments() {
        return new Object[]{domain};
    }

    @Override
    public ConstraintExecutor<EmailConstraint> executor() {
        return Executor.INSTANCE;
    }

    private static final class Executor implements ConstraintExecutor<EmailConstraint> {

        private static final Executor INSTANCE = new Executor();

        @Override
        public boolean test(Object value, EmailConstraint constraint) {
            if (value == null) {
                return true;
            }

            String address = String.valueOf(value);

            if (!SHAPE.matcher(address).matches()) {
                return false;
            }

            if (constraint.domain == null || constraint.domain.isBlank()) {
                return true;
            }

            return address.substring(address.lastIndexOf('@') + 1)
                    .equalsIgnoreCase(constraint.domain);
        }
    }

    /**
     * @return the domain an address has to be on, or {@code null} when any is accepted
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Sets the domain an address has to be on.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return custom validation message (may be {@code null})
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets custom validation message.
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
