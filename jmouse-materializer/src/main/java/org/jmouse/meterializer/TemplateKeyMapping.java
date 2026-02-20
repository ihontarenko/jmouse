package org.jmouse.meterializer;

import java.util.EnumMap;
import java.util.Map;

import static org.jmouse.core.Verify.nonNull;

public final class TemplateKeyMapping {

    private final Map<ElementType, String> mapping;

    public TemplateKeyMapping(Map<ElementType, String> mapping) {
        this.mapping = Map.copyOf(nonNull(mapping, "mapping"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String resolve(ElementType type) {
        String key = mapping.get(nonNull(type, "type"));

        if (key == null) {
            throw new IllegalArgumentException("No template-key mapped for type: " + type);
        }

        return key;
    }

    public static final class Builder {

        private final Map<ElementType, String> mapping = new EnumMap<>(ElementType.class);

        public Builder map(ElementType type, String templateKey) {
            mapping.put(nonNull(type, "type"), nonNull(templateKey, "templateKey"));
            return this;
        }

        public TemplateKeyMapping build() {
            return new TemplateKeyMapping(mapping);
        }

    }
}
