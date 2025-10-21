package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.core.access.Phase;

public record AuthorizedMethodInvocation(MethodInvocation invocation, Phase phase, Object result) {

    public boolean isAfter() {
        return phase().isAfter();
    }

    public boolean isBefore() {
        return phase().isBefore();
    }

}
