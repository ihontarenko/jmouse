package org.jmouse.web.mvc.method.converter;

import org.jmouse.core.MediaType;
import org.jmouse.core.convert.Conversion;
import org.jmouse.web.context.WebBeanContext;

import java.io.IOException;

/**
 * 🔄 {@link AbstractHttpMessageConverter} that bridges arbitrary objects
 * through a {@link Conversion} service by converting them to/from {@link String}.
 *
 * <p>Delegates actual string serialization to {@link StringHttpMessageConverter}.</p>
 */
public class ObjectToStringHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    /**
     * 🔧 Conversion service used for type transformations.
     */
    private Conversion conversion;

    /**
     * 📝 Delegate converter for handling String I/O.
     */
    private HttpMessageConverter<String> messageConverter;

    /**
     * 🛠️ Register supported media type: {@code text/plain}.
     */
    protected ObjectToStringHttpMessageConverter() {
        super(MediaType.TEXT_PLAIN);
    }

    /**
     * ✍️ Convert object to {@link String} and delegate to string converter.
     *
     * @param data    object to write
     * @param type    declared type
     * @param message target HTTP output
     * @throws IOException if conversion or writing fails
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void doWrite(Object data, Class<?> type, HttpOutputMessage message) throws IOException {
        Class<Object> sourceType = (Class<Object>) type;
        String converted = conversion.convert(data, sourceType, String.class);
        messageConverter.write(converted, type, message);
    }

    /**
     * 📥 Read string via delegate converter and convert to target type.
     *
     * @param clazz   target type
     * @param message source HTTP input
     * @return converted object
     * @throws IOException if reading or conversion fails
     */
    @Override
    protected Object doRead(Class<?> clazz, HttpInputMessage message) throws IOException {
        return conversion.convert(messageConverter.read(String.class, message), clazz);
    }

    /**
     * 🔍 Supports all object types (conversion decides final capability).
     *
     * @param clazz target class
     * @return always {@code true}
     */
    @Override
    protected boolean supportsType(Class<?> clazz) {
        return true;
    }

    /**
     * ✅ Check if writable: requires object → string converter.
     *
     * @param clazz     target type
     * @param mediaType content type
     * @return {@code true} if conversion is available
     */
    @Override
    public boolean isWritable(Class<?> clazz, MediaType mediaType) {
        return super.isWritable(clazz, mediaType) && conversion.hasConverter(clazz, String.class);
    }

    /**
     * ✅ Check if readable: requires string → object converter.
     *
     * @param clazz     target type
     * @param mediaType content type
     * @return {@code true} if conversion is available
     */
    @Override
    public boolean isReadable(Class<?> clazz, MediaType mediaType) {
        return super.isReadable(clazz, mediaType) && conversion.hasConverter(String.class, clazz);
    }

    /**
     * ⚙️ Initialize dependencies from {@link WebBeanContext}.
     *
     * <ul>
     *   <li>Resolves {@link Conversion} service.</li>
     *   <li>Resolves {@link StringHttpMessageConverter} delegate.</li>
     * </ul>
     *
     * @param context current web bean context
     */
    @Override
    public void doInitialize(WebBeanContext context) {
        conversion = context.getBean(Conversion.class);
        messageConverter = context.getBean(StringHttpMessageConverter.class);
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
    @Override
    public MediaType getDefaultContentType(Object writable) {
        return MediaType.TEXT_PLAIN;
    }
}
