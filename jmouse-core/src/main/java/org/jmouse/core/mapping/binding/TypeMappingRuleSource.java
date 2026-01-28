package org.jmouse.core.mapping.binding;

import org.jmouse.core.mapping.MappingContext;

public interface TypeMappingRuleSource {

    /**
     * @return resolved rule or {@code null} if source cannot provide it.
     */
    TypeMappingRule find(Class<?> sourceType, Class<?> targetType, MappingContext context);

}