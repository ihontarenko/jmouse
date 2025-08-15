package org.jmouse.web.negotiation;

import org.jmouse.core.MediaType;

import java.util.HashMap;
import java.util.Map;

abstract public class MapppedMediaTypeLookup implements MediaTypeLookup {

    private final Map<String, MediaType> mapping = new HashMap<>();

    public MapppedMediaTypeLookup() {

    }

    public void addExtension(String extension, MediaType mediaType) {
        mapping.put(extension, mediaType);
    }

    public MediaType getExtension(String extension) {
        return mapping.get(extension);
    }

    public boolean hasExtension(String extension) {
        return mapping.containsKey(extension);
    }

}
