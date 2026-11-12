package org.jmouse.web.http.converter;

import tools.jackson.databind.ObjectMapper;
import java.util.Set;
import org.jmouse.core.MediaType;

public record JacksonObjectMapperRegistration(ObjectMapper objectMapper, Set<MediaType> mediaTypes) {
    public boolean isApplicable(MediaType mediaType) {
        return mediaType != null && mediaTypes.stream().anyMatch(mediaType::includes);
    }
}
