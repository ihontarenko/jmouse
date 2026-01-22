package org.jmouse.core.mapping.virtuals;

import org.jmouse.core.mapping.runtime.MappingContext;

/**
 * Provides values for target properties when source data does not contain them.
 */
public interface VirtualPropertyResolver {

    boolean supports(String targetPropertyName, MappingContext context);

    VirtualValue resolve(String targetPropertyName, MappingContext context);

    static VirtualPropertyResolver noop() {
        return new NoopVirtualPropertyResolver();
    }
}
