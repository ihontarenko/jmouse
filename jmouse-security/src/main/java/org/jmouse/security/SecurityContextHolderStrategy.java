package org.jmouse.security;

public interface SecurityContextHolderStrategy {

    SecurityContext getContext();

    void setContext(SecurityContext context);

    void clearContext();

    SecurityContext newContext();

}
