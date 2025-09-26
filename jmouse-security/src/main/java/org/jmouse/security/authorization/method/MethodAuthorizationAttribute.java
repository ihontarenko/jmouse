package org.jmouse.security.authorization.method;

import org.jmouse.security.core.access.annotation.Authorize;

/**
 * 📑 Resolved authorization attribute for a method/class.
 */
public interface MethodAuthorizationAttribute {

    Kind kind();

    String expression();

    String[] required();

    enum Kind {
        PRE_AUTHORIZE, POST_AUTHORIZE;

        static Kind ofPhase(Authorize.Phase phase) {
            return switch (phase) {
                case PRE ->
                    PRE_AUTHORIZE;
                case POST ->
                    POST_AUTHORIZE;
            };
        }

    }

    final class Default implements MethodAuthorizationAttribute {

        private final String   expression;
        private final Kind     kind;
        private final String[] required;

        Default(Kind kind, String expression, String[] required) {
            this.kind = kind;
            this.expression = expression;
            this.required = required == null ? new String[0] : required.clone();
        }

        @Override
        public Kind kind() {
            return kind;
        }

        @Override
        public String expression() {
            return expression;
        }

        @Override
        public String[] required() {
            return required.clone();
        }

    }

}