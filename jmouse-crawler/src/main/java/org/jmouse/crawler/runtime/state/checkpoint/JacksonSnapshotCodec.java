package org.jmouse.crawler.runtime.state.checkpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jmouse.core.Verify;

public final class JacksonSnapshotCodec implements SnapshotCodec {

    private final ObjectMapper mapper;

    public JacksonSnapshotCodec(ObjectMapper mapper) {
        this.mapper = Verify.nonNull(mapper, "mapper").copy()
                .registerModule(new JavaTimeModule());
    }

    @Override
    public <T> String encode(T value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode snapshot", e);
        }
    }

    @Override
    public <T> T decode(String raw, Class<T> type) {
        try {
            return mapper.readValue(raw, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode snapshot into " + type.getName(), e);
        }
    }
}
