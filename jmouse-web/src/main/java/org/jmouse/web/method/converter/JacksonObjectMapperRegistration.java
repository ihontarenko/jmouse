package org.jmouse.web.method.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.core.MediaType;

import java.util.Set;

public record JacksonObjectMapperRegistration(ObjectMapper objectMapper, Set<MediaType> mediaTypes) {

    public boolean isApplicable(MediaType mediaType) {
        return mediaTypes.stream().anyMatch(mediaType::includes);
    }

}
