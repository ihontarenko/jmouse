package org.jmouse.scheduler.jdbc;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.jmouse.scheduler.TriggerParameters;

import java.util.Map;

/** Trigger parameters to and from the JSON object a store column holds. */
final class TriggerParametersJson {

    private static final TypeReference<Map<String, Object>> OBJECT = new TypeReference<>() {
    };

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_LONG_FOR_INTS).build();

    private TriggerParametersJson() {
    }

    static String write(TriggerParameters parameters) {
        try {
            return MAPPER.writeValueAsString(parameters.asMap());
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Trigger parameters could not be written as JSON.", exception);
        }
    }

    static TriggerParameters read(String json) {
        try {
            return TriggerParameters.of(MAPPER.readValue(json, OBJECT));
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("A stored trigger's parameters are not a JSON object.", exception);
        }
    }
}
