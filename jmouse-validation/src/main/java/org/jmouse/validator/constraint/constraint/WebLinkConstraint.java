package org.jmouse.validator.constraint.constraint;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A value that has to be a web address, optionally on one host. 🔗
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * @WebLink('message':'Must be a link')
 * @WebLink('host':'mouser.com','message':'A Mouser link')
 * }</pre>
 *
 * <h3>Behaviour</h3>
 * <ul>
 *     <li>{@code null} is valid — compose with {@code required} when presence is the question.</li>
 *     <li>An unparseable address fails; it does not throw.</li>
 *     <li>{@code host} restricts to that host or a subdomain of it, compared without regard to case.</li>
 * </ul>
 *
 * <p>⚠️ <strong>Four things here were wrong until 2026-08-27, and every one of them was invisible from
 * a document that read correctly:</strong></p>
 *
 * <ul>
 *   <li><strong>{@code null} was refused</strong>, alone among the constraints — so a field written
 *       {@code optional, url(…)} could never be left empty, which is the opposite of what it says.</li>
 *   <li><strong>{@code host} was dereferenced whether or not it was set</strong>, so the documented
 *       "omit to accept any" threw a {@link NullPointerException} out of validation.</li>
 *   <li><strong>{@code endsWith} let {@code evilmouser.com} pass a {@code mouser.com} restriction.</strong>
 *       A host check that can be defeated by registering a longer name is worse than none, because
 *       somebody is relying on it.</li>
 *   <li><strong>{@code URI.create} threw on a malformed address</strong> rather than failing it — so a
 *       person typing a bad link got a stack trace where a message belonged.</li>
 * </ul>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class WebLinkConstraint implements Constraint {

    private String message;
    private String host;

    @Override
    public String code() {
        return "web_link";
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Object[] arguments() {
        return new Object[]{host};
    }

    /** @return the host an address has to be on, or {@code null} when any is accepted */
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return custom validation message (may be {@code null})
     */
    public String getMessage() {
        return message();
    }

    /**
     * Sets custom validation message.
     *
     * @param message message override
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public ConstraintExecutor<? extends Constraint> executor() {
        return Executor.INSTANCE;
    }

    static final class Executor implements ConstraintExecutor<WebLinkConstraint> {

        private static final Executor INSTANCE = new Executor();

        @Override
        public boolean test(Object value, WebLinkConstraint constraint) {
            if (value == null) {
                return true;
            }

            String host = hostOf(String.valueOf(value));

            if (host == null) {
                return false;
            }

            return constraint.host == null || constraint.host.isBlank() || onHost(host, constraint.host);
        }

        /**
         * The host of an address, or {@code null} when it has none or is not an address at all.
         *
         * @param address the value as text
         * @return the host, lower-cased, or {@code null}
         */
        private static String hostOf(String address) {
            try {
                String host = new URI(address).getHost();

                return host == null ? null : host.toLowerCase();
            } catch (URISyntaxException malformed) {
                return null;
            }
        }

        /**
         * Whether an address's host is the required one, or a subdomain of it.
         *
         * <p>⚠️ The dot is what makes this a host check rather than a suffix check. {@code endsWith}
         * alone accepts {@code evilmouser.com} for {@code mouser.com}, which is a restriction anybody
         * can defeat by registering a longer name — and somebody is relying on it.</p>
         *
         * @param host     the address's host
         * @param required the host it has to be on
         * @return whether it is
         */
        private static boolean onHost(String host, String required) {
            String wanted = required.toLowerCase();

            return host.equals(wanted) || host.endsWith("." + wanted);
        }
    }
}
