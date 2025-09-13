package org.jmouse.web.mvc.method.converter;

import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.core.MediaType;
import org.jmouse.core.MimeType;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.request.Headers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 📦 Base implementation of {@link HttpMessageConverter}.
 *
 * <p>Handles supported media types and delegates
 * read/write operations to concrete implementations.</p>
 *
 * @param <T> the target type for conversion
 * @author Ivan
 */
public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T>,
        InitializingBeanSupport<WebBeanContext> {

    private final List<MediaType> supportedMediaTypes;
    private       Charset         defaultCharset;

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

    /** 📜 Get supported media types. */
    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return supportedMediaTypes;
    }

    /** ✅ Check if type is writable for the given media type. */
    @Override
    public boolean isWritable(Class<?> clazz, MediaType mediaType) {
        return isApplicable(clazz, mediaType);
    }

    /** ✅ Check if type is readable for the given media type. */
    @Override
    public boolean isReadable(Class<?> clazz, MediaType mediaType) {
        return isApplicable(clazz, mediaType);
    }

    /** ✅ Check if type is readable or writable for the given media type and class type. */
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
    public void write(T body, Class<?> type, HttpOutputMessage outputMessage) throws IOException, UnwritableException {
        doWrite(body, type, outputMessage);
    }

    /**
     * 📥 Read an object of the given type from the input message.
     */
    @Override
    public T read(Class<? extends T> clazz, HttpInputMessage inputMessage) throws IOException, UnreadableException {
        return doRead(clazz, inputMessage);
    }

    /**
     * 🧾 Populate default headers for the outgoing message if not already present.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Sets {@code Content-Type} using the provided {@code contentType}, or falls back to
     *       {@link #getDefaultContentType(Object)}. If the chosen media type lacks a charset,
     *       attaches {@link #getDefaultCharset()} when available.</li>
     *   <li>Sets {@code Content-Length} if it’s not already specified and
     *       {@link #getContentLength(Object, MediaType)} returns a value.</li>
     * </ul>
     *
     * @param message     target HTTP output message
     * @param writable    object that will be written (may be used to infer headers)
     * @param contentType explicit content type to prefer (may be {@code null})
     */
    public void writeDefaultHeaders(HttpOutputMessage message, T writable, MediaType contentType) {
        Headers   headers   = message.getHeaders();
        MediaType mediaType = headers.getContentType();

        if (mediaType == null) {
            mediaType = (contentType != null) ? contentType : getDefaultContentType(writable);
            if (mediaType != null) {
                if (mediaType.getCharset() == null) {
                    Charset charset = getDefaultCharset();
                    if (charset != null) {
                        mediaType = new MediaType(mediaType, Map.of(MimeType.PARAMETER_NAME_CHARSET, charset.name()));
                    }
                }
                headers.setContentType(mediaType);
            }
        }

        if (headers.getContentLength() <= 0) {
            Long contentLength = getContentLength(writable, headers.getContentType());
            if (contentLength != null) {
                headers.setContentLength(contentLength);
            }
        }
    }

    /**
     * 🎯 Determine a default {@link MediaType} for the given value.
     *
     * <p>By default, returns the first entry from {@link #getSupportedMediaTypes()}
     * if present; otherwise {@code null}.</p>
     *
     * @param writable the object to be written (may be ignored by default)
     * @return a default media type, or {@code null} if none
     */
    public MediaType getDefaultContentType(T writable) {
        MediaType       mediaType  = null;
        List<MediaType> mediaTypes = getSupportedMediaTypes();

        if (mediaTypes != null && !mediaTypes.isEmpty()) {
            mediaType = mediaTypes.getFirst();
        }

        return mediaType;
    }

    /**
     * 📏 Compute the {@code Content-Length} for the given value and media type.
     *
     * <p>Subclasses may override to provide an exact byte length (e.g., for
     * byte arrays or resources). Returning {@code null} indicates that the
     * length is unknown and should be omitted.</p>
     *
     * @param writable    the object that will be written
     * @param contentType the resolved media type (may be {@code null})
     * @return the content length in bytes, or {@code null} if unknown
     */
    public Long getContentLength(T writable, MediaType contentType) {
        return null;
    }

    /**
     * 🔤 Default character set used when the selected {@link MediaType} has no charset.
     *
     * <p>Subclasses may override to supply a framework-wide default, such as UTF-8.</p>
     *
     * @return the default {@link Charset}, or {@code null} if none
     */
    public Charset getDefaultCharset() {
        return defaultCharset;
    }

    /**
     * 🛠 Write the given data to the output.
     * @param data the object to write
     * @param type the declared type
     * @param outputMessage target message
     */
    protected abstract void doWrite(T data, Class<?> type, HttpOutputMessage outputMessage) throws IOException;

    /**
     * 🛠 Read the body from the input.
     * @param clazz target class
     * @param inputMessage source message
     */
    protected abstract T doRead(Class<? extends T> clazz, HttpInputMessage inputMessage) throws IOException;

    /** 🔍 Whether this converter supports the given class. */
    protected abstract boolean supportsType(Class<?> clazz);
}
