package org.jmouse.security.core;

import java.util.Map;
import java.util.Optional;

/**
 * 📦 Execution envelope that carries a SecurityContext and transport-specific natives.
 *
 * <p>Uniform entry point for authentication/authorization across HTTP, reactive, method AOP, messaging, etc.</p>
 */
public interface SecurityEnvelope {

    /**
     * Current security context (mutable).
     */
    SecurityContext getSecurityContext();

    void setSecurityContext(SecurityContext context);

    /**
     * Free-form bag for cross-cutting data (request-scoped).
     */
    Map<String, Object> getAttributes();

    /**
     * Access the underlying native object(s), e.g. HttpServletRequest, MethodInvocation, ...
     *
     * @param type native class to unwrap
     * @param <T>  type parameter
     * @return the native object if present
     */
    <T> Optional<T> getNative(Class<T> type);
}