package org.jmouse.security.web.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.security.jwt.JwtCodec;

import java.util.Map;

/**
 * 🧩 Jackson-based JsonMapper adapter.
 */
public final class JacksonJsonMapper implements JwtCodec.AdapterJson {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final ObjectMapper jacksonObjectMapper;

    public JacksonJsonMapper(ObjectMapper jacksonObjectMapper) {
        this.jacksonObjectMapper = jacksonObjectMapper;
    }

    @Override
    public Map<String, Object> readObject(String json) {
        try {
            return jacksonObjectMapper.readValue(json, MAP);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String writeObject(Map<String, Object> object) {
        try {
            return jacksonObjectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}