package org.jmouse.security.core;

public class InMemorySecurityContextRepository implements SecurityContextRepository {

    @Override
    public SecurityContext load(SecurityContext context) {
        return null;
    }

    @Override
    public void save(SecurityContext context) {

    }

    @Override
    public void clear(SecurityContext context) {

    }

}
