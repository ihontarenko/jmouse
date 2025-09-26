package org.jmouse.security.authorization;

import java.util.Map;

/**
 * ✅/⛔ Result of an authorization check (with optional details).
 */
public interface AuthorizationDecision {

    AuthorizationDecision DENY   = of(AuthorizationReasonCode.ACCESS_DENIED);
    AuthorizationDecision PERMIT = of(AuthorizationReasonCode.PERMITTED);

    static AuthorizationDecision of(ReasonCode reason) {
        return of(reason, null, Map.of());
    }

    static AuthorizationDecision of(ReasonCode reason, String message) {
        return of(reason, message, Map.of());
    }

    static AuthorizationDecision of(ReasonCode reason, String message, Map<String, Object> attributes) {
        return new Decision(reason, message, attributes);
    }

    static AuthorizationDecision permit(String message) {
        return of(AuthorizationReasonCode.PERMITTED, message);
    }

    static AuthorizationDecision deny(String message) {
        return of(AuthorizationReasonCode.ACCESS_DENIED, message);
    }

    default boolean isGranted() {
        return getReasonCode().isGranted();
    }

    ReasonCode getReasonCode();

    String getMessage();

    Map<String, Object> getAttributes();

    class Decision implements AuthorizationDecision {

        private final String              message;
        private final ReasonCode          code;
        private final Map<String, Object> attributes;

        public Decision(ReasonCode code, String message) {
            this(code, message, Map.of());
        }

        public Decision(ReasonCode code, String message, Map<String, Object> attributes) {
            this.code = code;
            this.attributes = attributes;
            this.message = message;
        }

        @Override
        public ReasonCode getReasonCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

    }

}