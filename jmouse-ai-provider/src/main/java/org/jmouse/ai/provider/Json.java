package org.jmouse.ai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

/**
 * The one place this module turns objects into bytes and back.
 *
 * <p>Package-private, and a static mapper rather than an injected one. A caller has no decision to make
 * here — the shape on the wire is a provider's, not a product's, and a product that configured this
 * mapper would be configuring how somebody else's API is read. One mapper is also the cheapest correct
 * answer: {@link ObjectMapper} is expensive to build, thread-safe once built, and this one is never
 * reconfigured.
 *
 * <p>Unknown properties are ignored deliberately. Providers add fields, and a client that refused a
 * response because it grew a field would break on a day nobody deployed anything.
 */
final class Json {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private Json() {
    }

    static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception unwritable) {
            throw new ProviderException(
                    "The request could not be written as JSON: " + unwritable.getMessage(), unwritable);
        }
    }

    static Map<String, Object> readMap(String body, String providerName) {
        try {
            return MAPPER.readValue(body, MAP);
        } catch (Exception unreadable) {
            throw new ProviderException(providerName + " answered with something that is not JSON: "
                                      + truncate(body), unreadable);
        }
    }

    /**
     * The same, for a body that may well not be JSON at all.
     *
     * <p>Used on an error response, where an HTML page from a proxy is a perfectly ordinary thing to
     * receive and failing to read it must not replace the failure being reported.
     */
    static Optional<Map<String, Object>> tryReadMap(String body) {
        try {
            return Optional.of(MAPPER.readValue(body, MAP));
        } catch (Exception notJson) {
            return Optional.empty();
        }
    }

    /** Long enough to identify what came back, short enough to put in a toast. */
    static String truncate(String body) {
        if (body == null || body.isBlank()) {
            return "an empty body";
        }

        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }
}
