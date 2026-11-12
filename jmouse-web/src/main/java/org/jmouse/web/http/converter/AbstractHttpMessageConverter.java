package org.jmouse.web.http.converter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.core.MediaType;
import org.jmouse.web.context.WebBeanContext;

/**
 * 📦 Base implementation of {@link HttpMessageConverter}.
 *
 * <p>Handles supported media types and delegates
 * read/write operations to concrete implementations.</p>
 *
 * @param <T> the target type for conversion
 * @author Ivan
 */
public abstract class AbstractHttpMessageConverter<T> implements
    HttpMessageConverter<T>,
    InitializingBeanSupport<WebBeanContext> {

    private final List<MediaType> supportedMediaTypes;
    private Charset defaultCharset;

    /**
     * ⚙️ Create a converter with a single supported media type.
     * @param supportedMediaType the media type
     */
    protected AbstractHttpMessageConverter(MediaType... supportedMediaType) {
        this.supportedMediaTypes = List.of(supportedMediaType);
    }

    /**
     * ⚙️ Create a converter with multiple supported media types.
     * @param supportedMediaTypes list of supported media types
     */
    protected AbstractHttpMessageConverter(List<MediaType> supportedMediaTypes) {
        this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
    }

    /**
     * 📜 Get supported media types.
     */
    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return supportedMediaTypes;
    }

    /**
     * ✅ Check if type is writable for the given media type.
     */
    @Override
    public boolean isWritable(Class<?> clazz, MediaType mediaType) {
        return isApplicable(clazz, mediaType);
    }

    /**
     * ✅ Check if type is readable for the given media type.
     */
    @Override
    public boolean isReadable(Class<?> clazz, MediaType mediaType) {
        return isApplicable(clazz, mediaType);
    }

    /**
     * ✅ Check if type is readable or writable for the given media type and class type.
     */
    private boolean isApplicable(Class<?> clazz, MediaType mediaType) {
        if (clazz != null && !supportsType(clazz)) {
            return false;
        }

        if (mediaType == null) {
            return true;
        }

        for (MediaType supportedMediaType : supportedMediaTypes) {
            if (supportedMediaType.includes(mediaType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * ✍️ Write the given object to the output message.
     */
    @Override
    public void write(T body, Class<?> type, HttpOutputMessage outputMessage)
        throws IOException, UnwritableException {
        doWrite(body, type, outputMessage);
    }

    /**
     * 📥 Read an object of the given type from the input message.
     */
    @Override
    public T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
        throws IOException, UnreadableException {
        return doRead(clazz, inputMessage);
    }

    /**
     * 🔤 Default character set used when the selected {@link MediaType} has no charset.
     *
     * <p>Subclasses may override to supply a framework-wide default, such as UTF-8.</p>
     *
     * @return the default {@link Charset}, or {@code null} if none
     */
    @Override
    public Charset getDefaultCharset() {
        return defaultCharset;
    }

    /**
     * 🛠 Write the given data to the output.
     * @param data the object to write
     * @param type the declared type
     * @param outputMessage target message
     */
    protected abstract void doWrite(T data, Class<?> type, HttpOutputMessage outputMessage)
        throws IOException;

    /**
     * 🛠 Read the body from the input.
     * @param clazz target class
     * @param inputMessage source message
     */
    protected abstract T doRead(Class<? extends T> clazz, HttpInputMessage inputMessage) throws IOException;

    /** 🔍 Whether this converter supports the given class. */
    protected abstract boolean supportsType(Class<?> clazz);
}
