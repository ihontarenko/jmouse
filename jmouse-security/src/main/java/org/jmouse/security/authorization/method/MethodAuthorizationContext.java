package org.jmouse.security.authorization.method;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.access.Phase;

public record MethodAuthorizationContext(MethodInvocation proxyInvocation, Phase phase, Object result) {

    public boolean isAfter() {
        return phase().isAfter();
    }

    public boolean isBefore() {
        return phase().isBefore();
    }

}
