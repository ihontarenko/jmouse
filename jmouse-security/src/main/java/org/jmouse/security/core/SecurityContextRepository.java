package org.jmouse.security.core;

public interface SecurityContextRepository {

    SecurityContext load(SecurityContext context);

    void save(SecurityContext context);

    void clear(SecurityContext context);

}
