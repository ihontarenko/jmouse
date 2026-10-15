package org.jmouse.parallax.store.support;

import org.jmouse.parallax.problem.ParallaxProblem;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * The typed documents Parallax stores in text columns (definitions, scene documents, settings, view states).
 * Reading one that does not parse is a stored-data defect, not a client error, and says so.
 */
public class ParallaxJson {

    private final ObjectMapper mapper;

    public ParallaxJson(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("A document could not be written: " + exception.getOriginalMessage(), exception);
        }
    }

    public <T> T read(String text, Class<T> type) {
        try {
            return mapper.readValue(text, type);
        } catch (JacksonException exception) {
            throw new IllegalStateException("A stored " + type.getSimpleName() + " does not parse: "
                    + exception.getOriginalMessage(), exception);
        }
    }

    public <T> T read(String text, TypeReference<T> type) {
        try {
            return mapper.readValue(text, type);
        } catch (JacksonException exception) {
            throw new IllegalStateException("A stored document does not parse: " + exception.getOriginalMessage(), exception);
        }
    }

    /** Client input: a document that does not parse is the caller's mistake. */
    public <T> T parseInput(String text, Class<T> type) {
        try {
            return mapper.readValue(text, type);
        } catch (JacksonException exception) {
            throw ParallaxProblem.invalid("invalid-document", "The document is not valid: " + exception.getOriginalMessage());
        }
    }

    public <T> T convert(Object value, Class<T> type) {
        try {
            return mapper.convertValue(value, type);
        } catch (IllegalArgumentException | JacksonException exception) {
            throw ParallaxProblem.invalid("invalid-document", "The document is not valid: " + exception.getMessage());
        }
    }

    public ObjectMapper mapper() {
        return mapper;
    }
}
