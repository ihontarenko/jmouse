package org.jmouse.core.mapping.virtuals;

import org.jmouse.core.mapping.runtime.MappingContext;

final class NoopVirtualPropertyResolver implements VirtualPropertyResolver {

    @Override
    public boolean supports(String targetPropertyName, MappingContext context) {
        return false;
    }

    @Override
    public VirtualValue resolve(String targetPropertyName, MappingContext context) {
        return null;
    }
}
