package org.jmouse.validator.constraint.constraint;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

import java.net.URI;

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
        return new Executor();
    }

    static final class Executor implements ConstraintExecutor<WebLinkConstraint> {

        @Override
        public boolean test(Object value, WebLinkConstraint constraint) {
            if (!(value instanceof String string)) {
                return false;
            }

            URI    uri  = URI.create(string);
            String host = uri.getHost();

            if (host == null) {
                return false;
            }

            return host.endsWith(constraint.getHost());
        }

    }

}
