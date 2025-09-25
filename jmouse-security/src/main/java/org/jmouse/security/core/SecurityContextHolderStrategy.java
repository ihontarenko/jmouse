package org.jmouse.security.core;

public interface SecurityContextHolderStrategy {

    SecurityContext getContext();

    void setContext(SecurityContext context);

    void clearContext();

}
