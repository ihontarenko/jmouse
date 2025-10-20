package org.jmouse.web.mvc.method.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.core.MediaType;
import org.jmouse.web.context.WebBeanContext;

import java.io.IOException;
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

    /**
     * 🔗 Available {@link JacksonObjectMapperResolver} bean.
     */
    protected JacksonObjectMapperResolver objectMapperResolver;

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
     * @return registered {@link JacksonObjectMapperResolver} bean
     */
    public JacksonObjectMapperResolver getObjectMapperResolver() {
        return objectMapperResolver;
    }

    /**
     * Resolver {@link JacksonObjectMapperResolver} that resolves jackson {@link ObjectMapper}.
     *
     * @param objectMapperResolver the resolver to set
     */
    public void setObjectMapperResolver(JacksonObjectMapperResolver objectMapperResolver) {
        this.objectMapperResolver = objectMapperResolver;
    }

    /**
     * Get the {@link ObjectMapper} associated with the given media type.
     *
     * @param mediaType the requested media type
     * @return the configured {@link ObjectMapper}, or {@code null} if not found
     */
    public ObjectMapper getObjectMapper(MediaType mediaType) {
        JacksonObjectMapperRegistration registration = getObjectMapperResolver().getObjectMapperRegistration(mediaType);
        ObjectMapper                    objectMapper = null;

        if (registration != null) {
            objectMapper = registration.objectMapper();
        }

        return objectMapper;
    }

    public ObjectMapper getObjectMapper(HttpMessage httpMessage) throws IOException {
        return getObjectMapper(httpMessage.getHeaders().getContentType());
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
        setObjectMapperResolver(
                WebBeanContext.getLocalBeans(JacksonObjectMapperResolver.class, context).getFirst()
        );
    }
}
