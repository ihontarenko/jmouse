package org.jmouse.web.mvc.method.converter;

import org.jmouse.core.MediaType;
import org.jmouse.core.MimeType;
import org.jmouse.web.http.Headers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * 🔄 Strategy interface for converting HTTP request/response bodies.
 *
 * <p>Responsible for serializing and deserializing objects to/from HTTP messages
 * based on the provided {@link MediaType}. Implementations handle both reading
 * and writing for specific content types.</p>
 *
 * @param <T> target object type for conversion
 *
 * @author Ivan Hontarenko
 */
public interface HttpMessageConverter<T> {

    /**
     * ✅ Checks if the converter can write the given class with the specified media type.
     *
     * @param clazz     target class
     * @param mediaType desired {@link MediaType}
     * @return {@code true} if writable
     */
    boolean isWritable(Class<?> clazz, MediaType mediaType);

    /**
     * ✅ Checks if the converter can read the given class with the specified media type.
     *
     * @param clazz     target class
     * @param mediaType desired {@link MediaType}
     * @return {@code true} if readable
     */
    boolean isReadable(Class<?> clazz, MediaType mediaType);

    /**
     * ✏️ Writes the given object to the HTTP output message.
     *
     * @param body          object to write
     * @param type          object type
     * @param outputMessage target HTTP output
     * @throws IOException         if I/O error occurs
     * @throws UnwritableException if writing is not possible
     */
    void write(T body, Class<?> type, HttpOutputMessage outputMessage)
            throws IOException, UnwritableException;

    /**
     * 📖 Reads an object from the HTTP input message.
     *
     * @param clazz        target class type
     * @param inputMessage source HTTP input
     * @return converted object
     * @throws IOException        if I/O error occurs
     * @throws UnreadableException if reading is not possible
     */
    T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, UnreadableException;

    /**
     * 📋 Returns the list of supported media types.
     *
     * @return supported {@link MediaType}s
     */
    List<MediaType> getSupportedMediaTypes();

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
    default void writeDefaultHeaders(HttpOutputMessage message, T writable, MediaType contentType) {
        Headers   headers   = message.getHeaders();
        MediaType mediaType = headers.getContentType();

        if (mediaType == null) {
            mediaType = (contentType != null) ? contentType : getDefaultContentType(writable);
        }

        if (mediaType != null) {

            if (mediaType.getCharset() == null) {
                Charset charset = getDefaultCharset();
                if (charset != null) {
                    mediaType = new MediaType(mediaType, Map.of(MimeType.PARAMETER_NAME_CHARSET, charset.name()));
                }
            }

            headers.setContentType(mediaType);
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
    default MediaType getDefaultContentType(T writable) {
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
    default Long getContentLength(T writable, MediaType contentType) {
        return -1L;
    }

    /**
     * 🔤 Default character set used when the selected {@link MediaType} has no charset.
     *
     * <p>Subclasses may override to supply a framework-wide default, such as UTF-8.</p>
     *
     * @return the default {@link Charset}, or {@code null} if none
     */
    Charset getDefaultCharset();

}
