package org.jmouse.security.authorization;

import java.util.Map;

public interface AccessResult {

    AccessResult DENY   = new Deny("ACCESS DENY!", Map.of());
    AccessResult PERMIT = new Permitted("ACCESS PERMITTED!", Map.of());

    boolean isGranted();

    String getMessage();

    Map<String, Object> getAttributes();

    abstract class AbstractAccessResult implements AccessResult {

        protected final String              message;
        protected final Map<String, Object> attributes;
        protected final boolean             granted;

        public AbstractAccessResult(boolean granted, String message, Map<String, Object> attributes) {
            this.attributes = attributes;
            this.message = message;
            this.granted = granted;
        }

        @Override
        public boolean isGranted() {
            return granted;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public String toString() {
            return "ACCESS_RESULT[%s]['%s']".formatted(isGranted() ? "PERMITTED" : "DENY", message);
        }
    }

    class Permitted extends AbstractAccessResult {

        public Permitted(String message, Map<String, Object> attributes) {
            super(true, message, attributes);
        }

        public Permitted(String message) {
            this(message, Map.of());
        }

        @Override
        public String toString() {
            return super.toString();
        }

    }

    class Deny extends AbstractAccessResult {

        public Deny(String message, Map<String, Object> attributes) {
            super(false, message, attributes);
        }

        public Deny(String message) {
            this(message, Map.of());
        }

        @Override
        public String toString() {
            return super.toString();
        }

    }

}