package org.jmouse.ai.jpa;

import java.time.LocalDateTime;

/**
 * One counted key, read back.
 *
 * <p>A separate type from the entity so that a management screen reads a value rather than a managed
 * instance it could accidentally write through — the counter is the only thing that writes this table,
 * and keeping it that way is cheaper as a type than as a rule.
 *
 * @param outcome a verdict name, {@code FAILED}, or a refusal reason — the axis that says what is wrong
 */
public record CallCount(
        String        callerId,
        String        toolName,
        String        actionName,
        String        outcome,
        long          callCount,
        LocalDateTime lastCalledAt
) {

    /** How the action reads in a log line or on a screen. */
    public String qualifiedName() {
        return toolName + "." + actionName;
    }
}
