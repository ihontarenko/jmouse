package org.jmouse.web.mvc.negotiation;

import org.jmouse.core.MediaType;

import java.util.HashMap;
import java.util.Map;

abstract public class MappingMediaTypeLookupProperties {

    private Map<String, MediaType> mediaTypes = new HashMap<>();
    private String                 keyName;

    public Map<String, MediaType> getMediaTypes() {
        return mediaTypes;
    }

    public void setMediaTypes(Map<String, MediaType> mediaTypes) {
        this.mediaTypes = mediaTypes;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

}
