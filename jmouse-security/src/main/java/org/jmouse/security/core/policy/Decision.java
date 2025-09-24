package org.jmouse.security.core.policy;

import java.util.Map;

public record Decision(Effect effect, Map<String, Object> obligations, Map<String, Object> advice) {

    public static Decision permit() {
        return new Decision(Effect.PERMIT, Map.of(), Map.of());
    }

    public static Decision deny() {
        return new Decision(Effect.DENY, Map.of(), Map.of());
    }

    public static Decision abstain() {
        return new Decision(Effect.ABSTAIN, Map.of(), Map.of());
    }

}
