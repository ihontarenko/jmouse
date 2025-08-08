package org.jmouse.mvc.converter;

import org.jmouse.core.MediaType;

import java.io.IOException;
import java.util.List;

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
    boolean canWrite(Class<?> clazz, MediaType mediaType);

    /**
     * ✅ Checks if the converter can read the given class with the specified media type.
     *
     * @param clazz     target class
     * @param mediaType desired {@link MediaType}
     * @return {@code true} if readable
     */
    boolean canRead(Class<?> clazz, MediaType mediaType);

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
}
