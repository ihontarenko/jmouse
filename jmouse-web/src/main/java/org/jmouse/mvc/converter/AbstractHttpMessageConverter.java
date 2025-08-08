package org.jmouse.mvc.converter;

import org.jmouse.core.MediaType;

import java.util.Collections;
import java.util.List;

public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T> {

    private final List<MediaType> supportedMediaTypes;

    protected AbstractHttpMessageConverter(MediaType supportedMediaType) {
        this.supportedMediaTypes = Collections.singletonList(supportedMediaType);
    }

    protected AbstractHttpMessageConverter(List<MediaType> supportedMediaTypes) {
        this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return supportedMediaTypes;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        if (!supports(clazz)) {
            return false;
        }

        for (MediaType supportedMediaType : supportedMediaTypes) {
            if (mediaType == null || supportedMediaType.includes(mediaType)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        if (!supports(clazz)) {
            return false;
        }

        for (MediaType supportedMediaType : supportedMediaTypes) {
            if (mediaType == null || supportedMediaType.includes(mediaType)) {
                return true;
            }
        }

        return false;
    }

    protected abstract boolean supports(Class<?> clazz);
}