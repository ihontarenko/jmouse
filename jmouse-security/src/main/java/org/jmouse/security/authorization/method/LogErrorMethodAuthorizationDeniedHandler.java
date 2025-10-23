package org.jmouse.security.authorization.method;

import org.jmouse.security.authorization.AccessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogErrorMethodAuthorizationDeniedHandler implements MethodAuthorizationDeniedHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogErrorMethodAuthorizationDeniedHandler.class);

    @Override
    public Object handleDeniedInvocation(MethodAuthorizationContext invocation, AccessResult decision) {
        LOGGER.error("Access denied! Method: {}", invocation.proxyInvocation().getMethod());
        return null;
    }

}
