package org.jmouse.context.binding.properties;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;
import org.jmouse.core.env.PropertyResolver;

import java.util.Map;
import java.util.Set;

public class PropertyResolverConverter implements GenericConverter<PropertyResolver, Map<String, Object>> {

    @Override
    public Map<String, Object> convert(PropertyResolver source, Class<PropertyResolver> sourceType, Class<Map<String, Object>> targetType) {
        return Map.of();
    }

    @Override
    public Set<ClassPair<? extends PropertyResolver, ? extends Map<String, Object>>> getSupportedTypes() {
        return Set.of();
    }

}
