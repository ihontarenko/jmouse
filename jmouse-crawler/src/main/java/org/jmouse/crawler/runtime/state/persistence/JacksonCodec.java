package org.jmouse.crawler.runtime.state.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.core.Verify;

public final class JacksonCodec implements Codec {

    private final ObjectMapper mapper;

    public JacksonCodec(ObjectMapper mapper) {
        this.mapper = Verify.nonNull(mapper, "mapperProvider");
    }

    @Override
    public <T> String encode(T value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode value as JSON", e);
        }
    }

    @Override
    public <T> T decode(String encoded, Class<T> type) {
        try {
            return mapper.readValue(encoded, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode JSON into " + type.getName(), e);
        }
    }
}
