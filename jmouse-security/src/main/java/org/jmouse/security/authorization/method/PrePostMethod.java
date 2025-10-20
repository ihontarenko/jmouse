package org.jmouse.security.authorization.method;

import org.jmouse.security.core.access.Phase;

import java.lang.reflect.Method;

public record PrePostMethod(Phase phase, Method method, Object target) {
}
