package org.jmouse.security.authorization;

import java.util.Map;

/**
 * ✅/⛔ Result of an authorization check (with optional details).
 */
public interface AuthorizationDecision {

    default boolean isGranted() {
        return getReasonCode().isGranted();
    }

    ReasonCode getReasonCode();

    String getMessage();

    Map<String, Object> getAttributes();

}