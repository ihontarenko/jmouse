package org.jmouse.security;

import java.security.Principal;
import java.util.Map;

public interface Decision {

    static Decision permit(Principal principal) {
        return new Default(Effect.PERMIT, principal, null, null, Map.of());
    }

    static Decision deny(String code, String message) {
        return new Default(Effect.DENY, null, code, message, Map.of());
    }

    static Decision defer(String message) {
        return new Default(Effect.DEFER, null, null, message, Map.of());
    }

    static Decision challenge(String scheme, String realm, String message) {
        return new Default(Effect.CHALLENGE, null, "challenge", message, Map.of("scheme", scheme, "realm", realm));
    }

    static Decision custom(Effect effect, String code, String message, Map<String, Object> meta) {
        return new Default(effect, null, code, message, meta);
    }

    Effect effect();

    /**
     * Principal for PERMIT (optional, may be null).
     */
    Principal principal();

    /**
     * Machine-friendly reason code: "unauthorized", "forbidden", "policy_violation", ...
     */
    String reasonCode();

    /**
     * Human-friendly message (optional).
     */
    String message();

    /**
     * Arbitrary meta (scheme/realm/location/claims/anything).
     */
    Map<String, Object> attributes();

    static boolean isTerminal(Decision decision) {
        return switch (decision.effect()) {
            case DENY, CHALLENGE, REDIRECT -> true;
            case PERMIT, DEFER -> false;
        };
    }

    enum Effect {PERMIT, DENY, CHALLENGE, REDIRECT, DEFER}

    class Default implements Decision {

        private final Effect              effect;
        private final Principal           principal;
        private final String              reasonCode;
        private final String              message;
        private final Map<String, Object> attributes;

        private Default(Effect effect, Principal principal, String reasonCode, String message, Map<String, Object> attributes) {
            this.effect = effect;
            this.principal = principal;
            this.reasonCode = reasonCode;
            this.message = message;
            this.attributes = (attributes == null) ? Map.of() : Map.copyOf(attributes);
        }

        @Override
        public Effect effect() {
            return effect;
        }

        @Override
        public Principal principal() {
            return principal;
        }

        @Override
        public String reasonCode() {
            return reasonCode;
        }

        @Override
        public String message() {
            return message;
        }

        @Override
        public Map<String, Object> attributes() {
            return attributes;
        }

        public boolean isPermit() {
            return effect == Effect.PERMIT;
        }

    }
}
