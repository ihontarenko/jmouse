package org.jmouse.security.context;

import org.jmouse.security.Subject;

import java.util.Map;

/**
 * 🧠 SecurityContext carries the authenticated Subject and optional details.
 */
public interface SecurityContext {

    static SecurityContext of(Subject subject, Map<Object, Object> details) {
        return new DefaultSecurityContext(
                subject, (details == null) ? Map.of() : Map.copyOf(details));
    }

    Subject subject();

    Map<Object, Object> details();

    record DefaultSecurityContext(Subject subject, Map<Object, Object> details) implements SecurityContext { }
}
