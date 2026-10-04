package org.jmouse.ai.jpa.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.jmouse.ai.model.AiCapability;

/** Stable names rather than ordinals; an unknown persisted capability fails closed. */
@Converter
public final class AiCapabilitiesConverter implements AttributeConverter<Set<AiCapability>, String> {
    @Override
    public String convertToDatabaseColumn(Set<AiCapability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            throw new IllegalArgumentException("Model capabilities cannot be empty.");
        }
        return capabilities.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    @Override
    public Set<AiCapability> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Stored model capabilities cannot be empty.");
        }
        return Arrays.stream(value.split(",")).map(AiCapability::valueOf).collect(Collectors.toUnmodifiableSet());
    }
}

