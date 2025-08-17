package org.jmouse.web.mvc.method.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.core.MediaType;
import org.jmouse.web.context.WebBeanContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 📦 Base {@link AbstractHttpMessageConverter} for JSON processing with Jackson.
 * <p>
 * Provides integration with {@link ObjectMapper} instances registered via
 * {@link JacksonObjectMapperRegistration}, selected by {@link MediaType}.
 *
 * @param <T> the target type handled by this converter
 */
public abstract class AbstractJacksonHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

    /** 🔗 Available {@link JacksonObjectMapperRegistration} beans. */
    protected List<JacksonObjectMapperRegistration> mapperRegistrations;

    /**
     * Create a converter supporting a single media type.
     *
     * @param supportedMediaType the supported {@link MediaType}
     */
    protected AbstractJacksonHttpMessageConverter(MediaType supportedMediaType) {
        super(supportedMediaType);
    }

    /**
     * Create a converter supporting multiple media types.
     *
     * @param supportedMediaTypes the supported media types
     */
    protected AbstractJacksonHttpMessageConverter(List<MediaType> supportedMediaTypes) {
        super(supportedMediaTypes);
    }

    /**
     * @return all registered {@link JacksonObjectMapperRegistration} beans
     */
    public List<JacksonObjectMapperRegistration> getMapperRegistrations() {
        return mapperRegistrations;
    }

    /**
     * Set the list of registered {@link JacksonObjectMapperRegistration} beans.
     *
     * @param mapperRegistrations the registrations to set
     */
    public void setMapperRegistrations(List<JacksonObjectMapperRegistration> mapperRegistrations) {
        this.mapperRegistrations = mapperRegistrations;
    }

    /**
     * Find the {@link JacksonObjectMapperRegistration} for a given media type.
     *
     * @param mediaType the requested media type
     * @return the matching registration, or {@code null} if not found
     */
    public JacksonObjectMapperRegistration getObjectMapperRegistration(MediaType mediaType) {
        JacksonObjectMapperRegistration registration = null;

        for (JacksonObjectMapperRegistration mapperRegistration : getMapperRegistrations()) {
            if (mapperRegistration.isApplicable(mediaType)) {
                registration = mapperRegistration;
            }
        }

        return registration;
    }

    /**
     * Get the {@link ObjectMapper} associated with the given media type.
     *
     * @param mediaType the requested media type
     * @return the configured {@link ObjectMapper}, or {@code null} if not found
     */
    public ObjectMapper getObjectMapper(MediaType mediaType) {
        JacksonObjectMapperRegistration registration = getObjectMapperRegistration(mediaType);
        ObjectMapper                    objectMapper = null;

        if (registration != null) {
            objectMapper = registration.objectMapper();
        }

        return objectMapper;
    }

    /** ✅ Check if type is writable for the given media type. */
    @Override
    public boolean isWritable(Class<?> clazz, MediaType mediaType) {
        boolean isWritable = super.isWritable(clazz, mediaType);

        if (!isWritable) {
            return false;
        }

        ObjectMapper objectMapper = getObjectMapper(mediaType);

        if (objectMapper == null) {
            return false;
        }

        AtomicReference<Throwable> atomicReference = new AtomicReference<>();
        if (!objectMapper.canSerialize(clazz, atomicReference)) {
            return false;
        }

        return isWritable;
    }

    /** Always supports any class type. */
    @Override
    protected boolean supportsType(Class<?> clazz) {
        return true;
    }

    /**
     * Initialize the converter by collecting all available
     * {@link JacksonObjectMapperRegistration} beans from the context.
     *
     * @param context the current {@link WebBeanContext}
     */
    @Override
    public void doInitialize(WebBeanContext context) {
        setMapperRegistrations(
                WebBeanContext.getLocalBeans(JacksonObjectMapperRegistration.class, context)
        );
    }
}
