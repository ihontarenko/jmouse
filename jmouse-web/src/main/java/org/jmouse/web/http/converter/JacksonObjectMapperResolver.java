package org.jmouse.web.http.converter;

import tools.jackson.databind.ObjectMapper;
import java.util.List;
import org.jmouse.core.MediaType;

/**
 * 🎨 Resolves an appropriate {@link ObjectMapper} based on the requested {@link MediaType}.
 * <p>
 * Acts as a simple registry of {@link JacksonObjectMapperRegistration} entries,
 * allowing flexible mapping between content types (e.g., <code>application/json</code>,
 * <code>application/+json</code>, etc.) and configured {@link ObjectMapper} instances.
 * </p>
 *
 * ⚙️ Typical usage:
 * <pre>{@code
 * ObjectMapper mapperProvider = resolver.resolveObjectMapper(MediaType.APPLICATION_JSON);
 * }</pre>
 *
 * @param mapperRegistrations list of registered Jackson mappers with their media type predicates
 */
public record JacksonObjectMapperResolver(List<JacksonObjectMapperRegistration> mapperRegistrations) {
    /**
     * 🔍 Finds a mapperProvider registration applicable for the given {@link MediaType}.
     * If multiple registrations match, the last one in the list takes precedence.
     *
     * @param mediaType the target media type to match
     * @return the matching {@link JacksonObjectMapperRegistration}, or {@code null} if none found
     */
    public JacksonObjectMapperRegistration getObjectMapperRegistration(MediaType mediaType) {
        JacksonObjectMapperRegistration registration = null;

        for (JacksonObjectMapperRegistration mapperRegistration : mapperRegistrations()) {
            if (mapperRegistration.isApplicable(mediaType)) {
                registration = mapperRegistration;
            }
        }

        return registration;
    }

    /**
     * 🧩 Resolves the {@link ObjectMapper} suitable for the specified media type.
     *
     * @param mediaType the content type for which to obtain a mapperProvider
     * @return the resolved {@link ObjectMapper} instance
     * @throws NullPointerException if no registration matches the given media type
     */
    public ObjectMapper resolveObjectMapper(MediaType mediaType) {
        return getObjectMapperRegistration(mediaType).objectMapper();
    }
}
